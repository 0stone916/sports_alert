package com.sportsalert.filter;

import com.sportsalert.model.Match;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HypeFilter {

    // These leagues include all matches (knockout-only or always notable)
    private static final Set<String> ALWAYS_INCLUDE_LEAGUES = Set.of(
            "uefa champions league", "uefa europa league", "uefa europa conference league",
            "fa cup", "efl cup",
            "fifa world cup", "uefa european championship",
            "copa del rey", "dfb-pokal", "coppa italia",
            "ufc",
            "wimbledon", "us open", "french open", "australian open",
            "lol worlds", "worlds", "mid-season invitational", "msi"
    );

    // Boxing title fight keywords — sport=="Fighting", non-UFC
    private static final Set<String> BOXING_TITLE_KEYWORDS = Set.of(
            "wbc", "wba", "ibf", "wbo", "ibo",
            "title fight", "world title", "world championship", "championship fight",
            "타이틀", "세계 챔피언", "챔피언전"
    );

    // Season leagues — only playoff/final rounds included
    private static final Set<String> BIG_SEASON_LEAGUES = Set.of(
            "english premier league", "premier league",
            "k league 1", "k league",
            "la liga", "bundesliga", "serie a", "ligue 1",
            "nba", "kbl",
            "mlb", "kbo",
            "lck", "lec", "lcs", "lck challengers"
    );

    private static final List<String> PLAYOFF_KEYWORDS = List.of(
            "final", "semi-final", "semifinal", "semi final",
            "quarter-final", "quarterfinal", "quarter final",
            "round of 16", "round of 8", "round of 4",
            "playoff", "play-off", "post-season", "postseason",
            "knockout", "elimination", "championship",
            "결승", "준결승", "8강", "16강", "4강", "플레이오프", "챔피언십"
    );

    public List<Match> filter(List<Match> matches) {
        return matches.stream()
                .filter(this::isHype)
                .collect(Collectors.toList());
    }

    private boolean isHype(Match match) {
        String league = match.getLeague().toLowerCase();
        String round = match.getRound().toLowerCase();

        if (ALWAYS_INCLUDE_LEAGUES.stream().anyMatch(league::contains)) return true;

        if ("Fighting".equalsIgnoreCase(match.getSport()) &&
            BOXING_TITLE_KEYWORDS.stream().anyMatch(kw -> league.contains(kw) || round.contains(kw))) return true;

        boolean isBigSeasonLeague = BIG_SEASON_LEAGUES.stream().anyMatch(league::contains);
        if (!isBigSeasonLeague) return false;

        return PLAYOFF_KEYWORDS.stream().anyMatch(kw -> round.contains(kw) || league.contains(kw));
    }
}
