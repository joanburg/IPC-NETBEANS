package controllerFiles;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.User;
import upv.ipc.sportlib.SportActivityApp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;

public class RegistrationController implements Initializable {

    @FXML private TextField nickField;
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private DatePicker dobPicker;

    @FXML private Label nickErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label passErrorLabel;
    @FXML private Label dobErrorLabel;

    @FXML private Button registerButton;
    @FXML private ImageView avatarView;
    private String avatarPath;
    private SportActivityApp app;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = SportActivityApp.getInstance();
        
       nickField.textProperty().addListener((obs, oldV, newV) -> {
          if (newV.isBlank()) nickErrorLabel.setText("");
            else if (!User.checkNickName(newV)) nickErrorLabel.setText("6-15 chars, letters/digits only.");
            else if (app.nickNameExists(newV)) nickErrorLabel.setText("Nickname already in use.");
            else nickErrorLabel.setText("");
          });

        emailField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.isBlank()) emailErrorLabel.setText("");
            else if (!User.checkEmail(newV)) emailErrorLabel.setText("Invalid email format.");
            else emailErrorLabel.setText("");
        });

        passField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.isBlank()) passErrorLabel.setText("");
            else if (!User.checkPassword(newV)) passErrorLabel.setText("8-20 chars: Upper, Lower, Digit, and Special.");
            else passErrorLabel.setText("");
        });

        dobPicker.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) dobErrorLabel.setText("");
            else if (!User.isOlderThan(newV, 12)) dobErrorLabel.setText("Must be 12+ years old.");
            else dobErrorLabel.setText("");
        });

        try {

            URL defaultUrl = getClass().getResource("/resources/logo.png");
            if (defaultUrl != null) {
                avatarPath = defaultUrl.toExternalForm();
                avatarView.setImage(new Image(avatarPath));
            }
        } catch (Exception e) {
            System.out.println("Could not load default avatar. Check path: /resources/logo.png");
        }

        BooleanBinding isInvalid = Bindings.createBooleanBinding(
                () -> !User.checkNickName(nickField.getText()) ||
                         app.nickNameExists(nickField.getText()) ||
                        !User.checkEmail(emailField.getText()) ||
                        !User.checkPassword(passField.getText()) ||
                        dobPicker.getValue() == null ||
                        !User.isOlderThan(dobPicker.getValue(), 12),
                nickField.textProperty(),
                emailField.textProperty(),
                passField.textProperty(),
                dobPicker.valueProperty()
        );

        registerButton.disableProperty().bind(isInvalid);
    }


    @FXML
    private void handleRegister(ActionEvent event) throws IOException{
        String nick = nickField.getText();
        String email = emailField.getText();
        String pass = passField.getText();
        LocalDate dob = dobPicker.getValue();

        boolean registered = app.registerUser(nick, email, pass, dob, avatarPath);
        if(registered) {
            Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Welcome.fxml"));
            switchScene(event, root, "Welcome - Running la Safor");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Welcome.fxml"));
        switchScene(event, root, "Welcome - Running la Safor");
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

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }
}