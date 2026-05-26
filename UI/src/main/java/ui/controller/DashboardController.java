package ui.controller;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import service.ShutdownService;
import windowsController.WindowControllerFacade;

public class DashboardController {

    private static final int DEFAULT_SHUTDOWN_TIME_SECONDS = 10;

    @FXML private ListView<String> windowsList;
    @FXML private ListView<String> shutdownList;

    // Поля для Duration mode
    @FXML private VBox durationBox;
    @FXML private TextField hoursField;
    @FXML private TextField minutesField;

    // Поля для Exact time mode
    @FXML private VBox exactBox;
    @FXML private TextField hourField;
    @FXML private TextField minuteField;

    @FXML private RadioButton durationMode;
    @FXML private ToggleGroup modeGroup;

    @FXML private Button startButton;

    private final ObservableList<String> windows = FXCollections.observableArrayList();
    private final ObservableList<String> shutdownTargets = FXCollections.observableArrayList();

    private Timeline timeline;
    private long secondsLeft;
    private boolean running = false;

    private WindowControllerFacade windowControllerFacade = new WindowControllerFacade();

    @FXML
    public void initialize() {
        windows.addAll(windowControllerFacade.getWindowNames());
        windowsList.setItems(windows);
        shutdownList.setItems(shutdownTargets);

        startButton.setOnAction(e -> toggleTimer());

        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> switchMode());

        switchMode();
    }

    private void switchMode() {
        boolean duration = durationMode.isSelected();
        durationBox.setVisible(duration);
        durationBox.setManaged(duration);
        exactBox.setVisible(!duration);
        exactBox.setManaged(!duration);
    }

    private void toggleTimer() {
        if (!running) {
            if (calculateSeconds()) {
                startTimer();
                running = true;
            }
        } else {
            pauseTimer();
            running = false;
        }
    }

    private boolean calculateSeconds() {
        try {
            if (durationMode.isSelected()) {
                int h = Integer.parseInt(hoursField.getText().isEmpty() ? "0" : hoursField.getText());
                int m = Integer.parseInt(minutesField.getText().isEmpty() ? "0" : minutesField.getText());
                secondsLeft = (h * 3600L) + (m * 60L);
            } else {
                int h = Integer.parseInt(hourField.getText());
                int m = Integer.parseInt(minuteField.getText());
                LocalTime now = LocalTime.now();
                LocalTime target = LocalTime.of(h, m);

                secondsLeft = ChronoUnit.SECONDS.between(now, target);
                if (secondsLeft < 0) secondsLeft += 24 * 3600;
            }
            return secondsLeft > 0;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid numbers!");
            alert.show();
            return false;
        }
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft--;
            long h = secondsLeft / 3600;
            long m = (secondsLeft % 3600) / 60;
            long s = secondsLeft % 60;

            startButton.setText(String.format("Pause (%02d:%02d:%02d)", h, m, s));

            if (secondsLeft <= 0) {
                timeline.stop();
                executeShutdown();
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pauseTimer() {
        if (timeline != null) timeline.pause();
        startButton.setText("Resume");
    }

    private void executeShutdown() {
        List<String> targetsCopy = List.copyOf(shutdownTargets);

        startButton.setDisable(true);

        Thread shutdownThread = new Thread(() -> {
            try {
                System.out.println("Starting window closing process...");
                windowControllerFacade.closeAllWindows(targetsCopy);

                int attempts = 0;
                while (!windowControllerFacade.areWindowsClosed(targetsCopy) && attempts < 50) {
                    Thread.sleep(200);
                    attempts++;
                }

                javafx.application.Platform.runLater(
                        () -> ShutdownService.shutdownInSeconds(DEFAULT_SHUTDOWN_TIME_SECONDS));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    @FXML
    public void addToShutdownList() {
        String selected = windowsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            shutdownTargets.add(selected);
            windows.remove(selected);
        }
    }

    @FXML
    public void removeFromShutdownList() {
        String selected = shutdownList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            shutdownTargets.remove(selected);
            windows.add(selected);
        }
    }

    @FXML
    public void refreshWindowsList() {
        List<String> windowNames = windowControllerFacade.getRefreshedWindowNames();

        shutdownTargets.setAll(shutdownTargets.stream()
                .filter(windowNames::contains)
                .toList()
        );

        windows.setAll(windowNames.stream()
                .filter(a -> !shutdownTargets.contains(a))
                .toList()
        );
    }
}
