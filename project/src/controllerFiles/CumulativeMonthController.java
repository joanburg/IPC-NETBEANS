package controllerFiles;

import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.Activity;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class CumulativeMonthController {

    @FXML private Label total_time;
    @FXML private Label total_distance;
    @FXML private Label total_ascent;
    @FXML private Label total_descent;
    @FXML private Button go_back;
    @FXML private ComboBox<String> monthPicker;
    @FXML private Spinner<Integer> yearPicker;

    private SportActivityApp app;

    @FXML
    private void initialize() {
        app = SportActivityApp.getInstance();

        List<String> months = new java.util.ArrayList<>();
        for (Month m : Month.values()) {
            months.add(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        monthPicker.setItems(FXCollections.observableArrayList(months));

        LocalDate now = LocalDate.now();
        monthPicker.getSelectionModel().select(now.getMonthValue() - 1);

        yearPicker.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(2000, now.getYear(), now.getYear())
        );
        monthPicker.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> updateTotals());
        yearPicker.valueProperty().addListener((obs, oldV, newV) -> updateTotals());

        updateTotals();
    }

    private void updateTotals() {
        //AI code
        int selectedMonth = monthPicker.getSelectionModel().getSelectedIndex() + 1;
        int selectedYear = yearPicker.getValue();

        List<Activity> activities = app.getUserActivities();

        Duration totalTime = Duration.ZERO;
        double totalDistance = 0;
        double totalAscent = 0;
        double totalDescent = 0;

        for (Activity a : activities) {
            LocalDate actDate = a.getStartTime().toLocalDate();
            if (actDate.getMonthValue() == selectedMonth && actDate.getYear() == selectedYear) {
                totalTime = totalTime.plus(a.getDuration());
                totalDistance += a.getTotalDistance();
                totalAscent += a.getElevationGain();
                totalDescent += a.getElevationLoss();
            }
        }

        total_time.setText(formatDuration(totalTime));
        total_distance.setText(String.format("%.2f km", totalDistance / 1000.0));
        total_ascent.setText(String.format("%.0f m", totalAscent));
        total_descent.setText(String.format("%.0f m", totalDescent));
        //end of AI code
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours > 0) return String.format("%dh %02dm", hours, minutes);
        else return String.format("%dm", minutes);
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
}