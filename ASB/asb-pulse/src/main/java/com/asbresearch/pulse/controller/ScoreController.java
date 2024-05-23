package com.asbresearch.pulse.controller;

import com.asbresearch.betfair.inplay.BetfairInPlayService;
import com.asbresearch.betfair.inplay.model.MatchScore;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(value = "/score", produces = "application/json")
class ScoreController {
    @Autowired
    private BetfairInPlayService betfairInPlayService;

    @GetMapping(path = "live/{eventId}")
    public MatchScore getScore(@ApiParam(value = "eventId", example = "20001") @PathVariable("eventId") String eventId) {
        Optional<MatchScore> matchScore = betfairInPlayService.score(Integer.valueOf(eventId));
        return matchScore.isPresent() ? matchScore.get() : null;
    }
}