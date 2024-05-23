package com.asbresearch.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ThreadUtils {

    public static ThreadFactoryBuilder threadFactoryBuilder(String threadName) {
        ThreadFactoryBuilder builder = new ThreadFactoryBuilder().setNameFormat(threadName + "-%d").setDaemon(true);
        builder.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught error", e));
        return builder;
    }
}
