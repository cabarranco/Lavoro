package com.asbresearch.pulse.service.audit;



import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class LogEntryTest {
    private static final String LOG_TEST = "2020-05-23 16:58:35,765 INFO com.asbresearch.pulse.service.strategy.DefaultStrategy [se-3] mtC.id=c4906ed3d277432bb616f824777a673b op.id=eb867044cfe143e58ad9565db82c709d stra.id=VQS05MOCS-MO.A.B-01 mt.id=1.170403029 et.id=29794960 End StrategyCriteriaEvaluator time=3ms";

    @Test
    void of() {
        LogEntry logEntry = LogEntry.of(LOG_TEST);
        assertThat(logEntry, notNullValue());
        assertThat(logEntry.getLogEntry(), is(LOG_TEST));
        assertThat(logEntry.getTimestamp(), is("2020-05-23 16:58:35.765+00"));
        assertThat(logEntry.getEventId(), is("29794960"));
        assertThat(logEntry.getMarketChangeId(), is("c4906ed3d277432bb616f824777a673b"));
        assertThat(logEntry.getMarketId(), is("1.170403029"));
        assertThat(logEntry.getOpportunityId(), is("eb867044cfe143e58ad9565db82c709d"));
        assertThat(logEntry.getStrategyId(), is("VQS05MOCS-MO.A.B-01"));
    }

    @Test
    void toCsvData() {
        LogEntry logEntry = LogEntry.of(LOG_TEST);
        assertThat(logEntry, notNullValue());
        assertThat(logEntry.toCsvData(), is("eb867044cfe143e58ad9565db82c709d|c4906ed3d277432bb616f824777a673b|VQS05MOCS-MO.A.B-01|1.170403029|29794960|2020-05-23 16:58:35,765 INFO com.asbresearch.pulse.service.strategy.DefaultStrategy [se-3] mtC.id=c4906ed3d277432bb616f824777a673b op.id=eb867044cfe143e58ad9565db82c709d stra.id=VQS05MOCS-MO.A.B-01 mt.id=1.170403029 et.id=29794960 End StrategyCriteriaEvaluator time=3ms|2020-05-23 16:58:35.765+00"));
    }
}