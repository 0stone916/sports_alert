package com.sportsalert.fetcher;

import com.sportsalert.model.Match;
import com.sportsalert.notifier.TelegramNotifier;
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
    private final TelegramNotifier notifier;
    private boolean rateLimitAlertSent = false;

    public ApiSportsFetcher(TelegramNotifier notifier) {
        this.notifier = notifier;
    }

    public List<Match> fetchMatches(LocalDate date) {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("[API-Sports] API_SPORTS_KEY is not set — skipping");
            return new ArrayList<>();
        }
        List<Match> matches = new ArrayList<>();
        matches.addAll(fetchSoccer(date));
        matches.addAll(fetchBasketball(date));
        matches.addAll(fetchBaseball(date));
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
        } catch (RateLimitException e) {
            return new ArrayList<>();
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
        } catch (RateLimitException e) {
            return new ArrayList<>();
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
        } catch (RateLimitException e) {
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[API-Sports] Baseball fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private JSONArray request(String url) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return doRequest(url);
            } catch (RateLimitException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == maxRetries) throw e;
                System.err.println("[API-Sports] 요청 실패 (시도 " + attempt + "/" + maxRetries + "): " + e.getMessage());
                Thread.sleep(1000L * attempt);
            }
        }
        throw new Exception("unreachable");
    }

    private JSONArray doRequest(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-apisports-key", API_KEY)
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        String remaining = res.headers().firstValue("x-ratelimit-requests-remaining").orElse("?");
        String limit = res.headers().firstValue("x-ratelimit-requests-limit").orElse("?");
        System.out.println("[API-Sports] 잔여 호출: " + remaining + "/" + limit + " — " + url);

        JSONObject json = new JSONObject(res.body());

        Object errorsObj = json.opt("errors");
        if (errorsObj instanceof JSONObject && ((JSONObject) errorsObj).length() > 0) {
            System.err.println("[API-Sports] 한도 초과 감지");
            if (!rateLimitAlertSent && notifier != null) {
                notifier.sendError("🚨 API한도 초과 - 축구/농구/야구 일정 누락", "");
                rateLimitAlertSent = true;
            }
            throw new RateLimitException();
        }

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

    private static class RateLimitException extends Exception {}
}
