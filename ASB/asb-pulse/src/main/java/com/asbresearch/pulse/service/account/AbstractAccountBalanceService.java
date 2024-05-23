package com.asbresearch.pulse.service.account;

import com.asbresearch.common.bigquery.BigQueryService;
import com.asbresearch.pulse.config.AccountProperties;
import com.asbresearch.pulse.config.AppProperties;
import com.asbresearch.pulse.entity.AccountBalance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.util.Precision;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.asbresearch.common.BigQueryUtil.csvValue;
import static com.asbresearch.pulse.util.Constants.PULSE_REPORTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.ZoneOffset.UTC;

abstract class AbstractAccountBalanceService implements AccountBalanceService {
    protected static final String ACCOUNT_DIR = "account";
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(UTC);

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final BigQueryService bigQueryService;
    private final AccountProperties accountProperties;

    protected AbstractAccountBalanceService(ObjectMapper objectMapper,
                                            AppProperties appProperties,
                                            BigQueryService bigQueryService,
                                            AccountProperties accountProperties) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.bigQueryService = bigQueryService;
        this.accountProperties = accountProperties;
    }

    @Override
    public void create(AccountBalance accountBalance) {
        if (accountBalance != null) {
            try {
                Path path = Paths.get(appProperties.getDataDirectory(), ACCOUNT_DIR, appProperties.getNode(), dateFormat(accountBalance.getTradeDate()), UUID.randomUUID() + ".json");
                if (!path.getParent().toFile().exists()) {
                    Files.createDirectories(path.getParent());
                }
                Files.writeString(path, objectMapper.writeValueAsString(accountBalance), CREATE);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Error trying to save account balance user=%s availableToBet=%s currency=%s",
                        accountBalance.getUsername(), accountBalance.getAvailableToBet(), accountBalance.getCurrency()), e);
            }
        }
    }

    @Override
    public Optional<AccountBalance> find(String username, Instant tradeDate) {
        try {
            Path dir = Paths.get(appProperties.getDataDirectory(), ACCOUNT_DIR, appProperties.getNode(), dateFormat(tradeDate));
            if (!dir.toFile().exists()) {
                return Optional.empty();
            }
            Set<Path> files = listFiles(dir);
            return files.stream()
                    .map(path -> readFileToString(path))
                    .map(content -> toAccountBalance(content)).filter(accountBalance -> accountBalance.getUsername().equals(username)).findFirst();
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Error finding account balance for user=%s tradeDate={}", username, tradeDate), ex);
        }
    }

    @Override
    public void updateBigQueryAccountBalance(AccountBalance accountBalance) {
        double availableToBetBalance = Precision.round(accountBalance.getAvailableToBet(), 2);
        double balanceSaving = Precision.round(availableToBetBalance * accountProperties.getPercentageBalanceToSave(), 2);
        String csvData = String.format("%s|%s|%s|%s|%s|%s|%s",
                csvValue(accountBalance.getTradeDate()),
                csvValue(accountProperties.getUser()),
                csvValue(availableToBetBalance),
                csvValue(accountBalance.getCurrency()),
                csvValue(balanceSaving),
                csvValue(Precision.round(availableToBetBalance - balanceSaving, 2)),
                csvValue(appProperties.getNode()));
        bigQueryService.insertRows(PULSE_REPORTING, "account_balance", Collections.singletonList(csvData));
    }

    protected Set<Path> listFiles(Path dir) throws IOException {
        Set<Path> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path);
                }
            }
        }
        return fileList;
    }

    protected String readFileToString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error trying to read file={}", path), e);
        }
    }

    protected AccountBalance toAccountBalance(String content) {
        try {
            return objectMapper.readValue(content, AccountBalance.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Error trying to convert json={}", content), e);
        }
    }

    protected String dateFormat(Instant tradeDate) {
        return DATE_FORMATTER.format(tradeDate);
    }
}
