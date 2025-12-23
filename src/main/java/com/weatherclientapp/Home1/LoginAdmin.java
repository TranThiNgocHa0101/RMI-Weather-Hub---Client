package com.weatherclientapp.Home1;

import com.weatherclientapp.Home.alertMessage;
import com.weatherclientapp.Home.database;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.regex.Pattern;


public class LoginAdmin implements Initializable {
    @FXML private ComboBox<String> roleBox;

    @FXML
    private Button Back2A;

    @FXML
    private Button Backbtn1A;

    @FXML
    private PasswordField Confirm_SignUpA;

    @FXML
    private TextField Email_SignUpA;

    @FXML
    private AnchorPane ForgotFomA;

    @FXML
    private Hyperlink Forgot_signinA;

    @FXML
    private TextField ID_ForgotA;

    @FXML
    private TextField ID_SignUpA;

    @FXML
    private TextField ID_signin;

    @FXML
    private TextField Username_signinA;

    @FXML
    private PasswordField pass_signinA;
    @FXML
    private StackPane signupAdmin;

    @FXML
    private ComboBox<String> select_signinA;

    @FXML
    private CheckBox showPassA;

    @FXML
    private TextField showPass_signinA;

    private Connection connection;
    private PreparedStatement prepare;
    private ResultSet result;
    public void LoginBtn() {
        alertMessage alert = new alertMessage();
        if(ID_signin.getText().isEmpty() || Username_signinA.getText().isEmpty() || pass_signinA.getText().isEmpty()) {
            alert.errorMessage("Incorrect ID/Username/Password");
        }
        else {
            String selectData = "SELECT admin_id, username, password FROM admin WHERE " + "admin_id = ? and username = ? and password = ?";
            connection = database.connectionDb();

            if(showPassA.isSelected()) {
                pass_signinA.setText(showPass_signinA.getText());
            } else {
                showPass_signinA.setText(pass_signinA.getText());
            }
            try{
                prepare = connection.prepareStatement(selectData);
                prepare.setString(1, ID_signin.getText());
                prepare.setString(2, Username_signinA.getText());
                prepare.setString(3, pass_signinA.getText());

                result = prepare.executeQuery();
                if(result.next()) {

                    alert.successMessage("Successfully Login!");

                    Parent root = FXMLLoader.load(getClass().getResource("/com/weatherclientapp/Admin/admin.fxml"));
                    Stage stage = new Stage();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.show();
                    Stage currentStage = (Stage) signupAdmin.getScene().getWindow(); // btnLogin là Button bạn click
                    currentStage.close();
                }
                else {
                    alert.errorMessage("Incorrect ID/Username/Password");
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void ShowPass() {
        if(showPassA.isSelected()) {
            showPass_signinA.setText(pass_signinA.getText());
            showPass_signinA.setVisible(true);
            pass_signinA.setVisible(false);
        }
        else {
            pass_signinA.setText(showPass_signinA.getText());
            showPass_signinA.setVisible(false);
            pass_signinA.setVisible(true);
        }
    }

    @FXML
    public void selectUser(ActionEvent event) {
        String selectedRole = roleBox.getSelectionModel().getSelectedItem();
        try {
            Parent root = null;
            if("Client".equals(selectedRole)) {
                root = FXMLLoader.load(getClass().getResource("/com/weatherclientapp/Login/signup.fxml"));
            } else if("Admin".equals(selectedRole)) {
                root = FXMLLoader.load(getClass().getResource("/com/weatherclientapp/Login/signupAdmin.fxml"));
            }
            if(root != null) {
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> roles = FXCollections.observableArrayList("Client", "Admin");
        roleBox.setItems(roles);
        roleBox.setValue("Admin");

    }
}
