package com.asbresearch.pulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("account")
public class AccountProperties {
    private String user;
    private String password;
    private String appKey;
    private double commissionRate = 0.02;
    private double percentageBalanceToSave = 0.15;
    private double maxAllocationSplitter = 200;
    private double opportunityMinAllocationSum = 50.0;
    private Double maxEventConcentration;
    private double maxStrategyConcentration = 0.8;
    private Double maxStrategyEventConcentration;
    private double minDayBets = 4.0;
    private String currencyCode = "GBP";
}
