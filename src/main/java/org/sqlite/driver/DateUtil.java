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
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

final class DateUtil {
	static final int DATE_CONFIG = 0;
	public static final String DATE_FORMAT = "date_format";
	static final int TIME_CONFIG = 1;
	public static final String TIME_FORMAT = "time_format";
	static final int TIMESTAMP_CONFIG = 2;
	public static final String TIMESTAMP_FORMAT = "timestamp_format";
	/**
	 * {@link ChronoField#EPOCH_DAY}
	 */
	public static final String EPOCH_DAY = "epochday";
	public static final String JULIANDAY = "julianday";
	public static final String UNIXEPOCH = "unixepoch";
	public static final String YYYY_MM_DD = "yyyy-MM-dd"; // See java.sql.Date.toString()
	public static final String HH_MM_SS = "HH:mm:ss"; // See java.sql.Time.toString()
	public static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; // ISO-8601

	private static final ThreadLocal<Map<String, DateFormat>> DATE_FORMATS = ThreadLocal.withInitial(HashMap::new);
	private static final Calendar UTC = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));

	private DateUtil() {
	}

	static String[] config(Properties info) {
		if (info == null) {
			return new String[]{YYYY_MM_DD, HH_MM_SS, DEFAULT_FORMAT};
		}
		final String[] config = new String[3];
		config[DATE_CONFIG] = info.getProperty(DATE_FORMAT, YYYY_MM_DD);
		config[TIME_CONFIG] = info.getProperty(TIME_FORMAT, HH_MM_SS);
		config[TIMESTAMP_CONFIG] = info.getProperty(TIMESTAMP_FORMAT, DEFAULT_FORMAT);
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
		double adj = ms < 0L ? 0.0 : 0.5;
		return (ms + adj) / 86400000.0 + 2440587.5;
	}

	private static class ParsedDate {
		private final Date value;
		private final boolean tz;
		private ParsedDate(Date value, boolean tz) {
			this.value = value;
			this.tz = tz;
		}
	}

	private static ParsedDate parseDate(String txt, Calendar cal) throws SQLException {
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
				layout = txt.charAt(10) == 'T' ? "yyyy-MM-dd'T'HH:mm" : "yyyy-MM-dd HH:mm";
				break;
			case 19: // YYYY-MM-DDTHH:MM:SS
				layout = txt.charAt(10) == 'T' ? "yyyy-MM-dd'T'HH:mm:ss" : "yyyy-MM-dd HH:mm:ss";
				break;
			case 23: // YYYY-MM-DDTHH:MM:SS.SSS
				layout = txt.charAt(10) == 'T' ? "yyyy-MM-dd'T'HH:mm:ss.SSS" : "yyyy-MM-dd HH:mm:ss.SSS";
				break;
			default: // YYYY-MM-DDTHH:MM:SS.SSSZhh:mm or parse error
				layout = txt.length() > 10 && txt.charAt(10) == 'T' ? DEFAULT_FORMAT : "yyyy-MM-dd HH:mm:ss.SSSXXX";
				tz = true;
		}
		final DateFormat df = getDateFormat(layout, cal);
		final Date date;
		try {
			date = df.parse(txt);
		} catch (ParseException e) {
			throw new SQLException(String.format("Unsupported timestamp format: '%s'", txt), e);
		}
		return new ParsedDate(date, tz);
	}

	static String formatDate(Date date, int length, Calendar cal) {
		final String layout;
		switch (length) {
			case 5: // HH:MM
				layout = "HH:mm";
				break;
			case 8: // HH:MM:SS
				layout = HH_MM_SS;
				break;
			case 10: // YYYY-MM-DD
				layout = YYYY_MM_DD;
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
		return formatDate(date, layout, cal);
	}

	static String formatDate(Date date, String layout, Calendar cal) {
		return getDateFormat(layout, cal).format(date);
	}

	private static DateFormat getDateFormat(String layout, Calendar cal) {
		DateFormat df = DATE_FORMATS.get().get(layout);
		if (df == null) {
			df = new SimpleDateFormat(layout);
			df.setLenient(false);
			DATE_FORMATS.get().put(layout, df);
		}
		df.setTimeZone(cal == null ? TimeZone.getDefault() : cal.getTimeZone());
		return df;
	}

	static java.sql.Date toDate(String txt, Calendar cal) throws SQLException {
		final ParsedDate date = parseDate(txt, cal);
		return new java.sql.Date(/*normalizeDate(*/date.value.getTime()/*)*/);
	}
	static java.sql.Date toDate(long unixepoch, Calendar cal) {
		return new java.sql.Date(normalizeDate(unixepoch, cal));
	}
	static java.sql.Date toDate(double jd, Calendar cal) {
		return new java.sql.Date(normalizeDate(fromJulianDay(jd), null));
	}

	static Time toTime(String txt, Calendar cal) throws SQLException {
		final ParsedDate date = parseDate(txt, cal);
		return new Time(date.value.getTime());
	}
	static Time toTime(long unixepoch) {
		return new Time(unixepoch);
	}
	static Time toTime(double jd) {
		return new Time(fromJulianDay(jd));
	}

	static Timestamp toTimestamp(String txt, Calendar cal) throws SQLException {
		final ParsedDate date = parseDate(txt, cal);
		return new Timestamp(date.value.getTime());
	}
	static Timestamp toTimestamp(long unixepoch) {
		return new Timestamp(unixepoch);
	}
	static Timestamp toTimestamp(double jd) {
		return new Timestamp(fromJulianDay(jd));
	}

	// must be 'normalized' by setting the hours, minutes, seconds, and milliseconds to zero in the particular time zone with which the instance is associated.
	static long normalizeDate(long unixepoch, Calendar cal) {
		if (cal == null) {
			synchronized (UTC) {
				return normalize(unixepoch, UTC);
			}
		} else {
			return normalize(unixepoch, UTC);
		}
	}
	private static long normalize(long unixepoch, Calendar cal) {
		cal.setTimeInMillis(unixepoch);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
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
