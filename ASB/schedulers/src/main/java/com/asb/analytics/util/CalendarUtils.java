package com.asb.analytics.util;

import com.asb.analytics.logs.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * Util class for calendars.
 *
 * Created by Claudio Paolicelli
 */
public class CalendarUtils {

    /**
     * Create the start date to collect the events.
     * Check when this method is call. If is before 04:00 that means we are from 00:00 to 03:59 and we must consider as
     * the start date the day before the current since we collect events from 04:00 to 03:59 of the day after.
     *
     * @return Instance of {@link Date}
     */
    public static Date softwareStartDateTime() {
        Calendar calendar = Calendar.getInstance();

        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        if ( currentHour < 4 ) {
            calendar.add(Calendar.DATE, -1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Logger.log().info("Start date:" + calendar.getTime());

        return calendar.getTime();
    }

    /**
     * Create the end date to collect the events.
     * Check when this method is call. If is after 03:59 that means we are from 04:00 to 23:59 and we must consider as
     * the stop date the day after the current since we collect events from 04:00 to 03:59 of the day after.
     *
     * @return Instance of {@link Date}
     */
    public static Date softwareStopDateTime() {

        Calendar calendar = Calendar.getInstance();

        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        Logger.log().info("Hour of the day:" + currentHour);

        if ( currentHour >= 4 ) {
            calendar.add(Calendar.DATE, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, 3);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        Logger.log().info("Stop date:" + calendar.getTime());

        return calendar.getTime();
    }

    public static String todayDate() {

        Calendar calendar = Calendar.getInstance();

        return String.format("%d-%s-%s",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) < 9
                        ? "0"+(calendar.get(Calendar.MONTH)+1) : calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH) <= 9
                        ? "0"+calendar.get(Calendar.DAY_OF_MONTH) : calendar.get(Calendar.DAY_OF_MONTH)
        );
    }
}
