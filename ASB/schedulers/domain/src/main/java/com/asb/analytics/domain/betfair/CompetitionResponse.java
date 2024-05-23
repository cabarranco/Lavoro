package com.asb.analytics.domain.betfair;

import java.util.List;

/**
 * Betfair Competitions response object
 *
 * @author Claudio Paolicelli
 */
public class CompetitionResponse {

    private List<Competition> competitions;

    public List<Competition> getCompetitions() {
        return competitions;
    }

    public void setCompetitions(List<Competition> competitions) {
        this.competitions = competitions;
    }
}
