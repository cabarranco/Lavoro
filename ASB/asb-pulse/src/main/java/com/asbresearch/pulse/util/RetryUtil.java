package com.asbresearch.pulse.util;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryUtil {
    private static final String MESSAGE = "Interrupted while waiting for retry";
    private static final int TIME_TO_WAIT_IN_SEC = 10;

    public static void retryWait(String message) {
        retryWait(message, null);
    }

    public static void retryWait(String message, Exception ex) {
        try {
            if (ex != null) {
                log.error("{} Retrying ..... in {}s", message, TIME_TO_WAIT_IN_SEC, ex);
            } else {
                log.error("{} Retrying ..... in {}s", message, TIME_TO_WAIT_IN_SEC);
            }
            TimeUnit.SECONDS.sleep(TIME_TO_WAIT_IN_SEC);
        } catch (InterruptedException e) {
            log.warn(MESSAGE);
            Thread.currentThread().interrupt();
            throw new RuntimeException(MESSAGE);
        }
    }
}
