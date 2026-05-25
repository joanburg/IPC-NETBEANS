package controllerFiles;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class EditProfileController implements Initializable {

    @FXML private Label nickLabel;
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private DatePicker dobPicker;
    @FXML private ImageView avatarView;

    @FXML private Label emailErrorLabel;
    @FXML private Label passErrorLabel;
    @FXML private Label dobErrorLabel;
    @FXML private Button saveButton;

    private SportActivityApp app;
    private User currentUser;
    private Image currentAvatar;
    private String avatarPath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = SportActivityApp.getInstance();
        currentUser = app.getCurrentUser();

        nickLabel.setText(currentUser.getNickName());
        emailField.setText(currentUser.getEmail());
        dobPicker.setValue(currentUser.getBirthDate());

        avatarPath = currentUser.getAvatarPath();

        if (avatarPath != null && !avatarPath.isEmpty()) {
            avatarView.setImage(new Image(avatarPath));
        }

        setupValidation();
    }

    private void setupValidation() {
        emailField.textProperty().addListener((o, oldV, newV) -> {
            emailErrorLabel.setText(User.checkEmail(newV) ? "" : "Invalid email format.");
            updateSaveButtonState();
        });

        passField.textProperty().addListener((o, oldV, newV) -> {
            if (newV.isEmpty()) {
                passErrorLabel.setText("");
            } else {
                passErrorLabel.setText(User.checkPassword(newV) ? "" : "8-20 chars: Upper, Lower, Digit, and Special.");
            }
            updateSaveButtonState();
        });

        dobPicker.valueProperty().addListener((o, oldV, newV) -> {
            dobErrorLabel.setText(User.isOlderThan(newV, 12) ? "" : "Must be 12+ years old.");
            updateSaveButtonState();
        });
    }

    private void updateSaveButtonState() {
        boolean isEmailValid = User.checkEmail(emailField.getText());
        boolean isPassValid = passField.getText().isEmpty() || User.checkPassword(passField.getText());
        boolean isDobValid = dobPicker.getValue() != null && User.isOlderThan(dobPicker.getValue(), 12);

        saveButton.setDisable(!(isEmailValid && isPassValid && isDobValid));
    }

    @FXML
    private void handleSelectImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select New Avatar");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(((Node)event.getSource()).getScene().getWindow());

        if (file != null) {
            avatarPath = file.toURI().toString();
            avatarView.setImage(new Image(avatarPath));
        }
    }

    @FXML
    private void handleSave(ActionEvent event) throws IOException {
        String email = emailField.getText();
        String pass = passField.getText();
        LocalDate dob = dobPicker.getValue();

        if (pass.isEmpty()) {
            pass = currentUser.getPassword();
        }

        boolean success = app.updateCurrentUser(email, pass, dob, avatarPath);

        if (success) {
            handleBack(event);
        } else {
            emailErrorLabel.setText("Update failed. Please check your connection.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Map.fxml"));
        switchScene(event, root, "map");
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }
}