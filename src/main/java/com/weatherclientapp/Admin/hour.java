package com.weatherclientapp.Admin;

public class hour {
    private Integer hour;
    private String usernames;

    // Constructor đúng
    public hour(Integer hour, String usernames) {
        this.hour = hour;
        this.usernames = usernames;
    }

    // Getter
    public Integer getHour() { return hour; }
    public String getUsernames() { return usernames; }
}
