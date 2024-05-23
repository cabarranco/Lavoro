package com.asbresearch.collector.mercurius;

import com.opencsv.bean.CsvBindByName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AsbMercuriusTrade {
    @CsvBindByName(column = "event_start_at")
    private String eventStartAt;
    @CsvBindByName(column = "execution_start")
    private String executionStart;
    @CsvBindByName(column = "execution_end")
    private String executionEnd;
    @CsvBindByName(column = "market_id")
    private String marketId;
    @CsvBindByName(column = "selection_id")
    private String selectionId;
    @CsvBindByName(column = "asian_handicap")
    private String asianHandicap;
    @CsvBindByName(column = "home")
    private String home;
    @CsvBindByName(column = "away")
    private String away;
    @CsvBindByName(column = "amount")
    private String amount;
    @CsvBindByName(column = "odds")
    private String odds;
    @CsvBindByName(column = "betfair_event_id")
    private String eventId;
    @CsvBindByName(column = "asb_has_hist_data")
    private boolean hasHistoricalData;
    @CsvBindByName(column = "asb_has_inplay_features")
    private boolean hasInplayFeatures;
    @CsvBindByName(column = "asb_has_price_analytics")
    private boolean hasPriceAnalytics;

    private String asbSelectionId;
}
