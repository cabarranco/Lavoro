package com.asb.analytics.api.betfair.filters;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * Betfair filter from time range
 *
 * @author Claudio Paolicelli
 */
public class TimeRange {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String from;

    private String to;


    // CONSTRUCTORS

    /**
     * Constructor of TimeRange
     *
     * @param from start string date
     * @param to end string date
     */
    public TimeRange(String from, String to) {
        this.from = from;
        this.to = to == null ? "" : to;
    }

    /**
     * Static constructor that return a TimeRange of 48h from the moment that the method is called.
     *
     * @return {@link TimeRange} instance
     */
    public static TimeRange fiveMinutes() {

        Calendar calendarFrom = Calendar.getInstance();

        // get al matches started 30 minutes before or in the next 2 days
        calendarFrom.add(Calendar.MINUTE, -5);

        String from = format.format(calendarFrom.getTime()).replaceAll(" ", "T") + "Z";

        return new TimeRange(from, null);

    }

    /**
     * Get all the matches
     * @return
     */
    public static TimeRange inPlay() {

        Calendar calendarFrom = Calendar.getInstance();
        Calendar calendarTo = Calendar.getInstance();

        // get al matches started 30 minutes before or in the next 2 days
        calendarFrom.add(Calendar.MINUTE, -120);

        String from = format.format(calendarFrom.getTime()).replaceAll(" ", "T") + "Z";
        String to = format.format(calendarTo.getTime()).replaceAll(" ", "T") + "Z";

        return new TimeRange(from, to);

    }

    /**
     * Get all the events for the current day, use timezone london
     *
     * @return {@link TimeRange} instance
     */
    public static TimeRange today() {

        Calendar calendarFrom = Calendar.getInstance();
        Calendar calendarTo = Calendar.getInstance();

        calendarFrom.set(Calendar.MINUTE, 0);
        calendarFrom.set(Calendar.SECOND, 0);
        calendarFrom.set(Calendar.HOUR_OF_DAY, 4);

        calendarTo.add(Calendar.DAY_OF_MONTH, 1);
        calendarTo.set(Calendar.MINUTE, 59);
        calendarTo.set(Calendar.SECOND, 59);
        calendarTo.set(Calendar.HOUR_OF_DAY, 3);

        String from = format.format(calendarFrom.getTime()).replaceAll(" ", "T") + "Z";
        String to = format.format(calendarTo.getTime()).replaceAll(" ", "T") + "Z";

        return new TimeRange(from, to);
    }

    // GETTER & SETTER

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
