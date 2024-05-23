package com.asbresearch.collector.model;

import com.asbresearch.common.model.BigQueryCreateRecord;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.asbresearch.common.BigQueryUtil.csvValue;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class InplayEventExceptions {
    @EqualsAndHashCode.Include
    private BigQueryCreateRecord bigQueryCreateRecord = new BigQueryCreateRecord();
    private String eventId;
    private String exceptionCode;

    public String toString() {
        return String.format("%s|%s|%s|%s",
                csvValue(bigQueryCreateRecord.getId()),
                csvValue(eventId),
                csvValue(exceptionCode),
                csvValue(bigQueryCreateRecord.getCreateTimestamp()));
    }
}
