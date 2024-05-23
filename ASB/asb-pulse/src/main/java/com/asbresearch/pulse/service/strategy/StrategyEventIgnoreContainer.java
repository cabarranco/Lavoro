package com.asbresearch.pulse.service.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StrategyEventIgnoreContainer {
    private static final String STRAT_EVENT_KEY_TEMPLATE = "%s#%s";

    private final Map<String, Boolean> eventsPerStrategyToIgnore = new ConcurrentHashMap<>();

    public void ignoreEventForStrat(String strategyId, String eventId) {
        if (strategyId != null && eventId != null) {
            eventsPerStrategyToIgnore.putIfAbsent(key(strategyId, eventId), Boolean.TRUE);
            log.info("Ignoring event={} from strategy={}", eventId, strategyId);
        }
    }

    public boolean contains(String strategyId, String eventId) {
        if (strategyId != null && eventId != null) {
            return eventsPerStrategyToIgnore.containsKey(key(strategyId, eventId));
        }
        return false;
    }

    private String key(String strategyId, String eventId) {
        return String.format(STRAT_EVENT_KEY_TEMPLATE, strategyId, eventId);
    }
}
