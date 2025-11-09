package org.example.kursovoi_4_course_1.Controllers;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.github.sarxos.webcam.Webcam;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManager;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManagerBbox;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManagerPoints;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class BboxController extends Controller {

    private Context context;

    @FXML private ImageView logoImageView;
    @FXML private StackPane cameraContainer;
    @FXML private AnchorPane cameraPane;
    @FXML private Label model1Value;
    @FXML private Label model2Value;
    @FXML private AnchorPane sideDrawer;
    @FXML private Button toggleButton;
    @FXML private Button adminButton;
    @FXML private Button logoutButton;

    private Webcam webcam = Webcam.getDefault();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread grabberThread;
    private ImageView cameraImageView;
    private static final double PANE_W = 340.0;
    private static final double PANE_H = 340.0;
    private boolean drawerOpen = false;

    // Model manager integration
    private ModelManager modelManager;
    private ModelManagerBbox detector;

    private boolean bboxLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();

        loadIcons(logoImageView, 140);
        model1Value.setText("bbox");
        model2Value.setText("points");

        cameraImageView = new ImageView();
        cameraImageView.setPreserveRatio(true);
        cameraImageView.setSmooth(true);
        cameraImageView.setFitWidth(PANE_W);
        cameraImageView.setFitHeight(PANE_H);
        cameraPane.getChildren().add(cameraImageView);

        // Загрузка моделей через ModelManager (поскольку модель в базе)
        try {
            modelManager = new ModelManager();
            modelManager.refreshModels();  // Загружает из API
            detector = modelManager.getBboxManager();
            if (detector != null) {
                bboxLoaded = true;
                System.out.println("✅ FACE_BBOX model loaded from API.");
            } else {
                System.err.println("❌ No BBOX model available.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading models: " + e.getMessage());
            e.printStackTrace();
            bboxLoaded = false;
        }

        startCamera();
    }

    private void startCamera() {
        if (running.getAndSet(true)) return;
        if (webcam == null) {
            running.set(false);
            return;
        }

        try {
            Dimension target = Arrays.stream(webcam.getViewSizes())
                    .sorted(Comparator.comparingInt(d -> d.width))
                    .filter(d -> d.width >= 640)
                    .findFirst()
                    .orElse(webcam.getViewSizes()[0]);
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
        final int sleepMs = 33; // ~30 FPS
        while (running.get()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image == null) continue;

                Graphics2D g = image.createGraphics();

                if (bboxLoaded && detector != null) {
                    float[] bbox = detector.predict(image);  // Уже в пикселях + сглаженный
                    if (bbox != null && bbox.length == 4) {
                        int x = (int) bbox[0];
                        int y = (int) bbox[1];
                        int w = (int) bbox[2];
                        int h = (int) bbox[3];

                        // Защита от некорректных значений
                        if (x >= 0 && y >= 0 && w > 0 && h > 0 &&
                                x + w <= image.getWidth() && y + h <= image.getHeight()) {
                            g.setColor(Color.RED);
                            g.setStroke(new BasicStroke(2));
                            g.drawRect(x, y, w, h);
                        }
                    }
                }

                g.dispose();
                Image fxImage = SwingFXUtils.toFXImage(image, null);
                Platform.runLater(() -> cameraImageView.setImage(fxImage));

                Thread.sleep(sleepMs);

            } catch (Exception e) {
                e.printStackTrace();
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
        Platform.runLater(() -> cameraImageView.setImage(null));

        try {
            if (modelManager != null) modelManager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}