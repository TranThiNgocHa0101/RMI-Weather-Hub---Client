package com.weatherclientapp.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ChatWindow {

    private final String username;
    private Stage stage;

    // Thay TextArea bằng ScrollPane chứa VBox để làm bong bóng chat
    private VBox messageContainer;
    private ScrollPane scrollPane;
    private TextField inputField;

    // Cấu hình UDP
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private final int SERVER_PORT = 9876;

    public ChatWindow(String username) {
        this.username = username;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("Trợ lý ảo WeatherBot");

        // --- GIAO DIỆN CHÍNH ---
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        // 1. Header (Thanh tiêu đề đẹp)
        HBox header = new HBox(10);
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

        // Avatar Bot trên header
        StackPane botAvatar = createAvatar("WB", Color.web("#2980b9"));

        VBox titleBox = new VBox(2);
        Label title = new Label("WeatherBot AI");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        Label status = new Label("Đang hoạt động");
        status.setFont(Font.font("Segoe UI", 12));
        status.setTextFill(Color.GREEN);

        titleBox.getChildren().addAll(title, status);
        header.getChildren().addAll(botAvatar, titleBox);

        // 2. Vùng chứa tin nhắn (Message Area)
        messageContainer = new VBox(10); // Khoảng cách giữa các tin nhắn
        messageContainer.setPadding(new Insets(15));
        messageContainer.setStyle("-fx-background-color: #f5f6fa;"); // Màu nền xám nhẹ dịu mắt

        scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true); // Bong bóng tự giãn theo chiều ngang
        scrollPane.setStyle("-fx-background: #f5f6fa; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Tự động cuộn xuống dưới cùng khi có tin mới
        messageContainer.heightProperty().addListener((observable, oldValue, newValue) ->
                scrollPane.setVvalue(1.0));

        // 3. Vùng nhập liệu (Input Area)
        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(15));
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        inputField = new TextField();
        inputField.setPromptText("Nhập câu hỏi (vd: thời tiết Hà Nội)...");
        inputField.setStyle("-fx-background-radius: 20; -fx-padding: 10; -fx-font-size: 14px; -fx-background-color: #f0f2f5;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");

        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        inputBox.getChildren().addAll(inputField, sendBtn);

        // Lắp ráp Layout
        root.setTop(header);
        root.setCenter(scrollPane);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 400, 600);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> closeConnection());

        // Kết nối mạng
        initUdpConnection();

        stage.show();
    }

    // =======================================================
    // UI HELPER: TẠO BONG BÓNG CHAT (Messenger Style)
    // =======================================================
    private void addMessage(String text, boolean isUser) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(0, 0, 0, 0));

        // 1. Tạo bong bóng chứa text
        Label messageBubble = new Label(text);
        messageBubble.setWrapText(true); // Tự xuống dòng nếu quá dài
        messageBubble.setMaxWidth(280);  // Chiều rộng tối đa của bong bóng
        messageBubble.setPadding(new Insets(10, 15, 10, 15)); // Padding trong bong bóng
        messageBubble.setFont(Font.font("Segoe UI", 14));

        if (isUser) {
            // --- STYLE NGƯỜI DÙNG (Căn Phải - Màu Xanh) ---
            row.setAlignment(Pos.CENTER_RIGHT);
            messageBubble.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-background-radius: 18 18 2 18;");
            messageBubble.setTextFill(Color.WHITE);

            // Không cần avatar cho mình (giống Messenger)
            row.getChildren().add(messageBubble);

        } else {
            // --- STYLE BOT (Căn Trái - Màu Xám) ---
            row.setAlignment(Pos.CENTER_LEFT);
            messageBubble.setStyle("-fx-background-color: #e4e6eb; -fx-text-fill: black; -fx-background-radius: 18 18 18 2;");
            messageBubble.setTextFill(Color.BLACK);

            // Avatar Bot nhỏ bên cạnh tin nhắn
            StackPane avatar = createAvatar("WB", Color.web("#2980b9"));
            avatar.setMaxSize(30, 30); // Thu nhỏ avatar trong dòng chat

            row.getChildren().addAll(avatar, messageBubble);
        }

        // Thêm vào giao diện
        Platform.runLater(() -> {
            messageContainer.getChildren().add(row);
        });
    }

    // Hàm tạo Avatar hình tròn
    private StackPane createAvatar(String text, Color color) {
        Circle circle = new Circle(18, color);
        Text initial = new Text(text);
        initial.setFill(Color.WHITE);
        initial.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        return new StackPane(circle, initial);
    }

    // =======================================================
    // LOGIC UDP (GIỮ NGUYÊN NHƯNG GỌI addMessage THAY VÌ appendText)
    // =======================================================

    private void initUdpConnection() {
        try {
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
            startUdpListener();

            // Tin nhắn chào mừng từ hệ thống
            Platform.runLater(() -> addMessage("Chào " + username + "! Tôi có thể giúp gì cho bạn hôm nay?", false));

        } catch (Exception e) {
            Platform.runLater(() -> addMessage("⚠️ Lỗi kết nối Server: " + e.getMessage(), false));
        }
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        // 1. Hiện tin nhắn của mình (Bong bóng xanh, bên phải)
        addMessage(msg, true);
        inputField.clear();

        // 2. Gửi qua UDP
        new Thread(() -> {
            try {
                byte[] sendData = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, serverAddress, SERVER_PORT);

                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.send(sendPacket);
                }
            } catch (Exception e) {
                Platform.runLater(() -> addMessage("⚠️ Không gửi được tin nhắn.", false));
            }
        }).start();
    }

    private void startUdpListener() {
        Thread listener = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (udpSocket != null && !udpSocket.isClosed()) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(receivePacket);

                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

                    // Loại bỏ chữ "Bot: " ở đầu nếu có để hiển thị tự nhiên hơn
                    final String cleanResponse = response.startsWith("Bot:") ? response.substring(4).trim() : response;

                    // 2. Hiện tin nhắn Bot (Bong bóng xám, bên trái)
                    Platform.runLater(() -> addMessage(cleanResponse, false));

                } catch (Exception e) {
                    if (!udpSocket.isClosed()) e.printStackTrace();
                    break;
                }
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    private void closeConnection() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}