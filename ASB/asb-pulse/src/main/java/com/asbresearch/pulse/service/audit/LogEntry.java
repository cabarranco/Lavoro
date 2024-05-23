package com.asbresearch.pulse.service.audit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Data
public class LogEntry {
    private static final Pattern OpIdPattern = Pattern.compile("op.id=([^ ]+)");
    private static final Pattern MarketChangeIdPattern = Pattern.compile("mtC.id=([^ ]+)");
    private static final Pattern StratIdPattern = Pattern.compile("stra.id=([^ ]+)");
    private static final Pattern MarketIdPattern = Pattern.compile("mt.id=([^ ]+)");
    private static final Pattern EventIdPattern = Pattern.compile("et.id=([^ ]+)");
    private static final Pattern TimestampPattern = Pattern.compile("^([0-9]{4})-([0-9]{2})-([0-9]{2})\\s+([0-9]{2}):([0-9]{2}):([0-9]{2}),([0-9]+)\\s+");

    private String opportunityId;
    private String marketChangeId;
    private String strategyId;
    private String marketId;
    private String eventId;
    private String logEntry;
    private String timestamp;

    public String toCsvData() {
        return String.format("%s|%s|%s|%s|%s|%s|%s",
                cellValue(trimToEmpty(opportunityId)),
                cellValue(trimToEmpty(marketChangeId)),
                cellValue(trimToEmpty(strategyId)),
                cellValue(trimToEmpty(marketId)),
                cellValue(trimToEmpty(eventId)),
                cellValue(trimToEmpty(logEntry)),
                cellValue(trimToEmpty(timestamp)));
    }

    private String cellValue(String cellValue) {
        if (cellValue.contains("|")) {
            return String.format("\"%s\"", cellValue.replaceAll("\"", "\"\""));
        } else {
            return cellValue;
        }
    }

    public static LogEntry of(String logLine) {
        LogEntry log = new LogEntry();
        log.setLogEntry(logLine);

        Matcher matcher = OpIdPattern.matcher(logLine);
        if (matcher.find()) {
            log.setOpportunityId(matcher.group(1));
        }
        matcher = MarketChangeIdPattern.matcher(logLine);
        if (matcher.find()) {
            log.setMarketChangeId(matcher.group(1));
        }
        matcher = StratIdPattern.matcher(logLine);
        if (matcher.find()) {
            log.setStrategyId(matcher.group(1));
        }
        matcher = MarketIdPattern.matcher(logLine);
        if (matcher.find()) {
            log.setMarketId(matcher.group(1));
        }
        matcher = EventIdPattern.matcher(logLine);
        if (matcher.find()) {
            log.setEventId(matcher.group(1));
        }
        matcher = TimestampPattern.matcher(logLine);
        if (matcher.find()) {
            log.setTimestamp(String.format("%s-%s-%s %s:%s:%s.%s+00", matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5), matcher.group(6), matcher.group(7)));
        }
        return log;
    }
}
