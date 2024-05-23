package com.asbresearch.collector.mercurius;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@AllArgsConstructor
@ConditionalOnProperty(prefix = "collector", name = "mercuriusPredictionCollector", havingValue = "on")
public class MercuriusPredictionCollectionStarter {
    private final MercuriusPredictionCollector mercuriusPredictionCollector;

    @PostConstruct
    public void start() {
        mercuriusPredictionCollector.collectMercuriusPredictions();
    }
}
