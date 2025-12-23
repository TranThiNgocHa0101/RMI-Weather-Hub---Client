package com.weatherclientapp.client;

import com.weatherclientapp.common.WeatherData;
import com.weatherclientapp.common.WeatherService;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        try {
            System.out.println("Đang kết nối đến Server...");

            // 1. Lấy sổ địa chỉ (Registry) từ máy chủ (localhost) cổng 1099
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // 2. Tìm kiếm dịch vụ có tên "WeatherSystem"
            // (Phải ép kiểu về Interface WeatherService)
            WeatherService service = (WeatherService) registry.lookup("WeatherSystem");

            System.out.println(">>> Connect success!");

            // 3. Vòng lặp chat với server
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\nNhập tên thành phố (hoặc 'exit' để thoát): ");
                String city = scanner.nextLine();

                if (city.equalsIgnoreCase("exit")) {
                    System.out.println("Tạm biệt!");
                    break;
                }

                // GỌI HÀM TỪ XA (Remote Call)
                // Đây là lúc phép màu xảy ra: Hàm chạy ở Server nhưng trả về Client
                WeatherData data = service.getWeatherInformation(city);

                // Hiển thị kết quả
                System.out.println(data.toString());
            }

        } catch (Exception e) {
            System.err.println("Lỗi kết nối: " + e.getMessage());
            System.err.println("Gợi ý: Bạn đã chạy ServerMain ở project kia chưa?");
        }
    }
}