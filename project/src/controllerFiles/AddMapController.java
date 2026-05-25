/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllerFiles;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AddMapController {

    @FXML
    private TextField mapNameField;
    @FXML
    private Button selectImageButton;
    @FXML
    private Label imageFileLabel;
    @FXML
    private TextField latMinField;
    @FXML
    private TextField latMaxField;
    @FXML
    private TextField lonMinField;
    @FXML
    private TextField lonMaxField;
    @FXML
    private Label nameErrorLabel;
    @FXML
    private Label imageErrorLabel;
    @FXML
    private Label latMinErrorLabel;
    @FXML
    private Label latMaxErrorLabel;
    @FXML
    private Label lonMinErrorLabel;
    @FXML
    private Label lonMaxErrorLabel;
    @FXML
    private Button addMapButton;

    private SportActivityApp app;
    private File selectedImageFile;

    @FXML
    public void initialize() {
        app = SportActivityApp.getInstance();

        // Add listeners for real-time validation
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        mapNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                nameErrorLabel.setText("Map name is required");
            } else if (newVal.trim().length() < 3) {
                nameErrorLabel.setText("Name must be at least 3 characters");
            } else {
                nameErrorLabel.setText("");
            }
            updateAddMapButtonState();
        });

        latMinField.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinateField(latMinField,
                latMinErrorLabel, -90, 90, "Latitude Min"));
        latMaxField.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinateField(latMaxField,
                latMaxErrorLabel, -90, 90, "Latitude Max"));
        lonMinField.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinateField(lonMinField,
                lonMinErrorLabel, -180, 180, "Longitude Min"));
        lonMaxField.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinateField(lonMaxField,
                lonMaxErrorLabel, -180, 180, "Longitude Max"));
    }

    private void validateCoordinateField(TextField field, Label errorLabel, double min, double max, String fieldName) {
        String text = field.getText();
        if (text == null || text.trim().isEmpty()) {
            errorLabel.setText(fieldName + " is required");
        } else {
            try {
                double value = Double.parseDouble(text.trim());
                if (value < min || value > max) {
                    errorLabel.setText(fieldName + " must be between " + min + " and " + max);
                } else {
                    errorLabel.setText("");
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Invalid number format");
            }
        }
        updateAddMapButtonState();
    }

    private void updateAddMapButtonState() {
        boolean isNameValid = mapNameField.getText() != null && mapNameField.getText().trim().length() >= 3;
        boolean isImageValid = selectedImageFile != null && selectedImageFile.exists();
        boolean areCoordsValid = areCoordinatesValid();

        addMapButton.setDisable(!(isNameValid && isImageValid && areCoordsValid));
    }

    private boolean areCoordinatesValid() {
        try {
            double latMin = Double.parseDouble(latMinField.getText().trim());
            double latMax = Double.parseDouble(latMaxField.getText().trim());
            double lonMin = Double.parseDouble(lonMinField.getText().trim());
            double lonMax = Double.parseDouble(lonMaxField.getText().trim());

            // Check valid ranges
            if (latMin < -90 || latMin > 90)
                return false;
            if (latMax < -90 || latMax > 90)
                return false;
            if (lonMin < -180 || lonMin > 180)
                return false;
            if (lonMax < -180 || lonMax > 180)
                return false;

            // Check min < max
            if (latMin >= latMax)
                return false;
            if (lonMin >= lonMax)
                return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @FXML
    private void handleSelectImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Map Image File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imageFileLabel.setText(selectedImageFile.getName());
            imageErrorLabel.setText("");
            updateAddMapButtonState();
        }
    }

    @FXML
    private void handleAddMap(ActionEvent event) {
        try {
            String mapName = mapNameField.getText().trim();
            double latMin = Double.parseDouble(latMinField.getText().trim());
            double latMax = Double.parseDouble(latMaxField.getText().trim());
            double lonMin = Double.parseDouble(lonMinField.getText().trim());
            double lonMax = Double.parseDouble(lonMaxField.getText().trim());

            System.out.println("Adding map: " + mapName);
            System.out.println("Coordinates: latMin=" + latMin + ", latMax=" + latMax +
                    ", lonMin=" + lonMin + ", lonMax=" + lonMax);
            System.out.println("Image file: " + selectedImageFile.getAbsolutePath());

            // Create maps directory if it doesn't exist
            File mapsDir = new File("maps");
            if (!mapsDir.exists()) {
                mapsDir.mkdirs();
                System.out.println("Created maps directory");
            }

            // Copy image to maps directory
            String fileName = selectedImageFile.getName();
            if (!fileName.toLowerCase().endsWith(".jpg") && !fileName.toLowerCase().endsWith(".jpeg")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".jpg";
            }

            File targetFile = new File(mapsDir, fileName);
            Files.copy(selectedImageFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied image to: " + targetFile.getAbsolutePath());

            // Add the map region using the library
            SportActivityApp app = SportActivityApp.getInstance();
            MapRegion newRegion = app.addMapRegion(mapName, targetFile, latMin, latMax, lonMin, lonMax);

            if (newRegion != null) {
                System.out.println("✅ Map added successfully!");
                System.out.println("Region name: " + newRegion.getName());
                System.out.println("Region image path: " + newRegion.getImagePath());

                showSuccess("Map '" + mapName + "' has been successfully added to the system!");
                handleBack(event);
            } else {
                System.err.println("❌ addMapRegion returned null");
                showError("Failed to add map. Check console for details.");
            }

        } catch (IOException e) {
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            showError("Error copying image file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
            showError("Error adding map: " + e.getMessage());
        }
    }

    private boolean validateAllFields() {
        boolean valid = true;

        if (mapNameField.getText() == null || mapNameField.getText().trim().length() < 3) {
            nameErrorLabel.setText("Map name is required (min 3 characters)");
            valid = false;
        }

        if (selectedImageFile == null || !selectedImageFile.exists()) {
            imageErrorLabel.setText("Please select an image file");
            valid = false;
        }

        if (!areCoordinatesValid()) {
            // Individual error messages are already set by the listeners
            valid = false;
        }

        return valid;
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Map.fxml"));
        switchScene(event, root, "Map");
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Map Added Successfully");
        alert.setContentText(message);
        alert.showAndWait();
    }
}