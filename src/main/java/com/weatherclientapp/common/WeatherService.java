package com.weatherclientapp.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WeatherService extends Remote {
    // 1. Lấy thời tiết hiện tại
    WeatherData getWeatherInformation(String city) throws RemoteException;

    // 2. Lấy dự báo (cho biểu đồ)
    List<WeatherData> getForecast(String city) throws RemoteException;

    // 3. Chatbot
    String chatWithBot(String message) throws RemoteException;

    // --- TÍNH NĂNG MỚI: ĐĂNG KÝ NHẬN CẢNH BÁO (CALLBACK) ---
    // Client gọi hàm này để gửi "số điện thoại" (Interface Callback) cho Server
    void registerForAlerts(WeatherCallback client) throws RemoteException;

    // Client gọi hàm này khi tắt app hoặc tắt chuông
    void unregisterForAlerts(WeatherCallback client) throws RemoteException;


}