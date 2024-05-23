package com.asbresearch.pulse.service.strategy;

import com.asbresearch.betfair.esa.Client;
import com.asbresearch.betfair.esa.cache.market.MarketChangeEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import static com.asbresearch.pulse.util.Constants.MCD_EVENT_ID;
import static com.asbresearch.pulse.util.Constants.MCD_MARKET_ID;
import static com.asbresearch.pulse.util.Constants.MCD_STRATEGY_ID;
import static com.asbresearch.pulse.util.Constants.OPPORTUNITY_ID;

@Slf4j
@AllArgsConstructor
@Getter
public class StrategyMarketChangeRunner implements Runnable {
    private final MarketChangeEvent marketChangeEvent;
    private final Map<String, String> copyOfMdcContext;
    private final Strategy strategy;
    private final Instant createTs;
    private final Map<String, Instant> timestampPerMarketChange;

    @Override
    public void run() {
        try {
            Instant latestUpdateTs = timestampPerMarketChange.get(marketChangeEvent.getMarket().getMarketId());
            setUpLoggingMdcContext(marketChangeEvent, copyOfMdcContext, strategy);
            if (latestUpdateTs != null && !createTs.isBefore(latestUpdateTs)) {
                strategy.onMarketChange(marketChangeEvent.getSnap());
            } else {
                String startTimeInTxt = MDC.get(Client.START_TIME);
                if (startTimeInTxt != null) {
                    log.warn("Discarding elapsed market change marketId={} created={} timeInQueue={}ms",
                            marketChangeEvent.getMarket().getMarketId(),
                            createTs,
                            System.currentTimeMillis() - Long.parseLong(startTimeInTxt));
                }
            }
        } catch (RuntimeException ex) {
            log.error("Error processing strategy", ex);
        }
    }

    private void setUpLoggingMdcContext(MarketChangeEvent marketChangeEvent, Map<String, String> copyOfMdcContext, Strategy strategy) {
        MDC.clear();
        if (copyOfMdcContext != null) {
            MDC.setContextMap(copyOfMdcContext);
        }
        MDC.put(OPPORTUNITY_ID, UUID.randomUUID().toString().replace("-", ""));
        MDC.put(MCD_STRATEGY_ID, strategy.getId());
        MDC.put(MCD_MARKET_ID, marketChangeEvent.getMarket().getMarketId());
        MDC.put(MCD_EVENT_ID, marketChangeEvent.getMarket().getSnap().getMarketDefinition().getEventId());
    }
}
