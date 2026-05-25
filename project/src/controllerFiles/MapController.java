package controllerFiles;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.scene.control.Label;
import javafx.scene.Group;
import javafx.scene.control.ListView;
import javafx.scene.Node;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import upv.ipc.sportlib.*;
import javafx.scene.shape.Polyline;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class MapController implements Initializable {

    private Group zoomGroup;

    @FXML
    private Pane mapPane;
    @FXML
    private ListView<Activity> map_listview;
    @FXML
    private ScrollPane map_scrollpane;
    @FXML
    private Slider zoom_slider;
    @FXML
    private Label mousePosition;
    @FXML
    private Button statsButton;

    private ContextMenu mapContextMenu;
    private boolean insertionMode = false;
    private SportActivityApp app;
    private MapProjection projection;
    private MapRegion currentRegion;
    private Activity currentActivity = null;
    private GeoPoint firstPoint = null; // Stores the start/center
    private AnnotationType pendingType = null; // Stores what we are drawing
    private String pendingText = "";
    private String pendingColor = "#FF0000";
    @FXML
    private Label sessionStats;
    @FXML
    private SplitPane splitPane;
    @FXML
    private ImageView mapView;

    private LineChart<Number, Number> elevationChart;
    private Circle mapMarker;
    private boolean chartVisible = false;
    @FXML
    private Button cumulativeButton;
    
    private Stage legendStage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = SportActivityApp.getInstance();
        statsButton.setDisable(true);
        zoom_slider.setMin(0.5); // zoom mínimo: 50 %
        zoom_slider.setMax(1.5); // zoom máximo: 150 %
        zoom_slider.setValue(1.0); // valor inicial: 100 %

        refreshActivityList();
        map_listview.getSelectionModel().clearSelection();
        zoom_slider.valueProperty().addListener(
                (observable, oldVal, newVal) -> zoom((Double) newVal));

        MenuItem miText = new MenuItem("📝 Add Text");
        MenuItem miCircle = new MenuItem("⭕ Add Point");
        mapContextMenu = new ContextMenu(miText, miCircle);

        map_listview.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                statsButton.setDisable(false);
                loadActivityData(newVal);
            }
        });

        map_listview.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Activity activity, boolean empty) {
                super.updateItem(activity, empty);
                if (empty || activity == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String date = activity.getStartTime() != null ? activity.getStartTime().toLocalDate().toString()
                            : "Unknown date";
                    setText(activity.getName() + " — " + date);
                    setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                }
            }
        });
        buildMap(new File("maps/upv.jpg"));
    }

    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText(
                "sceneX: " + (int) event.getSceneX() +
                        ", sceneY: " + (int) event.getSceneY() + "\n" +
                        "         X: " + (int) event.getX() +
                        ",          Y: " + (int) event.getY());
    }

    @FXML
    void zoomIn(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal + 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal - 0.1);
    }

    private void zoom(double scaleValue) {
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();

        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);

        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    void listClicked(MouseEvent event) {

    }

    @FXML
    private void handleStats(ActionEvent event) throws IOException {
        closeLegend();
        if (this.currentActivity == null) {
            showError("No activity loaded. Please import a GPX file first.");
            return;
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/Statistics.fxml"));
        Parent root = loader.load();

        StatisticsController controller = loader.getController();
        controller.setActivityData(currentActivity);
        switchSceneMenu(event, root, "Activity Statistics", true);

    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        closeLegend();
        SportActivityApp app = SportActivityApp.getInstance();
        app.logout();
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Welcome.fxml"));
        switchSceneMenu(event, root, "Demo mapas - IPC", false);

    }

    @FXML
    private void handleProfile(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/EditProfile.fxml"));
        switchSceneMenu(event, root, "Edit Profile", false);
    }

    @FXML
    private void handleSessions(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/SessionsHistory.fxml"));
        switchSceneMenu(event, root, "Sessions History", false);
    }

    @FXML
    private void handleImportGPX(ActionEvent event) {
        closeLegend();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select GPX File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GPX Files", "*.gpx"));
        File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            try {
                currentActivity = app.importActivity(file);

                if (currentActivity != null) {
                    refreshActivityList();
                    map_listview.getSelectionModel().select(currentActivity);
                    currentRegion = currentActivity.getSuggestedMap();
                    File mapImageFile = new File(currentRegion.getImagePath());
                    Image img = buildMap(mapImageFile);
                    this.projection = new MapProjection(currentRegion, img.getWidth(), img.getHeight());
                    drawRouteColoredBySpeed(currentActivity);
                    loadElevationChart(currentActivity);
                    Point2D startPixel = projection.project(currentActivity.getStartPoint());
                    centerMapOn(startPixel, img);
                    if (statsButton != null)
                        statsButton.setDisable(false);
                }
            } catch (Exception e) {
                showError("Error processing GPX: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddMapMenu(ActionEvent event) throws IOException {
        closeLegend();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/AddMap.fxml"));
        Parent root = loader.load();
        switchSceneMenu(event, root, "Add New Map", false);
    }

    // Auxiliary method

    private void refreshActivityList() {
        List<Activity> activities = app.getUserActivities();
        map_listview.getItems().setAll(activities);
        map_listview.getSelectionModel().clearSelection();
    }

    private void loadActivityData(Activity activity) {
        closeLegend();
        currentActivity = activity;
        currentRegion = activity.getSuggestedMap();
        if (currentRegion == null) {
            showError("No map region found for this activity.");
            return;
        }
        File mapFile = new File(currentRegion.getImagePath());
        Image mapImage = buildMap(mapFile);

        if (mapImage != null) {
            this.projection = new MapProjection(currentRegion, mapImage.getWidth(), mapImage.getHeight());
            drawRouteColoredBySpeed(activity);
            for (Annotation ann : activity.getAnnotations()) {
                displayAnnotation(ann);
            }
            loadElevationChart(activity);

        }
        
            Point2D startPixel = projection.project(activity.getStartPoint());
            centerMapOn(startPixel, mapImage);

    }

    private void showInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Next Step");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private Image buildMap(File imgFile) {
        if (!imgFile.exists()) {
            map_scrollpane.setContent(new Label("Image not found: " + imgFile.getAbsolutePath()));
            return null;
        }

        Image img = new Image(imgFile.toURI().toString());
        double w = img.getWidth();
        double h = img.getHeight();

        mapPane.getChildren().clear();

        ImageView background = new ImageView(img);
        background.setFitWidth(w);
        background.setFitHeight(h);
        background.setPreserveRatio(true);
        mapPane.getChildren().add(background);
        mapPane.setPrefSize(w, h);
        mapPane.setMinSize(w, h);
        mapPane.setMaxSize(w, h);
        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                onMapRightClick(e.getX(), e.getY());
            } else if (e.getButton() == MouseButton.PRIMARY) {

                if (firstPoint != null) {
                    complexAnnotation(e.getX(), e.getY());
                } else if (insertionMode) {
                    insertionMode = false;
                    mapPane.setStyle("");
                    addPoi(e.getX(), e.getY());
                }
            }
        });

        if (zoomGroup == null) {
            zoomGroup = new Group(mapPane);
        } else {
            zoomGroup.getChildren().setAll(mapPane);
        }

        Group contentGroup = new Group(zoomGroup);
        map_scrollpane.setContent(contentGroup);

        double zoomLevel = zoom_slider.getValue();
        zoomGroup.setScaleX(zoomLevel);
        zoomGroup.setScaleY(zoomLevel);

        return img;
    }

    private void onMapRightClick(double x, double y) {
        if (currentActivity == null || projection == null)
            return;

        GeoPoint geoPoint = projection.unproject(x, y);

        Dialog<Annotation> dialog = new Dialog<>();
        dialog.setTitle("Add Annotation");
        dialog.setHeaderText("Create a new map annotation");

        TextField textDescription = new TextField();
        ColorPicker picker = new ColorPicker(Color.RED);
        ComboBox<AnnotationType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(AnnotationType.values()));
        typeCombo.setValue(AnnotationType.POINT);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(textDescription, 1, 1);
        grid.add(new Label("Color:"), 0, 2);
        grid.add(picker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // AI code
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                AnnotationType type = typeCombo.getValue();

                if (type == AnnotationType.POINT || type == AnnotationType.TEXT) {
                    return new Annotation(type, textDescription.getText(), toHex(picker.getValue()), 2.0,
                            List.of(geoPoint));
                } else {
                    this.firstPoint = geoPoint;
                    this.pendingType = type;
                    this.pendingText = textDescription.getText();
                    this.pendingColor = toHex(picker.getValue());

                    mapPane.setCursor(Cursor.CROSSHAIR);
                    showInformation("Click anywhere on the map to set the "
                            + (type == AnnotationType.LINE ? "end point" : "radius"));
                    return null;
                }
            }
            return null;
        });
        // end of AI code

        dialog.showAndWait().ifPresent(ann -> {
            Annotation saved = app.addAnnotation(currentActivity, ann);
            if (saved != null)
                displayAnnotation(saved);
        });
    }

    private void complexAnnotation(double x, double y) {
        GeoPoint secondPoint = projection.unproject(x, y);
        Annotation complexAnn = new Annotation(
                pendingType,
                pendingText,
                pendingColor,
                3.0,
                List.of(firstPoint, secondPoint));
        Annotation saved = app.addAnnotation(currentActivity, complexAnn);
        if (saved != null) {
            displayAnnotation(saved);
        }
        firstPoint = null;
        pendingType = null;
        mapPane.setCursor(Cursor.DEFAULT);
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // AI code
    private void displayAnnotation(Annotation ann) {
        List<GeoPoint> gps = ann.getGeoPoints();
        if (gps.isEmpty() || projection == null)
            return;

        Color annotationColor = Color.web(ann.getColor());
        Point2D p1 = projection.project(gps.get(0));

        switch (ann.getType()) {
            case POINT:
                Circle dot = new Circle(p1.getX(), p1.getY(), 5, annotationColor);
                dot.setStroke(Color.WHITE);
                mapPane.getChildren().add(dot);
                break;

            case TEXT:
                break;

            case LINE:
                if (gps.size() >= 2) {
                    Point2D p2 = projection.project(gps.get(1));
                    Line line = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    line.setStroke(annotationColor);
                    line.setStrokeWidth(ann.getStrokeWidth());
                    mapPane.getChildren().add(line);
                }
                break;

            case CIRCLE:
                if (gps.size() >= 2) {
                    Point2D edge = projection.project(gps.get(1));
                    double pixelRadius = p1.distance(edge);
                    if (pixelRadius > 500)
                        pixelRadius = 50;
                    Circle circle = new Circle(p1.getX(), p1.getY(), pixelRadius);
                    circle.setStroke(annotationColor);
                    circle.setStrokeWidth(ann.getStrokeWidth());
                    circle.setFill(annotationColor.deriveColor(0, 1, 1, 0.3));
                    mapPane.getChildren().add(circle);
                }
                break;

            default:
                break;
        }

        addLabel(p1, ann);
    }

    private void addLabel(Point2D pos, Annotation ann) {
        if (ann.getText() == null || ann.getText().isEmpty())
            return;

        Label label = new Label(ann.getText());
        label.setTextFill(Color.web(ann.getColor()));
        label.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 8 4 8; " +
                "-fx-background-radius: 5; " +
                "-fx-border-color: " + ann.getColor() + "; " +
                "-fx-border-radius: 5; " +
                "-fx-border-width: 1;");

        label.setLayoutX(pos.getX() + 10);
        label.setLayoutY(pos.getY() - 15);

        mapPane.getChildren().add(label);
    }
    // end of AI code

    private void drawRoute(Activity activity) {
        List<TrackPoint> points = activity.getTrackPoints();
        if (points == null || points.isEmpty())
            return;

        Polyline routeLine = new Polyline();
        routeLine.setStroke(Color.BLUE);
        routeLine.setStrokeWidth(3);
        routeLine.getStrokeDashArray().addAll(5.0, 5.0);

        for (TrackPoint tp : points) {
            Point2D pixel = projection.project(tp);
            routeLine.getPoints().addAll(pixel.getX(), pixel.getY());
        }
        Point2D startPx = projection.project(activity.getStartPoint());
        Circle startMarker = new Circle(startPx.getX(), startPx.getY(), 6, Color.LIMEGREEN);
        startMarker.setStroke(Color.BLACK);

        Point2D endPx = projection.project(activity.getEndPoint());
        Circle endMarker = new Circle(endPx.getX(), endPx.getY(), 6, Color.RED);
        endMarker.setStroke(Color.BLACK);

        mapPane.getChildren().addAll(routeLine, startMarker, endMarker);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void addPoi(double x, double y) {
        if (currentActivity == null || projection == null) {
            showError("Please select or import an activity first.");
            return;
        }

        Dialog<Annotation> poiDialog = new Dialog<>();
        poiDialog.setTitle("New Point of Interest");
        poiDialog.setHeaderText("Mark a location on the map");
        ButtonType okButton = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        TextField nameField = new TextField();
        nameField.setPromptText("Name of POI (e.g. Refreshment Point)");
        VBox vbox = new VBox(10, new Label("Name:"), nameField);
        poiDialog.getDialogPane().setContent(vbox);
        GeoPoint geoPos = projection.unproject(x, y);
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return new Annotation(
                        AnnotationType.POINT,
                        nameField.getText().trim(),
                        "#3498db", // Nice blue color
                        2.0,
                        List.of(geoPos));
            }
            return null;
        });
        Optional<Annotation> result = poiDialog.showAndWait();
        if (result.isPresent()) {
            Annotation saved = app.addAnnotation(currentActivity, result.get());
            if (saved != null) {
                displayAnnotation(saved);
            }
        }
    }

    private void switchSceneMenu(ActionEvent event, Parent root, String title, boolean wait) {
        if (wait) {
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            Stage mainStage = (Stage) map_scrollpane.getScene().getWindow();
            stage.initOwner(mainStage);
            stage.showAndWait();

        } else {
            Stage stage = (Stage) map_scrollpane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        }
    }

    @FXML
    private void handleCumulative(ActionEvent event) throws IOException {
        closeLegend();
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/CumulativeMonth.fxml"));
        switchSceneMenu(event, root, "Monthly Stats", false);
    }

    private void loadElevationChart(Activity activity) {
        List<TrackPoint> points = activity.getTrackPoints();
        if (points == null || points.isEmpty())
            return;

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Distance (km)");
        yAxis.setLabel("Altitude (m)");

        elevationChart = new LineChart<>(xAxis, yAxis);
        elevationChart.setTitle("Elevation graph");
        elevationChart.setLegendVisible(false);
        elevationChart.setCreateSymbols(false);
        elevationChart.setPrefWidth(280);
        elevationChart.setAnimated(false);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        double accDist = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i > 0)
                accDist += points.get(i).distanceTo(points.get(i - 1));
            series.getData().add(new XYChart.Data<>(accDist / 1000.0, points.get(i).getElevation()));
        }
        elevationChart.getData().add(series);

        // Añadir a la interfaz
        if (!chartVisible) {
            splitPane.getItems().add(elevationChart);
            splitPane.setDividerPositions(0.22, 0.65);
            chartVisible = true;
        } else {
            splitPane.getItems().set(2, elevationChart);
        }

        if (mapMarker == null) {
            mapMarker = new Circle(7, Color.DODGERBLUE);
            mapMarker.setStroke(Color.WHITE);
            mapMarker.setStrokeWidth(2);
            mapMarker.setVisible(false);
            mapPane.getChildren().add(mapMarker);
        } else {
            mapMarker.setVisible(false);
        }

        setupChartMouseListener(points);
    }

    private void setupChartMouseListener(List<TrackPoint> points) {
        // Pre-calculate distances for the points
        double[] distances = new double[points.size()];
        double accDist = 0;
        distances[0] = 0;
        for (int i = 1; i < points.size(); i++) {
            accDist += points.get(i).distanceTo(points.get(i - 1));
            distances[i] = accDist;
        }

        Tooltip tooltip = new Tooltip();

        elevationChart.setOnMouseMoved(e -> {
            NumberAxis xAxis = (NumberAxis) elevationChart.getXAxis();
            double xInChart = xAxis.sceneToLocal(e.getSceneX(), e.getSceneY()).getX();
            double km = xAxis.getValueForDisplay(xInChart).doubleValue();

            if (km >= 0 && km <= xAxis.getUpperBound()) {
                int idx = findClosestIndex(distances, km * 1000.0);
                TrackPoint closest = points.get(idx);

                String info = String.format("Lat: %.5f\nLon: %.5f\nAlt: %.1f m\nDist: %.2f km",
                        closest.getLatitude(), closest.getLongitude(), closest.getElevation(), km);

                tooltip.setText(info);
                if (!tooltip.isShowing()) {
                    tooltip.show(elevationChart, e.getScreenX() + 15, e.getScreenY() + 15);
                } else {
                    tooltip.setAnchorX(e.getScreenX() + 15);
                    tooltip.setAnchorY(e.getScreenY() + 15);
                }

                Point2D p = projection.project(closest);
                mapMarker.setCenterX(p.getX());
                mapMarker.setCenterY(p.getY());
                mapMarker.setVisible(true);
            } else {
                tooltip.hide();
                mapMarker.setVisible(false);
            }
        });

        elevationChart.setOnMouseExited(e -> {
            tooltip.hide();
            mapMarker.setVisible(false);
        });
    }

    private int findClosestIndex(double[] distances, double targetDist) {
        int low = 0;
        int high = distances.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (distances[mid] < targetDist) {
                low = mid + 1;
            } else if (distances[mid] > targetDist) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        if (low >= distances.length) {
            return distances.length - 1;
        }
        if (high < 0) {
            return 0;
        }
        return (Math.abs(distances[low] - targetDist) < Math.abs(distances[high] - targetDist)) ? low : high;
    }

    // AI code
    private void drawRouteColoredBySpeed(Activity activity) {
        List<TrackPoint> points = activity.getTrackPoints();
        if (points == null || points.size() < 2)
            return;

        removeSpeedLegend();
        closeLegend();

        for (int i = 0; i < points.size() - 1; i++) {
            TrackPoint current = points.get(i);
            TrackPoint next = points.get(i + 1);

            double speedKmph = current.speedTo(next);
            Color segmentColor = getColorForSpeedFixed(speedKmph);

            Point2D p1 = projection.project(current);
            Point2D p2 = projection.project(next);

            Line segment = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            segment.setStroke(segmentColor);
            segment.setStrokeWidth(4);
            segment.setStrokeLineCap(StrokeLineCap.ROUND);

            mapPane.getChildren().add(segment);
        }

        addStartEndMarkers(activity);
        addSpeedLegend();
    }

    private Color getColorForSpeedFixed(double speedKmph) {
        if (speedKmph > 15)
            return Color.RED;
        if (speedKmph > 10)
            return Color.ORANGE;
        if (speedKmph > 6)
            return Color.YELLOW;
        if (speedKmph > 0)
            return Color.GREEN;
        return Color.GRAY;
    }

    private void addStartEndMarkers(Activity activity) {
        Point2D startPx = projection.project(activity.getStartPoint());
        Circle startMarker = new Circle(startPx.getX(), startPx.getY(), 7, Color.LIMEGREEN);
        startMarker.setStroke(Color.BLACK);
        startMarker.setStrokeWidth(1.5);

        Point2D endPx = projection.project(activity.getEndPoint());
        Circle endMarker = new Circle(endPx.getX(), endPx.getY(), 7, Color.RED);
        endMarker.setStroke(Color.BLACK);
        endMarker.setStrokeWidth(1.5);

        mapPane.getChildren().addAll(startMarker, endMarker);
    }

    private void addSpeedLegend() {
        if (legendStage != null && legendStage.isShowing()) {
            legendStage.close();
        }

        VBox legend = new VBox(4);
        legend.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-padding: 8 10 8 10; "
                + "-fx-border-color: #cccccc; -fx-border-radius: 5; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 2, 2);");

        Label titleLabel = new Label("🏃 Speed");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        legend.getChildren().add(titleLabel);

        legend.getChildren().add(createLegendItem(Color.RED, ">15 km/h"));
        legend.getChildren().add(createLegendItem(Color.ORANGE, "10-15 km/h"));
        legend.getChildren().add(createLegendItem(Color.YELLOW, "6-10 km/h"));
        legend.getChildren().add(createLegendItem(Color.GREEN, "0-6 km/h"));

        legendStage = new Stage();
        legendStage.initModality(Modality.NONE);
        legendStage.initOwner(map_scrollpane.getScene().getWindow());
        legendStage.setAlwaysOnTop(true);
        legendStage.setResizable(false);
        legendStage.setTitle("");
        legendStage.setX(10);
        legendStage.setY(60);

        Scene legendScene = new Scene(legend);
        legendScene.setFill(Color.TRANSPARENT);
        legendStage.setScene(legendScene);
        legendStage.show();

        map_scrollpane.getScene().getWindow().xProperty().addListener((obs, old, newVal) -> {
            if (legendStage != null) {
                legendStage.setX(newVal.doubleValue() + 10);
            }
        });
        map_scrollpane.getScene().getWindow().yProperty().addListener((obs, old, newVal) -> {
            if (legendStage != null) {
                legendStage.setY(newVal.doubleValue() + 60);
            }
        });
    }

    private void removeSpeedLegend() {
        mapPane.getChildren().removeIf(node -> {
            Object data = node.getUserData();
            return data != null && "speedLegend".equals(data);
        });

        if (legendStage != null) {
            legendStage.close();
            legendStage = null;
        }
    }

    private HBox createLegendItem(Color color, String text) {
        Circle circle = new Circle(6, color);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(0.5);
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px;");
        HBox hbox = new HBox(8, circle, label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        return hbox;
    }
    
    private void closeLegend() {
        if (legendStage != null && legendStage.isShowing()) {
            legendStage.close();
            legendStage = null;
        }
    }
    
    @FXML
    private void onMenuShowing() {
        closeLegend();
    }
    
    @FXML
    private void handleDeleteActivity(ActionEvent event) {
        Activity selected = map_listview.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an activity to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Activity");
        confirm.setHeaderText("Delete " + selected.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            app.removeActivity(selected);
            refreshActivityList();
            mapPane.getChildren().clear();
            buildMap(new File("maps/upv.jpg"));
            closeLegend();
            showInformation("Activity deleted successfully.");
        }
    }
    // end of AI code

    
    //AI code
    private void centerMapOn(Point2D pixelPoint, Image mapImage) {
    // Necesitamos esperar a que el layout esté listo antes de calcular
    map_scrollpane.applyCss();
    map_scrollpane.layout();

    double zoomLevel   = zoom_slider.getValue();
    double contentW    = mapImage.getWidth()  * zoomLevel;
    double contentH    = mapImage.getHeight() * zoomLevel;
    double viewportW   = map_scrollpane.getViewportBounds().getWidth();
    double viewportH   = map_scrollpane.getViewportBounds().getHeight();

    // Coordenadas del punto en el espacio escalado
    double scaledX = pixelPoint.getX() * zoomLevel;
    double scaledY = pixelPoint.getY() * zoomLevel;

    // Rango desplazable real
    double scrollableW = contentW - viewportW;
    double scrollableH = contentH - viewportH;

    if (scrollableW <= 0 || scrollableH <= 0) return; // imagen cabe entera, no hace falta

    double hVal = (scaledX - viewportW / 2.0) / scrollableW;
    double vVal = (scaledY - viewportH / 2.0) / scrollableH;

    // Clamp a [0,1]
    hVal = Math.max(0, Math.min(1, hVal));
    vVal = Math.max(0, Math.min(1, vVal));

    final double fH = hVal;
    final double fV = vVal;

    // Runlater garantiza que el scroll se aplica después del render
    javafx.application.Platform.runLater(() -> {
        map_scrollpane.setHvalue(fH);
        map_scrollpane.setVvalue(fV);
    });
}
    //end of AI code
}