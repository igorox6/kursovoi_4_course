package org.example.kursovoi_4_course_1.Controllers;

import ai.onnxruntime.OrtException;
import com.github.sarxos.webcam.Webcam;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.DBClasses.TypeDisplay;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManager;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManagerBbox;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManagerPoints;

import java.awt.*;
import java.awt.image.BufferedImage;
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

    private static final double PANE_W = 340.0;
    private static final double PANE_H = 340.0;
    private static final int FPS_SLEEP_MS = 33;
    private static final int MIN_WEB_CAM_WIDTH = 640;
    private static final Color BBOX_COLOR = Color.RED;
    private static final Color POINTS_COLOR = Color.GREEN;
    private static final int POINT_SIZE = 3;
    private static final int CROP_MARGIN = 10;

    private Webcam webcam = Webcam.getDefault();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread grabberThread;
    private ImageView cameraImageView;
    private boolean drawerOpen = false;

    private ModelManager modelManager;
    private ModelManagerBbox detector;
    private ModelManagerPoints pointsManager;
    private boolean bboxLoaded = false;
    private boolean pointsLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();
        loadIcons(logoImageView, 140);
        model1Value.setText("bbox: loading...");
        model2Value.setText("points: loading...");
        setupCameraView();
        loadModelsAsync();
        startCamera();
    }

    private void setupCameraView() {
        cameraImageView = new ImageView();
        cameraImageView.setPreserveRatio(true);
        cameraImageView.setSmooth(true);
        cameraImageView.setFitWidth(PANE_W);
        cameraImageView.setFitHeight(PANE_H);
        cameraPane.getChildren().add(cameraImageView);
    }

    private void loadModelsAsync() {
        try {
            modelManager = new ModelManager();
            modelManager.asyncRefreshModels()
                    .thenRun(() -> {
                        detector = modelManager.getBboxManager();
                        pointsManager = modelManager.getPointsManager();
                        bboxLoaded = (detector != null);
                        pointsLoaded = (pointsManager != null);
                        Platform.runLater(() -> {
                            model1Value.setText("bbox: " + (bboxLoaded ? "loaded " : "not loaded "));
                            model2Value.setText("points: " + (pointsLoaded ? "loaded " : "not loaded"));
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            model1Value.setText("bbox: error ");
                            model2Value.setText("points: error ");
                        });
                        bboxLoaded = false;
                        pointsLoaded = false;
                        return null;
                    });
        } catch (Exception e) {
            bboxLoaded = false;
            pointsLoaded = false;
        }
    }

    private void startCamera() {
        if (running.getAndSet(true)) return;
        if (webcam == null) {
            running.set(false);
            return;
        }
        try {
            java.awt.Dimension[] sizes = webcam.getViewSizes();
            java.awt.Dimension targetSize = Arrays.stream(sizes)
                    .sorted(Comparator.comparingInt(d -> -d.width * d.height))
                    .filter(d -> d.width >= MIN_WEB_CAM_WIDTH)
                    .findFirst()
                    .orElse(sizes[0]);
            webcam.setViewSize(targetSize);
        } catch (Exception ex) {
        }
        webcam.open();
        grabberThread = new Thread(this::grabFrames, "WebcamGrabber");
        grabberThread.setDaemon(true);
        grabberThread.start();
    }

    private void grabFrames() {
        while (running.get()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image == null) {
                    Thread.sleep(100);
                    continue;
                }
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                TypeDisplay displayType = context.getTypeDisplay();
                float[] bbox = null;
                if (bboxLoaded && detector != null) {
                    bbox = detector.predict(image);
                    if (bbox != null && bbox.length == 4) {
                        int x = Math.max(0, (int) bbox[0]);
                        int y = Math.max(0, (int) bbox[1]);
                        int w = Math.min(image.getWidth() - x, (int) bbox[2]);
                        int h = Math.min(image.getHeight() - y, (int) bbox[3]);
                        if (w > 0 && h > 0) {
                            if (displayType == TypeDisplay.BBOX || displayType == TypeDisplay.ALL) {
                                g.setColor(BBOX_COLOR);
                                g.setStroke(new BasicStroke(2));
                                g.drawRect(x, y, w, h);
                            }
                        }
                    }
                }
                if (pointsLoaded && pointsManager != null &&
                        (displayType == TypeDisplay.KEYPOINTS || displayType == TypeDisplay.ALL)) {
                    BufferedImage cropImage = image;
                    int cropX = 0, cropY = 0, cropW = image.getWidth(), cropH = image.getHeight();
                    if (bbox != null) {
                        cropX = Math.max(0, (int) bbox[0] - CROP_MARGIN);
                        cropY = Math.max(0, (int) bbox[1] - CROP_MARGIN);
                        cropW = Math.min(image.getWidth() - cropX, (int) bbox[2] + 2 * CROP_MARGIN);
                        cropH = Math.min(image.getHeight() - cropY, (int) bbox[3] + 2 * CROP_MARGIN);
                        if (cropX >= 0 && cropY >= 0 && cropW > 0 && cropH > 0 &&
                                cropX + cropW <= image.getWidth() && cropY + cropH <= image.getHeight()) {
                            cropImage = image.getSubimage(cropX, cropY, cropW, cropH);
                        } else {
                            cropImage = image;
                            cropX = 0;
                            cropY = 0;
                            cropW = image.getWidth();
                            cropH = image.getHeight();
                        }
                    }
                    float[] keypointsNorm = pointsManager.runInference(cropImage);
                    if (keypointsNorm != null && keypointsNorm.length == 30) {
                        g.setColor(POINTS_COLOR);
                        for (int i = 0; i < 30; i += 2) {
                            float kpX = keypointsNorm[i];
                            float kpY = keypointsNorm[i + 1];
                            if (kpX >= 0 && kpX <= 1 && kpY >= 0 && kpY <= 1) {
                                int drawX = cropX + (int) (kpX * cropW);
                                int drawY = cropY + (int) (kpY * cropH);
                                drawX = Math.max(0, Math.min(image.getWidth() - 1, drawX));
                                drawY = Math.max(0, Math.min(image.getHeight() - 1, drawY));
                                g.fillOval(drawX - POINT_SIZE / 2, drawY - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
                            }
                        }
                    }
                }
                g.dispose();
                WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                Platform.runLater(() -> cameraImageView.setImage(fxImage));
                Thread.sleep(FPS_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
            }
        }
    }

    @FXML
    private void handleToggleDrawer() {
        if (sideDrawer == null || toggleButton == null) return;
        double distance = 120;
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), sideDrawer);
        FadeTransition fadeAdmin = new FadeTransition(Duration.millis(200), adminButton);
        FadeTransition fadeLogout = new FadeTransition(Duration.millis(200), logoutButton);
        if (!drawerOpen) {
            slide.setByX(-distance);
            fadeAdmin.setToValue(1);
            fadeLogout.setToValue(1);
            if (adminButton != null) adminButton.setVisible(true);
            if (logoutButton != null) logoutButton.setVisible(true);
            toggleButton.setText("→");
            toggleButton.setMaxWidth(170.0);
        } else {
            slide.setByX(distance);
            fadeAdmin.setToValue(0);
            fadeLogout.setToValue(0);
            fadeLogout.setOnFinished(e -> {
                if (adminButton != null) adminButton.setVisible(false);
                if (logoutButton != null) logoutButton.setVisible(false);
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
        if (grabberThread != null && grabberThread.isAlive()) {
            try {
                grabberThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        Platform.runLater(() -> {
            if (cameraImageView != null) cameraImageView.setImage(null);
        });
        if (modelManager != null) {
            try {
                modelManager.close();
            } catch (OrtException e) {
            }
        }
    }
}
