package com.asb.analytics.domain.mercurius;

import java.util.HashMap;

public class Prediction {

    private Float draw;
    private Float home;
    private Float away;
    private Integer id;
    private HashMap<String, Float> totalGoals;

    public Float getDraw() {
        return draw;
    }

    public void setDraw(Float draw) {
        this.draw = draw;
    }

    public Float getHome() {
        return home;
    }

    public void setHome(Float home) {
        this.home = home;
    }

    public Float getAway() {
        return away;
    }

    public void setAway(Float away) {
        this.away = away;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public HashMap<String, Float> getTotalGoals() {
        return totalGoals;
    }

    public void setTotalGoals(HashMap<String, Float> totalGoals) {
        this.totalGoals = totalGoals;
    }
}
