package com.asbresearch.betfair.ref.entities;

import com.asbresearch.betfair.ref.enums.PersistenceType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class UpdateInstruction {
    @EqualsAndHashCode.Include
    private final String betId;
    private final PersistenceType newPersistenceType;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UpdateInstruction(@JsonProperty("betId") String betId,
                             @JsonProperty("newPersistenceType") PersistenceType newPersistenceType) {
        this.betId = betId;
        this.newPersistenceType = newPersistenceType;
    }
}
