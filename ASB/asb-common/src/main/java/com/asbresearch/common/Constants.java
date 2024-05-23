package com.asbresearch.common;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String SOCCER = "Soccer";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final long GAME_DURATION_SECS = TimeUnit.MINUTES.toSeconds(105);
    public static final String BACK = "B";
    public static final String ODD = "odd";
    public static final String MCD_STRATEGY_ID = "strategy.id";
    public static final String MCD_MARKET_ID = "market.id";
    public static final String MCD_EVENT_ID = "event.id";
    public static final String OPPORTUNITY_ID = "opportunity.id";

    public static <T> Collection<List<T>> partitionBasedOnSize(Collection<T> inputList, int size) {
        final AtomicInteger counter = new AtomicInteger(0);
        return inputList.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / size))
                .values();
    }
}
