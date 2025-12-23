package com.weatherclientapp.Home;

import com.weatherclientapp.data.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class database {

    private static final String URL = "jdbc:mysql://localhost:3306/face_id";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Kết nối DB
    public static Connection connectionDb() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    // Lấy tất cả user
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM client"; // bảng client trong DB

        try (Connection conn = connectionDb();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String email = rs.getString("email");
                String uname = rs.getString("username");
                String pwd = rs.getString("password");

                // Lấy BLOB
                Blob blob = rs.getBlob("face_image");
                byte[] face = null;
                if (blob != null) {
                    face = blob.getBytes(1, (int) blob.length());
                }

                LocalDate date = rs.getDate("date").toLocalDate();
                LocalDate updateDate = rs.getDate("update_date").toLocalDate();

                users.add(new User(id, email, uname, pwd, face, date, updateDate));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
}
