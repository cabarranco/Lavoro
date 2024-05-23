package com.asb.analytics;

import com.asb.analytics.api.adapters.EventAdapter;
import com.asb.analytics.bigquery.BigQueryServices;
import com.asb.analytics.bigquery.Row;
import com.asb.analytics.bigquery.RowContent;
import com.asb.analytics.controllers.EventsController;
import com.asb.analytics.controllers.MarketsController;
import com.asb.analytics.domain.InternalDictionary;
import com.asb.analytics.domain.betfair.*;
import com.asb.analytics.logs.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

public class AnalyticsTick implements Callable<Boolean> {

    private String sessionToken;
    private String eventId;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private Gson gson = new GsonBuilder().create();
    private final String gcloudToken;

    AnalyticsTick(
            String sessionToken,
            String eventId,
            String gcloudToken) {
        this.eventId = eventId;
        this.sessionToken = sessionToken;
        this.gcloudToken = gcloudToken;
    }

    @Override
    public Boolean call() {

            EventsController eventsController = new EventsController(sessionToken, eventId);
            MarketsController marketsController = new MarketsController(sessionToken);

            // GET EVENTS

        List<EventResponse> events = new ArrayList<>();
        try {
            events = eventsController.getSoccerEvents();
        } catch (Exception e) {
            Logger.planeLog().error("RETRIEVING GET SOCCER EVENTS ERROR");
            Logger.log().error(e.getMessage());
        }

        EventResponse eventResponse = null;

            for (EventResponse response : events) {
                if(eventId.equals(response.getEvent().getId())) {
                    eventResponse = response;
                }
            }

            if(eventResponse != null) {

                boolean isLiveEvent = eventResponse.isLive();

                List<MarketCatalogue> catalogues = new ArrayList<>();
                try {
                    catalogues = marketsController.getMarketCatalogues(eventResponse);
                } catch (Exception e) {
                    Logger.planeLog().error("RETRIEVING MARKET CATALOGUE ERROR");
                    Logger.log().error(e.getMessage());
                }

                List<String> marketIds = EventAdapter.getMarketIds(catalogues);

                HashMap<String, Double> lastSavedRunners = new HashMap<>();

                boolean oddsChanged = false;
                List<MarketBook> marketBooks = new ArrayList<>();

                do {

                    List<Row> values = new ArrayList<>();

                    // GET MARKET BOOKS

                    try {
                        marketBooks = marketsController.getMarketBooks(marketIds);
                    } catch (Exception e) {
                        Logger.planeLog().error("RETRIEVING MARKET BOOKS ERROR");
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

                            if (marketName.equalsIgnoreCase("Asian Handicap")) {
                                runnerName += " " + handicap.replaceAll("_", "");
                            }

                            Integer marketType = InternalDictionary.MARKET_TYPE.get(marketName);
                            int selection = 0;

                            if (InternalDictionary.SELECTIONS.get(marketName) != null
                                    && InternalDictionary.SELECTIONS.get(marketName).get(runnerName) != null) {
                                selection = InternalDictionary.SELECTIONS.get(marketName).get(runnerName);
                            }

                            if (availableToBack.size() > 0) {

                                Double previousPriceB1 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_1");
                                Double previousSizeB1 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_1");

                                if ( (previousPriceB1 == null) || (previousSizeB1 == null)
                                        || !previousPriceB1.equals(availableToBack.get(0).getPrice())
                                        || (isLiveEvent && !previousSizeB1.equals(availableToBack.get(0).getSize()))) {
                                    oddsChanged = true;
                                }

                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_1", availableToBack.get(0).getPrice());
                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_1", availableToBack.get(0).getSize());
                            }

                            if (availableToBack.size() > 1) {

                                Double previousPriceB2 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_2");
                                Double previousSizeB2 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_2");

                                if ((previousPriceB2 == null) || (previousSizeB2 == null)
                                        || !previousPriceB2.equals(availableToBack.get(1).getPrice())
                                        || (isLiveEvent && !previousSizeB2.equals(availableToBack.get(1).getSize()))) {
                                    oddsChanged = true;
                                }


                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_2", availableToBack.get(1).getPrice());
                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_2", availableToBack.get(1).getSize());
                            }

                            if (availableToBack.size() > 2) {

                                Double previousPriceB3 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_3");
                                Double previousSizeB3 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_3");

                                if ((previousPriceB3 == null) || (previousSizeB3 == null)
                                        || !previousPriceB3.equals(availableToBack.get(2).getPrice())
                                        || (isLiveEvent && !previousSizeB3.equals(availableToBack.get(2).getSize()))) {
                                    oddsChanged = true;
                                }


                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_3", availableToBack.get(2).getPrice());
                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_3", availableToBack.get(2).getSize());
                            }

                            if (availableToLay.size() > 0) {

                                Double previousPriceL1 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_1");
                                Double previousSizeL1 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_1");

                                if ((previousPriceL1 == null) || (previousSizeL1 == null)
                                        || !previousPriceL1.equals(availableToLay.get(0).getPrice())
                                        || (isLiveEvent && !previousSizeL1.equals(availableToLay.get(0).getSize())) ) {
                                    oddsChanged = true;
                                }


                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_1", availableToLay.get(0).getPrice());
                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_1", availableToLay.get(0).getSize());
                            }

                            if (availableToLay.size() > 1) {

                                Double previousPriceL2 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_2");
                                Double previousSizeL2 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_2");

                                if ((previousPriceL2 == null) || (previousSizeL2 == null)
                                        || !previousPriceL2.equals(availableToLay.get(1).getPrice())
                                        || (isLiveEvent && !previousSizeL2.equals(availableToLay.get(1).getSize()))) {
                                    oddsChanged = true;
                                }


                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_2", availableToLay.get(1).getPrice());
                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_2", availableToLay.get(1).getSize());
                            }

                            if (availableToLay.size() > 2) {

                                Double previousPriceL3 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_3");
                                Double previousSizeL3 = lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_3");

                                if ((previousPriceL3 == null) || (previousSizeL3 == null)
                                        || !previousPriceL3.equals(availableToLay.get(2).getPrice())
                                        || (isLiveEvent && !previousSizeL3.equals(availableToLay.get(2).getSize())) ) {
                                    oddsChanged = true;
                                }


                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_3", availableToLay.get(2).getPrice());
                                lastSavedRunners.put(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_3", availableToLay.get(2).getSize());
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
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_1"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_1"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_2"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_2"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_price_3"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_back_size_3"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_1"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_1"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_2"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_2"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_price_3"),
                                        lastSavedRunners.get(marketBook.getMarketId() + runner.getSelectionId() + handicap + "_lay_size_3"),
                                        dateTimeFormat.format(marketBook.getDate())
                                );


                                values.add(new Row<>(content));
                            }
                        }
                    }

                    if (values.size() > 0) {
                        BigQueryServices.oddsSizes()
                                .insertBigQuery(gson, values, gcloudToken, "event_prices_sizes");
                    } else {
                        events.clear();
                    }
                } while (marketBooks.size() > 0);

                System.out.println("Event not live anymore");
            }

            return false;
    }
}

