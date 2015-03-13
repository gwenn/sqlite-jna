/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

final class DateUtil {
  public static final String DATE_FORMAT = "date_format";
  public static final String TIME_FORMAT = "time_format";
  public static final String TIMESTAMP_FORMAT = "timestamp_format";
  public static final String JULIANDAY = "julianday";
  public static final String UNIXEPOCH = "unixepoch";
  public static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; // ISO-8601

  private static final ThreadLocal<Map<String, DateFormat>> DATE_FORMATS = new ThreadLocal<Map<String, DateFormat>>() {
    @Override
    protected Map<String, DateFormat> initialValue() {
      return new HashMap<String, DateFormat>();
    }
  };

  private DateUtil() {
  }

  static String[] config(Properties info) {
    if (info == null) {
      return new String[]{DEFAULT_FORMAT, DEFAULT_FORMAT, DEFAULT_FORMAT};
    }
    final String[] config = new String[3];
    config[0] = info.getProperty(DATE_FORMAT, DEFAULT_FORMAT);
    config[1] = info.getProperty(TIME_FORMAT, DEFAULT_FORMAT);
    config[2] = info.getProperty(TIMESTAMP_FORMAT, DEFAULT_FORMAT);
    return config;
  }

  // 1970-01-01 00:00:00 is JD 2440587.5
  private static long fromJulianDay(double jd) {
    jd -= 2440587.5;
    jd *= 86400000.0;
    return (long) jd;
  }

  // 1970-01-01 00:00:00 is JD 2440587.5
  static double toJulianDay(long ms) {
    double adj = (ms < 0L) ? 0.0 : 0.5;
    return (ms + adj) / 86400000.0 + 2440587.5;
  }

  private static class ParsedDate {
    private final Date value;
    private final boolean tz;
    public ParsedDate(Date value, boolean tz) {
      this.value = value;
      this.tz = tz;
    }
  }

  private static ParsedDate parseDate(String txt) throws SQLException {
    boolean tz = false;
    final String layout;
    switch (txt.length()) {
      case 5: // HH:MM
        layout = "HH:mm";
        break;
      case 8: // HH:MM:SS
        layout = "HH:mm:ss";
        break;
      case 10: // YYYY-MM-DD
        layout = "yyyy-MM-dd";
        break;
      case 12: // HH:MM:SS.SSS
        layout = "HH:mm:ss.SSS";
        break;
      case 16: // YYYY-MM-DDTHH:MM
        if (txt.charAt(10) == 'T') {
          layout = "yyyy-MM-dd'T'HH:mm";
        } else {
          layout = "yyyy-MM-dd HH:mm";
        }
        break;
      case 19: // YYYY-MM-DDTHH:MM:SS
        if (txt.charAt(10) == 'T') {
          layout = "yyyy-MM-dd'T'HH:mm:ss";
        } else {
          layout = "yyyy-MM-dd HH:mm:ss";
        }
        break;
      case 23: // YYYY-MM-DDTHH:MM:SS.SSS
        if (txt.charAt(10) == 'T') {
          layout = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        } else {
          layout = "yyyy-MM-dd HH:mm:ss.SSS";
        }
        break;
      default: // YYYY-MM-DDTHH:MM:SS.SSSZhh:mm or parse error
        if (txt.length() > 10 && txt.charAt(10) == 'T') {
          layout = DEFAULT_FORMAT;
        } else {
          layout = "yyyy-MM-dd HH:mm:ss.SSSXXX";
        }
        tz = true;
    }
    final DateFormat df = getDateFormat(layout);
    final Date date;
    try {
      date = df.parse(txt);
    } catch (ParseException e) {
      throw new SQLException(String.format("Unsupported timestamp format: '%s'", txt), e);
    }
    return new ParsedDate(date, tz);
  }

  static String formatDate(Date date, int length) {
    final String layout;
    switch (length) {
      case 5: // HH:MM
        layout = "HH:mm";
        break;
      case 8: // HH:MM:SS
        layout = "HH:mm:ss";
        break;
      case 10: // YYYY-MM-DD
        layout = "yyyy-MM-dd";
        break;
      case 12: // HH:MM:SS.SSS
        layout = "HH:mm:ss.SSS";
        break;
      case 16: // YYYY-MM-DDTHH:MM
        layout = "yyyy-MM-dd'T'HH:mm";
        break;
      case 19: // YYYY-MM-DDTHH:MM:SS
        layout = "yyyy-MM-dd'T'HH:mm:ss";
        break;
      case 23: // YYYY-MM-DDTHH:MM:SS.SSS
        layout = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        break;
      default: // YYYY-MM-DDTHH:MM:SS.SSSZhh:mm or parse error
        layout = DEFAULT_FORMAT;
    }
    return formatDate(date, layout);
  }

  static String formatDate(Date date, String layout) {
    return getDateFormat(layout).format(date);
  }

  private static DateFormat getDateFormat(String layout) {
    DateFormat df = DATE_FORMATS.get().get(layout);
    if (df == null) {
      df = new SimpleDateFormat(layout);
      DATE_FORMATS.get().put(layout, df);
    }
    return df;
  }

  static java.sql.Date toDate(String txt, Calendar cal) throws SQLException {
    final ParsedDate date = parseDate(txt);
    if (date.tz || cal == null) {
      return new java.sql.Date(date.value.getTime());
    }
    cal.setTime(date.value);
    return new java.sql.Date(cal.getTime().getTime());
  }
  static java.sql.Date toDate(long unixepoch, Calendar cal) {
    if (cal == null) {
      return new java.sql.Date(unixepoch);
    }
    cal.setTimeInMillis(unixepoch);
    return new java.sql.Date(cal.getTime().getTime());
  }
  static java.sql.Date toDate(double jd, Calendar cal) {
    if (cal == null) {
      return new java.sql.Date(fromJulianDay(jd));
    }
    cal.setTimeInMillis(fromJulianDay(jd));
    return new java.sql.Date(cal.getTime().getTime());
  }

  static Time toTime(String txt, Calendar cal) throws SQLException {
    final ParsedDate date = parseDate(txt);
    if (date.tz || cal == null) {
      return new Time(date.value.getTime());
    }
    cal.setTime(date.value);
    return new Time(cal.getTime().getTime());
  }
  static Time toTime(long unixepoch, Calendar cal) {
    if (cal == null) {
      return new Time(unixepoch);
    }
    cal.setTimeInMillis(unixepoch);
    return new Time(cal.getTime().getTime());
  }
  static Time toTime(double jd, Calendar cal) {
    if (cal == null) {
      return new Time(fromJulianDay(jd));
    }
    cal.setTimeInMillis(fromJulianDay(jd));
    return new Time(cal.getTime().getTime());
  }

  static Timestamp toTimestamp(String txt, Calendar cal) throws SQLException {
    final ParsedDate date = parseDate(txt);
    if (date.tz || cal == null) {
      return new Timestamp(date.value.getTime());
    }
    cal.setTime(date.value);
    return new Timestamp(cal.getTime().getTime());
  }
  static Timestamp toTimestamp(long unixepoch, Calendar cal) {
    if (cal == null) {
      return new Timestamp(unixepoch);
    }
    cal.setTimeInMillis(unixepoch);
    return new Timestamp(cal.getTime().getTime());
  }
  static Timestamp toTimestamp(double jd, Calendar cal) {
    if (cal == null) {
      return new Timestamp(fromJulianDay(jd));
    }
    cal.setTimeInMillis(fromJulianDay(jd));
    return new Timestamp(cal.getTime().getTime());
  }

  public static void main(String[] args) throws ParseException {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    Date date = df.parse("2014-09-23T15:42:00.000+00:00");
    System.out.println("GMT = " + date.toGMTString());

    df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    date = df.parse("2014-09-23T15:42:00.000");
    System.out.println("GMT = " + date.toGMTString());

    df.setCalendar(Calendar.getInstance());
    date = df.parse("2014-09-23T15:42:00.000");
    System.out.println("GMT = " + date.toGMTString());

    final long l = 1092941466L; // 2004-08-19 18:51:06Z
    System.out.println("new Date(l) = " + new Date(l * 1000)); // Thu Aug 19 20:51:06 CEST 2004

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(l * 1000);
    System.out.println("cal = " + cal.getTime());

    final double jd = 2456924.08783652;
    final Date d = new Date(fromJulianDay(jd));
    System.out.println("d = " + d);
    System.out.println("d.toGMTString() = " + d.toGMTString());
  }
}
