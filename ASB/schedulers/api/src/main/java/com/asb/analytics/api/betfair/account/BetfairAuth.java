package com.asb.analytics.api.betfair.account;

import com.asb.analytics.api.Config;
import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.SimpleResponse;
import org.json.JSONObject;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class BetfairAuth {

    private static final String USER_DEV = "claudio.paolicelli@asbresearch.com";
    private static final String PWD_DEV = "Cl4udio??";

    private static final String USER_PROD = "fdr@asbresearch.com";
    private static final String PWD_PROD = "asbcheqai87";

    public static String login(String username, String password) {
        try {

            SimpleResponse response = HttpConnector
                    .connect(Config.BETFAIR_AUTH_ENDPOINT)
                    .timeout(5000)
                    .header("Accept", "application/json")
                    .header("X-Application", Config.BETFAIR_APP_KEY)
                    .header("content-type", "application/x-www-form-urlencoded")
                    .body(String.format("username=%s&password=%s", username, password))
                    .method(HttpConnector.POST)
                    .execute();

            JSONObject jsonObj = new JSONObject(response.getBody());

            return String.valueOf(jsonObj.get("token"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String login() {
        return login(USER_DEV, PWD_DEV);
    }
}
