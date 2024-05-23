package com.asbresearch.collector.mercurius;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PredictionRequest {
    private Long id;
    private Long homeTeamId;
    private Long awayTeamId;
    private String date;
}
