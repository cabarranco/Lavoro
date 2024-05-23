package com.asbresearch.collector.util;

import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.collector.config.CollectorProperties;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;

@UtilityClass
public class Constants {
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String MATCH_ODDS = "Match Odds";
    public static final String MATCH_ODDS_UNMANAGED = "Match Odds Unmanaged";
    public static final String OVER_UNDER_05_GOALS = "Over/Under 0.5 Goals";
    public static final String OVER_UNDER_05_GOALS_UNMANAGED = "Over/Under 0.5 Goals Unmanaged";
    public static final String OVER_UNDER_15_GOALS = "Over/Under 1.5 Goals";
    public static final String OVER_UNDER_15_GOALS_UNMANAGED = "Over/Under 1.5 Goals Unmanaged";
    public static final String OVER_UNDER_25_GOALS = "Over/Under 2.5 Goals";
    public static final String OVER_UNDER_25_GOALS_UNMANAGED = "Over/Under 2.5 Goals Unmanaged";
    public static final String OVER_UNDER_35_GOALS = "Over/Under 3.5 Goals";
    public static final String OVER_UNDER_35_GOALS_UNMANAGED = "Over/Under 3.5 Goals Unmanaged";
    public static final String CORRECT_SCORE = "Correct Score";
    public static final String CORRECT_SCORE_UNMANAGED = "Correct Score Unmanaged";
    public static final String ASIAN_HANDICAP = "Asian Handicap";
    public static final String ASIAN_HANDICAP_UNMANAGED = "Asian Handicap Unmanaged";

    public static final String eventIdCol = "eventId";
    public static final String timestampCol = "timestamp";
    public static final String kickOffTimeCol = "kickOffTime";
    public static final String secondHalfEndTimeCol = "secondHalfEndTime";
    public static final String updateTimeCol = "updateTime";
    public static final String updateTypeCol = "updateType";
    public static final String scoreCol = "score";
    public static final String teamCol = "team";
    public static final String asbSelectionIdCol = "asbSelectionId";
    public static final String totalMatchedCol = "totalMatched";
    public static final String publishTimeCol = "publishTime";
    public static final String startTimeCol = "startTime";
    public static final String backPriceCol = "backPrice";
    public static final String layPriceCol = "layPrice";
    public static final String backSizeCol = "backSize";
    public static final String laySizeCol = "laySize";
    public static final String YELLOW_CARD = "YellowCard";
    public static final String RED_CARD = "RedCard";
    public static final String GOAL = "Goal";
    public static final String HOME = "home";
    public static final String AWAY = "away";
    public static TimeRange TimeRangeForTradingDay = timeRangeForTradingDay();

    private static TimeRange timeRangeForTradingDay() {
        ZonedDateTime from = LocalDate.now().atStartOfDay(UTC).plusHours(4);
        return new TimeRange(from.toInstant(), from.plusHours(24).toInstant());
    }

    public static Set<String> getMarketTypeCodes(CollectorProperties collectorProperties) {
        return List.of(collectorProperties.getMarketTypeCodes().split(",")).stream().map(s -> s.trim()).collect(Collectors.toSet());
    }

    public static String endDate(CollectorProperties collectorProperties) {
        String inputDate = StringUtils.trimToNull(collectorProperties.getEndDate());
        if (inputDate == null) {
            return LocalDate.now().format(dateTimeFormatter);
        }
        return LocalDate.parse(inputDate, dateTimeFormatter).format(dateTimeFormatter);
    }

    public static String startDate(CollectorProperties collectorProperties) {
        String inputDate = StringUtils.trimToNull(collectorProperties.getStartDate());
        if (inputDate == null) {
            return LocalDate.now().minusDays(1).format(dateTimeFormatter);
        }
        return LocalDate.parse(inputDate, dateTimeFormatter).format(dateTimeFormatter);
    }
}
