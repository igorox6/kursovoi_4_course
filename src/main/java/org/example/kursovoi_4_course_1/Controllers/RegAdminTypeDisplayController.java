package org.example.kursovoi_4_course_1.Controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.example.kursovoi_4_course_1.App;
import org.example.kursovoi_4_course_1.InnerClasses.BufferedImageTranscoder;
import org.example.kursovoi_4_course_1.InnerClasses.Context;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.embed.swing.SwingFXUtils;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

public class RegAdminTypeDisplayController extends Controller {

    private Application app;
    private Context context;

    @FXML
    protected ImageView logoImageView;

    @FXML
    private Button typeBox1;

    @FXML
    private Button typeBox2;

    @FXML
    private Button typeBox3;

    @FXML
    private ImageView cameraImageView; // Переименовать в previewImageView если нужно, но оставим как в FXML

    @FXML
    private Label currentTypeLabel;

    @FXML
    private Label model1Value;

    @FXML
    private Label model2Value;

    @FXML
    private Button applyButton;

    @FXML
    private AnchorPane sideDrawer;

    @FXML
    private Button toggleButton;

    @FXML
    private Button adminButton;

    @FXML
    private Button modelButton;

    @FXML
    private Button userButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Label adminName;

    private boolean drawerOpen = false;

    private String selectedType = "Рамка с точками"; // По умолчанию

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();
        this.app = context.getApp();

        loadIcons(logoImageView,180);
        model1Value.setText("w2201");
        model2Value.setText("waw201");

        if (context.getAdminReg() != null) {
            adminName.setText(context.getAdminReg().getLogin());
        }
        else{adminName.setText("Admin");}

        // Инициализация выдвижной панели
        adminButton.setVisible(false);
        modelButton.setVisible(false);
        userButton.setVisible(false);
        logoutButton.setVisible(false);
        toggleButton.setText("←");
        drawerOpen = false;

        // Выделение выбранного типа по умолчанию
        selectType(typeBox2);

        // Установка начального изображения (заменить на реальный путь)
        cameraImageView.setImage(new Image(getClass().getResourceAsStream("/images/silhouette_1.png")));
    }


    @FXML
    private void handleToggleDrawer() {
        double distance = 10.0;
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), sideDrawer);
        FadeTransition fadeAdmin = new FadeTransition(Duration.millis(20), adminButton);
        FadeTransition fadeModel = new FadeTransition(Duration.millis(20), modelButton);
        FadeTransition fadeUser = new FadeTransition(Duration.millis(20), userButton);
        FadeTransition fadeLogout = new FadeTransition(Duration.millis(20), logoutButton);

        if (!drawerOpen) { // opening
            slide.setByX(-distance);
            adminButton.setVisible(true);
            modelButton.setVisible(true);
            userButton.setVisible(true);
            logoutButton.setVisible(true);
            adminButton.setOpacity(0);
            modelButton.setOpacity(0);
            userButton.setOpacity(0);
            logoutButton.setOpacity(0);
            fadeAdmin.setToValue(1);
            fadeModel.setToValue(1);
            fadeUser.setToValue(1);
            fadeLogout.setToValue(1);
            toggleButton.setText("→");
            toggleButton.setPrefWidth(160.0);
        } else { // closing
            slide.setByX(distance);
            fadeAdmin.setToValue(0);
            fadeModel.setToValue(0);
            fadeUser.setToValue(0);
            fadeLogout.setToValue(0);
            fadeLogout.setOnFinished(e -> {
                adminButton.setVisible(false);
                modelButton.setVisible(false);
                userButton.setVisible(false);
                logoutButton.setVisible(false);
            });
            toggleButton.setText("←");
            toggleButton.setPrefWidth(44.0);
        }

        slide.play();
        fadeAdmin.play();
        fadeModel.play();
        fadeUser.play();
        fadeLogout.play();
        drawerOpen = !drawerOpen;
    }

    @FXML
    private void handleFrameType1() {
        selectType(typeBox1);
        loadPreviewImage("ramka");
    }

    @FXML
    private void handleFrameType2() {
        selectType(typeBox2);
        loadPreviewImage("tochki");
    }

    @FXML
    private void handleFrameType3() {
        selectType(typeBox3);
        loadPreviewImage("ramka_s_tochkami");
    }

    private void selectType(Button selectedButton) {
        // Сброс стилей всех кнопок
        typeBox1.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;");
        typeBox2.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;");
        typeBox3.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;");

        // Установка стиля для выбранной
        selectedButton.setStyle("-fx-background-color: white; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;");

        // Обновление лейбла
        selectedType = selectedButton.getText();
        currentTypeLabel.setText("Сейчас используется: " + selectedType);
    }

    private void loadPreviewImage(String type) {
        // Различная загрузка изображений в зависимости от типа
        String imagePath;
        switch (type) {
            case "ramka":
                imagePath = "/images/silhouette_3.png"; // Заменить на реальный путь к изображению для "Рамка"
                break;
            case "tochki":
                imagePath = "/images/silhouette_1.png"; // Заменить на реальный путь к изображению для "Ключевые точки"
                break;
            case "ramka_s_tochkami":
                imagePath = "/images/silhouette_2.png"; // Заменить на реальный путь к изображению для "Рамка с точками"
                break;
            default:
                imagePath = "/images/silhouette_1.png";
        }
        try {
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            cameraImageView.setImage(image);
        } catch (NullPointerException e) {
            // Если ресурс не найден, можно загрузить из файла или использовать дефолт
            e.printStackTrace();
        }
    }

    // Альтернативный метод для загрузки изображения из файла (если нужно различную загрузку с диска)
    private void loadImageFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        Stage stage = (Stage) cameraImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            cameraImageView.setImage(image);
        }
    }

    @FXML
    private void handleApply() {
        // Здесь можно добавить логику применения изменений, например, сохранение выбранного типа в контексте
        // context.setDisplayType(selectedType);
        // Затем переключение сцены если нужно
        // context.switchScene("SomeOtherView.fxml");
    }



    @FXML
    private void handleLogout() {
        if (context.getIsAdminLogin()){
            context.setIsAdminLogin(false);
            context.switchScene("Bbox-view.fxml");
        }
        else{
            context.switchScene("Login-view.fxml");
        }
    }

    @FXML
    private void handleModelType() {
        context.switchScene("RegAdmin-Model-choose-view.fxml");
    }

    @FXML
    private void handleUsers() {
        context.switchScene("RegAdmin-Users-Check-view.fxml");
    }


}