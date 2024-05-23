package com.asb.analytics.domain.betfair;


import com.asb.analytics.domain.SimpleItem;

/**
 * Betfair competitions object
 *
 * @author Claudio Paolicelli
 */
public class Competition {

    private SimpleItem competition;

    private String marketCount;

    private String competitionRegion;

    public SimpleItem getCompetition() {
        return competition;
    }

    public void setCompetition(SimpleItem competition) {
        this.competition = competition;
    }

    public String getMarketCount() {
        return marketCount;
    }

    public void setMarketCount(String marketCount) {
        this.marketCount = marketCount;
    }

    public String getCompetitionRegion() {
        return competitionRegion;
    }

    public void setCompetitionRegion(String competitionRegion) {
        this.competitionRegion = competitionRegion;
    }
}
