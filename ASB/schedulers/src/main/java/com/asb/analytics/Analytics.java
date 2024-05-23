package com.asb.analytics;

import com.asb.analytics.api.adapters.EventAdapter;
import com.asb.analytics.bigquery.BigQueryServices;
import com.asb.analytics.bigquery.Row;
import com.asb.analytics.bigquery.RowContent;
import com.asb.analytics.controllers.MarketsController;
import com.asb.analytics.domain.InternalDictionary;
import com.asb.analytics.domain.betfair.*;
import com.asb.analytics.logs.EventLogModel;
import com.asb.analytics.logs.Logger;
import com.asb.analytics.mongo.MongoConnector;
import com.asb.analytics.mongo.utils.MongoUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class Analytics implements Callable<Boolean> {

    private String sessionToken;
    private List<Integer> eventIds;
    private final List<EventResponse> eventResponses;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private Gson gson = new GsonBuilder().create();
    private final HashMap<String, Double> lastSavedRunners;
    private final String gcloudToken;
    private final MongoDatabase database = new MongoConnector().connect();

    Analytics(
            String sessionToken,
            List<Integer> eventId,
            List<EventResponse> eventResponses,
            HashMap<String, Double> lastSavedRunners,
            String gcloudToken
    ) {
        this.eventIds = eventId;
        this.sessionToken = sessionToken;
        this.eventResponses = eventResponses;
        this.lastSavedRunners = lastSavedRunners;
        this.gcloudToken = gcloudToken;
    }

    @Override
    public Boolean call() {

        Logger.log().info("Storing prices sizes...");
        MarketsController marketsController = new MarketsController(sessionToken);

        List<EventLogModel> eventLogs = MongoUtils.query(database).getEventsLog();

        // GET EVENTS
        List<Row> values = new ArrayList<>();

        for (EventResponse eventResponse : eventResponses) {
            if(eventResponse != null && !eventIds.contains(Integer.valueOf(eventResponse.getEvent().getId()))) {

                boolean isLogActive = eventLogs.stream()
                        .anyMatch(log -> log.getEventId().equals(eventResponse.getEvent().getId()));

                boolean isLiveEvent = eventResponse.isLive();

                List<MarketCatalogue> catalogues = new ArrayList<>();
                try {
                    catalogues = marketsController.getMarketCatalogues(eventResponse);
                } catch (Exception e) {
                    Logger.planeLog().error("RETRIEVING MARKET CATALOGUE ERROR");
                    Logger.log().error(e.getMessage());
                }

                List<String> marketIds = EventAdapter.getMarketIds(catalogues);

                boolean oddsChanged = false;

                Logger.log(isLogActive).info(String.format("Market catalogues for event %s(%s): %d",
                        eventResponse.getEvent().getName(), eventResponse.getEvent().getId(), catalogues.size())
                );

                // GET MARKET BOOKS

                List<MarketBook> marketBooks = new ArrayList<>();
                try {
                    marketBooks = marketsController.getMarketBooks(marketIds);
                } catch (Exception e) {
                    Logger.planeLog().error("RETRIEVING MARKET BOOK ERROR");
                    Logger.log().error(e.getMessage());
                }

                for (MarketBook marketBook : marketBooks) {

                    Optional<MarketCatalogue> optionalCatalogue = catalogues.stream()
                            .filter(mc -> mc.getMarketId().equalsIgnoreCase(marketBook.getMarketId()))
                            .findFirst();

                    MarketCatalogue marketCatalogue = null;
                    String marketName = "";

                    if (optionalCatalogue.isPresent()) {
                        marketCatalogue = optionalCatalogue.get();
                        marketName = marketCatalogue.getMarketName();
                    }


                for (Runner runner : marketBook.getRunners()) {

                    List<PriceSize> availableToBack = runner.getEx().getAvailableToBack();
                    List<PriceSize> availableToLay = runner.getEx().getAvailableToLay();

                    String runnerName = "";

                    if (marketCatalogue != null) {
                        Optional<Runner> optionalRunner = marketCatalogue.getRunners().stream()
                                .filter(run -> run.getSelectionId() == runner.getSelectionId())
                                .findFirst();

                        if (optionalRunner.isPresent()) {
                            runnerName = optionalRunner.get().getRunnerName();
                        }

                        if ("The Draw".equalsIgnoreCase(runnerName)) {
                            runnerName = "draw";
                        }

                        if (runnerName.equalsIgnoreCase(eventResponse.getEvent().getTeam1())) {
                            runnerName = "home";
                        }

                        if (runnerName.equalsIgnoreCase(eventResponse.getEvent().getTeam2())) {
                            runnerName = "away";
                        }
                    }

                    String handicap = "";

                    if (marketName.equalsIgnoreCase("Asian Handicap")) {

                        double h = runner.getHandicap();
                        // if is not one of these handicap we don't need it
                        if (h != 1.5 && h != -1.5 && h != 0.5 && h != -0.5)
                            continue;
                        else handicap = "_" + h;
                    }

                    int selection = 0;

                    if (marketName.equalsIgnoreCase("Asian Handicap")) {
                        runnerName += " " + handicap.replaceAll("_", "");
                    }

                    if (InternalDictionary.SELECTIONS.get(marketName) != null
                            && InternalDictionary.SELECTIONS.get(marketName).get(runnerName) != null) {
                        selection = InternalDictionary.SELECTIONS.get(marketName).get(runnerName);
                    }

                    Logger.planeLog(isLogActive).info("Runner: " + runnerName + " selection: " + selection);

                    if (availableToBack.size() > 0) {

                        Double previousPriceB1 = lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_back_price_1");
                        Double previousSizeB1 = lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_back_size_1");

                        double currentPriceB1 = availableToBack.get(0).getPrice();
                        double currentSizeB1 = availableToBack.get(0).getSize();

                        oddsChanged = isOddChanged(
                                previousSizeB1,
                                currentSizeB1,
                                previousPriceB1,
                                currentPriceB1,
                                isLiveEvent
                        );

                        logBackPriceSize(
                                previousSizeB1,
                                currentSizeB1,
                                previousPriceB1,
                                currentPriceB1,
                                oddsChanged,
                                isLogActive
                        );

                        lastSavedRunners.put(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_back_price_1", currentPriceB1);
                        lastSavedRunners.put(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_back_size_1", currentSizeB1);
                    }

                    if (availableToLay.size() > 0) {

                        Double previousPriceL1 = lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_lay_price_1");
                        Double previousSizeL1 = lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_lay_size_1");

                        double currentPriceL1 = availableToLay.get(0).getPrice();
                        double currentSizeL1 = availableToLay.get(0).getSize();

                        oddsChanged = isOddChanged(
                                previousSizeL1,
                                currentSizeL1,
                                previousPriceL1,
                                currentPriceL1,
                                isLiveEvent
                        );

                        logLayPriceSize(
                                previousSizeL1,
                                currentSizeL1,
                                previousPriceL1,
                                currentPriceL1,
                                oddsChanged,
                                isLogActive
                        );

                        lastSavedRunners.put(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_lay_price_1", availableToLay.get(0).getPrice());
                        lastSavedRunners.put(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_lay_size_1", availableToLay.get(0).getSize());
                    }


                    if (oddsChanged) {
                        oddsChanged = false;
                        RowContent content = new RowContent(
                                Integer.valueOf(eventResponse.getEvent().getId()),
                                marketBook.isInplay(),
                                marketBook.getMarketId(),
                                selection,
                                runner.getSelectionId(),
                                marketBook.getStatus(),
                                marketBook.getTotalMatched(),
                                lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_back_price_1"),
                                lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_back_size_1"),
                                null, null, null, null,
                                lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_lay_price_1"),
                                lastSavedRunners.get(marketBook.getMarketId() + "_" + runner.getSelectionId() + "_" + selection + "_lay_size_1"),
                                null, null, null, null,
                                dateTimeFormat.format(marketBook.getDate())
                        );

                        values.add(new Row<>(content));
                    }
                }
            }
            }
        }

        if (values.size() > 0) {
            BigQueryServices.oddsSizes()
                    .insertBigQuery(gson, values, gcloudToken, "event_prices_sizes");
        }

        return false;
    }

    private void logBackPriceSize(
            Double previousSize,
            Double currentSize,
            Double previousPrice,
            Double currentPrice,
            boolean oddsChanged,
            boolean isLogActive
    ) {

        if (oddsChanged) {
            Logger.planeLog(isLogActive).error(String.format("Previous Price B1: %2.1f - Current Price B1: %2.1f", previousPrice, currentPrice));
            Logger.planeLog(isLogActive).error(String.format("Previous Size B1: %2.1f - Current Size B1: %2.1f", previousSize, currentSize));
        } else {
            Logger.planeLog(isLogActive).info(String.format("Previous Price B1: %2.1f - Current Price B1: %2.1f", previousPrice, currentPrice));
            Logger.planeLog(isLogActive).info(String.format("Previous Size B1: %2.1f - Current Size B1: %2.1f", previousSize, currentSize));
        }
    }

    private void logLayPriceSize(
            Double previousSize,
            Double currentSize,
            Double previousPrice,
            Double currentPrice,
            boolean oddsChanged,
            boolean isLogActive
    ) {


        if (oddsChanged) {
            Logger.planeLog(isLogActive).error(String.format("Previous Price L1: %2.1f - Current Price L1: %2.1f", previousPrice, currentPrice));
            Logger.planeLog(isLogActive).error(String.format("Previous Size L1: %2.1f - Current Size L1: %2.1f", previousSize, currentSize));
        } else {
            Logger.planeLog(isLogActive).info(String.format("Previous Price L1: %2.1f - Current Price L1: %2.1f", previousPrice, currentPrice));
            Logger.planeLog(isLogActive).info(String.format("Previous Size L1: %2.1f - Current Size L1: %2.1f", previousSize, currentSize));
        }
    }

    private boolean isOddChanged(
            Double previousSize,
            Double currentSize,
            Double previousPrice,
            Double currentPrice,
            boolean isLiveEvent
    ) {
        boolean oddsChanged = false;
        if ((previousPrice == null) || (previousSize == null)
                || !previousPrice.equals(currentPrice)
                || (isLiveEvent && !previousSize.equals(currentSize)) ) {
            oddsChanged = true;
        }

        return oddsChanged;
    }
}

