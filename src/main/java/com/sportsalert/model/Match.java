package com.sportsalert.model;

public class Match {
    private final String sport;
    private final String league;
    private final String homeTeam;
    private final String awayTeam;
    private final String date;
    private final String time;
    private final String round;

    public Match(String sport, String league, String homeTeam, String awayTeam,
                 String date, String time, String round) {
        this.sport = sport;
        this.league = league;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = date;
        this.time = time;
        this.round = round;
    }

    public String getSport()    { return sport; }
    public String getLeague()   { return league; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public String getDate()     { return date; }
    public String getTime()     { return time; }
    public String getRound()    { return round; }
}
