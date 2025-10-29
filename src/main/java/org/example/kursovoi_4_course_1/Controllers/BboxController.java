package org.example.kursovoi_4_course_1.Controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.example.kursovoi_4_course_1.App;
import org.example.kursovoi_4_course_1.InnerClasses.BufferedImageTranscoder;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class BboxController extends Controller {

    private Application app;
    private Context context;

    @FXML
    private ImageView logoImageView;

    @FXML
    private StackPane cameraContainer;

    @FXML
    private AnchorPane cameraPane;

    @FXML
    private Label model1Value;

    @FXML
    private Label model2Value;

    @FXML
    private AnchorPane sideDrawer;

    @FXML
    private Button toggleButton;

    @FXML
    private Button adminButton;

    @FXML
    private Button logoutButton;

    private Webcam webcam = Webcam.getDefault();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread grabberThread;
    private ImageView cameraImageView;
    private static final double CONTAINER_W = 360.0;
    private static final double CONTAINER_H = 360.0;
    private static final double PANE_W = 340.0;
    private static final double PANE_H = 340.0;
    private boolean drawerOpen = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();
        this.app = context.getApp();

        loadIcons(logoImageView,140);
        model1Value.setText("w2201");
        model2Value.setText("waw201");

        cameraContainer.setPrefSize(CONTAINER_W, CONTAINER_H);
        cameraPane.setPrefSize(PANE_W, PANE_H);

        cameraImageView = new ImageView();
        cameraImageView.setPreserveRatio(true);
        cameraImageView.setSmooth(true);
        cameraImageView.setCache(true);
        cameraImageView.setFitWidth(PANE_W);
        cameraImageView.setFitHeight(PANE_H);
        cameraPane.getChildren().add(cameraImageView);
        AnchorPane.setTopAnchor(cameraImageView, 0.0);
        AnchorPane.setBottomAnchor(cameraImageView, 0.0);
        AnchorPane.setLeftAnchor(cameraImageView, 0.0);
        AnchorPane.setRightAnchor(cameraImageView, 0.0);

        startCamera();
    }


    private void startCamera() {
        if (running.getAndSet(true)) return;
        if (webcam == null) {
            running.set(false);
            return;
        }

        try {
            Dimension[] supported = webcam.getViewSizes();
            Dimension target = Arrays.stream(supported)
                    .sorted(Comparator.comparingInt(d -> d.width))
                    .filter(d -> d.width >= 640)
                    .findFirst()
                    .orElse(supported[supported.length - 1]);
            webcam.setViewSize(target);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        webcam.open();
        grabberThread = new Thread(this::grabFrames, "WebcamGrabber");
        grabberThread.setDaemon(true);
        grabberThread.start();
    }

    private void grabFrames() {
        final int sleepMs = 33;
        while (running.get()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    Image fxImage = SwingFXUtils.toFXImage(image, null);
                    Platform.runLater(() -> cameraImageView.setImage(fxImage));
                }
                Thread.sleep(sleepMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @FXML
    private void handleToggleDrawer() {
        double distance = 120;
        TranslateTransition slide = new TranslateTransition(Duration.millis(20), sideDrawer);
        FadeTransition fadeAdmin = new FadeTransition(Duration.millis(20), adminButton);
        FadeTransition fadeLogout = new FadeTransition(Duration.millis(20), logoutButton);

        if (!drawerOpen) {
            slide.setByX(-distance);
            fadeAdmin.setToValue(1);
            fadeLogout.setToValue(1);
            adminButton.setVisible(true);
            logoutButton.setVisible(true);
            toggleButton.setText("→");
            toggleButton.setMaxWidth(170.0);
        } else {
            slide.setByX(distance);
            fadeAdmin.setToValue(0);
            fadeLogout.setToValue(0);

            fadeLogout.setOnFinished(e -> {
                adminButton.setVisible(false);
                logoutButton.setVisible(false);
            });
            toggleButton.setText("←");
            toggleButton.setMaxWidth(44.0);
        }

        slide.play();
        fadeAdmin.play();
        fadeLogout.play();
        drawerOpen = !drawerOpen;
    }

    @FXML
    private void handleAdminLogin() {
        stop();
        context.setIsAdminLogin(true);
        context.switchScene("Login-view.fxml");
    }

    @FXML
    private void handleLogout() {
        stop();
        context.switchScene("Login-view.fxml");
    }

    private void stop() {
        running.set(false);
        if (grabberThread != null) {
            try {
                grabberThread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (webcam != null && webcam.isOpen()) webcam.close();
        Platform.runLater(() -> {
            if (cameraImageView != null) cameraImageView.setImage(null);
        });
    }


}
