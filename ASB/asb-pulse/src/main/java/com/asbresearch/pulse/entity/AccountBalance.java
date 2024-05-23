package com.asbresearch.pulse.entity;

import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;


@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountBalance {
    @EqualsAndHashCode.Include
    private final String username;
    @EqualsAndHashCode.Include
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant tradeDate;
    private final Double availableToBet;
    private final String currency;

    @JsonCreator(mode = PROPERTIES)
    public AccountBalance(@JsonProperty("username") String username,
                          @JsonProperty("tradeDate") Instant tradeDate,
                          @JsonProperty("availableToBet") Double availableToBet,
                          @JsonProperty("currency") String currency) {

        Preconditions.checkNotNull(username, "username must be provide");
        Preconditions.checkNotNull(tradeDate, "tradeDate must be provide");
        Preconditions.checkNotNull(availableToBet, "availableToBet must be provide");
        Preconditions.checkNotNull(currency, "currency must be provide");

        this.username = username;
        this.tradeDate = tradeDate;
        this.availableToBet = availableToBet;
        this.currency = currency;
    }

    public static AccountBalance of(String username, double availableToBet, String currency, Instant tradeDate) {
        return new AccountBalance(username, tradeDate, availableToBet, currency);
    }
}
