package com.asb.analytics;

import com.asb.analytics.api.HttpConnector;
import com.asb.analytics.api.SimpleResponse;
import com.asb.analytics.bigquery.BigQueryInsertLine;
import com.asb.analytics.bigquery.MarketCatalogue;
import com.asb.analytics.bigquery.Row;
import com.asb.analytics.controllers.MarketsController;
import com.asb.analytics.domain.InternalDictionary;
import com.asb.analytics.domain.betfair.EventResponse;
import com.asb.analytics.domain.betfair.Runner;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class MarketCatalogueCollector{

    private final String sessionToken;
    private final List<Integer> eventIds;
    private final List<String> allEventsIds;
    private final BigQuery bigQuery;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final List<EventResponse> eventResponses;
    private final Gson gson = new GsonBuilder().create();
    private final String gcloudToken;

    MarketCatalogueCollector(
            String sessionToken,
            List<Integer> eventIds,
            BigQuery bigQuery,
            List<EventResponse> eventResponses,
            List<String> allEventsIds,
            String gcloudToken
    ) {
        this.bigQuery = bigQuery;
        this.sessionToken = sessionToken;
        this.eventIds = eventIds;
        this.eventResponses = eventResponses;
        this.allEventsIds = allEventsIds;
        this.gcloudToken = gcloudToken;
    }

    Boolean collect() {

        MarketsController marketsController = new MarketsController(sessionToken);

        // GET EVENTS

        String concatIds = allEventsIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String query = String.format("SELECT selectionId, marketId, selection FROM `asbanalytics.betstore.market_catalogue` where eventId IN (%s)", concatIds);
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

        List<Row> rows = new ArrayList<>();

        Iterable<FieldValueList> iterable = new FluentIterable<FieldValueList>() {
            @Override
            public Iterator<FieldValueList> iterator() {
                return Collections.emptyIterator();
            }
        };

        try {
            iterable = bigQuery.query(queryConfig).iterateAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (EventResponse response : eventResponses) {
            if(response != null) {

                List<com.asb.analytics.domain.betfair.MarketCatalogue> catalogues = new ArrayList<>();
                try {
                    catalogues = marketsController.getMarketCatalogues(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (com.asb.analytics.domain.betfair.MarketCatalogue catalogue : catalogues) {
                    for (Runner runner : catalogue.getRunners()) {

                        String runnerName = runner.getRunnerName();

                        if (runnerName.equalsIgnoreCase(response.getEvent().getTeam1())) {
                            runnerName = "Home";
                        } else if (runnerName.equalsIgnoreCase(response.getEvent().getTeam2())) {
                            runnerName = "Away";
                        } else if ("The Draw".equalsIgnoreCase(runnerName)) {
                            runnerName = "Draw";
                        }

                        if ("Asian Handicap".equalsIgnoreCase(catalogue.getMarketName())) {

                            double handicap = runner.getHandicap();
                            if (handicap == 1.5 || handicap == -1.5 || handicap == 0.5 || handicap == -0.5) {
                                if ("home".equalsIgnoreCase(runnerName) || "away".equalsIgnoreCase(runnerName)) {
                                    runnerName = runnerName + " AH " + ((handicap) > 0 ? "+" + handicap : handicap);
                                } else continue;
                            } else continue;
                        }

                        Integer marketType = InternalDictionary.MARKET_TYPE.get(catalogue.getMarketName());
                        int selection = 0;

                        if (InternalDictionary.SELECTIONS.get(catalogue.getMarketName()) != null
                                && InternalDictionary.SELECTIONS.get(catalogue.getMarketName()).get(runnerName) != null) {
                            selection = InternalDictionary.SELECTIONS.get(catalogue.getMarketName()).get(runnerName);
                        }

                        boolean found = false;

                        // Print the results.
                        for (FieldValueList row : iterable) {
                            found = row.get(0).getLongValue() == runner.getSelectionId();
                            found = found && row.get(1).getStringValue().equalsIgnoreCase(catalogue.getMarketId());
                            found = found && row.get(2).getLongValue() == selection;

                            if (found)
                                break;
                        }

                        if (!found) {
                            String frequency = eventIds.contains(Integer.valueOf(response.getEvent().getId()))
                                    ? "TICK" : "30SECONDS";

                            rows.add(new Row<>(
                                    new MarketCatalogue(
                                            catalogue.getMarketId(),
                                            catalogue.getMarketName(),
                                            runner.getSelectionId(),
                                            runnerName,
                                            Integer.valueOf(response.getEvent().getId()),
                                            response.getEvent().getName(),
                                            dateFormat.format(response.getEvent().getOpenDate()),
                                            catalogue.getCompetition().getName(),
                                            selection,
                                            frequency
                                    )
                            ));
                        }
                    }
                }
            }
        }

        if (rows.size() > 0) {

            String marketBody = gson.toJson(new BigQueryInsertLine(rows));

            try {

                SimpleResponse response = HttpConnector
                        .connect("https://content-bigquery.googleapis.com/bigquery/v2/projects/asbanalytics/datasets/betstore/tables/market_catalogue/insertAll?alt=json")
                        .timeout(5000)
                        .header("Authorization", "Bearer " + gcloudToken)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .body(marketBody)
                        .method(HttpConnector.POST)
                        .execute();

                if (response.getCode() == 200)
                    System.out.println("Market catalogue saved");
                else System.out.println("Error saving market catalogue");
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else System.out.println("No Market catalogue to save");

        return false;
    }
}

