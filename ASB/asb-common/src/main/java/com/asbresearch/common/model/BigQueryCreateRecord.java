package com.asbresearch.common.model;

import com.asbresearch.common.BigQueryUtil;
import java.time.Instant;
import lombok.Value;

@Value
public class BigQueryCreateRecord {
    private final String id = BigQueryUtil.shortUUID();
    private final Instant createTimestamp = Instant.now();
}
