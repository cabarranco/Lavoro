package com.asbresearch.collector.matcher;

import com.asbresearch.collector.config.CollectorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("JaroWinklerDistanceMatcher")
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "jaroWinklerDistanceMatcher", havingValue = "on")
@EnableConfigurationProperties({CollectorProperties.class})
public class JaroWinklerDistanceMatcher implements TeamNameMatcher {
    private final JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
    private final double threshold;

    public JaroWinklerDistanceMatcher(CollectorProperties collectorProperties) {
        this.threshold = collectorProperties.getJaroWinklerThreshold();
    }

    @Override
    public boolean isSameTeam(String left, String right) {
        Double distance = jaroWinklerDistance.apply(left, right);
        log.debug("JaroWinklerDistance left={} right={} distance={}", left, right, distance);
        return distance >= threshold;
    }
}
