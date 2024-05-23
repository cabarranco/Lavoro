package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.ItemClass;
import com.asbresearch.betfair.ref.util.InstantDeserializer;
import com.asbresearch.betfair.ref.util.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class StatementItem {
    @EqualsAndHashCode.Include
    private final String refId;
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant itemDate;
    private final double amount;
    private final double balance;
    private final ItemClass itemClass;
    private final Map<String, String> itemClassData;
    private final StatementLegacyData legacyData;

    @JsonCreator(mode = PROPERTIES)
    public StatementItem(@JsonProperty("refId") String refId,
                         @JsonProperty("itemDate") Instant itemDate,
                         @JsonProperty("amount") double amount,
                         @JsonProperty("balance") double balance,
                         @JsonProperty("itemClass") ItemClass itemClass,
                         @JsonProperty("itemClassData") Map<String, String> itemClassData,
                         @JsonProperty("legacyData") StatementLegacyData legacyData) {
        this.refId = refId;
        this.itemDate = itemDate;
        this.amount = amount;
        this.balance = balance;
        this.itemClass = itemClass;
        this.itemClassData = itemClassData != null ? ImmutableMap.copyOf(itemClassData) : ImmutableMap.of();
        this.legacyData = legacyData;
    }
}
