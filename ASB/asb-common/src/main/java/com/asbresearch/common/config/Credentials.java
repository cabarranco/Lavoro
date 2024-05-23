package com.asbresearch.common.config;

import lombok.Data;

@Data
public class Credentials {
    private String location = "classpath:credentials.json";
    private String encodedKey;
}
