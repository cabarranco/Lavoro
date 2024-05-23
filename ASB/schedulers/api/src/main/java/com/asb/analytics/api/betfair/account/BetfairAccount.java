package com.asb.analytics.api.betfair.account;

import com.asb.analytics.api.Config;
import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.SimpleResponse;
import com.asb.analytics.api.adapters.AccountAdapter;
import com.asb.analytics.api.betfair.betting.BetfairBetting;
import com.asb.analytics.domain.betfair.Wallet;

/**
 * Collection of all the BetfairAccount api methods from Betfair
 *
 * @author Claudio Paolicelli
 */
public class BetfairAccount {

    private final String sessionToken;
    private final String applicationKey;

    private BetfairAccount(String sessionToken, String applicationKey) {
        this.sessionToken = sessionToken;
        this.applicationKey = applicationKey;
    }

    private BetfairAccount(String sessionToken) {
        this.sessionToken = sessionToken;
        this.applicationKey = null;
    }

    /**
     * Static constructor for betfair betting api class
     *
     * @param sessionToken get from {@link BetfairAccount}
     * @return new instance of {@link BetfairBetting}
     */
    public static BetfairAccount init(String sessionToken, String applicationKey) {
        return new BetfairAccount(sessionToken, applicationKey);
    }

    public static BetfairAccount init(String sessionToken) {
        return new BetfairAccount(sessionToken);
    }

    public String getDeveloperAppKeys() {

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_ACCOUNT_ENDPOINT + "getDeveloperAppKeys/")
                    .timeout(5000)
                    .header("content-type", "application/json")
                    .header("X-Authentication", this.sessionToken)
                    .body("{\"filter\":{ }}")
                    .method(HttpConnector.POST)
                    .execute();

            return AccountAdapter.getApplicationId(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Wallet getUserWallet() {

        try {
            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_ACCOUNT_ENDPOINT + "getAccountFunds/")
                    .timeout(5000)
                    .header("content-type", "application/json")
                    .header("X-Application", this.applicationKey)
                    .header("X-Authentication", this.sessionToken)
                    .body("{\"filter\":{ }}")
                    .method(HttpConnector.POST)
                    .execute();

            return AccountAdapter.getWallet(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
