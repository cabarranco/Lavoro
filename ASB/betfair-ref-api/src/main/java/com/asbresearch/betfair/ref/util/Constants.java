package com.asbresearch.betfair.ref.util;

import com.asbresearch.betfair.ref.entities.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;


public final class Constants {
    public static final TimeRange DEFAULT_TRADING_TIME_RANGE = timeRangeForTradingDay(24);
    public static final String SUCCESS = "SUCCESS";

    private Constants() {
    }

    public static TimeRange timeRangeForTradingDay(int hours) {
        ZonedDateTime from = LocalDate.now().atStartOfDay(UTC).plusHours(4);
        return new TimeRange(Instant.now(), from.plusHours(hours).toInstant());
    }
}
