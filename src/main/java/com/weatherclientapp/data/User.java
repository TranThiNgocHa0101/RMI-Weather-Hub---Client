package com.weatherclientapp.data;

import java.time.LocalDate;

public class User {

    private int id;
    private String email;
    private String username;
    private String password;
    private byte[] faceImage;
    private LocalDate date;
    private LocalDate updateDate;

    /* ===== Constructor ===== */

    public User() {
    }

    public User(int id, String email, String username, String password,
                byte[] faceImage, LocalDate date, LocalDate updateDate) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.faceImage = faceImage;
        this.date = date;
        this.updateDate = updateDate;
    }

    /* ===== Getter ===== */

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public byte[] getFaceImage() {
        return faceImage;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getUpdateDate() {
        return updateDate;
    }

    /* ===== Setter ===== */

    public void setId(int id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFaceImage(byte[] faceImage) {
        this.faceImage = faceImage;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setUpdateDate(LocalDate updateDate) {
        this.updateDate = updateDate;
    }
}
