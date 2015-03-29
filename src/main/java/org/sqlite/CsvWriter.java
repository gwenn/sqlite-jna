package org.sqlite;

import java.io.Closeable;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

// CsvWriter provides an interface for writing CSV data
// (compatible with rfc4180 and extended with the option of having a separator other than ",").
// Successive calls to the write method will automatically insert the separator.
// The endOfRecord method tells when a line break is inserted.
public class CsvWriter implements Closeable, Flushable {
  private final Appendable out;
  // values separator
  private final char sep;
  // specify if values should be double-quoted (when they contain a separator, a double-quote or a newline)
  private final boolean quoted;
  // True to use \r\n as the line terminator
  private boolean useCRLF;
  // true at start of record
  private boolean sor;
  // character marking the start of a line comment.
  private char comment;
  private boolean safe;

  public CsvWriter(Appendable out) {
    this(out, ',', true);
  }

  public CsvWriter(Appendable out, char sep, boolean quoted) {
    if (out == null) {
      throw new IllegalArgumentException("null output");
    }
    this.out = out;
    this.sep = sep;
    this.quoted = quoted;
    sor = true;
  }

  public boolean isUseCRLF() {
    return useCRLF;
  }
  public void setUseCRLF(boolean useCRLF) {
    this.useCRLF = useCRLF;
  }

  // Exports result to CSV.
  public void writeResultSet(ResultSet rs/*TODO, String nullValue*/, boolean headers) throws IOException, SQLException {
    final ResultSetMetaData metaData = rs.getMetaData();
    final int nCol = metaData.getColumnCount();
    if (headers) {
      for (int i = 1; i <= nCol; i++) {
        if (i == 1 && comment != 0) {
          write(comment + metaData.getColumnLabel(i));
          continue;
        }
        write(metaData.getColumnLabel(i));
      }
      endOfRecord();
    }
    while (rs.next()) {
      for (int i = 1; i <= nCol; i++) {
        writeValue(rs.getObject(i));
      }
      endOfRecord();
    }
  }

  // Ensures that values are quoted when needed.
  public void writeRecord(Object... values) throws IOException {
    for (Object value : values) {
      writeValue(value);
    }
    endOfRecord();
  }

  // Ensures that values are quoted when needed.
  public void writeRecord(Iterable<?> values) throws IOException {
    for (Object value : values) {
      writeValue(value);
    }
    endOfRecord();
  }

  // Ensures that values are quoted when needed.
  public void writeRecord(String[] values, int n) throws IOException {
    if (n < 0) {
      return;
    }
    for (int i = 0; i < n; i++) {
      write(values[i]);
    }
    endOfRecord();
  }

  public void writeComment(String... values) throws IOException {
    boolean first = true;
    for (String value : values) {
      if (first && comment != 0) {
        first = false;
        write(comment + value);
        continue;
      }
      writeValue(value);
    }
    endOfRecord();
  }

  // Ensures that value is quoted when needed.
  // Value's type is used to encode value to text.
  public void writeValue(Object value) throws IOException {
    if (value == null) {
      write((CharSequence) value);
    } else if (value instanceof CharSequence) {
      write((CharSequence) value);
    } else if (value instanceof Number) {
      write(value.toString());
    } else if (value instanceof Boolean) {
      write(value.toString()); // TODO parameterizable ("true"|"false")
    } else if (value instanceof Character) {
      write(value.toString());
    } else {
      throw new IllegalArgumentException("unsupported type: " + value.getClass());
    }
  }

  // Write ensures that value is quoted when needed.
  public void write(CharSequence value) throws IOException {
    if (!sor) {
      out.append(sep);
    }
    if (value == null || value.length() == 0) {
      sor = false;
      return;
    }
    // In quoted mode, value is enclosed between quotes if it contains sep, quote or \n.
    if (quoted) {
      int last = 0;
      char c;
      for (int i = 0; i < value.length(); i++) {
        c = value.charAt(i);
        if (c != '"' && c != '\r' && c != '\n' && c != sep) {
          continue;
        }
        if (last == 0) {
          out.append('"');
        }
        out.append(value, last, i + 1);
        if (c == '"') {
          out.append(c); // escaped with another double quote
        }
        last = i + 1;
      }
      out.append(value, last, value.length());
      if (last != 0) {
        out.append('"');
      }
    } else if (safe) {
      // check that value does not contain sep or \n
      for (int i = 0; i < value.length(); i++) {
        if (value.charAt(i) == '\n' || value.charAt(i) == sep) {
          throw new IOException("Illegal character in " + value);
        }
      }
      out.append(value);
    } else {
      out.append(value);
    }
    sor = false;
  }

  // endOfRecord tells when a line break must be inserted.
  public void endOfRecord() throws IOException {
    if (useCRLF) {
      out.append('\r');
    }
    out.append('\n');
    sor = true;
  }

  @Override
  public void flush() throws IOException {
    if (out instanceof Flushable) {
      ((Flushable) out).flush();
    }
  }

  @Override
  public void close() throws IOException {
    if (out instanceof Closeable) {
      ((Closeable) out).close();
    }
  }

  public void setComment(char comment) {
    this.comment = comment;
  }

  public void setSafe(boolean safe) {
    this.safe = safe;
  }

  public static void main(String[] args) throws IOException {
    final CsvReader r = new CsvReader(new FileReader(args[0])/*, '\t', false*/);
    r.setTrim(true);
    final CsvWriter w  = new CsvWriter(new FileWriter(args[1])/*, '\t', false*/);
    final String[] values = new String[25];
    for (int n; (n = r.scanRecord(values)) != -1; ) {
      w.writeRecord(values, n);
    }
    w.close();
    r.close();
  }
}
