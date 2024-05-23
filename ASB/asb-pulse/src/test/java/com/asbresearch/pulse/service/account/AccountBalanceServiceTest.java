package com.asbresearch.pulse.service.account;

import com.asbresearch.pulse.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;

public class AccountBalanceServiceTest {
    private BetfairWalletAccountService accountBalanceService;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setDataDirectory(tempDir.getAbsolutePath());
//        accountBalanceService = new DefaultAccountBalanceService(new ObjectMapper(), appProperties);
    }

//    @Test
//    void create() throws IOException {
//        Instant tradeDate = Instant.parse("2020-05-21T15:21:05.547191700Z");
//        accountBalanceService.create(AccountBalance.of("testUser1", 1500.0, "GBP", tradeDate));
//        accountBalanceService.create(AccountBalance.of("testUser2", 100.0, "USD", tradeDate));
//
//
//        Set<Path> paths = accountBalanceService.listFiles(Paths.get(tempDir.getAbsolutePath(), DefaultAccountBalanceService.ACCOUNT_DIR, accountBalanceService.dateFormat(tradeDate)));
//        assertThat(paths, notNullValue());
//        assertThat(paths.size(), is(2));
//        List<String> jsons = paths.stream().map(path -> accountBalanceService.readFileToString(path)).collect(Collectors.toList());
//        assertThat(jsons, notNullValue());
//        assertThat(jsons.size(), is(2));
//        assertThat(jsons, hasItem("{\"username\":\"testUser1\",\"tradeDate\":\"2020-05-21T15:21:05.547191700Z\",\"availableToBet\":1500.0,\"currency\":\"GBP\"}"));
//        assertThat(jsons, hasItem("{\"username\":\"testUser2\",\"tradeDate\":\"2020-05-21T15:21:05.547191700Z\",\"availableToBet\":100.0,\"currency\":\"USD\"}"));
//    }
//
//    @Test
//    void find() {
//        Instant tradeDate = Instant.parse("2020-05-21T15:21:05.547191700Z");
//        accountBalanceService.create(AccountBalance.of("testUser1", 1500.0, "GBP", tradeDate));
//        accountBalanceService.create(AccountBalance.of("testUser2", 100.0, "USD", tradeDate));
//
//        Optional<AccountBalance> accountBalance = accountBalanceService.find("testUser1", tradeDate);
//        assertThat(accountBalance, notNullValue());
//        assertThat(accountBalance.isPresent(), is(true));
//        assertThat(accountBalance.get().getUsername(), is("testUser1"));
//        assertThat(accountBalance.get().getTradeDate(), is(tradeDate));
//        assertThat(accountBalance.get().getAvailableToBet(), is(1500.0));
//        assertThat(accountBalance.get().getCurrency(), is("GBP"));
//    }
//
//    @Test
//    void find_noDirectoryExist() {
//        Instant tradeDate = Instant.parse("2020-05-22T15:21:05.547191700Z");
//        Optional<AccountBalance> accountBalance = accountBalanceService.find("testUser1", tradeDate);
//        assertThat(accountBalance, notNullValue());
//        assertThat(accountBalance.isPresent(), is(false));
//    }
//
//    @Test
//    void find_noAccountExist() {
//        Instant tradeDate = Instant.parse("2020-05-22T15:21:05.547191700Z");
//        accountBalanceService.create(AccountBalance.of("testUser2", 100.0, "USD", tradeDate));
//
//        Optional<AccountBalance> accountBalance = accountBalanceService.find("testUser1", tradeDate);
//        assertThat(accountBalance, notNullValue());
//        assertThat(accountBalance.isPresent(), is(false));
//    }
}