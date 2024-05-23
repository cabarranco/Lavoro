package com.asbresearch.pulse.model;

import com.asbresearch.pulse.service.MarketSelection;
import com.asbresearch.pulse.service.SelectionPrice;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpportunitySelection {
    private final MarketSelection marketSelection;
    private final SelectionPrice selectionPrice;

    public static OpportunitySelection of(MarketSelection marketSelection, SelectionPrice selectionPrice) {
        return new OpportunitySelection(marketSelection, selectionPrice);
    }
}
