package com.weatherclientapp.client;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.weatherclientapp.Home.database;
import com.weatherclientapp.common.WeatherCallback;
import com.weatherclientapp.common.WeatherData;
import com.weatherclientapp.common.WeatherService;
import com.weatherclientapp.data.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart; // [MỚI] Import BarChart
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GuiClient {

    private static int userId;
    private int loggedUserId;

    private final String loggedUsername;
    public WeatherService service;
    private WeatherCallback myCallback;

    public WebView mapView;
    public TextField cityInput;
    private Stage mainStage;
    private ToggleButton alertBtn;

    // UI Components
    private ImageView weatherIcon;
    private Label resultCity, resultTemp, resultDesc, resultHumidity;

    // --- HAI BIỂU ĐỒ (CẬP NHẬT) ---
    private LineChart<String, Number> forecastChart; // Biểu đồ Nhiệt độ
    private BarChart<String, Number> rainChart;      // [MỚI] Biểu đồ Lượng mưa

    // Dữ liệu tạm để in báo cáo
    private WeatherData currentDataForReport;
    private List<WeatherData> currentForecastForReport;

    // Cache dữ liệu thời tiết theo city
    private final ConcurrentHashMap<String, WeatherData> weatherCache = new ConcurrentHashMap<>();

    public GuiClient(int userId, String username) {
        this.loggedUserId = userId;  // dùng để load avatar
        this.loggedUsername = username;
    }

    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        connectToServer();

        try {
            myCallback = new ClientCallbackImpl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Map setup
        mapView = new WebView();
        WebEngine webEngine = mapView.getEngine();
        try {
            webEngine.setUserStyleSheetLocation(null);
            webEngine.load(Objects.requireNonNull(getClass().getResource("/map.html")).toExternalForm());
        } catch (Exception e) {
            System.err.println("Lỗi load map.html: " + e.getMessage());
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newVal) -> {
            if (newVal == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", new JavaBridge(this));
                preloadAllMarkers(Arrays.asList(
                        "Hanoi",
                        "Ho Chi Minh",
                        "Da Nang",
                        "Hue",
                        "Can Tho"
                ));

            }
        });

        ScrollPane rightScroll = createRightPanel();
        // Cho phép cuộn chuột mượt mà
        rightScroll.setPannable(true);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(mapView, rightScroll);
        splitPane.setDividerPositions(0.65);

        Platform.runLater(this::updateWeather);

        Scene scene = new Scene(splitPane, 1280, 800);
        primaryStage.setTitle("Hệ thống Giám sát Thiên tai & Báo cáo (Visual Master)");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (alertBtn.isSelected()) toggleAlert(false);
            Platform.exit();
            System.exit(0);
        });
    }

    // ================== CREATE RIGHT PANEL ==================
    private ScrollPane createRightPanel() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f0f4f8);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // --- AVATAR & USER INFO ---
        ImageView avatar = new ImageView();
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setPreserveRatio(true);
        avatar.setImage(loadAvatarSafe(loggedUserId));

        Circle clip = new Circle();
        clip.centerXProperty().bind(avatar.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(avatar.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(avatar.fitWidthProperty().divide(2));
        avatar.setClip(clip);

        Label userLabel = new Label("Xin chào, " + loggedUsername);
        userLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        userLabel.setTextFill(Color.DARKBLUE);
        userLabel.setCursor(Cursor.HAND);
        userLabel.setOnMouseClicked(e -> openProfileWindow());

        // Lấy thông tin user từ CSDL
        User currentUser = loadUserFromDB(loggedUserId);

        // Tạo Popup thông tin
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 2; -fx-padding: 10; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

        if (currentUser != null) {
            infoBox.getChildren().addAll(
                    createInfoLabel("ID", String.valueOf(currentUser.getId())),
                    createInfoLabel("Email", currentUser.getEmail()),
                    createInfoLabel("Username", currentUser.getUsername()),
                    createInfoLabel("Ngày tạo", currentUser.getDate() != null ? currentUser.getDate().toString() : ""),
                    createInfoLabel("Ngày cập nhật", currentUser.getUpdateDate() != null ? currentUser.getUpdateDate().toString() : "")
            );

            // Nếu có ảnh trong DB thì hiện thêm trong popup
            if (currentUser.getFaceImage() != null) {
                ImageView popupAvatar = new ImageView(new Image(new ByteArrayInputStream(currentUser.getFaceImage())));
                popupAvatar.setFitWidth(50); popupAvatar.setFitHeight(50); popupAvatar.setPreserveRatio(true);
                Circle popupClip = new Circle(25, 25, 25);
                popupAvatar.setClip(popupClip);
                infoBox.getChildren().add(0, popupAvatar);
            }
        }

        Popup popup = new Popup();
        popup.getContent().add(infoBox);

        avatar.setOnMouseEntered(e -> {
            if (!popup.isShowing()) popup.show(avatar, avatar.localToScreen(avatar.getBoundsInLocal()).getMinX(), avatar.localToScreen(avatar.getBoundsInLocal()).getMaxY() + 5);
        });
        avatar.setOnMouseExited(e -> popup.hide());
        avatar.setOnMouseClicked(e -> openProfileWindow());

        // Spacer + Title
        Region spacerLeft = new Region(); HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        Label titleLabel = new Label("THÔNG TIN");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        Region spacerRight = new Region(); HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // --- BUTTONS ---
        alertBtn = new ToggleButton("🔔 Cảnh Báo");
        alertBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        alertBtn.setOnAction(e -> toggleAlert(alertBtn.isSelected()));

        Button pdfBtn = new Button("🖨️ Xuất PDF");
        pdfBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> exportToPDF());

        Button chatBtn = new Button("💬 Chatbox");
        chatBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        chatBtn.setOnAction(e -> new ChatWindow(loggedUsername).show());
        // Nút Đăng xuất (Thêm mới)
        Button logoutBtn = new Button("🚪 Đăng xuất");
        logoutBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> handleLogout());

        header.getChildren().addAll(avatar, userLabel, spacerLeft, titleLabel, spacerRight, alertBtn, pdfBtn, chatBtn, logoutBtn);
        // Search Box
        cityInput = new TextField("Hanoi");
        Button searchBtn = new Button("Kiểm tra");
        searchBtn.setOnAction(e -> updateWeather());
        cityInput.setOnAction(e -> updateWeather());
        HBox searchBox = new HBox(10, cityInput, searchBtn);
        searchBox.setAlignment(Pos.CENTER);

        // Add contents
        content.getChildren().addAll(header, searchBox, createDashboardCard(), createChartContainer());

        return new ScrollPane(content);
    }

    private HBox createInfoLabel(String field, String value) {
        Label fieldLabel = new Label(field + ": ");
        fieldLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #3498db; -fx-font-weight: bold;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        HBox box = new HBox(5, fieldLabel, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 1. Hủy đăng ký nhận cảnh báo thiên tai (Tránh lỗi Server treo)
            if (alertBtn.isSelected()) {
                try {
                    service.unregisterForAlerts(myCallback);
                } catch (Exception e) {
                    System.err.println("Lỗi ngắt kết nối callback: " + e.getMessage());
                }
            }

            // 2. Xóa cache dữ liệu của phiên làm việc cũ
            weatherCache.clear();

            // 3. Đóng màn hình chính
            mainStage.close();

            // 4. Mở lại màn hình Login
            Platform.runLater(() -> {
                try {
                    // Đảm bảo đường dẫn FXML này là chính xác trong project của bạn
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/weatherclientapp/Login/signup.fxml"));
                    Stage loginStage = new Stage();
                    loginStage.setScene(new Scene(loader.load()));
                    loginStage.setTitle("Đăng nhập hệ thống");
                    loginStage.show();
                } catch (Exception ex) {
                    System.err.println("Không thể mở màn hình đăng nhập: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        }
    }
    private User loadUserFromDB(int userId) {
        String sql = "SELECT * FROM client WHERE id=?";
        User user = null;
        try (Connection con = database.connectionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setUsername(rs.getString("username"));
                user.setFaceImage(rs.getBytes("face_image"));
                // Không load password để bảo mật
                java.sql.Date d = rs.getDate("date");
                java.sql.Date u = rs.getDate("update_date");
                if (d != null) user.setDate(d.toLocalDate());
                if (u != null) user.setUpdateDate(u.toLocalDate());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return user;
    }

    private Image loadAvatarSafe(int userId) {
        Image img = null;
        String sql = "SELECT face_image FROM client WHERE id=?";
        try (Connection con = database.connectionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                byte[] imgBytes = rs.getBytes("face_image");
                if (imgBytes != null && imgBytes.length > 0) {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes)) {
                        img = new Image(bis);
                        if (img.isError()) img = null;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (img == null) {
            InputStream defaultAvatar = getClass().getResourceAsStream("/default_avatar.png");
            if (defaultAvatar != null) img = new Image(defaultAvatar);
        }
        return img;
    }

    // ================== DASHBOARD CARD ==================
    private VBox createDashboardCard() {
        resultCity = new Label("---");
        resultCity.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(100);
        weatherIcon.setFitHeight(100);
        resultTemp = new Label("-- °C");
        resultTemp.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        resultTemp.setTextFill(Color.RED);
        resultDesc = new Label("---");
        resultHumidity = new Label("Độ ẩm: ---");

        VBox c = new VBox(5, resultCity, weatherIcon, resultTemp, resultDesc, resultHumidity);
        c.setAlignment(Pos.CENTER);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        c.setPadding(new Insets(20));
        return c;
    }

    // ================== CHART CONTAINER (CẬP NHẬT) ==================
    private VBox createChartContainer() {
        // 1. Biểu đồ Nhiệt độ
        CategoryAxis xTemp = new CategoryAxis(); xTemp.setLabel("Thời gian");
        NumberAxis yTemp = new NumberAxis(); yTemp.setLabel("Nhiệt độ (°C)");
        forecastChart = new LineChart<>(xTemp, yTemp);
        forecastChart.setTitle("Diễn biến Nhiệt độ");
        forecastChart.setPrefHeight(250);
        forecastChart.setLegendVisible(false);

        // 2. Biểu đồ Lượng mưa [MỚI]
        CategoryAxis xRain = new CategoryAxis(); xRain.setLabel("Thời gian");
        NumberAxis yRain = new NumberAxis(); yRain.setLabel("Lượng mưa (mm)");

        rainChart = new BarChart<>(xRain, yRain);
        rainChart.setTitle("Dự báo Lượng Mưa");
        rainChart.setPrefHeight(250);
        rainChart.setLegendVisible(false);
        rainChart.setStyle("-fx-bar-fill: #3498db;"); // Màu xanh biển

        // Container chứa cả 2 biểu đồ
        VBox container = new VBox(20, forecastChart, rainChart);
        container.setPadding(new Insets(10, 0, 20, 0));
        return container;
    }

    // ================== UPDATE WEATHER (CẬP NHẬT) ==================
    public void updateWeather() {
        String city = cityInput.getText().trim();
        if (city.isEmpty()) return;

        new Thread(() -> {
            try {
                WeatherData c = service.getWeatherInformation(city);
                List<WeatherData> f = service.getForecast(city);

                this.currentDataForReport = c;
                this.currentForecastForReport = f;
                weatherCache.put(city, c);

                Platform.runLater(() -> {
                    // Cập nhật thông tin chung
                    resultCity.setText(c.getCity());
                    resultTemp.setText(String.format("%.1f °C", c.getTemperature()));
                    resultDesc.setText(c.getDescription());
                    resultHumidity.setText(String.format("Độ ẩm: %.0f%%", c.getHumidity()));
                    try {
                        weatherIcon.setImage(new Image("https://openweathermap.org/img/wn/" + c.getIcon() + "@4x.png", true));
                    } catch (Exception ignored) {}

                    // Cập nhật Biểu đồ Nhiệt độ
                    forecastChart.getData().clear();
                    XYChart.Series<String, Number> tempSeries = new XYChart.Series<>();
                    for (WeatherData item : f) {
                        tempSeries.getData().add(new XYChart.Data<>(item.getDescription(), item.getTemperature()));
                    }
                    forecastChart.getData().add(tempSeries);

                    // Cập nhật Biểu đồ Lượng mưa [MỚI]
                    rainChart.getData().clear();
                    XYChart.Series<String, Number> rainSeries = new XYChart.Series<>();
                    for (WeatherData item : f) {
                        // Cắt chuỗi thời gian cho gọn (lấy giờ HH:mm)
                        String timeLabel = item.getDescription();
                        if (timeLabel != null && timeLabel.length() > 10) {
                            timeLabel = timeLabel.substring(11, 16);
                        }
                        rainSeries.getData().add(new XYChart.Data<>(timeLabel, item.getRainVolume()));
                    }
                    rainChart.getData().add(rainSeries);
                });
            } catch (Exception ignored) { ignored.printStackTrace(); }
        }).start();
    }

    // ================== PDF EXPORT (CẬP NHẬT) ==================
    private void exportToPDF() {
        if (currentDataForReport == null) {
            new Alert(Alert.AlertType.WARNING, "Chưa có dữ liệu để in!").show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu Báo Cáo Thời Tiết");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("BaoCao_" + currentDataForReport.getCity() + ".pdf");
        File file = fileChooser.showSaveDialog(mainStage);

        if (file != null) {
            try {
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                com.itextpdf.text.Font fontTitle = new com.itextpdf.text.Font(bf, 24, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
                com.itextpdf.text.Font fontHeader = new com.itextpdf.text.Font(bf, 14, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font fontNormal = new com.itextpdf.text.Font(bf, 12, com.itextpdf.text.Font.NORMAL);

                Paragraph title = new Paragraph("BÁO CÁO TÌNH HÌNH THỜI TIẾT", fontTitle);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                document.add(new Paragraph("Thời gian xuất: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()), fontNormal));
                document.add(new Paragraph("Khu vực: " + currentDataForReport.getCity(), fontHeader));
                document.add(new Paragraph("----------------------------------------------------------"));
                document.add(new Paragraph("Nhiệt độ: " + currentDataForReport.getTemperature() + " °C", fontNormal));
                document.add(new Paragraph("Độ ẩm: " + currentDataForReport.getHumidity() + " %", fontNormal));
                document.add(new Paragraph("Trạng thái: " + currentDataForReport.getDescription(), fontNormal));
                document.add(new Paragraph("----------------------------------------------------------\n\n"));

                if (currentForecastForReport != null && !currentForecastForReport.isEmpty()) {
                    document.add(new Paragraph("DỰ BÁO CHI TIẾT:", fontHeader));
                    document.add(new Paragraph(" ", fontNormal));

                    // Tạo bảng 4 cột: Thời gian | Nhiệt độ | Độ ẩm | Mưa
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    addCellToTable(table, "Thời gian", fontHeader, BaseColor.LIGHT_GRAY);
                    addCellToTable(table, "Nhiệt độ (°C)", fontHeader, BaseColor.LIGHT_GRAY);
                    addCellToTable(table, "Độ ẩm (%)", fontHeader, BaseColor.LIGHT_GRAY);
                    addCellToTable(table, "Mưa (mm)", fontHeader, BaseColor.LIGHT_GRAY);

                    for (WeatherData item : currentForecastForReport) {
                        addCellToTable(table, item.getDescription(), fontNormal, BaseColor.WHITE);
                        addCellToTable(table, String.valueOf(item.getTemperature()), fontNormal, BaseColor.WHITE);
                        addCellToTable(table, String.valueOf(item.getHumidity()), fontNormal, BaseColor.WHITE);
                        addCellToTable(table, String.valueOf(item.getRainVolume()), fontNormal, BaseColor.WHITE);
                    }
                    document.add(table);
                }

                document.add(new Paragraph("\n\n* Báo cáo tự động từ hệ thống RMI.", fontNormal));
                document.close();
                java.awt.Desktop.getDesktop().open(file);

            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Lỗi xuất PDF: " + e.getMessage()).show();
            }
        }
    }

    private void addCellToTable(PdfPTable table, String text, com.itextpdf.text.Font font, BaseColor color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(color);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    // ================== CALLBACK ==================
    private class ClientCallbackImpl extends UnicastRemoteObject implements WeatherCallback {
        protected ClientCallbackImpl() throws RemoteException { super(); }
        @Override
        public void onEmergencyAlert(String payload) throws RemoteException {
            Platform.runLater(() -> {
                String[] parts = payload.split("\\|");
                String title = parts.length > 0 ? parts[0] : "CẢNH BÁO";
                String msg = parts.length > 1 ? parts[1] : payload;
                shakeStage(mainStage);
                showCoolAlert(title, msg);
            });
        }
    }

    private void toggleAlert(boolean isSubscribing) {
        if (service == null) return;
        new Thread(() -> {
            try {
                if (isSubscribing) {
                    service.registerForAlerts(myCallback);
                    Platform.runLater(() -> {
                        alertBtn.setText("📡 Đang giám sát...");
                        alertBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(231, 76, 60, 0.6), 10, 0, 0, 0);");
                    });
                } else {
                    service.unregisterForAlerts(myCallback);
                    Platform.runLater(() -> {
                        alertBtn.setText("🔔 Cảnh Báo");
                        alertBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> alertBtn.setSelected(!isSubscribing));
            }
        }).start();
    }

    // ================== JAVA <-> JS BRIDGE ==================
    public static class JavaBridge {
        private final GuiClient mainApp;
        public JavaBridge(GuiClient app) { this.mainApp = app; }
        public void updateFromMap(String s) {
            if (mainApp != null) Platform.runLater(() -> {
                mainApp.cityInput.setText(s);
                mainApp.updateWeather();
            });
        }
        public void onMarkerHover(String s) {
            WeatherData d = mainApp.weatherCache.get(s);
            if (d != null) {
                Platform.runLater(() -> {
                    String script = String.format(Locale.US, "updateMarkerTooltip('%s', %.1f, %.0f, '%s')", s, d.getTemperature(), d.getHumidity(), d.getIcon());
                    mainApp.mapView.getEngine().executeScript(script);
                });
            } else {
                new Thread(() -> {
                    try {
                        if (mainApp.service == null) return;
                        WeatherData data = mainApp.service.getWeatherInformation(s);
                        mainApp.weatherCache.put(s, data);
                        Platform.runLater(() -> {
                            String script = String.format(Locale.US, "updateMarkerTooltip('%s', %.1f, %.0f, '%s')", s, data.getTemperature(), data.getHumidity(), data.getIcon());
                            mainApp.mapView.getEngine().executeScript(script);
                        });
                    } catch (Exception ignored) {}
                }).start();
            }
        }
    }

    private void connectToServer() {
        try {
            Registry r = LocateRegistry.getRegistry("localhost", 1099);
            service = (WeatherService) r.lookup("WeatherSystem");
        } catch (Exception ignored) {}
    }

    private void preloadAllMarkers(List<String> cities) {
        new Thread(() -> {
            for (String city : cities) {
                try {
                    WeatherData d = service.getWeatherInformation(city);
                    weatherCache.put(city, d);
                    Platform.runLater(() -> {
                        String script = String.format(Locale.US, "updateMarkerTooltip('%s', %.1f, %.0f, '%s')", city, d.getTemperature(), d.getHumidity(), d.getIcon());
                        mapView.getEngine().executeScript(script);
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }
        }).start();
    }

    // ================== ALERT UI ==================
    private void showCoolAlert(String title, String message) {
        Stage alertStage = new Stage();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        alertStage.initOwner(mainStage);
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #8B0000, #1a0505); -fx-border-color: #FF0000; -fx-border-width: 3; -fx-background-radius: 20; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(255, 0, 0, 0.8), 30, 0, 0, 0);");
        Label iconLabel = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 60px; -fx-text-fill: #FFD700;");
        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setFont(Font.font("Arial Black", 24));
        lblTitle.setTextFill(Color.WHITE);
        lblTitle.setWrapText(true);
        Label lblMsg = new Label(message);
        lblMsg.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblMsg.setTextFill(Color.web("#ffcccc"));
        lblMsg.setWrapText(true);
        lblMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button btnDismiss = new Button("XÁC NHẬN AN TOÀN");
        btnDismiss.setStyle("-fx-background-color: #FF0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-cursor: hand;");
        btnDismiss.setOnAction(e -> alertStage.close());
        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                try { Thread.sleep(600); } catch (Exception ignored) {}
            }
        }).start();
        root.getChildren().addAll(iconLabel, lblTitle, lblMsg, btnDismiss);
        Scene scene = new Scene(root);
        scene.setFill(null);
        alertStage.setScene(scene);
        alertStage.show();
        alertStage.setX(mainStage.getX() + mainStage.getWidth() / 2 - 225);
        alertStage.setY(mainStage.getY() + mainStage.getHeight() / 2 - 175);
    }

    private void shakeStage(Stage stage) {
        Timeline timeline = new Timeline();
        double originalX = stage.getX();
        double originalY = stage.getY();
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * 50), e -> {
                stage.setX(originalX + r.nextInt(20) - 10);
                stage.setY(originalY + r.nextInt(20) - 10);
            }));
        }
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(500), e -> {
            stage.setX(originalX);
            stage.setY(originalY);
        }));
        timeline.play();
    }

    // ================== MỞ PROFILE ==================
    private void openProfileWindow() {
        try {
            Stage profileStage = new Stage();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/weatherclientapp/user/Profile.fxml"));
            profileStage.setScene(new Scene(loader.load()));
            profileStage.setTitle("Thông tin cá nhân");
            profileStage.show();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ================== OPEN CLIENT ==================
    public static void openClient(int dbId, String username) {
        Platform.runLater(() -> {
            try {
                GuiClient app = new GuiClient(dbId, username);
                Stage stage = new Stage();
                app.start(stage);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}