package com.asbresearch.collector.betfair.mapping;

import java.util.HashMap;
import java.util.Map;

import static com.asbresearch.collector.util.Constants.ASIAN_HANDICAP;
import static com.asbresearch.collector.util.Constants.ASIAN_HANDICAP_UNMANAGED;
import static com.asbresearch.collector.util.Constants.CORRECT_SCORE;
import static com.asbresearch.collector.util.Constants.CORRECT_SCORE_UNMANAGED;
import static com.asbresearch.collector.util.Constants.MATCH_ODDS;
import static com.asbresearch.collector.util.Constants.MATCH_ODDS_UNMANAGED;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_05_GOALS;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_05_GOALS_UNMANAGED;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_15_GOALS;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_15_GOALS_UNMANAGED;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_25_GOALS;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_25_GOALS_UNMANAGED;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_35_GOALS;
import static com.asbresearch.collector.util.Constants.OVER_UNDER_35_GOALS_UNMANAGED;

public class AsbSelectionMapping {

    public static final Map<String, Integer> marketType = Map.ofEntries(
            Map.entry(MATCH_ODDS, 1),
            Map.entry(MATCH_ODDS_UNMANAGED, 1),
            Map.entry(OVER_UNDER_05_GOALS, 2),
            Map.entry(OVER_UNDER_05_GOALS_UNMANAGED, 2),
            Map.entry(OVER_UNDER_15_GOALS, 2),
            Map.entry(OVER_UNDER_15_GOALS_UNMANAGED, 2),
            Map.entry(OVER_UNDER_25_GOALS, 2),
            Map.entry(OVER_UNDER_25_GOALS_UNMANAGED, 2),
            Map.entry(OVER_UNDER_35_GOALS, 2),
            Map.entry(OVER_UNDER_35_GOALS_UNMANAGED, 2),
            Map.entry(CORRECT_SCORE, 3),
            Map.entry(CORRECT_SCORE_UNMANAGED, 3),
            Map.entry(ASIAN_HANDICAP, 4),
            Map.entry(ASIAN_HANDICAP_UNMANAGED, 4));
    public static final Map<String, HashMap<String, Integer>> selections;

    static {
        // Match Odds
        HashMap<String, Integer> matchOdds = new HashMap<>();
        matchOdds.put("home", 1);
        matchOdds.put("away", 2);
        matchOdds.put("draw", 3);
        // this is to match market catalogue runner name
        matchOdds.put("Home", 1);
        matchOdds.put("Away", 2);
        matchOdds.put("Draw", 3);

        // Over/Under
        HashMap<String, Integer> overUnder = new HashMap<>();
        overUnder.put("Under 0.5 Goals", 2052);
        overUnder.put("Over 0.5 Goals", 2051);
        overUnder.put("Under 1.5 Goals", 2152);
        overUnder.put("Over 1.5 Goals", 2151);
        overUnder.put("Over 2.5 Goals", 2251);
        overUnder.put("Under 2.5 Goals", 2252);
        overUnder.put("Over 3.5 Goals", 2351);
        overUnder.put("Under 3.5 Goals", 2352);

        // Correct Score
        HashMap<String, Integer> correctScore = new HashMap<>();
        correctScore.put("0 - 0", 300);
        correctScore.put("0 - 1", 301);
        correctScore.put("0 - 2", 302);
        correctScore.put("0 - 3", 303);
        correctScore.put("1 - 0", 310);
        correctScore.put("1 - 1", 311);
        correctScore.put("1 - 2", 312);
        correctScore.put("1 - 3", 313);
        correctScore.put("2 - 0", 320);
        correctScore.put("2 - 1", 321);
        correctScore.put("2 - 2", 322);
        correctScore.put("2 - 3", 323);
        correctScore.put("3 - 0", 330);
        correctScore.put("3 - 1", 331);
        correctScore.put("3 - 2", 332);
        correctScore.put("3 - 3", 333);
        correctScore.put("Any Other Home Win", 340);
        correctScore.put("Any Other Away Win", 304);
        correctScore.put("Any Other Draw", 344);

        // Asian Handicap
        HashMap<String, Integer> asianHandicap = new HashMap<>();
        asianHandicap.put("home 0.5", 41105);
        asianHandicap.put("home 1.5", 41115);
        asianHandicap.put("home -0.5", 41205);
        asianHandicap.put("home -1.5", 41215);
        asianHandicap.put("away 0.5", 42105);
        asianHandicap.put("away 1.5", 42115);
        asianHandicap.put("away -0.5", 42205);
        asianHandicap.put("away -1.5", 42215);

        // this is to match market catalogue runner name
        asianHandicap.put("Home AH +0.5", 41105);
        asianHandicap.put("Home AH +1.5", 41115);
        asianHandicap.put("Home AH -0.5", 41205);
        asianHandicap.put("Home AH -1.5", 41215);
        asianHandicap.put("Away AH +0.5", 42105);
        asianHandicap.put("Away AH +1.5", 42115);
        asianHandicap.put("Away AH -0.5", 42205);
        asianHandicap.put("Away AH -1.5", 42215);

        selections = Map.ofEntries(
                Map.entry(MATCH_ODDS, matchOdds),
                Map.entry(MATCH_ODDS_UNMANAGED, matchOdds),
                Map.entry(OVER_UNDER_05_GOALS, overUnder),
                Map.entry(OVER_UNDER_05_GOALS_UNMANAGED, overUnder),
                Map.entry(OVER_UNDER_15_GOALS, overUnder),
                Map.entry(OVER_UNDER_15_GOALS_UNMANAGED, overUnder),
                Map.entry(OVER_UNDER_25_GOALS, overUnder),
                Map.entry(OVER_UNDER_25_GOALS_UNMANAGED, overUnder),
                Map.entry(OVER_UNDER_35_GOALS, overUnder),
                Map.entry(OVER_UNDER_35_GOALS_UNMANAGED, overUnder),
                Map.entry(CORRECT_SCORE, correctScore),
                Map.entry(CORRECT_SCORE_UNMANAGED, correctScore),
                Map.entry(ASIAN_HANDICAP, asianHandicap),
                Map.entry(ASIAN_HANDICAP_UNMANAGED, asianHandicap));
    }
}
