package com.asbresearch.pulse.model;

import com.asbresearch.pulse.util.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EventCriteria {
    private static final Pattern SCORE_PATTERN = Pattern.compile("[0-9]+[-][0-9]+");

    private final String type;
    private final Set<String> includeCompetitions;
    private final Set<String> excludeCompetitions;
    private final boolean live;
    private final Set<String> currentLiveScores;
    private final String previousScore;
    private final boolean preLive;
    private final Integer startFromKickOff;
    private final Integer endFromKickOff;


    @JsonCreator(mode = PROPERTIES)
    public EventCriteria(@JsonProperty("type") String type,
                         @JsonProperty("includeCompetitions") Set<String> includeCompetitions,
                         @JsonProperty("excludeCompetitions") Set<String> excludeCompetitions,
                         @JsonProperty("live") boolean live,
                         @JsonProperty("currentLiveScores") Set<String> currentLiveScores,
                         @JsonProperty("previousScore") String previousScore,
                         @JsonProperty("preLive") boolean preLive,
                         @JsonProperty("startFromKickOff") Integer startFromKickOff,
                         @JsonProperty("endFromKickOff") Integer endFromKickOff) {
        if (previousScore != null) {
            previousScore = previousScore.replaceAll("\\s", "");
        }
        if (currentLiveScores == null) {
            currentLiveScores = emptySet();
        }
        currentLiveScores = currentLiveScores.stream().map(s -> s.replaceAll("\\s", "")).collect(ImmutableSet.toImmutableSet());
        checkNotNull(type, "Event type must be provided");
        checkArgument(isValidScores(currentLiveScores), "Invalid currentLiveScores=" + currentLiveScores);
        checkArgument(isValidScore(previousScore), "Invalid score=" + previousScore);
        checkArgument(isValidDistancFromKickOffTime(startFromKickOff), "startFromKickOff must be positive integer");
        checkArgument(isValidDistancFromKickOffTime(endFromKickOff), "endFromKickOff must be positive integer");
        checkArgument(endFromKickOff > startFromKickOff, "endFromKickOff must be greater than startFromKickOff");

        this.type = type;
        this.includeCompetitions = includeCompetitions == null ? ImmutableSet.of() : ImmutableSet.copyOf(includeCompetitions);
        this.excludeCompetitions = excludeCompetitions == null ? ImmutableSet.of() : ImmutableSet.copyOf(excludeCompetitions);
        this.live = live;
        this.currentLiveScores = currentLiveScores;
        this.previousScore = previousScore;
        this.preLive = preLive;
        this.startFromKickOff = startFromKickOff;
        this.endFromKickOff = endFromKickOff;
    }

    protected static EventCriteria liveAndPreLifeFootball(Set<String> includeCompetitions, Set<String> currentLiveScores, String previousScore, int endFromKickOff) {
        return new EventCriteria(Constants.SOCCER, includeCompetitions, emptySet(), true, currentLiveScores, previousScore, true, 0, endFromKickOff);
    }

    private boolean isValidDistancFromKickOffTime(Integer distance) {
        if (distance != null && distance < 0) {
            return false;
        }
        return true;
    }

    private boolean isValidScores(Set<String> currentLiveScores) {
        if (!currentLiveScores.isEmpty()) {
            return currentLiveScores.stream().allMatch(this::isValidScore);
        }
        return true;
    }

    private boolean isValidScore(String score) {
        if (score != null) {
            return SCORE_PATTERN.matcher(score).matches();
        }
        return true;
    }
}
