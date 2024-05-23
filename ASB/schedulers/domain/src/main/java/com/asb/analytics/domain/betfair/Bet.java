package com.asb.analytics.domain.betfair;

import java.util.Collections;
import java.util.List;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class Bet {

    private String marketId;

    private List<Instruction> instructions;

    // CONSTRUCTOR

    public Bet(String marketId, Runner runner, double size, String side) {
        this.marketId = marketId;

        this.instructions = Collections.singletonList(
                new Instruction(
                        runner.getSelectionId(),
                        side,
                        "LIMIT",
                        new LimitOrder(
                                round(size),
                                runner.getEx().getAvailableToBack().get(0).getPrice(),
                                "LAPSE"
                        )
                )
        );
    }

    public Bet(String marketId, long selectionId, String side, String orderType, Double price, Double size, String persistenceType) {
        this.marketId = marketId;

        this.instructions = Collections.singletonList(
                new Instruction(
                        selectionId,
                        side,
                        orderType,
                        new LimitOrder(
                                size,
                                price,
                                persistenceType
                        )
                )
        );
    }

    public Bet(String marketId, List<Instruction> instructions) {
        this.marketId = marketId;
        this.instructions = instructions;
    }

    private static double round(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    // GETTERS & SETTERS

    public Double getSize() {
        return this.instructions.get(0).getLimitOrder().getSize();
    }

    public Double getPrice() {
        return this.instructions.get(0).getLimitOrder().getPrice();
    }

    public String getSide() {
        return this.instructions.get(0).getSide();
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }
}
