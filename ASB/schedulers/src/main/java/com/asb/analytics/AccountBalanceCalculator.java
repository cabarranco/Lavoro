package com.asb.analytics;

import com.asb.analytics.api.betfair.account.BetfairAccount;
import com.asb.analytics.api.betfair.account.BetfairAuth;
import com.asb.analytics.bigquery.AccountBalance;
import com.asb.analytics.bigquery.BigQueryServices;
import com.asb.analytics.bigquery.Row;
import com.asb.analytics.domain.User;
import com.asb.analytics.domain.betfair.Wallet;
import com.asb.analytics.exceptions.UserNotFoundException;
import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountBalanceCalculator {

    private static MongoDatabase database;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final Gson gson = new GsonBuilder().create();

    static {
        database = new MongoConnector().connect("asb_research");
    }

    public static void main(String[] args) {

        List<User> users = new ArrayList<>();
        List<Row> values = new ArrayList<>();

        ExecuteShellCommand com = new ExecuteShellCommand();
        String gCloudToken = com.executeCommand("gcloud auth print-access-token");

        System.out.println("cloud token: " + gCloudToken);

        try {
            users = MongoUtils
                    .query(database)
                    .getUsers();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Users count: " + users.size());

        for (User user : users) {
            String token = BetfairAuth.login(user.getUsername(), user.getPassword());

            Wallet wallet = BetfairAccount.init(token, user.getApplicationKey()).getUserWallet();

            double availableToBet = wallet.getAvailableToBetBalance();
//            double balanceSaving = availableToBet * user.getBalanceToSavePercentage();

            AccountBalance accountBalance = new AccountBalance(
                    dateTimeFormat.format(new Date()),
                    user.getUsername(),
                    availableToBet,
                    "Pound Sterling"
            );

            values.add(new Row<>(accountBalance));
        }

        if (values.size() > 0 && !gCloudToken.isEmpty()) {
            BigQueryServices.pulseReporting()
                    .insertBigQuery(gson, values, gCloudToken, "account_balance");
        }
    }


}
