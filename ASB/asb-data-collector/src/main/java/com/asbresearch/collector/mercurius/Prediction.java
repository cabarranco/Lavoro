package com.asbresearch.collector.mercurius;

import lombok.Data;

import java.util.Map;

@Data
public class Prediction {
    private Double draw;
    private Double home;
    private Double away;
    private Integer id;
    private Map<String, Double> totalGoals;
}
