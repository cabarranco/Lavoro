package com.asbresearch.pulse.service.strategy;

import com.asbresearch.pulse.model.StrategySpec;
import java.util.List;

public interface StrategyProvider {
    List<Strategy> getCurrentStrategies();
    List<StrategySpec> getCurrentStrategySpec();
}
