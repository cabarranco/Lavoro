package com.asb.analytics.domain.betfair;

/**
 * This is an extended version of the bet that contains useful information
 *
 * @author Claudio Paolicelli
 */
public class ExtendedBet {

    private Bet bet;

    private Double size;

    private String outcome;

    private String marketName;

    // CONSTRUCTOR

    public ExtendedBet(Bet bet, Double size, String outcome) {
        this.bet = bet;
        this.size = size;
        this.outcome = outcome;
    }


    // GETTER & SETTERS


    public Bet getBet() {
        return bet;
    }

    public void setBet(Bet bet) {
        this.bet = bet;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }
}
