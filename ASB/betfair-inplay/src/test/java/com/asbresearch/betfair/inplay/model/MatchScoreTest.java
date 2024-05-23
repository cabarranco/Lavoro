package com.asbresearch.betfair.inplay.model;

import com.asbresearch.betfair.inplay.model.MatchScore;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

public class MatchScoreTest {

    @Test
    public void score_0_0() {
        MatchScore matchScore = new MatchScore.Builder().build();
        Assert.assertThat(matchScore.getHome(), is(0));
        Assert.assertThat(matchScore.getAway(), is(0));
        Assert.assertThat(matchScore.currentScore(), is("0-0"));
        Assert.assertThat(matchScore.previousScore(), is("0-0"));
    }

    @Test
    public void score_1_0() {
        MatchScore matchScore = new MatchScore.Builder().homeGoal().build();
        Assert.assertThat(matchScore.getHome(), is(1));
        Assert.assertThat(matchScore.getAway(), is(0));
        Assert.assertThat(matchScore.currentScore(), is("1-0"));
        Assert.assertThat(matchScore.previousScore(), is("0-0"));
    }

    @Test
    public void score_2_1() {
        MatchScore matchScore = new MatchScore.Builder()
                .homeGoal()
                .homeGoal()
                .awayGoal()
                .build();
        Assert.assertThat(matchScore.getHome(), is(2));
        Assert.assertThat(matchScore.getAway(), is(1));
        Assert.assertThat(matchScore.currentScore(), is("2-1"));
        Assert.assertThat(matchScore.previousScore(), is("2-0"));
    }

    @Test
    public void score_2_2() {
        MatchScore matchScore = new MatchScore.Builder()
                .homeGoal()
                .homeGoal()
                .awayGoal()
                .awayGoal()
                .build();
        Assert.assertThat(matchScore.getHome(), is(2));
        Assert.assertThat(matchScore.getAway(), is(2));
        Assert.assertThat(matchScore.currentScore(), is("2-2"));
        Assert.assertThat(matchScore.previousScore(), is("2-1"));
    }

}