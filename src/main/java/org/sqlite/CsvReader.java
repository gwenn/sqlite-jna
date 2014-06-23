package org.sqlite;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

// CsvReader provides an interface for reading CSV data
// (compatible with rfc4180 and extended with the option of having a separator other than ",").
// Successive calls to the scan method will step through the 'fields', skipping the separator/newline between the fields.
// The endOfRecord method tells when a field is terminated by a line break.
// Lexing is adapted from csv_read_one_field function in SQLite3 shell sources.
public class CsvReader implements Closeable {
  private static final int MAX_SIZE = 64 * 1024;

  private final Reader reader;
  // values separator
  private final char sep;
  // specify if values may be quoted (when they contain separator or newline)
  private final boolean quoted;
  // true when the most recent field has been terminated by a newline (not a separator).
  private boolean eor;
  // true at end of file
  private boolean eof;
  // current line number (not record number)
  private int lineNumber;
  // trim spaces (only on not-quoted values). Break rfc4180 rule: "Spaces are considered part of a field and should not be ignored."
  private boolean trim;
  // character marking the start of a line comment. When specified, line comment appears as empty line.
  private char comment;

  private int[] buf;
  private int n;
  // true when the current line is empty (or a line comment)
  private boolean empty;

  public CsvReader(Reader reader) {
    this(reader, ',', true);
  }
  public CsvReader(Reader reader, char sep, boolean quoted) {
    if (reader == null) {
      throw new IllegalArgumentException("null input");
    }
    this.reader = reader;
    this.sep = sep;
    this.quoted = quoted;
    lineNumber = 1;
    buf = new int[1024];
    eor = true;
  }

  /*
   * Empty lines (or line comments) are skipped.
   * Extra fields are skipped (when the number of fields is greater than `values` size).
   * Returns the number of fields read.
   */
  public int scanRecord(String[] values) throws IOException {
    int i;
    for (i = 0; i < values.length && !isEndOfFile(); i++) {
      String value = scanText();
      if (i == 0) { // skip empty line (or line comment)
        while (isEmptyLine()) {
          value = scanText();
        }
      }
      values[i] = value;
      if (isEndOfRecord()) {
        return i + 1;
      }
    }
    skip(); // Extra values are skipped.
    return i + 1;
  }

  // read text until next separator or eol/eof
  public String scanText() throws IOException {
    n = 0;
    int c = read();
    if (c == -1) {
      empty = eor;
      return "";
    } else if (quoted && c == '"') { // quoted field (may contains separator, newline and escaped quote)
      empty = false;
      quotedField();
      // quoted-field are not trimmed
      return new String(buf, 0, n); // FIXME
    } else if (eor && comment != 0 && c == comment) { // line comment
      empty = true;
      lineComment();
      return "";
    }
    // non-quoted field
    nonQuotedField(c);
    if (n == 0) {
      return "";
    }
    int offset = 0;
    if (trim) {
      while (Character.isWhitespace(buf[offset]) && offset < n) {
        offset++;
      }
      while (n > offset && Character.isWhitespace(buf[n-1])) {
        n--;
      }
    }
    return new String(buf, offset, n-offset);
  }

  // Scan until the separator or newline following the closing quote (and ignore escaped quote)
  private void quotedField() throws IOException {
    int startLineNumber = lineNumber;
    int c, pc = 0, ppc = 0;
    while (true) {
      c = read();
      if (c == '\n') {
        lineNumber++;
      } else if (c == '"') {
        if (pc == c) { // escaped quote
          pc = 0;
          continue;
        }
      }
      if (pc == '"' && c == sep) {
        n--;
        eor = false;
        break;
      } else if ((pc == '"' && c == '\n') || (pc == '"' && c == -1)) {
        n--;
        eor = true;
        break;
      } else if (ppc == '"' /*&& pc == '\r'*/ && c == '\n') {
        n -= 2;
        eor = true;
        break;
      }
      if (pc == '"' && c != '\r') {
        throw new IOException(String.format("unescaped \" character at line %d", lineNumber));
      } else if (c == -1) {
        throw new IOException(String.format("unterminated \"-quoted field at line %d", startLineNumber));
      }
      append(c);
      ppc = pc;
      pc = c;
    }
  }

  // Scan until separator or newline, marking end of field.
  private void nonQuotedField(int c) throws IOException {
    while (c != -1) {
      if (c == sep) {
        empty = false;
        eor = false;
        break;
      } else if (c == '\n') {
        if (n > 0 && buf[n-1] == '\r') {
          n--;
        }
        empty = eor && n == 0; // FIXME empty & trim
        eor = true;
        lineNumber++;
        break;
      }
      append(c);
      c = read();
    }
  }

  // Scan until newline, marking end of comment.
  private void lineComment() throws IOException {
    int c;
    while ((c = read()) != -1) {
      if (c == '\n') {
        lineNumber++;
        break;
      }
    }
  }

  // read date until next separator or eol/eof
  public Date scanDate(DateFormat dateFormat, int length) throws IOException, IllegalArgumentException {
    return scanDate(dateFormat, length, false);
  }
  // read optional date until next separator or eol/eof
  public Date scanDate(DateFormat dateFormat, int length, boolean nullable) throws IOException, IllegalArgumentException {
    final String text = scanText();
    if (text.length() == length) {
      try {
        return dateFormat.parse(text);
      } catch (ParseException e) {
        throw new IllegalArgumentException("unexpected date format:", e);
      }
    } else if (nullable && text.length() == 0) {
      return null;
    }
    throw new IllegalArgumentException(String.format("unexpected date format: '%s'", text));
  }

  // read double until next separator or eol/eof
  public double scanDouble() throws IOException, IllegalArgumentException {
    return Double.parseDouble(scanText());
  }

  // read float until next separator or eol/eof
  public float scanFloat() throws IOException, IllegalArgumentException {
    return Float.parseFloat(scanText());
  }

  // read int until next separator or eol/eof
  public int scanInt() throws IOException, IllegalArgumentException {
    return Integer.parseInt(scanText());
  }

  // read bool until next separator or eol/eof
  public boolean scanBool(String trueValue) throws IOException {
    return trueValue.equals(scanText());
  }

  // read char until next separator or eol/eof
  public char scanChar() throws IOException, IllegalArgumentException {
    final String text = scanText();
    if (text.length() == 1) {
      return text.charAt(0);
    }
    throw new IllegalArgumentException(String.format("expected character but got '%s'", text));
  }

  public int peek() throws IOException {
    reader.mark(1);
    final int c = read();
    reader.reset();
    return c;
  }

  // skip until eol (eol included)
  public void skip() throws IOException {
    if (isEndOfRecord()) { // TODO
      return;
    }
    while (!eof) {
      int c = read();
      if (c == '\n') {
        lineNumber++;
        break;
      }
    }
    eor = true;
  }

  // Returns current line number (not record number)
  public int getLineNumber() {
    return lineNumber;
  }
  // Returns true when the most recent field has been terminated by a newline (not a separator).
  public boolean isEndOfRecord() {
    return eor || eof;
  }
  // Returns true at end of file/stream
  public boolean isEndOfFile() {
    return eof;
  }
  // Returns true when the current line is empty or a line comment.
  public boolean isEmptyLine() {
    return empty && (eor || eof);
  }
  // Returns the values separator used
  public char getSep() {
    return sep;
  }

  public void setTrim(boolean trim) {
    this.trim = trim;
  }

  public void setComment(char comment) {
    this.comment = comment;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  private void append(int c) throws IOException {
    if (n >= buf.length) {
      int newSize = buf.length * 2;
      if (newSize > MAX_SIZE) {
        throw new IOException("token too long");
      }
      buf = Arrays.copyOf(buf, newSize);
    }
    buf[n++] = c;
  }

  private int read() throws IOException {
    int c = reader.read();
    if (c == -1) {
      eof = true;
    }
    return c;
  }
}
