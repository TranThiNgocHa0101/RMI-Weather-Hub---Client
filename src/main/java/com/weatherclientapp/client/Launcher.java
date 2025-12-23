package com.weatherclientapp.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/weatherclientapp/Login/signup.fxml"));
            Parent root = loader.load();
            stage.setTitle("Login");
            stage.setScene(new Scene(root, 330, 560)); // set đúng kích thước FXML
            stage.setResizable(false); // không cho resize
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
