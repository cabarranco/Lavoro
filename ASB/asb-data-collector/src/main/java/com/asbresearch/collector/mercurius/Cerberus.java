package com.asbresearch.collector.mercurius;

import com.asbresearch.collector.config.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Collection of all the betting api methods from Betfair
 *
 * @author Claudio Paolicelli
 */
@Slf4j
@Service
public class Cerberus {
    private final Gson gson;

    public Cerberus() {
        this.gson = new GsonBuilder().create();
    }

    /**
     * Call to Cerberus project in Mercurius to retrieve all the predictions for match odds and correct scores.
     *
     * @param homeTeamId    mercurius home team id
     * @param awayTeamId    mercurius away team id
     * @param date          event start date time
     * @param competitionId mercurius competition id
     * @return list of prediction for correct scores and match odds.
     */
    public List<Prediction> getPredictions(Long homeTeamId, Long awayTeamId, String date, Long competitionId) {
        Map<String, Set<PredictionRequest>> request = new HashMap<>();
        request.put(competitionId.toString(), Collections.singleton(new PredictionRequest(161325935L, homeTeamId, awayTeamId, date)));
        String jsonBet = gson.toJson(request);
        List<Prediction> result = new ArrayList<>();
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
            if (response.getCode() == 200) {
                result.addAll(EventAdapter.getPredictionsTable(response.getBody()));
            } else {
                log.error("Mercurius code={} response={}", response.getCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error getPredictions for homeTeamId={} awayTeamId={} date={} competitionId={}", homeTeamId, awayTeamId, date, competitionId, e);
        }
        return result;
    }
}
