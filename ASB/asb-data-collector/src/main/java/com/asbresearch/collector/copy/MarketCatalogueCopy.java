package com.asbresearch.collector.copy;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("MarketCatalogueCopy")
@Slf4j
@ConditionalOnProperty(prefix = "copy", name = "marketCatalogueCopy", havingValue = "on")
public class MarketCatalogueCopy {
    private final MarketCatalogueCopyAsync marketCatalogueCopyAsync;

    @Autowired
    public MarketCatalogueCopy(MarketCatalogueCopyAsync marketCatalogueCopyAsync) {
        this.marketCatalogueCopyAsync = marketCatalogueCopyAsync;
    }

    @PostConstruct
    public void execute() {
        marketCatalogueCopyAsync.copy();
    }

}
