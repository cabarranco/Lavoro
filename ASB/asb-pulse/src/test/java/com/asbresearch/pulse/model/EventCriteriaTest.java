package com.asbresearch.pulse.model;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventCriteriaTest {

    @Test
    void validEventCriteria() {
        EventCriteria eventCriteria = EventCriteria.liveAndPreLifeFootball(Collections.singleton("PLL"), Sets.newHashSet("0-1", "1-1"), "0-0", 80);
        assertThat(eventCriteria.getCurrentLiveScores(), notNullValue());
        assertThat(eventCriteria.getCurrentLiveScores(), hasItem("0-1"));
        assertThat(eventCriteria.getCurrentLiveScores(), hasItem("1-1"));
        assertThat(eventCriteria.getCurrentLiveScores().size(), is(2));

        assertThat(eventCriteria.getIncludeCompetitions(), notNullValue());
        assertThat(eventCriteria.getIncludeCompetitions(), hasItem("PLL"));
        assertThat(eventCriteria.getIncludeCompetitions().size(), is(1));

        assertThat(eventCriteria.getPreviousScore(), is("0-0"));
        assertThat(eventCriteria.getStartFromKickOff(), is(0));
        assertThat(eventCriteria.getEndFromKickOff(), is(80));
    }

    @Test
    void invalidPreviousScore() {
        Throwable exception = assertThrows(IllegalArgumentException.class,
                () -> new EventCriteria("Soccer", emptySet(), emptySet(), true, emptySet(), "abc", true, 30, 30));
        assertTrue(exception.getMessage().contains("Invalid score=abc"));
    }

    @Test
    void invalidCurrentScores() {
        Throwable exception = assertThrows(IllegalArgumentException.class,
                () -> new EventCriteria("Soccer", emptySet(), emptySet(), true, Collections.singleton("abc"), "1-2", true, 30, 30));
        assertTrue(exception.getMessage().contains("Invalid currentLiveScores=[abc]"));
    }
}