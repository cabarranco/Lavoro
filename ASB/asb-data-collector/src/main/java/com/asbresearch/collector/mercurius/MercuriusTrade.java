package com.asbresearch.collector.mercurius;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class MercuriusTrade {
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
}
