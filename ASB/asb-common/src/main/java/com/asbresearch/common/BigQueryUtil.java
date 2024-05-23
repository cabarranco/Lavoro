package com.asbresearch.common;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.experimental.UtilityClass;

import static java.time.ZoneOffset.UTC;

@UtilityClass
public class BigQueryUtil {
    public static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(UTC);

    public static final String TEMP_DATASET = "tmp";
    public static final String BETSTORE_DATASET = "betstore";
    public static final String RESEARCH_DATASET = "research";
    public static final String PULSE_REPORTING_DATASET = "pulse_reporting";

    public static final String INPLAY_FEATURES_TABLE = "event_inplay_features";
    public static final String PRELIVE_FEATURES_TABLE = "event_prelive_features";
    public static final String EVENT_DETAILS_TABLE = "inplay_event_details";
    public static final String EVENT_PRICES_ANALYTICS_TABLE = "event_prices_analytics";
    public static final String INPLAY_STAGING_TABLE = "betfair_soccer_inplay_1970_staging";
    public static final String INPLAY_EVENT_EXCEPTIONS_TABLE = "inplay_event_exceptions";
    public static final String HISTORICAL_DATA_TABLE = "betfair_historical_data";
    public static final String BETFAIR_MARKET_CATALOGUE_TABLE = "betfair_market_catalogue";
    public static final String ACCOUNT_BALANCE_TABLE = "account_balance";
    public static final String SOCCER_INPLAY_TABLE = "betfair_soccer_inplay";

    public static String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Instant) {
            return INSTANT_FORMATTER.format((Instant)value);
        }
        String cellValue = value.toString();
        if (cellValue.contains("|")) {
            return String.format("\"%s\"", cellValue.replaceAll("\"", "\"\""));
        } else {
            return cellValue;
        }
    }

    public static String shortUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
