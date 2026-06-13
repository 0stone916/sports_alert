package com.sportsalert;

import com.sportsalert.fetcher.ApiSportsFetcher;
import com.sportsalert.fetcher.LoLEsportsFetcher;
import com.sportsalert.fetcher.SportsDBFetcher;
import com.sportsalert.filter.HypeFilter;
import com.sportsalert.model.Match;
import com.sportsalert.notifier.TelegramNotifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String mode = System.getenv().getOrDefault("ALERT_MODE", "daily");
        if ("weekly".equals(mode)) {
            runWeekly();
        } else {
            runDaily();
        }
    }

    private static void runDaily() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        System.out.println("[Daily] Checking matches for: " + tomorrow);

        List<Match> allMatches = new ArrayList<>();
        allMatches.addAll(new ApiSportsFetcher().fetchMatches(tomorrow));
        allMatches.addAll(new SportsDBFetcher().fetchMatches(tomorrow));
        allMatches.addAll(new LoLEsportsFetcher().fetchMatches(tomorrow));
        System.out.println("Total fetched: " + allMatches.size());

        List<Match> hyped = new HypeFilter().filter(allMatches);
        System.out.println("Hyped matches: " + hyped.size());

        if (hyped.isEmpty()) {
            System.out.println("No notable matches tomorrow. Skipping notification.");
            return;
        }

        try {
            new TelegramNotifier().send(hyped, tomorrow);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runWeekly() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(6);
        System.out.println("[Weekly] Checking matches from " + today + " to " + end);

        ApiSportsFetcher apiSportsFetcher = new ApiSportsFetcher();
        SportsDBFetcher sportsDBFetcher = new SportsDBFetcher();
        LoLEsportsFetcher lolFetcher = new LoLEsportsFetcher();
        HypeFilter filter = new HypeFilter();

        Map<LocalDate, List<Match>> matchesByDay = new LinkedHashMap<>();
        for (int i = 0; i <= 6; i++) {
            LocalDate date = today.plusDays(i);
            List<Match> all = new ArrayList<>();
            all.addAll(apiSportsFetcher.fetchMatches(date));
            all.addAll(sportsDBFetcher.fetchMatches(date));
            all.addAll(lolFetcher.fetchMatches(date));
            List<Match> hyped = filter.filter(all);
            if (!hyped.isEmpty()) {
                matchesByDay.put(date, hyped);
            }
        }

        int total = matchesByDay.values().stream().mapToInt(List::size).sum();
        System.out.println("Weekly hyped matches: " + total);

        if (matchesByDay.isEmpty()) {
            System.out.println("No notable matches this week. Skipping notification.");
            return;
        }

        try {
            new TelegramNotifier().sendWeekly(matchesByDay, today, end);
        } catch (Exception e) {
            System.err.println("Failed to send weekly notification: " + e.getMessage());
            System.exit(1);
        }
    }
}
