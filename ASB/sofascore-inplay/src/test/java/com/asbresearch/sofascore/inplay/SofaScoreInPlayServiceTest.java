package com.asbresearch.sofascore.inplay;

import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

@Slf4j
@Ignore
public class SofaScoreInPlayServiceTest {

    @Test
    public void testLiveService() throws Exception {
        SofaScoreLiveEventService sofaScoreLiveEventService = new SofaScoreLiveEventService(null, 1000, Logger.Level.FULL);
        int counter = 0;
        while (true) {
            log.info("SofaScore Live Event Ids={}", sofaScoreLiveEventService.getLiveEventIds());
            TimeUnit.SECONDS.sleep(10);
            counter++;
            if ( counter > 2) {
                break;
            }
        }
        sofaScoreLiveEventService.stop();
    }
}
