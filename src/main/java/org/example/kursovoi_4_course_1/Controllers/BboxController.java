package org.example.kursovoi_4_course_1.Controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import org.example.kursovoi_4_course_1.App;
import org.example.kursovoi_4_course_1.InnerClasses.BufferedImageTranscoder;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.Interfaces.InitializableController;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class BboxController implements InitializableController, Initializable {

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

    // Webcam-Capture
    private Webcam webcam = Webcam.getDefault();

    // Thread control
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread grabberThread;

    // ImageView for camera feed
    private ImageView cameraImageView;

    // Fixed sizes (match FXML)
    private static final double CONTAINER_W = 360.0;
    private static final double CONTAINER_H = 360.0;
    private static final double PANE_W = 340.0;
    private static final double PANE_H = 340.0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadIcons();

        // Set model values (пример)
        model1Value.setText("w2201");
        model2Value.setText("waw201");

        // Установим принудительные размеры контейнера/пэйна (на всякий случай, дублируем FXML)
        cameraContainer.setPrefSize(CONTAINER_W, CONTAINER_H);
        cameraContainer.setMinSize(CONTAINER_W, CONTAINER_H);
        cameraContainer.setMaxSize(CONTAINER_W, CONTAINER_H);

        cameraPane.setPrefSize(PANE_W, PANE_H);
        cameraPane.setMinSize(PANE_W, PANE_H);
        cameraPane.setMaxSize(PANE_W, PANE_H);

        // Создадим ImageView фиксированного размера (не будем привязывать к общей ширине окна)
        cameraImageView = new ImageView();
        cameraImageView.setPreserveRatio(true);
        cameraImageView.setSmooth(true);
        cameraImageView.setCache(true);

        // Подогнали размеры под внутренний панель (немного меньше рамки)
        cameraImageView.setFitWidth(PANE_W);
        cameraImageView.setFitHeight(PANE_H);

        // Добавим в cameraPane и прикрепим к 0,0 (будет фиксированного размера)
        cameraPane.getChildren().add(cameraImageView);
        AnchorPane.setTopAnchor(cameraImageView, 0.0);
        AnchorPane.setBottomAnchor(cameraImageView, 0.0);
        AnchorPane.setLeftAnchor(cameraImageView, 0.0);
        AnchorPane.setRightAnchor(cameraImageView, 0.0);

        // Запускаем камеру
        startCamera();
    }

    private void loadIcons() {
        BufferedImageTranscoder trans = new BufferedImageTranscoder();

        InputStream file = null;
        try {
            file = getClass().getResourceAsStream("/images/logo.svg");
            if (file == null) {
                throw new IOException("SVG file not found");
            }

            TranscoderInput transIn = new TranscoderInput(file);
            trans.addTranscodingHint(ImageTranscoder.KEY_WIDTH, 200f);
            trans.transcode(transIn, null);

            BufferedImage img = trans.getBufferedImage();
            if (img != null) {
                Image fxImage = SwingFXUtils.toFXImage(img, null);
                logoImageView.setImage(fxImage);
                logoImageView.setPreserveRatio(true);
                logoImageView.setFitWidth(140);
                logoImageView.setFitHeight(60);
            }

        } catch (TranscoderException | IOException ex) {
            ex.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void startCamera() {
        if (running.getAndSet(true)) return; // already running

        if (webcam == null) {
            System.err.println("No webcam found");
            running.set(false);
            return;
        }

        // Попробуем выбрать подходящее разрешение (>= 640 width), иначе максимум
        try {
            Dimension[] supported = webcam.getViewSizes();
            Dimension target = null;

            if (supported != null && supported.length > 0) {
                target = Arrays.stream(supported)
                        .sorted(Comparator.comparingInt(d -> d.width))
                        .filter(d -> d.width >= 640)
                        .findFirst()
                        .orElse(supported[supported.length - 1]);
            }

            if (target == null) {
                target = WebcamResolution.VGA.getSize();
            }

            // Установим выбранное разрешение (даёт лучшее качество, чем дефолтные маленькие размеры)
            webcam.setViewSize(target);
            System.out.println("Using webcam resolution: " + target.width + "x" + target.height);
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
                if (image != null) {
                    // Конвертация; SwingFXUtils обычно даёт корректный результат
                    Image fxImage = SwingFXUtils.toFXImage(image, null);

                    // Чтобы избежать резких масштабирований: сделаем предварительную подгонку
                    Platform.runLater(() -> {
                        // Поставим изображение в ImageView (который фиксирован по размеру)
                        cameraImageView.setImage(fxImage);
                    });
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
    protected void handleBack() {
        stop();
        ((App) app).switchScene("Login-view.fxml");
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
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        Platform.runLater(() -> {
            if (cameraImageView != null) cameraImageView.setImage(null);
        });
    }

    @Override
    public void setMainApp() {
        this.context = Context.getInstance();
        this.app = context.getApp();
    }

}
