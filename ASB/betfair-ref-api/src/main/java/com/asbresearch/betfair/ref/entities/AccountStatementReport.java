package com.asbresearch.betfair.ref.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class AccountStatementReport {
    private final List<StatementItem> accountStatement;
    private final boolean moreAvailable;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AccountStatementReport(@JsonProperty("accountStatement") List<StatementItem> accountStatement,
                                  @JsonProperty("moreAvailable") boolean moreAvailable) {
        this.accountStatement = accountStatement != null ? ImmutableList.copyOf(accountStatement) : ImmutableList.of();
        this.moreAvailable = moreAvailable;
    }
}
