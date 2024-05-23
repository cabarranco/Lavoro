package com.asbresearch.pulse.service.strategy;

import java.util.concurrent.LinkedBlockingDeque;

public class LIFOStrategyTaskQueue extends LinkedBlockingDeque<Runnable> {

    @Override
    public boolean offer(Runnable runnable) {
        return offerFirst(runnable);
    }
}
