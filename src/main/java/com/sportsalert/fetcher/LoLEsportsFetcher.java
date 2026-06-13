package com.sportsalert.fetcher;

import com.sportsalert.model.Match;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoLEsportsFetcher {

    // Public API key embedded in lolesports.com frontend
    private static final String API_URL = "https://esports-api.lolesports.com/persisted/gw/getSchedule?hl=ko-KR";
    private static final String API_KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final HttpClient client = HttpClient.newHttpClient();

    public List<Match> fetchMatches(LocalDate date) {
        List<Match> matches = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("x-api-key", API_KEY)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            JSONArray events = json.getJSONObject("data")
                    .getJSONObject("schedule")
                    .getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                if (!"match".equals(event.optString("type"))) continue;

                String startTime = event.optString("startTime", "");
                if (startTime.isEmpty()) continue;

                ZonedDateTime kstTime = ZonedDateTime.parse(startTime).withZoneSameInstant(KST);
                if (!kstTime.toLocalDate().equals(date)) continue;

                JSONObject league = event.getJSONObject("league");
                JSONArray teams = event.getJSONObject("match").getJSONArray("teams");

                String team1 = teams.length() > 0 ? teams.getJSONObject(0).optString("name", "TBD") : "TBD";
                String team2 = teams.length() > 1 ? teams.getJSONObject(1).optString("name", "TBD") : "TBD";
                String timeStr = String.format("%02d:%02d", kstTime.getHour(), kstTime.getMinute());

                matches.add(new Match(
                        "LoL",
                        league.optString("name", "LoL Esports"),
                        team1,
                        team2,
                        date.toString(),
                        timeStr,
                        event.optString("blockName", "")
                ));
            }
        } catch (Exception e) {
            System.err.println("[LoL Esports] fetch failed: " + e.getMessage());
        }
        return matches;
    }
}
