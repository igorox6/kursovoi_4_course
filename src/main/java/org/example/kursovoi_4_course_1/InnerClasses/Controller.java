package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.OrtException;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    protected Context context;
    protected void loadIcons(ImageView logoImageView, int WidthPixels){
        BufferedImageTranscoder trans = new BufferedImageTranscoder();
        InputStream file = null;
        try {
            file = getClass().getResourceAsStream("/images/logo.svg");
            if (file == null) throw new IOException("SVG file not found");
            TranscoderInput transIn = new TranscoderInput(file);
            trans.addTranscodingHint(ImageTranscoder.KEY_WIDTH, 200f);
            trans.transcode(transIn, null);
            BufferedImage img = trans.getBufferedImage();
            if (img != null) {
                Image fxImage = SwingFXUtils.toFXImage(img, null);
                logoImageView.setImage(fxImage);
                logoImageView.setPreserveRatio(true);
                logoImageView.setFitWidth(WidthPixels);
                logoImageView.setFitHeight(WidthPixels/2.5);
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
