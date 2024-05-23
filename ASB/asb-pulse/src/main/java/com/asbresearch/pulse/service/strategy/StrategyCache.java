package com.asbresearch.pulse.service.strategy;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StrategyCache {
    private final Map<String, Strategy> cache = new ConcurrentHashMap<>();

    @Autowired
    public StrategyCache(StrategyProvider strategyProvider) {
        Preconditions.checkNotNull(strategyProvider, "strategyProvider must be provided");
        strategyProvider.getCurrentStrategies().forEach(strategy -> cache.put(strategy.getId(), strategy));
        log.info("Loaded Strategy count={} strategy={}", cache.size(), cache.keySet());
    }

    public Optional<Strategy> getStrategyById(String strategyId) {
        Strategy strategy = cache.get(strategyId);
        return strategy == null ? Optional.empty() : Optional.of(strategy);
    }

    public Collection<Strategy> getStrategies() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public Map<String, Strategy> getCache() {
        return Collections.unmodifiableMap(cache);
    }

    public Set<String> getStrategyIds() {
        return Collections.unmodifiableSet(cache.keySet());
    }
}
