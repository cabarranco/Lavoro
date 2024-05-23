package com.asb.analytics;

import com.asb.analytics.logs.Logger;

import java.util.concurrent.Callable;

public class TokenRefresher implements Callable<String> {

    @Override
    public String call() {

        Logger.log().info("Retrieving gcloud token...");

        ExecuteShellCommand com = new ExecuteShellCommand();
        String token = com.executeCommand("gcloud auth print-access-token");

        if (token.isEmpty())
            Logger.log().error("Token not retrieved");
        else Logger.log().info("gcloud token: " + token);

        return token;
    }
}
