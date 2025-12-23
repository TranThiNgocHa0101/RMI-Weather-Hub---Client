package com.weatherclientapp.Admin;

import com.weatherclientapp.common.WeatherData;
import com.weatherclientapp.common.WeatherService;
import com.weatherclientapp.data.User;
import com.weatherclientapp.Home.database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {
    @FXML
    private PieChart hourlyPieChart;
    @FXML
    private BarChart<String, Number> weatherChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;
    @FXML
    private AreaChart<String, Number> clientChar;
    @FXML
    private Button btnLogoutadmin;

    @FXML
    private TableView<User> tableUsers;
    @FXML
    private TableColumn<User, Integer> colId;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colDate;
    @FXML
    private TableColumn<User, byte[]> colFaceImage;

    @FXML
    private TextField searchText;



    @FXML
    private TableView<hour> hourlyUsersTable;

    @FXML
    private TableColumn<hour, String> colHour;

    @FXML
    private TableColumn<hour, String> colUsernames;
    private final String URL = "jdbc:mysql://localhost:3306/face_id";
    private final String USER = "root";
    private final String PASSWORD = "";
    private ObservableList<User> masterData;
    private LineChart<String, Number> forecastChart;

    public void initialize() {
        loadClientChart();

        colHour.setCellValueFactory(new PropertyValueFactory<>("hour"));
        colUsernames.setCellValueFactory(new PropertyValueFactory<>("usernames"));

        loadHourlyData();

        // Cấu hình TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Cột ảnh
        colFaceImage.setCellValueFactory(new PropertyValueFactory<>("faceImage"));
        colFaceImage.setCellFactory(column -> new TableCell<User, byte[]>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(byte[] item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Image img = new Image(new ByteArrayInputStream(item));
                    imageView.setImage(img);
                    setGraphic(imageView);
                }
            }
        });

        // Load dữ liệu
        List<User> users = database.getAllUsers();
        masterData = FXCollections.observableArrayList(users);

        // Filter + Sort cho TableView
        FilteredList<User> filteredData = new FilteredList<>(masterData, p -> true);
        searchText.textProperty().addListener((obs, oldVal, newVal) -> {
            String lowerCaseFilter = newVal.toLowerCase();
            filteredData.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true; // hiện tất cả
                }
                return user.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableUsers.comparatorProperty());
        tableUsers.setItems(sortedData);
    }

    private void loadHourlyData() {
        Map<Integer, List<String>> hourlyMap = new HashMap<>();

        // Khởi tạo map 24 giờ
        for (int i = 0; i < 24; i++) hourlyMap.put(i, new ArrayList<>());

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, date FROM client")) {

            while (rs.next()) {
                String username = rs.getString("username");
                Timestamp ts = rs.getTimestamp("date");
                if (ts != null) {
                    int hour = ts.toLocalDateTime().getHour(); // Lấy giờ trực tiếp
                    hourlyMap.get(hour).add(username);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // PieChart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) {
            int count = hourlyMap.get(h).size();
            if (count > 0) {
                pieChartData.add(new PieChart.Data(h + ":00", count));
            }
        }
        hourlyPieChart.setData(pieChartData);

        // TableView
        ObservableList<hour> tableData = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) {
            List<String> users = hourlyMap.get(h);
            if (!users.isEmpty()) {
                tableData.add(new hour(h, String.join(", ", users)));
            }
        }
        hourlyUsersTable.setItems(tableData);

        // Set TableColumn
        colHour.setCellValueFactory(new PropertyValueFactory<>("hour"));
        colUsernames.setCellValueFactory(new PropertyValueFactory<>("usernames"));
    }


    private void loadClientChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Users");

        List<User> users = database.getAllUsers();

        Map<LocalDate, Long> countByDate = users.stream()
                .collect(Collectors.groupingBy(User::getDate, Collectors.counting()));

        countByDate.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date.toString(), count));
        });

        clientChar.getData().clear();
        clientChar.getData().add(series);
    }
    @FXML
    private void handleLogout() {
        try {
            // Load file FXML của Signup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/weatherclientapp/Login/signup.fxml"));
            Parent root = loader.load();

            // Lấy Stage hiện tại từ nút Logout
            Stage stage = (Stage) btnLogoutadmin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Signup Page");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
