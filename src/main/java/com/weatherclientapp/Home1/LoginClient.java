package com.weatherclientapp.Home1;

import com.weatherclientapp.Home.alertMessage;
import com.weatherclientapp.Home.data;
import com.weatherclientapp.Home.database;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import com.weatherclientapp.client.GuiClient;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ResourceBundle;




public class LoginClient implements Initializable {

    @FXML private AnchorPane loginForm;
    @FXML private AnchorPane SignUpForm;
    @FXML private AnchorPane ForgotForm;
    @FXML private AnchorPane PassForm;

    @FXML private TextField username_signin;
    @FXML private PasswordField pass_signin;
    @FXML private TextField showPass_signin;
    @FXML private CheckBox showPass;
    @FXML private Button loginbtn;
    @FXML private Hyperlink Forgot_signin;
    @FXML private Button createbtn;
    @FXML private Button Signinbtn;

    @FXML private TextField Username_SignUp;
    @FXML private TextField Email_SignUp;
    @FXML private PasswordField Pass_SignUp;
    @FXML private PasswordField Confirm_SignUp;
    @FXML private Button Signupbtn;
    @FXML private Button Backbtn1;

    @FXML private TextField username_Forgot;
    @FXML private Button Proceedbtn;
    @FXML private Button Backbtn;

    @FXML private PasswordField pass_Pass;
    @FXML private PasswordField confirm_Pass;
    @FXML private Button changePassbtn;
    @FXML private ComboBox roleBox;
    @FXML private ImageView faceidbtn;
    @FXML private ImageView faceid_Signbtn;
    @FXML private ImageView faceid_Forgetbtn;

    private Connection connection;
    private PreparedStatement prepare;
    private ResultSet result;
    private alertMessage alert = new alertMessage();

    @FXML
    public void switchFormSelect(ActionEvent event) {
        try {
            String role = (String) roleBox.getValue();
            if (role == null) {
                alertMessage alert = new alertMessage();
                alert.errorMessage("Please select Admin or Client");
                return;
            }

            Parent root;
            if (role.equals("Admin")) {
                root = FXMLLoader.load(getClass().getResource("/com/weatherclientapp/Login/signupAdmin.fxml"));

                ((Stage) loginForm.getScene().getWindow()).close();

            } else {
                return;
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    public void LoginBtn() {
        if(username_signin.getText().isEmpty() || pass_signin.getText().isEmpty()){
            alert.errorMessage("Incorrect Username/Password");
            return;
        }

        String selectData = "SELECT username, password, face_image FROM client WHERE username = ? AND password = ?";
        connection = database.connectionDb();

        try {
            prepare = connection.prepareStatement(selectData);
            prepare.setString(1, username_signin.getText());
            prepare.setString(2, pass_signin.getText());
            result = prepare.executeQuery();

            if(result.next()) {
                String dbUsername = result.getString("username");
                String dbPassword = result.getString("password");

                // Login thành công
                int dbId=1;
                data.id=dbId;
                data.username = dbUsername;
                alert.successMessage("Successfully Login!");

// Mở giao diện Client
                com.weatherclientapp.client.GuiClient.openClient(dbId,dbUsername);

// Đóng cửa sổ login
                loginbtn.getScene().getWindow().hide();


            } else {
                alert.errorMessage("Incorrect Username/Password");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void ShowPass() {
        if(showPass.isSelected()) {
            showPass_signin.setText(pass_signin.getText());
            showPass_signin.setVisible(true);
            pass_signin.setVisible(false);
        } else {
            pass_signin.setText(showPass_signin.getText());
            showPass_signin.setVisible(false);
            pass_signin.setVisible(true);
        }
    }

    @FXML
    public void register() {
        if(Email_SignUp.getText().isEmpty() || Username_SignUp.getText().isEmpty()
                || Pass_SignUp.getText().isEmpty() || Confirm_SignUp.getText().isEmpty()){
            alert.errorMessage("All fields are necessary to be filled");
            return;
        }

        if(!Pass_SignUp.getText().equals(Confirm_SignUp.getText())){
            alert.errorMessage("Password does not match");
            return;
        }

        if(Pass_SignUp.getText().length() < 8){
            alert.errorMessage("Invalid Password, at least 8 characters needed");
            return;
        }

        String checkUsername = "SELECT * FROM client WHERE username = ?";
        connection = database.connectionDb();

        try {
            // Kiểm tra username đã tồn tại chưa
            prepare = connection.prepareStatement(checkUsername);
            prepare.setString(1, Username_SignUp.getText());
            result = prepare.executeQuery();

            if(result.next()) {
                alert.errorMessage(Username_SignUp.getText() + " is already taken");
                return;
            }

            // Insert user mới
            String insertData = "INSERT INTO client (email, username, password, date, update_date) VALUES (?,?,?,?,?)";
            prepare = connection.prepareStatement(insertData);
            prepare.setString(1, Email_SignUp.getText());
            prepare.setString(2, Username_SignUp.getText());
            prepare.setString(3, Pass_SignUp.getText());
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            prepare.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            prepare.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            prepare.executeUpdate();

            // --- Gửi email ---
            String selectEmail = "SELECT email FROM client WHERE username = ?";
            prepare = connection.prepareStatement(selectEmail);
            prepare.setString(1, Username_SignUp.getText());
            ResultSet emailResult = prepare.executeQuery();
            String toEmail = null;
            if(emailResult.next()) {
                toEmail = emailResult.getString("email");
            } else {
                alert.errorMessage("Failed to retrieve email from database");
                return;
            }

            String subject = "Registration Successful";
            String body = "Hello " + Username_SignUp.getText() + ",\n\n" +
                    "You have successfully registered to our system.\n\n" +
                    "Thanks,\nFaceID Team";

            String finalToEmail = toEmail;
            new Thread(() -> {
                try {
                    SendMail.send(finalToEmail, subject, body);
                    javafx.application.Platform.runLater(() ->
                            alert.successMessage("Email sent successfully to " + finalToEmail));
                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() ->
                            alert.errorMessage("Failed to send email"));
                }
            }).start();

            registerClearFields();
            SignUpForm.setVisible(false);
            loginForm.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void registerClearFields() {
        Email_SignUp.setText("");
        Username_SignUp.setText("");
        Pass_SignUp.setText("");
        Confirm_SignUp.setText("");
    }

    // ------------------- SWITCH FORM -------------------
    @FXML
    public void switchFormLogin(ActionEvent event) {
        Object src = event.getSource();

        if(src == Signinbtn || src == Backbtn || src == Backbtn1) {
            loginForm.setVisible(true);
            SignUpForm.setVisible(false);
            ForgotForm.setVisible(false);
            PassForm.setVisible(false);
        } else if(src == createbtn) {
            SignUpForm.setVisible(true);
            loginForm.setVisible(false);
            ForgotForm.setVisible(false);
            PassForm.setVisible(false);
        } else if(src == Forgot_signin) {
            ForgotForm.setVisible(true);
            loginForm.setVisible(false);
            SignUpForm.setVisible(false);
            PassForm.setVisible(false);
        }
    }
    @FXML
    public void forgotPassword() {
        if(username_Forgot.getText().isEmpty()) {
            alert.errorMessage("Please fill all blank fields");
            return;
        }

        String selectData = "SELECT username, face_image FROM client WHERE username = ?";
        connection = database.connectionDb();

        try {
            prepare = connection.prepareStatement(selectData);
            prepare.setString(1, username_Forgot.getText());
            result = prepare.executeQuery();

            if(result.next()) {
                String dbUsername = result.getString("username");
                alert.successMessage("Identity verified! You can reset your password.");

                loginForm.setVisible(false);
                SignUpForm.setVisible(false);
                ForgotForm.setVisible(false);
                PassForm.setVisible(true);

            } else {
                alert.errorMessage("Username not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert.errorMessage("Error during verification");
        }
    }



    @FXML
    public void changePassword() throws SQLException {

        if (pass_Pass.getText().isEmpty() || confirm_Pass.getText().isEmpty()) {
            alert.errorMessage("Please fill all blank fields");
            return;
        }

        if (!pass_Pass.getText().equals(confirm_Pass.getText())) {
            alert.errorMessage("Password does not match");
            return;
        }

        if (pass_Pass.getText().length() < 8) {
            alert.errorMessage("Invalid Password, at least 8 character needed");
            return;
        }

        // UPDATE PASSWORD
        String updateData = "UPDATE client SET password = ?, update_date = ? WHERE username = ?";
        connection = database.connectionDb();

        try {
            prepare = connection.prepareStatement(updateData);
            prepare.setString(1, pass_Pass.getText());
            prepare.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            prepare.setString(3, username_Forgot.getText());
            prepare.executeUpdate();

            alert.successMessage("Successfully changed Password");

            // Reset form
            PassForm.setVisible(false);
            loginForm.setVisible(true);
            username_signin.setText("");
            pass_signin.setText("");
            pass_signin.setVisible(true);
            showPass_signin.setVisible(false);
            showPass.setSelected(false);
            pass_Pass.setText("");
            confirm_Pass.setText("");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // GET EMAIL - SỬA LẠI PHẦN NÀY
        String selectEmail = "SELECT email FROM client WHERE username = ?";
        prepare = connection.prepareStatement(selectEmail);
        prepare.setString(1, username_Forgot.getText());  // Dùng username đổi pass
        ResultSet emailResult = prepare.executeQuery();

        String toEmail = null;
        if (emailResult.next()) {
            toEmail = emailResult.getString("email");
        } else {
            alert.errorMessage("Failed to retrieve email from database");
            return;
        }

        // CONTENT EMAIL
        String subject = "Password Changed Successfully";
        String body = "Hello " + username_Forgot.getText() + ",\n\n"
                + "Your password has been successfully changed.\n\n"
                + "If this wasn't you, please contact FaceID Team immediately.\n\n"
                + "Regards,\nFaceID Team";

        String finalToEmail = toEmail;

        new Thread(() -> {
            try {
                SendMail.send(finalToEmail, subject, body);
                javafx.application.Platform.runLater(() ->
                        alert.successMessage("Email sent successfully to " + finalToEmail));
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        alert.errorMessage("Failed to send email"));
            }
        }).start();
    }



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loginForm.setVisible(true);
        SignUpForm.setVisible(false);
        ForgotForm.setVisible(false);
        PassForm.setVisible(false);
        showPass_signin.setVisible(false);

    }
}
