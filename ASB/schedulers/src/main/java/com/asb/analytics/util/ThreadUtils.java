package com.asb.analytics.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ThreadUtils {

    public static ThreadFactoryBuilder threadFactoryBuilder(String threadName) {
        ThreadFactoryBuilder builder = new ThreadFactoryBuilder().setNameFormat(threadName + "-%d").setDaemon(true);
        builder.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        return builder;
    }
}
