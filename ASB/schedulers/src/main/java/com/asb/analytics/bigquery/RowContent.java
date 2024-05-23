package com.asb.analytics.bigquery;

public class RowContent {

    int eventId;
    boolean inplay;
    String marketid;
    int selection;
    long selectionid;
    String status;
    Double totalMatched;

    Double back_price_1;
    Double back_size_1;
    Double back_price_2;
    Double back_size_2;
    Double back_price_3;
    Double back_size_3;

    Double lay_price_1;
    Double lay_size_1;
    Double lay_price_2;
    Double lay_size_2;
    Double lay_price_3;
    Double lay_size_3;

    String timestamp;

    public RowContent(
            int eventId,
            boolean inplay,
            String marketid,
            int selection,
            long selectionid,
            String status,
            Double totalMatched,
            Double back_price_1,
            Double back_size_1,
            Double back_price_2,
            Double back_size_2,
            Double back_price_3,
            Double back_size_3,
            Double lay_price_1,
            Double lay_size_1,
            Double lay_price_2,
            Double lay_size_2,
            Double lay_price_3,
            Double lay_size_3,
            String timestamp
    ) {
        this.eventId = eventId;
        this.inplay = inplay;
        this.marketid = marketid;
        this.selection = selection;
        this.selectionid = selectionid;
        this.status = status;
        this.totalMatched = totalMatched;
        this.back_price_1 = back_price_1;
        this.back_size_1 = back_size_1;
        this.back_price_2 = back_price_2;
        this.back_size_2 = back_size_2;
        this.back_price_3 = back_price_3;
        this.back_size_3 = back_size_3;
        this.lay_price_1 = lay_price_1;
        this.lay_size_1 = lay_size_1;
        this.lay_price_2 = lay_price_2;
        this.lay_size_2 = lay_size_2;
        this.lay_price_3 = lay_price_3;
        this.lay_size_3 = lay_size_3;
        this.timestamp = timestamp;
    }
}