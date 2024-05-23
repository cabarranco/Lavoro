package com.asbresearch.pulse.mapping;

import com.asbresearch.betfair.ref.enums.Side;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class UserRunnerCodeTest {

    @Test
    void matchOdds() {
        UserRunnerCode userRunnerCode = new UserRunnerCode("MO.H.B");
        assertThat(userRunnerCode.getMarket(), is("MO"));
        assertThat(userRunnerCode.getSelection(), is("H"));
        assertThat(userRunnerCode.getSide(), is(Side.BACK));

        userRunnerCode = new UserRunnerCode("MO.A.L");
        assertThat(userRunnerCode.getMarket(), is("MO"));
        assertThat(userRunnerCode.getSelection(), is("A"));
        assertThat(userRunnerCode.getSide(), is(Side.LAY));
    }

    @Test
    void overUnder() {
        UserRunnerCode userRunnerCode = new UserRunnerCode("OU05.O.B");
        assertThat(userRunnerCode.getMarket(), is("OU05"));
        assertThat(userRunnerCode.getSelection(), is("O"));
        assertThat(userRunnerCode.getSide(), is(Side.BACK));
    }

    @Test
    void correctScore() {
        UserRunnerCode userRunnerCode = new UserRunnerCode("CS.30.L");
        assertThat(userRunnerCode.getMarket(), is("CS"));
        assertThat(userRunnerCode.getSelection(), is("30"));
        assertThat(userRunnerCode.getSide(), is(Side.LAY));
    }

    @Test
    void asianHandicap() {
        UserRunnerCode userRunnerCode = new UserRunnerCode("AH.H-25.B");
        assertThat(userRunnerCode.getMarket(), is("AH"));
        assertThat(userRunnerCode.getSelection(), is("H-25"));
        assertThat(userRunnerCode.getSide(), is(Side.BACK));
    }
}