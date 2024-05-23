package com.asbresearch.collector.copy;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("SoccerInplayCopy")
@Slf4j
@ConditionalOnProperty(prefix = "copy", name = "soccerInplayCopy", havingValue = "on")
public class SoccerInplayCopy {
    private final SoccerInplayCopyAsync soccerInplayCopyAsync;

    @Autowired
    public SoccerInplayCopy(SoccerInplayCopyAsync soccerInplayCopyAsync) {
        this.soccerInplayCopyAsync = soccerInplayCopyAsync;
    }

    @PostConstruct
    public void execute() {
        soccerInplayCopyAsync.copy();
    }

}
