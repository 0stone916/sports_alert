package com.sportsalert.notifier;

import com.sportsalert.model.Match;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelegramNotifier {

    private static final String API_URL = "https://api.telegram.org/bot%s/sendMessage";
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final String[] KR_DAY_NAMES = {"일", "월", "화", "수", "목", "금", "토"};

    private final String token;
    private final String channelId;
    private final HttpClient client = HttpClient.newHttpClient();

    public TelegramNotifier() {
        this.token = System.getenv("TELEGRAM_BOT_TOKEN");
        this.channelId = System.getenv("TELEGRAM_CHANNEL_ID");
        if (token == null || token.isBlank()) throw new IllegalStateException("TELEGRAM_BOT_TOKEN is not set");
        if (channelId == null || channelId.isBlank()) throw new IllegalStateException("TELEGRAM_CHANNEL_ID is not set");
    }

    public void sendError(String context, String errorMessage) {
        try {
            String text = context + (errorMessage != null && !errorMessage.isEmpty() ? "\n" + errorMessage : "");
            sendRaw(text);
        } catch (Exception e) {
            System.err.println("[Telegram] 에러 알림 전송 실패: " + e.getMessage());
        }
    }

    public void send(List<Match> matches, LocalDate date) throws Exception {
        String message = buildDailyMessage(matches, date);
        sendRaw(message);
        System.out.println("Sent " + matches.size() + " alerts.");
    }

    public void sendWeekly(Map<LocalDate, List<Match>> matchesByDay, LocalDate from, LocalDate to) throws Exception {
        List<String> messages = buildWeeklyMessages(matchesByDay, from, to);
        for (String message : messages) {
            sendRaw(message);
            if (messages.size() > 1) Thread.sleep(300);
        }
        int total = matchesByDay.values().stream().mapToInt(List::size).sum();
        System.out.println("Sent weekly alert: " + total + " matches across " + matchesByDay.size() + " days.");
    }

    private void sendRaw(String message) throws Exception {
        String body = "chat_id=" + URLEncoder.encode(channelId, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                + "&parse_mode=HTML";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(API_URL, token)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Telegram API returned " + response.statusCode() + ": " + response.body());
        }
    }

    private String buildDailyMessage(List<Match> matches, LocalDate date) {
        Map<String, List<Match>> bySport = new LinkedHashMap<>();
        for (Match m : matches) {
            bySport.computeIfAbsent(m.getSport(), k -> new ArrayList<>()).add(m);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<b>🏆 내일의 주요 경기 (").append(date).append(")</b>\n\n");

        for (Map.Entry<String, List<Match>> entry : bySport.entrySet()) {
            sb.append(sportEmoji(entry.getKey())).append(" <b>").append(entry.getKey()).append("</b>\n");
            for (Match m : entry.getValue()) {
                sb.append("• ");
                if (!m.getRound().isEmpty()) sb.append("[").append(m.getRound()).append("] ");
                sb.append(m.getHomeTeam()).append(" vs ").append(m.getAwayTeam());
                sb.append(" — ").append(m.getLeague());
                if (!m.getTime().isEmpty()) sb.append(" <i>(").append(m.getTime()).append(" KST)</i>");
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("총 ").append(matches.size()).append("경기 | Sports Alert");
        return sb.toString();
    }

    private List<String> buildWeeklyMessages(Map<LocalDate, List<Match>> matchesByDay, LocalDate from, LocalDate to) {
        String header = String.format("<b>🗓 이번 주 주요 경기 (%s ~ %s)</b>\n\n",
                formatDateKR(from), formatDateKR(to));
        int total = matchesByDay.values().stream().mapToInt(List::size).sum();
        String footer = "총 " + total + "경기 | Sports Alert";

        List<String> dayBlocks = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Match>> entry : matchesByDay.entrySet()) {
            dayBlocks.add(buildDayBlock(entry.getKey(), entry.getValue()));
        }

        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder(header);

        for (String block : dayBlocks) {
            if (current.length() + block.length() > MAX_MESSAGE_LENGTH) {
                messages.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(block);
        }
        current.append(footer);
        messages.add(current.toString().trim());

        return messages;
    }

    private String buildDayBlock(LocalDate date, List<Match> matches) {
        String dayName = KR_DAY_NAMES[date.getDayOfWeek().getValue() % 7];
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<b>📅 %d월 %d일 (%s)</b>\n", date.getMonthValue(), date.getDayOfMonth(), dayName));

        Map<String, List<Match>> bySport = new LinkedHashMap<>();
        for (Match m : matches) {
            bySport.computeIfAbsent(m.getSport(), k -> new ArrayList<>()).add(m);
        }

        for (Map.Entry<String, List<Match>> entry : bySport.entrySet()) {
            sb.append(sportEmoji(entry.getKey())).append(" <b>").append(entry.getKey()).append("</b>\n");
            for (Match m : entry.getValue()) {
                sb.append("• ");
                if (!m.getRound().isEmpty()) sb.append("[").append(m.getRound()).append("] ");
                sb.append(m.getHomeTeam()).append(" vs ").append(m.getAwayTeam());
                sb.append(" — ").append(m.getLeague());
                if (!m.getTime().isEmpty()) sb.append(" <i>(").append(m.getTime()).append(" KST)</i>");
                sb.append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private String formatDateKR(LocalDate date) {
        return String.format("%d.%02d.%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String sportEmoji(String sport) {
        switch (sport) {
            case "Soccer":     return "⚽";
            case "Basketball": return "🏀";
            case "Baseball":   return "⚾";
            case "Fighting":   return "🥊";
            case "Tennis":     return "🎾";
            case "LoL":        return "🎮";
            default:           return "🏅";
        }
    }
}
