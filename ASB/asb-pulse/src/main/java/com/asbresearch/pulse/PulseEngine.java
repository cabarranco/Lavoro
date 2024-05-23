package com.asbresearch.pulse;

import com.asbresearch.common.BigQueryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = {PulseEngine.class, BigQueryUtil.class})
@Slf4j
public class PulseEngine {
    public static void main(String[] args) {
        SpringApplication.run(PulseEngine.class, args);
    }
}
