package com.weatherclientapp.common;

import java.rmi.Remote;
import java.rmi.RemoteException; // <--- ĐỪNG QUÊN IMPORT NÀY

public interface WeatherCallback extends Remote {
    // Thêm 'throws RemoteException' vào cuối dòng này
    void onEmergencyAlert(String message) throws RemoteException;
}