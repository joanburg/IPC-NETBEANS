package controllerFiles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class SessionHistoryController implements Initializable {

    @FXML private TableView<Session> sessionTable;
    @FXML private TableColumn<Session, LocalDateTime> colDate;
    @FXML private TableColumn<Session, String> colDuration;
    @FXML private TableColumn<Session, Integer> colImported;
    @FXML private TableColumn<Session, Integer> colViewed;
    @FXML private TableColumn<Session, Integer> colAnnotations;

    @FXML private Label totalImportedLabel;
    @FXML private Label totalViewedLabel;
    @FXML private Label totalAnnotationsLabel;

    private SportActivityApp app;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = SportActivityApp.getInstance();
        colImported.setCellValueFactory(new PropertyValueFactory<>("ImportedActivities"));
        colViewed.setCellValueFactory(new PropertyValueFactory<>("ViewedActivities"));
        colAnnotations.setCellValueFactory(new PropertyValueFactory<>("AnnotationsCreated"));

        colDuration.setCellValueFactory(cellData -> {
            long seconds = cellData.getValue().getDuration().toSeconds();
            return new javafx.beans.property.SimpleStringProperty(formatDuration(seconds));
        });

        List<Session> sessions = app.getSessionsByUser(app.getCurrentUser());
        ObservableList<Session> sessionData = FXCollections.observableArrayList(sessions);
        sessionTable.setItems(sessionData);
        
        calculateTotals(sessions);
    }

    private void calculateTotals(List<Session> sessions) {
        int imp = 0, view = 0, ann = 0;
        for (Session s : sessions) {
            imp += s.getImportedActivities();
            view += s.getViewedActivities();
            ann += s.getAnnotationsCreated();
        }
        totalImportedLabel.setText(String.valueOf(imp));
        totalViewedLabel.setText(String.valueOf(view));
        totalAnnotationsLabel.setText(String.valueOf(ann));
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException{
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
