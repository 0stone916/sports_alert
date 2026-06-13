package com.sportsalert.fetcher;

import com.sportsalert.model.Match;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SportsDBFetcher {

    private static final String BASE_URL = "https://www.thesportsdb.com/api/v1/json/3/eventsday.php";
    private static final String[] SPORTS = {"Fighting", "Tennis"};
    private final HttpClient client = HttpClient.newHttpClient();

    public List<Match> fetchMatches(LocalDate date) {
        List<Match> matches = new ArrayList<>();
        for (String sport : SPORTS) {
            try {
                matches.addAll(fetchSport(date, sport));
                Thread.sleep(500);
            } catch (Exception e) {
                System.err.println("[TheSportsDB] " + sport + " fetch failed: " + e.getMessage());
            }
        }
        return matches;
    }

    private List<Match> fetchSport(LocalDate date, String sport) throws Exception {
        String url = BASE_URL + "?d=" + date + "&s=" + sport;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "SportsAlertBot/1.0")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        List<Match> matches = new ArrayList<>();
        JSONObject json = new JSONObject(response.body());
        if (json.isNull("events")) return matches;

        JSONArray events = json.getJSONArray("events");
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            String homeTeam = e.optString("strHomeTeam", "");
            String awayTeam = e.optString("strAwayTeam", "");
            if (homeTeam.isEmpty() && awayTeam.isEmpty()) continue;

            String round = e.optString("strRound", "");
            if (round.isEmpty() || round.matches("\\d+")) round = "";

            matches.add(new Match(
                    sport,
                    e.optString("strLeague", ""),
                    homeTeam,
                    awayTeam,
                    e.optString("dateEvent", ""),
                    e.optString("strTime", ""),
                    round
            ));
        }
        return matches;
    }
}
