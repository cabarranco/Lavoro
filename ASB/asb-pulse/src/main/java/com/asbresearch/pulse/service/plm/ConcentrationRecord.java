package com.asbresearch.pulse.service.plm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ConcentrationRecord {
    @EqualsAndHashCode.Include
    private final String id;
    private final double maxTradingDayAvailableBalance;
    private final double usedBalance;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ConcentrationRecord(@JsonProperty("id") String id,
                               @JsonProperty("maxTradingDayAvailableBalance") double maxTradingDayAvailableBalance,
                               @JsonProperty("useBalance") double usedBalance) {

        Preconditions.checkNotNull(id, "id must be provided");
        this.id = id;
        this.maxTradingDayAvailableBalance = maxTradingDayAvailableBalance;
        this.usedBalance = usedBalance;
    }

    public static ConcentrationRecord of(String id, double maxTradingDayAvailableBalance, double usedBalance) {
        return new ConcentrationRecord(id, maxTradingDayAvailableBalance, usedBalance);
    }

    public ConcentrationRecord updateBalance(double maxTradingDayAvailableBalance, double usedBalance) {
        return of(id, Math.max(this.maxTradingDayAvailableBalance, maxTradingDayAvailableBalance), this.usedBalance + usedBalance);
    }
}
