package com.asbresearch.betfair.inplay.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class MatchScore {
    private final int home;
    private final int away;
    private final List<Integer> homeScores;
    private final List<Integer> awayScores;

    private MatchScore(List<Integer> homeScores, List<Integer> awayScores) {
        Preconditions.checkNotNull(homeScores, "Home scores is required");
        Preconditions.checkNotNull(awayScores, "Away scores is required");
        Preconditions.checkArgument(!homeScores.isEmpty(), "Home scores cannot be empty");
        Preconditions.checkArgument(!awayScores.isEmpty(), "Away scores cannot be empty");
        Preconditions.checkArgument(homeScores.size() == awayScores.size(), "Home and Away scores size must be same");

        home = homeScores.get(homeScores.size() - 1);
        away = awayScores.get(awayScores.size() - 1);

        this.homeScores = ImmutableList.copyOf(homeScores);
        this.awayScores = ImmutableList.copyOf(awayScores);
    }

    public String currentScore() {
        return String.format("%d-%d", home, away);
    }

    public List<String> scores() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < homeScores.size(); i++) {
            result.add(String.format("%d-%d", homeScores.get(i), awayScores.get(i)));
        }
        return result;
    }

    public String previousScore() {
        if (homeScores.size() == 1) {
            return "0-0";
        }
        return String.format("%d-%d", homeScores.get(homeScores.size() - 2), awayScores.get(awayScores.size() - 2));
    }

    public static class Builder {
        private final List<Integer> homeScores = Lists.newArrayList(0);
        private final List<Integer> awayScores = Lists.newArrayList(0);

        public Builder homeGoal() {
            homeScores.add(homeScores.get(homeScores.size() - 1) + 1);
            awayScores.add(awayScores.get(awayScores.size() - 1));
            return this;
        }

        public Builder awayGoal() {
            homeScores.add(homeScores.get(homeScores.size() - 1));
            awayScores.add(awayScores.get(awayScores.size() - 1) + 1);
            return this;
        }

        public MatchScore build() {
            return new MatchScore(homeScores, awayScores);
        }
    }
}
