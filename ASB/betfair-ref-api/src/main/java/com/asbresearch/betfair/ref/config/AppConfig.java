package com.asbresearch.betfair.ref.config;

import lombok.Value;

@Value
public class AppConfig {
    private final String appKey;
    private final String userName;
    private final String password;
}
