package com.weatherclientapp.client;

import com.weatherclientapp.Home.database;
import com.weatherclientapp.data.User;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate;

public class Profile {

    /* ===== FXML ===== */
    @FXML private ImageView avatarImage;
    @FXML private Button btnUpload, btnSave, btnTogglePassword;
    @FXML private TextField txtId, txtUsername, txtEmail, txtPasswordVisible;
    @FXML private PasswordField txtPassword;
    @FXML private DatePicker dpDate, dpUpdateDate;

    private File selectedImageFile;
    private User currentUser;

    /* ===== INIT ===== */
    @FXML
    public void initialize() {
        initPasswordToggle();
        loadUserFromDB(1);
        Circle clip = new Circle();

        // luôn đúng tâm và đúng bán kính
        clip.centerXProperty().bind(avatarImage.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(avatarImage.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(
                avatarImage.fitWidthProperty().divide(2)
        );

        avatarImage.setClip(clip);
    }

    /* ===== LOAD USER ===== */
    private void loadUserFromDB(int userId) {
        String sql = "SELECT * FROM client WHERE id=?";

        try (Connection con = database.connectionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentUser = new User();
                currentUser.setId(rs.getInt("id"));
                currentUser.setEmail(rs.getString("email"));
                currentUser.setUsername(rs.getString("username"));
                currentUser.setPassword(rs.getString("password"));
                currentUser.setFaceImage(rs.getBytes("face_image"));
                currentUser.setDate(rs.getDate("date").toLocalDate());
                currentUser.setUpdateDate(rs.getDate("update_date").toLocalDate());

                // Fill UI
                txtId.setText(String.valueOf(currentUser.getId()));
                txtEmail.setText(currentUser.getEmail());
                txtUsername.setText(currentUser.getUsername());
                txtPassword.setText(currentUser.getPassword());
                dpDate.setValue(currentUser.getDate());
                dpUpdateDate.setValue(currentUser.getUpdateDate());

                if (currentUser.getFaceImage() != null) {
                    avatarImage.setImage(
                            new Image(new ByteArrayInputStream(currentUser.getFaceImage()))
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===== PASSWORD TOGGLE ===== */
    private void initPasswordToggle() {
        txtPasswordVisible.managedProperty().bind(txtPasswordVisible.visibleProperty());
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        btnTogglePassword.setOnAction(e -> {
            boolean show = txtPasswordVisible.isVisible();
            txtPasswordVisible.setVisible(!show);
            txtPassword.setVisible(show);
        });
    }

    /* ===== UPLOAD IMAGE ===== */
    @FXML
    private void uploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Avatar");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fc.showOpenDialog(btnUpload.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            avatarImage.setImage(new Image(file.toURI().toString()));
        }
    }

    /* ===== SAVE / UPDATE ===== */
    @FXML
    private void saveProfile() {
        String sql = """
            UPDATE client
            SET email=?, username=?, password=?, face_image=?, update_date=?
            WHERE id=?
        """;

        try (Connection con = database.connectionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, txtEmail.getText());
            ps.setString(2, txtUsername.getText());
            ps.setString(3, txtPassword.getText());

            if (selectedImageFile != null) {
                ps.setBytes(4, Files.readAllBytes(selectedImageFile.toPath()));
            } else {
                ps.setBytes(4, currentUser.getFaceImage());
            }

            ps.setDate(5, Date.valueOf(LocalDate.now()));
            ps.setInt(6, Integer.parseInt(txtId.getText()));

            ps.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION,
                    "Profile updated successfully!").show();
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();


        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Update failed!").show();
        }
    }
}
