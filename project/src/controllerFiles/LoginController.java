package controllerFiles;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class LoginController implements Initializable {
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        BooleanBinding b = Bindings.createBooleanBinding(() -> userField.getText().isEmpty() || passField.getText().isEmpty(),
                userField.textProperty(), passField.textProperty());
        loginButton.disableProperty().bind(b);

    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String nick = userField.getText();
        String pass = passField.getText();

        if (nick.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        SportActivityApp app = SportActivityApp.getInstance();

        boolean authenticated = app.login(nick, pass);

        if (authenticated) {
            navigateToMainMap(event);
        } else {
            errorLabel.setText("Invalid nickname or password.");
        }
    }

    public void handleBackNav(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Welcome.fxml"));
        switchScene(event, root, "Welcome - Running la Safor");
    }

    private void navigateToMainMap(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Map.fxml"));
            switchScene(event, root, "Running la Safor - Dashboard");
        } catch (IOException e) {
            errorLabel.setText("Error loading the map interface.");
            e.printStackTrace();
        }
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }
}