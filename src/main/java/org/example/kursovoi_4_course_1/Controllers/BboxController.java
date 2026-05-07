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

    private static final Color BBOX_COLOR = new Color(0, 255, 0);
    private static final Color POINTS_COLOR = new Color(255, 0, 0);
    private static final Color POINTS_BORDER = new Color(255, 255, 255);
    private static final Color LINE_COLOR = new Color(255, 200, 0);
    private static final Color TEXT_COLOR = new Color(0, 255, 200);
    private static final Color TEXT_BG = new Color(0, 0, 0, 180);

    private static final int POINT_RADIUS = 4;
    private static final int CROP_MARGIN = 15;

    private static final int[][] CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3},
            {4, 5},
            {7, 8},
            {6, 9},
    };

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

                        bboxLoaded = detector != null;
                        pointsLoaded = pointsManager != null;

                        Platform.runLater(() -> {
                            model1Value.setText("bbox: " + (bboxLoaded ? "loaded" : "not loaded"));
                            model2Value.setText("points: " + (pointsLoaded ? "loaded (10 pts)" : "not loaded"));
                        });
                    })
                    .exceptionally(ex -> {
                        bboxLoaded = false;
                        pointsLoaded = false;

                        Platform.runLater(() -> {
                            model1Value.setText("bbox: error");
                            model2Value.setText("points: error");
                        });

                        System.err.println("Model load error: " + ex.getMessage());
                        ex.printStackTrace();

                        return null;
                    });

        } catch (Exception e) {
            bboxLoaded = false;
            pointsLoaded = false;

            model1Value.setText("bbox: error");
            model2Value.setText("points: error");

            System.err.println("ModelManager init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startCamera() {
        if (running.getAndSet(true)) return;

        if (webcam == null) {
            running.set(false);
            System.err.println("Webcam not found");
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
        } catch (Exception e) {
            System.err.println("Webcam size setup error: " + e.getMessage());
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
                int bboxX = 0;
                int bboxY = 0;
                int bboxW = 0;
                int bboxH = 0;

                // ===== BBOX =====
                if (bboxLoaded && detector != null) {
                    try {
                        bbox = detector.predict(image);
                    } catch (Exception e) {
                        bbox = null;
                        System.err.println("Bbox predict error: " + e.getMessage());
                    }

                    if (bbox != null && bbox.length == 4) {
                        bboxX = Math.max(0, (int) bbox[0]);
                        bboxY = Math.max(0, (int) bbox[1]);
                        bboxW = Math.min(image.getWidth() - bboxX, (int) bbox[2]);
                        bboxH = Math.min(image.getHeight() - bboxY, (int) bbox[3]);

                        if (bboxW > 0 && bboxH > 0) {
                            if (displayType == TypeDisplay.BBOX || displayType == TypeDisplay.ALL) {
                                g.setColor(BBOX_COLOR);
                                g.setStroke(new BasicStroke(2));
                                g.drawRect(bboxX, bboxY, bboxW, bboxH);
                            }
                        }
                    }
                }

                // ===== KEYPOINTS =====
                boolean needPoints = pointsLoaded
                        && pointsManager != null
                        && (displayType == TypeDisplay.KEYPOINTS || displayType == TypeDisplay.ALL);

                if (needPoints) {
                    BufferedImage cropImage = image;

                    int cropX = 0;
                    int cropY = 0;
                    int cropW = image.getWidth();
                    int cropH = image.getHeight();

                    if (bbox != null && bboxW > 20 && bboxH > 20) {
                        cropX = Math.max(0, bboxX - CROP_MARGIN);
                        cropY = Math.max(0, bboxY - CROP_MARGIN);
                        cropW = Math.min(image.getWidth() - cropX, bboxW + 2 * CROP_MARGIN);
                        cropH = Math.min(image.getHeight() - cropY, bboxH + 2 * CROP_MARGIN);

                        if (cropW > 10 && cropH > 10 &&
                                cropX + cropW <= image.getWidth() &&
                                cropY + cropH <= image.getHeight()) {
                            cropImage = image.getSubimage(cropX, cropY, cropW, cropH);
                        } else {
                            cropX = 0;
                            cropY = 0;
                            cropW = image.getWidth();
                            cropH = image.getHeight();
                            cropImage = image;
                        }
                    }

                    float[] keypointsNorm = null;

                    try {
                        keypointsNorm = pointsManager.runInference(cropImage);
                    } catch (Exception e) {
                        System.err.println("Points inference error: " + e.getMessage());
                    }

                    if (keypointsNorm != null && keypointsNorm.length == 20) {
                        int[] drawXs = new int[10];
                        int[] drawYs = new int[10];

                        for (int i = 0; i < 10; i++) {
                            float kpX = keypointsNorm[i * 2];
                            float kpY = keypointsNorm[i * 2 + 1];

                            drawXs[i] = cropX + (int) (kpX * cropW);
                            drawYs[i] = cropY + (int) (kpY * cropH);

                            drawXs[i] = Math.max(0, Math.min(image.getWidth() - 1, drawXs[i]));
                            drawYs[i] = Math.max(0, Math.min(image.getHeight() - 1, drawYs[i]));
                        }

                        g.setColor(LINE_COLOR);
                        g.setStroke(new BasicStroke(1.5f));

                        for (int[] conn : CONNECTIONS) {
                            g.drawLine(
                                    drawXs[conn[0]],
                                    drawYs[conn[0]],
                                    drawXs[conn[1]],
                                    drawYs[conn[1]]
                            );
                        }

                        for (int i = 0; i < 10; i++) {
                            g.setColor(POINTS_COLOR);
                            g.fillOval(
                                    drawXs[i] - POINT_RADIUS,
                                    drawYs[i] - POINT_RADIUS,
                                    POINT_RADIUS * 2,
                                    POINT_RADIUS * 2
                            );

                            g.setColor(POINTS_BORDER);
                            g.drawOval(
                                    drawXs[i] - POINT_RADIUS,
                                    drawYs[i] - POINT_RADIUS,
                                    POINT_RADIUS * 2,
                                    POINT_RADIUS * 2
                            );

                            g.setColor(Color.YELLOW);
                            g.setFont(new Font("Arial", Font.PLAIN, 10));
                            g.drawString(String.valueOf(i), drawXs[i] + 5, drawYs[i] - 5);
                        }

                        if (displayType == TypeDisplay.ALL) {
                            drawGeometry(g, drawXs, drawYs);
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
                System.err.println("Frame processing error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void drawGeometry(Graphics2D g, int[] xs, int[] ys) {
        double iod = dist(xs, ys, 4, 5);
        double noseChin = dist(xs, ys, 6, 9);
        double mouthW = dist(xs, ys, 7, 8);
        double faceW = dist(xs, ys, 0, 3);

        double browMidX = (xs[0] + xs[3]) / 2.0;
        double browMidY = (ys[0] + ys[3]) / 2.0;
        double faceH = Math.sqrt(Math.pow(browMidX - xs[9], 2) + Math.pow(browMidY - ys[9], 2));

        double eyeMidX = (xs[4] + xs[5]) / 2.0;
        double eyeMidY = (ys[4] + ys[5]) / 2.0;
        double eyeNose = Math.sqrt(Math.pow(eyeMidX - xs[6], 2) + Math.pow(eyeMidY - ys[6], 2));

        double cx = (xs[6] + xs[9]) / 2.0;
        double sym = 0;

        int[][] symPairs = {
                {0, 3},
                {4, 5},
                {7, 8}
        };

        for (int[] pair : symPairs) {
            double dl = Math.abs(xs[pair[0]] - cx);
            double dr = Math.abs(xs[pair[1]] - cx);
            sym += Math.min(dl, dr) / (Math.max(dl, dr) + 1e-6);
        }

        sym /= symPairs.length;

        String[] lines = {
                String.format("IOD: %.1f", iod),
                String.format("Nose-Chin: %.1f", noseChin),
                String.format("Mouth W: %.1f", mouthW),
                String.format("Face W: %.1f", faceW),
                String.format("Face H: %.1f", faceH),
                String.format("H/W: %.2f", faceH / (faceW + 1e-6)),
                String.format("Eye/Nose: %.2f", iod / (eyeNose + 1e-6)),
                String.format("Symmetry: %.3f", sym),
        };

        int boxW = 180;
        int lineH = 16;
        int boxH = lines.length * lineH + 8;

        g.setColor(TEXT_BG);
        g.fillRect(5, 5, boxW, boxH);

        g.setColor(BBOX_COLOR);
        g.drawRect(5, 5, boxW, boxH);

        g.setColor(TEXT_COLOR);
        g.setFont(new Font("Consolas", Font.PLAIN, 12));

        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 10, 20 + i * lineH);
        }
    }

    private double dist(int[] xs, int[] ys, int a, int b) {
        return Math.sqrt(Math.pow(xs[a] - xs[b], 2) + Math.pow(ys[a] - ys[b], 2));
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
            if (cameraImageView != null) {
                cameraImageView.setImage(null);
            }
        });

        if (modelManager != null) {
            try {
                modelManager.close();
            } catch (OrtException e) {
                System.err.println("ModelManager close error: " + e.getMessage());
            }
        }
    }
}