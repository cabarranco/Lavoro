package com.asbresearch.betfair.esa.cache.market;

import com.asbresearch.betfair.esa.cache.util.LevelPriceSize;
import com.asbresearch.betfair.esa.cache.util.PriceSize;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class MarketRunnerPrices {
    List<PriceSize> atl = Collections.emptyList();
    List<PriceSize> atb = Collections.emptyList();
    List<PriceSize> trd = Collections.emptyList();
    List<PriceSize> spb = Collections.emptyList();
    List<PriceSize> spl = Collections.emptyList();

    List<LevelPriceSize> batb = Collections.emptyList();
    List<LevelPriceSize> batl = Collections.emptyList();
    List<LevelPriceSize> bdatb = Collections.emptyList();
    List<LevelPriceSize> bdatl = Collections.emptyList();

    double ltp;
    double spn;
    double spf;
    double tv;
}