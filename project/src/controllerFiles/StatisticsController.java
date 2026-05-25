package controllerFiles;

import javafx.fxml.Initializable;
import upv.ipc.sportlib.Activity;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import upv.ipc.sportlib.SportActivityApp;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

public class StatisticsController implements Initializable {

    @FXML
    private Label nameLabel, distLabel, durLabel, speedLabel, paceLabel;
    @FXML
    private Label posGainLabel, negGainLabel, minAltLabel, maxAltLabel;

    public void setActivityData(Activity activity) {
        nameLabel.setText(activity.getName());

        double kms = activity.getTotalDistance() / 1000.0;
        distLabel.setText(String.format("%.2f km", kms));

        durLabel.setText(formatDuration(activity.getDuration()));

        speedLabel.setText(String.format("%.2f km/h", activity.getAverageSpeed()));
        paceLabel.setText(String.format("%.2f min/km", activity.getAveragePace()));

        posGainLabel.setText(String.format("%.1f m", activity.getElevationGain()));
        negGainLabel.setText(String.format("%.1f m", activity.getElevationLoss()));
        minAltLabel.setText(String.format("%.1f m", activity.getMinElevation()));
        maxAltLabel.setText(String.format("%.1f m", activity.getMaxElevation()));
    }

    private String formatDuration(Duration d) {
        long s = d.getSeconds();
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SportActivityApp app = SportActivityApp.getInstance();
        if (app.getCurrentUser() != null) {
            List<Activity> activities = app.getUserActivities();
            if (!activities.isEmpty()) {
                setActivityData(activities.get(0));
            }
        }
    }
}
