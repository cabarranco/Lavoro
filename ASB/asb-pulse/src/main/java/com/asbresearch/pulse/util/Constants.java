package com.asbresearch.pulse.util;

import com.asbresearch.betfair.ref.entities.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

import static java.time.ZoneOffset.UTC;

@UtilityClass
public class Constants {
    public static final String SOCCER = "Soccer";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final TimeRange DEFAULT_TRADING_TIME_RANGE = timeRangeForTradingDay(24);
    public static final long GAME_DURATION_SECS = TimeUnit.MINUTES.toSeconds(105);
    public static final String BACK = "B";
    public static final String ODD = "odd";
    public static final String MCD_STRATEGY_ID = "strategy.id";
    public static final String MCD_MARKET_ID = "market.id";
    public static final String MCD_EVENT_ID = "event.id";
    public static final String OPPORTUNITY_ID = "opportunity.id";
    public static final String PULSE_REPORTING = "pulse_reporting";
    public static final String RESEARCH = "research";

    public static TimeRange timeRangeForTradingDay(int hours) {
        ZonedDateTime from = LocalDate.now().atStartOfDay(UTC).plusHours(4);
        return new TimeRange(Instant.now(), from.plusHours(hours).toInstant());
    }
}
