package com.asbresearch.pulse.mapping;

import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.pulse.util.Constants;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@JsonSerialize(using = UserRunnerCodeSerializer.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class UserRunnerCode {
    private static final Pattern USER_RUNNER_ID_PATTERN = Pattern.compile("(MO|CS|AH|OU\\d+)[.]([A-Z0-9-+]+)[.](B|L)");

    private final String market;
    private final String selection;
    private final Side side;
    @EqualsAndHashCode.Include
    private final String code;

    public UserRunnerCode(String userRunnerCode) {
        Preconditions.checkNotNull(userRunnerCode, "userRunnerCode must be provided");
        Matcher matcher = USER_RUNNER_ID_PATTERN.matcher(userRunnerCode);
        Preconditions.checkArgument(matcher.matches(), String.format("userRunnerId=%s not in correct format", userRunnerCode));
        market = matcher.group(1);
        selection = matcher.group(2);
        if (Constants.BACK.equals(matcher.group(3))) {
            side = Side.BACK;
        } else {
            side = Side.LAY;
        }
        this.code = userRunnerCode;
    }
}
