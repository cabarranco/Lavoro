package com.asbresearch.betfair.inplay.util;

import com.asbresearch.betfair.inplay.model.InPlayResponse;
import com.asbresearch.betfair.inplay.model.UpdateDetail;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class InPlayUtil {
    public static final String SECOND_HALF_END = "SecondHalfEnd";
    public static final String KICK_OFF = "KickOff";
    public static final String SECOND_HALF_KICK_OFF = "SecondHalfKickOff";
    public static final String IN_PLAY = "IN_PLAY";
    public static final String COMPLETE = "COMPLETE";
    public static final String GOAL = "Goal";
    public static final String HOME = "home";

    public static List<String> getScoreHistory(List<UpdateDetail> updateDetails) {
        List<Integer> home = new ArrayList<>();
        List<Integer> away = new ArrayList<>();
        for (int i = 0; i < updateDetails.size(); i++) {
            UpdateDetail updateDetail = updateDetails.get(i);
            if (KICK_OFF.equalsIgnoreCase(updateDetail.getUpdateType())) {
                home.add(0);
                away.add(0);
                continue;
            }
            if (GOAL.equalsIgnoreCase(updateDetail.getUpdateType())) {
                if (HOME.equals(updateDetail.getTeam())) {
                    home.add(home.get(i - 1) + 1);
                    away.add(away.get(i - 1));
                } else {
                    away.add(away.get(i - 1) + 1);
                    home.add(home.get(i - 1));
                }
                continue;
            }
            home.add(home.get(i - 1));
            away.add(away.get(i - 1));
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < home.size(); i++) {
            result.add(String.format("%s-%s", home.get(i), away.get(i)));
        }
        return result;
    }

    public static boolean isSecondHalfEnd(InPlayResponse inPlayResponse) {
        for (UpdateDetail updateDetail : inPlayResponse.getUpdateDetails()) {
            if (SECOND_HALF_END.equals(updateDetail.getUpdateType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInPlay(InPlayResponse inPlayResponse) {
        return IN_PLAY.equals(inPlayResponse.getStatus());
    }

    public static boolean isGameFinished(InPlayResponse inPlayResponse) {
        return COMPLETE.equals(inPlayResponse.getStatus());
    }

    public static boolean isEndOfGame(InPlayResponse inPlayResponse) {
        return isSecondHalfEnd(inPlayResponse) || isGameFinished(inPlayResponse);
    }
}
