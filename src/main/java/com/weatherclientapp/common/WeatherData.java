package com.weatherclientapp.common;

import java.io.Serializable;

public class WeatherData implements Serializable {
    // ID phiên bản để đảm bảo tương thích khi truyền qua mạng (RMI)
    private static final long serialVersionUID = 1L;

    private String city;
    private double temperature;
    private double humidity;
    private String description;
    private String icon;       // Mã icon thời tiết (vd: "10d", "04n")
    private double rainVolume; // Lượng mưa (mm)

    /**
     * Constructor chính
     * Lưu ý: rainVolume mặc định là 0.0, sẽ được set sau nếu có dữ liệu mưa
     */
    public WeatherData(String city, double temperature, double humidity, String description, String icon) {
        this.city = city;
        this.temperature = temperature;
        this.humidity = humidity;
        this.description = description;
        this.icon = icon;
        this.rainVolume = 0.0; // Khởi tạo mặc định
    }

    // ================= GETTERS =================
    public String getCity() {
        return city;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public double getRainVolume() {
        return rainVolume;
    }

    // ================= SETTERS =================
    // Cần setter này để cập nhật lượng mưa sau khi khởi tạo object
    public void setRainVolume(double rainVolume) {
        this.rainVolume = rainVolume;
    }

    // ================= TO STRING =================
    @Override
    public String toString() {
        return String.format("%s: %.1f°C, Độ ẩm: %.0f%%, %s, Mưa: %.1fmm",
                city, temperature, humidity, description, rainVolume);
    }
}