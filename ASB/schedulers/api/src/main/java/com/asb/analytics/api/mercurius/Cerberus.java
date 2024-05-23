package com.asb.analytics.api.mercurius;

import com.asb.analytics.api.Config;
import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.Logger;
import com.asb.analytics.api.SimpleResponse;
import com.asb.analytics.api.adapters.EventAdapter;
import com.asb.analytics.domain.mercurius.Prediction;
import com.asb.analytics.domain.mercurius.PredictionRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Collection of all the betting api methods from Betfair
 *
 * @author Claudio Paolicelli
 */
public class Cerberus {

    private final Gson gson;

    public Cerberus() {
        this.gson = new GsonBuilder().create();
    }

    /**
     * Call to Cerberus project in Mercurius to retrieve all the predictions for match odds and correct scores.
     *
     * @param homeTeamId mercurius home team id
     * @param awayTeamId mercurius away team id
     * @param date event start date time
     * @param competitionId mercurius competition id
     *
     * @return list of prediction for correct scores and match odds.
     */
    public List<Prediction> getPredictions(Long homeTeamId, Long awayTeamId, String date, Long competitionId) {

        HashMap request = new HashMap();

        request.put(
                competitionId.toString(),
                Collections.singleton(
                        new PredictionRequest(
                                161325935L,
                                homeTeamId,
                                awayTeamId,
                                date
                        )
                ));

        String jsonBet = gson.toJson(request);

        try {

            byte[] src = "asbresearch:Q62Wt&59DjtX%xYc".getBytes(StandardCharsets.UTF_8);
            String encoding = Base64.getEncoder().encodeToString(src);

            SimpleResponse response = HttpConnector
                    .connect(Config.MERCURIUS_CERBERUS + "predictions/totals/")
                    .timeout(5000)
                    .header("Authorization", "Basic " + encoding)
                    .header("Content-Type", "application/json")
                    .body(jsonBet)
                    .method(HttpConnector.POST)
                    .execute();

            Logger.log().info("Mercurius predictions response code: " + response.getCode());

            if (response.getCode() != 200) {
                Logger.log().error("Merucrius response: " + response.getBody());
                return new ArrayList<>();
            }

            return EventAdapter.getPredictionsTable(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
