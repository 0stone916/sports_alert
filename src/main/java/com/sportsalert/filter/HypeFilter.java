package com.sportsalert.filter;

import com.sportsalert.model.Match;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HypeFilter {

    private static final Set<String> FINAL_KEYWORDS = Set.of(
            "final", "semi-final", "semifinal", "semi final",
            "결승", "준결승"
    );

    // 월드컵 32강부터 전경기
    private static final Set<String> WORLD_CUP_KNOCKOUT_KEYWORDS = Set.of(
            "round of 32", "32강",
            "round of 16", "16강",
            "quarter", "8강",
            "semi", "4강",
            "final", "결승", "준결승", "3rd place", "third place"
    );

    private static final Set<String> NBA_PLAYOFF_KEYWORDS = Set.of(
            "conference semi", "conference final", "nba final",
            "final", "semi-final", "semifinal",
            "결승", "준결승"
    );

    private static final Set<String> UFC_TITLE_KEYWORDS = Set.of(
            "title", "championship", "belt",
            "타이틀", "챔피언십"
    );

    private static final Set<String> BOXING_TITLE_KEYWORDS = Set.of(
            "wbc", "wba", "ibf", "wbo", "ibo",
            "title fight", "world title", "world championship",
            "타이틀", "세계 챔피언", "챔피언전"
    );

    private static final Set<String> LOL_WORLDS_KEYWORDS = Set.of(
            "lol worlds", "worlds", "mid-season invitational", "msi"
    );

    private static final Set<String> LOL_PLAYOFF_KEYWORDS = Set.of(
            "final", "semi-final", "semifinal", "playoff",
            "결승", "준결승", "플레이오프"
    );

    public List<Match> filter(List<Match> matches) {
        return matches.stream()
                .filter(this::isHype)
                .collect(Collectors.toList());
    }

    private boolean isHype(Match match) {
        String sport = match.getSport();
        String league = match.getLeague().toLowerCase();
        String round = match.getRound().toLowerCase();
        String home = match.getHomeTeam().toLowerCase();
        String away = match.getAwayTeam().toLowerCase();

        if ("Soccer".equals(sport))     return isSoccerHype(league, round, home, away);
        if ("Basketball".equals(sport)) return isBasketballHype(league, round);
        if ("Baseball".equals(sport))   return isBaseballHype(league, round);
        if ("Fighting".equals(sport))   return isFightingHype(league, round);
        if ("LoL".equals(sport))        return isLolHype(league, round, home, away);
        return false;
    }

    // 챔피언스리그 결승/준결승 + 월드컵(한국 전경기 + 32강부터 전경기)
    private boolean isSoccerHype(String league, String round, String home, String away) {
        if (league.contains("world cup")) {
            boolean isKorea = home.contains("korea") || away.contains("korea");
            if (isKorea) return true;
            return WORLD_CUP_KNOCKOUT_KEYWORDS.stream().anyMatch(kw -> round.contains(kw));
        }
        if (league.contains("champions league")) {
            return FINAL_KEYWORDS.stream().anyMatch(kw -> round.contains(kw) || league.contains(kw));
        }
        return false;
    }

    // NBA 준결승/결승/파이널
    private boolean isBasketballHype(String league, String round) {
        if (!league.contains("nba")) return false;
        return NBA_PLAYOFF_KEYWORDS.stream().anyMatch(kw -> round.contains(kw) || league.contains(kw));
    }

    // 월드시리즈만
    private boolean isBaseballHype(String league, String round) {
        return league.contains("world series") || round.contains("world series");
    }

    // UFC 타이틀전 + 복싱 WBA/WBC/IBF/WBO 타이틀전
    private boolean isFightingHype(String league, String round) {
        if (league.contains("ufc")) {
            return UFC_TITLE_KEYWORDS.stream().anyMatch(kw -> league.contains(kw) || round.contains(kw));
        }
        return BOXING_TITLE_KEYWORDS.stream().anyMatch(kw -> league.contains(kw) || round.contains(kw));
    }

    // MSI/롤드컵 전경기 + LCK(T1 전경기 + 플레이오프)
    private boolean isLolHype(String league, String round, String home, String away) {
        if (LOL_WORLDS_KEYWORDS.stream().anyMatch(league::contains)) return true;
        if (league.contains("lck")) {
            boolean isT1 = home.contains("t1") || away.contains("t1");
            boolean isPlayoff = LOL_PLAYOFF_KEYWORDS.stream().anyMatch(round::contains);
            return isT1 || isPlayoff;
        }
        return false;
    }
}
