package com.asb.analytics.domain;

import java.util.HashMap;

public class InternalDictionary {

    public static HashMap<String, Integer> MARKET_TYPE = new HashMap<>();
    public static HashMap<String, HashMap<String, Integer>> SELECTIONS = new HashMap<>();

    static {
        MARKET_TYPE.put("Match Odds", 1);
        MARKET_TYPE.put("Over/Under 0.5 Goals", 2);
        MARKET_TYPE.put("Over/Under 1.5 Goals", 2);
        MARKET_TYPE.put("Over/Under 2.5 Goals", 2);
        MARKET_TYPE.put("Over/Under 3.5 Goals", 2);
        MARKET_TYPE.put("Correct Score", 3);
        MARKET_TYPE.put("Asian Handicap", 4);

        // Match Odds
        HashMap<String, Integer> MATCH_ODDS = new HashMap<>();
        MATCH_ODDS.put("home", 1);
        MATCH_ODDS.put("away", 2);
        MATCH_ODDS.put("draw", 3);

        // this is to match market catalogue runner name
        MATCH_ODDS.put("Home", 1);
        MATCH_ODDS.put("Away", 2);
        MATCH_ODDS.put("Draw", 3);
        SELECTIONS.put("Match Odds", MATCH_ODDS);

        // Over/Under
        HashMap<String, Integer> OVER_UNDER = new HashMap<>();
        OVER_UNDER.put("Over 0.5 Goals", 2051);
        OVER_UNDER.put("Under 0.5 Goals", 2052);
        OVER_UNDER.put("Over 1.5 Goals", 2151);
        OVER_UNDER.put("Under 1.5 Goals", 2152);
        OVER_UNDER.put("Over 2.5 Goals", 2251);
        OVER_UNDER.put("Under 2.5 Goals", 2252);
        OVER_UNDER.put("Over 3.5 Goals", 2231);
        OVER_UNDER.put("Under 3.5 Goals", 2232);
        SELECTIONS.put("Over/Under 0.5 Goals", OVER_UNDER);
        SELECTIONS.put("Over/Under 1.5 Goals", OVER_UNDER);
        SELECTIONS.put("Over/Under 2.5 Goals", OVER_UNDER);
        SELECTIONS.put("Over/Under 3.5 Goals", OVER_UNDER);

        // Correct Score
        HashMap<String, Integer> CORRECT_SCORE = new HashMap<>();
        CORRECT_SCORE.put("0 - 0", 300);
        CORRECT_SCORE.put("0 - 1", 301);
        CORRECT_SCORE.put("0 - 2", 302);
        CORRECT_SCORE.put("0 - 3", 303);
        CORRECT_SCORE.put("1 - 0", 310);
        CORRECT_SCORE.put("1 - 1", 311);
        CORRECT_SCORE.put("1 - 2", 312);
        CORRECT_SCORE.put("1 - 3", 313);
        CORRECT_SCORE.put("2 - 0", 320);
        CORRECT_SCORE.put("2 - 1", 321);
        CORRECT_SCORE.put("2 - 2", 322);
        CORRECT_SCORE.put("2 - 3", 323);
        CORRECT_SCORE.put("3 - 0", 330);
        CORRECT_SCORE.put("3 - 1", 331);
        CORRECT_SCORE.put("3 - 2", 332);
        CORRECT_SCORE.put("3 - 3", 333);
        CORRECT_SCORE.put("Any Other Home Win", 340);
        CORRECT_SCORE.put("Any Other Away Win", 304);
        CORRECT_SCORE.put("Any Other Draw", 344);
        SELECTIONS.put("Correct Score", CORRECT_SCORE);

        // Asian Handicap
        HashMap<String, Integer> ASIAN_HANDICAP = new HashMap<>();
        ASIAN_HANDICAP.put("home 0.5", 41105);
        ASIAN_HANDICAP.put("home 1.5", 41115);
        ASIAN_HANDICAP.put("home -0.5", 41205);
        ASIAN_HANDICAP.put("home -1.5", 41215);
        ASIAN_HANDICAP.put("away 0.5", 42105);
        ASIAN_HANDICAP.put("away 1.5", 42115);
        ASIAN_HANDICAP.put("away -0.5", 42205);
        ASIAN_HANDICAP.put("away -1.5", 42215);

        // this is to match market catalogue runner name
        ASIAN_HANDICAP.put("Home AH +0.5", 41105);
        ASIAN_HANDICAP.put("Home AH +1.5", 41115);
        ASIAN_HANDICAP.put("Home AH -0.5", 41205);
        ASIAN_HANDICAP.put("Home AH -1.5", 41215);
        ASIAN_HANDICAP.put("Away AH +0.5", 42105);
        ASIAN_HANDICAP.put("Away AH +1.5", 42115);
        ASIAN_HANDICAP.put("Away AH -0.5", 42205);
        ASIAN_HANDICAP.put("Away AH -1.5", 42215);
        SELECTIONS.put("Asian Handicap", ASIAN_HANDICAP);
    }

}
