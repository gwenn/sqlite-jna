package org.sqlite.driver;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DateUtil {
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
  static long fromJulianDay(double jd) {
    jd -= 2440587.5;
    jd *= 86400000.0;
    return (long) jd;
  }

  // 1970-01-01 00:00:00 is JD 2440587.5
  static double toJulianDay(long ms) {
    double adj = (ms < 0) ? 0 : 0.5;
    return (ms + adj) / 86400000.0 + 2440587.5;
  }

  static java.util.Date parseDate(String txt) throws SQLException {
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
    }
    DateFormat df = getDateFormat(layout);
    final java.util.Date date;
    try {
      date = df.parse(txt);
    } catch (ParseException e) {
      throw new SQLException(String.format("Unsupported timestamp format: '%s'", txt), e);
    }
    return date;
  }

  static String formatDate(java.util.Date date, int length) {
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

  static String formatDate(java.util.Date date, String layout) {
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
}
