package com.dz.couple.module.anniversary.util;

import com.ibm.icu.util.ChineseCalendar;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class AnniversaryDateUtil {
    public static final String CALENDAR_SOLAR = "SOLAR";
    public static final String CALENDAR_LUNAR = "LUNAR";

    private AnniversaryDateUtil() {
    }

    public static LocalDate toLocalDate(Date d) {
        if (d == null) {
            return null;
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date toDate(LocalDate d) {
        if (d == null) {
            return null;
        }
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Occurrence solarOccurrence(LocalDate today, Date anniversaryDate) {
        if (today == null || anniversaryDate == null) {
            return null;
        }
        LocalDate base = toLocalDate(anniversaryDate);
        if (base == null) {
            return null;
        }
        MonthDay md = MonthDay.of(base.getMonth(), base.getDayOfMonth());
        LocalDate next = safeAtYear(md, today.getYear());
        if (next.isBefore(today)) {
            next = safeAtYear(md, today.getYear() + 1);
        }
        int daysLeft = (int) ChronoUnit.DAYS.between(today, next);
        return new Occurrence(next, daysLeft);
    }

    private static LocalDate safeAtYear(MonthDay md, int year) {
        try {
            return md.atYear(year);
        } catch (RuntimeException e) {
            if (md.getMonthValue() == 2 && md.getDayOfMonth() == 29) {
                return LocalDate.of(year, 2, 28);
            }
            throw e;
        }
    }

    public static Occurrence lunarOccurrence(LocalDate today, Integer lunarMonth, Integer lunarDay, Integer lunarLeap) {
        if (today == null || lunarMonth == null || lunarDay == null) {
            return null;
        }
        int m = lunarMonth;
        int d = lunarDay;
        boolean leap = lunarLeap != null && lunarLeap == 1;
        if (m < 1 || m > 12 || d < 1 || d > 30) {
            return null;
        }

        int chineseYear = chineseYearOf(today);
        LocalDate next = lunarToSolar(chineseYear, m, d, leap);
        if (next == null || next.isBefore(today)) {
            next = lunarToSolar(chineseYear + 1, m, d, leap);
        }
        if (next == null) {
            return null;
        }
        int daysLeft = (int) ChronoUnit.DAYS.between(today, next);
        return new Occurrence(next, daysLeft);
    }

    private static int chineseYearOf(LocalDate date) {
        ChineseCalendar c = new ChineseCalendar();
        c.setTime(toDate(date));
        return c.get(ChineseCalendar.EXTENDED_YEAR);
    }

    private static LocalDate lunarToSolar(int chineseYear, int lunarMonth, int lunarDay, boolean leap) {
        try {
            ChineseCalendar c = new ChineseCalendar();
            c.clear();
            c.set(ChineseCalendar.EXTENDED_YEAR, chineseYear);
            c.set(ChineseCalendar.MONTH, lunarMonth - 1);
            c.set(ChineseCalendar.DATE, lunarDay);
            c.set(ChineseCalendar.IS_LEAP_MONTH, leap ? 1 : 0);
            return toLocalDate(c.getTime());
        } catch (RuntimeException e) {
            if (leap) {
                try {
                    ChineseCalendar c = new ChineseCalendar();
                    c.clear();
                    c.set(ChineseCalendar.EXTENDED_YEAR, chineseYear);
                    c.set(ChineseCalendar.MONTH, lunarMonth - 1);
                    c.set(ChineseCalendar.DATE, lunarDay);
                    c.set(ChineseCalendar.IS_LEAP_MONTH, 0);
                    return toLocalDate(c.getTime());
                } catch (RuntimeException ignore) {
                    return null;
                }
            }
            return null;
        }
    }

    public static class Occurrence {
        private final LocalDate nextDate;
        private final int daysLeft;

        public Occurrence(LocalDate nextDate, int daysLeft) {
            this.nextDate = nextDate;
            this.daysLeft = daysLeft;
        }

        public LocalDate getNextDate() {
            return nextDate;
        }

        public int getDaysLeft() {
            return daysLeft;
        }
    }
}

