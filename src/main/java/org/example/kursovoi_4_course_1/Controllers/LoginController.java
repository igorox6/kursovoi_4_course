package org.example.kursovoi_4_course_1.Controllers;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.example.kursovoi_4_course_1.InnerClasses.BufferedImageTranscoder;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.Interfaces.InitializableController;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class LoginController implements InitializableController, Initializable {

    private Application app;
    private Context context;

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ImageView logoImageView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadIcons();
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
                logoImageView.setFitWidth(180);
                logoImageView.setFitHeight(80);
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


    @FXML
    protected void handleLogin() {
        // Здесь добавьте логику авторизации, например, проверку логина/пароля
        System.out.println("Логин: " + loginField.getText());
        System.out.println("Пароль: " + passwordField.getText());

        // Если успешно, переключитесь на следующую сцену, например:
        // ((HelloApplication) app).switchScene("main-view.fxml");
    }

    @Override
    public void setMainApp(Application app, Context context) {
        this.app = app;
        this.context = context;
    }

    @Override
    public void setMainAppWithObject(Application app, Context context, Object data) {
        this.app = app;
        this.context = context;
        // Если нужно обработать data, добавьте здесь
    }
}