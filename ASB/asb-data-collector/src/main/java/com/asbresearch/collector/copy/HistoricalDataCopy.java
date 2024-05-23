package com.asbresearch.collector.copy;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("HistoricalDataCopy")
@Slf4j
@ConditionalOnProperty(prefix = "copy", name = "historicalDataCopy", havingValue = "on")
public class HistoricalDataCopy {
    private final HistoricalDataCopyAsync historicalDataCopyAsync;

    @Autowired
    public HistoricalDataCopy(HistoricalDataCopyAsync historicalDataCopyAsync) {
        this.historicalDataCopyAsync = historicalDataCopyAsync;
    }

    @PostConstruct
    public void execute() {
        historicalDataCopyAsync.copy();
    }

}
