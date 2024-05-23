package com.asbresearch.pulse.service;

import com.asbresearch.betfair.esa.cache.util.LevelPriceSize;
import com.asbresearch.betfair.esa.cache.util.PriceSize;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelectionPrice {
    public static final SelectionPrice NULL = new SelectionPrice(null, null);

    private final PriceSize back;
    private final PriceSize lay;

    public static SelectionPrice of(LevelPriceSize back, LevelPriceSize lay) {
        return new SelectionPrice(PriceSize.from(back), PriceSize.from(lay));
    }

    public static SelectionPrice back(PriceSize back) {
        return new SelectionPrice(back, null);
    }

    public static SelectionPrice lay(PriceSize lay) {
        return new SelectionPrice(null, lay);
    }

    public static double getLayLimitPrice(double currentPrice) {
        if (currentPrice >= 1.01 && currentPrice < 2) {
            return 2.0;
        } else if (currentPrice >= 2 && currentPrice < 3) {
            return 3.0;
        } else if (currentPrice >= 3 && currentPrice < 4) {
            return 4.0;
        } else if (currentPrice >= 4 && currentPrice < 6) {
            return 6.0;
        } else if (currentPrice >= 6 && currentPrice < 10) {
            return 10.0;
        } else if (currentPrice >= 10 && currentPrice < 20) {
            return 20.0;
        } else if (currentPrice >= 20 && currentPrice < 30) {
            return 30.0;
        } else if (currentPrice >= 30 && currentPrice < 50) {
            return 50.0;
        } else if (currentPrice >= 50 && currentPrice < 100) {
            return 100.0;
        } else {
            return 1000.0;
        }
    }
}
