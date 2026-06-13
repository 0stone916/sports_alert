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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApiSportsFetcher {

    private static final String API_KEY = System.getenv("API_SPORTS_KEY");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final HttpClient client = HttpClient.newHttpClient();

    public List<Match> fetchMatches(LocalDate date) {
        List<Match> matches = new ArrayList<>();
        matches.addAll(fetchSoccer(date));
        matches.addAll(fetchBasketball(date));
        matches.addAll(fetchBaseball(date));
        matches.addAll(fetchMMA(date));
        return matches;
    }

    private List<Match> fetchSoccer(LocalDate date) {
        try {
            Thread.sleep(300);
            JSONArray response = request("https://v3.football.api-sports.io/fixtures?date=" + date);
            List<Match> matches = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                JSONObject item = response.getJSONObject(i);
                JSONObject fixture = item.getJSONObject("fixture");
                JSONObject league = item.getJSONObject("league");
                JSONObject teams = item.getJSONObject("teams");
                String home = teams.getJSONObject("home").optString("name", "");
                String away = teams.getJSONObject("away").optString("name", "");
                if (home.isEmpty() && away.isEmpty()) continue;
                matches.add(new Match("Soccer", league.optString("name", ""),
                        home, away, date.toString(),
                        toKST(fixture.optString("date", "")),
                        league.optString("round", "")));
            }
            return matches;
        } catch (Exception e) {
            System.err.println("[API-Sports] Soccer fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Match> fetchBasketball(LocalDate date) {
        try {
            Thread.sleep(300);
            JSONArray response = request("https://v1.basketball.api-sports.io/games?date=" + date);
            List<Match> matches = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                JSONObject item = response.getJSONObject(i);
                JSONObject league = item.getJSONObject("league");
                JSONObject teams = item.getJSONObject("teams");
                String home = teams.getJSONObject("home").optString("name", "");
                String away = teams.getJSONObject("away").optString("name", "");
                if (home.isEmpty() && away.isEmpty()) continue;
                matches.add(new Match("Basketball", league.optString("name", ""),
                        home, away, date.toString(),
                        toKST(item.optString("date", "")),
                        league.optString("round", "")));
            }
            return matches;
        } catch (Exception e) {
            System.err.println("[API-Sports] Basketball fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Match> fetchBaseball(LocalDate date) {
        try {
            Thread.sleep(300);
            JSONArray response = request("https://v1.baseball.api-sports.io/games?date=" + date);
            List<Match> matches = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                JSONObject item = response.getJSONObject(i);
                JSONObject league = item.getJSONObject("league");
                JSONObject teams = item.getJSONObject("teams");
                String home = teams.getJSONObject("home").optString("name", "");
                String away = teams.getJSONObject("away").optString("name", "");
                if (home.isEmpty() && away.isEmpty()) continue;
                matches.add(new Match("Baseball", league.optString("name", ""),
                        home, away, date.toString(),
                        toKST(item.optString("date", "")),
                        league.optString("round", "")));
            }
            return matches;
        } catch (Exception e) {
            System.err.println("[API-Sports] Baseball fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Match> fetchMMA(LocalDate date) {
        try {
            Thread.sleep(300);
            JSONArray response = request("https://v1.mma.api-sports.io/fights?date=" + date);
            List<Match> matches = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                JSONObject item = response.getJSONObject(i);
                String home = "", away = "";
                if (item.has("fighters")) {
                    JSONObject fighters = item.getJSONObject("fighters");
                    JSONObject h = fighters.optJSONObject("home");
                    JSONObject a = fighters.optJSONObject("away");
                    home = h != null ? h.optString("name", "") : "";
                    away = a != null ? a.optString("name", "") : "";
                } else {
                    JSONObject h = item.optJSONObject("home");
                    JSONObject a = item.optJSONObject("away");
                    home = h != null ? h.optString("name", "") : "";
                    away = a != null ? a.optString("name", "") : "";
                }
                if (home.isEmpty() && away.isEmpty()) continue;
                JSONObject league = item.optJSONObject("league");
                String leagueName = league != null ? league.optString("name", "MMA") : "MMA";
                matches.add(new Match("Fighting", leagueName,
                        home, away, date.toString(),
                        toKST(item.optString("date", "")),
                        item.optString("round", "")));
            }
            return matches;
        } catch (Exception e) {
            System.err.println("[API-Sports] MMA fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private JSONArray request(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-apisports-key", API_KEY)
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(res.body());
        JSONArray arr = json.optJSONArray("response");
        return arr != null ? arr : new JSONArray();
    }

    private String toKST(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            ZonedDateTime kst = ZonedDateTime.parse(isoDate).withZoneSameInstant(KST);
            return kst.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return "";
        }
    }
}
