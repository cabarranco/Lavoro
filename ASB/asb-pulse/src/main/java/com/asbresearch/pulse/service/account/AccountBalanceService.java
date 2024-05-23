package com.asbresearch.pulse.service.account;

import com.asbresearch.pulse.entity.AccountBalance;

import java.time.Instant;
import java.util.Optional;

public interface AccountBalanceService {
    void create(AccountBalance accountBalance);

    Optional<AccountBalance> find(String username, Instant tradeDate);

    double getAvailableToBetBalance();

    void updateBigQueryAccountBalance(AccountBalance accountBalance);
}
