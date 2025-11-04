package org.example.kursovoi_4_course_1.Controllers;

import com.google.gson.JsonObject;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.InnerClasses.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class RegAdminModelChooseView extends Controller {

    private Context context;
    private ModelManager modelManager;
    private boolean drawerOpen = false;
    private JsonObject selectedModel;

    @FXML private ImageView logoImageView;
    @FXML private Label adminName;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ListView<String> modelListView;
    @FXML private ImageView previewImageView;
    @FXML private Label previewHint;
    @FXML private Button drawerToggleButton;
    @FXML private VBox drawerButtons;
    @FXML private Label versionLabel;
    @FXML private Label lossLabel;
    @FXML private Label releaseDateLabel;
    @FXML private TextArea commentTextArea;
    @FXML private Label currentUsedLabel;
    @FXML private Button applyButton;
    @FXML private Button typeDefButton;
    @FXML private Button modelTypeButton;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();

        // Загружаем логотип
        loadIcons(logoImageView, 140);

        // Имя админа
        adminName.setText("Admin");

        try {
            modelManager = new ModelManager();
            loadCurrentUsed();

            // Типы моделей
            typeComboBox.setItems(FXCollections.observableArrayList("FACE_BBOX", "FACE_KEYPOINTS"));

            // Слушатели
            typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) loadModelsForType(newVal);
            });

            modelListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) updateModelInfo(newVal);
            });

            typeComboBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось загрузить модели: " + e.getMessage());
        }
    }

    private void loadCurrentUsed() {
        JsonObject bbox = modelManager.getBestBboxMeta();
        JsonObject points = modelManager.getBestPointsMeta();

        String b = bbox != null ? "V" + bbox.get("version").getAsString() : "—";
        String p = points != null ? "V" + points.get("version").getAsString() : "—";

        currentUsedLabel.setText("Сейчас используются: BBOX - " + b + ", POINTS - " + p);
    }

    private void loadModelsForType(String type) {
        List<JsonObject> all = modelManager.getAllModelsInfo();
        if (all == null) return;

        List<JsonObject> filtered = all.stream()
                .filter(m -> m.get("type").getAsString().equals(type))
                .sorted(Comparator.comparingInt(m -> -m.get("version").getAsInt()))
                .collect(Collectors.toList());

        ObservableList<String> names = FXCollections.observableArrayList();
        for (JsonObject m : filtered) {
            names.add("V" + m.get("version").getAsString() + " - Loss: " + m.get("loss").getAsString());
        }
        modelListView.setItems(names);

        Map<String, Object> data = new HashMap<>();
        data.put("models", filtered);
        modelListView.setUserData(data);
    }

    private void updateModelInfo(String selected) {
        Map<String, Object> data = (Map<String, Object>) modelListView.getUserData();
        if (data == null) return;
        List<JsonObject> models = (List<JsonObject>) data.get("models");
        if (models == null) return;

        for (JsonObject m : models) {
            String name = "V" + m.get("version").getAsString() + " - Loss: " + m.get("loss").getAsString();
            if (name.equals(selected)) {
                selectedModel = m;
                versionLabel.setText("Версия: " + m.get("version").getAsString());
                lossLabel.setText("Потери: " + m.get("loss").getAsString());
                releaseDateLabel.setText("Дата: " + m.get("createdAt").getAsString());
                commentTextArea.setText(m.get("comment").getAsString());
                previewHint.setText("Превью модели: " + name);
                previewImageView.setImage(null); // Можно добавить превью позже
                break;
            }
        }
    }

    @FXML
    private void onToggleDrawer() {
        double distance = 200;
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), drawerButtons);
        FadeTransition fade = new FadeTransition(Duration.millis(250), drawerButtons);

        if (!drawerOpen) {
            drawerButtons.setVisible(true);
            drawerButtons.setManaged(true);
            slide.setByX(distance);
            fade.setToValue(1.0);
            drawerToggleButton.setText("←");
        } else {
            slide.setByX(-distance);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> {
                drawerButtons.setVisible(false);
                drawerButtons.setManaged(false);
            });
            drawerToggleButton.setText("→");
        }
        slide.play();
        fade.play();
        drawerOpen = !drawerOpen;
    }

    @FXML
    private void onApply() {
        if (selectedModel == null) {
            showAlert("Предупреждение", "Выберите модель.");
            return;
        }

        String type = selectedModel.get("type").getAsString();
        try {
            byte[] bytes = modelManager.fetchModelBytesById(selectedModel.get("id").getAsInt());

            if ("FACE_BBOX".equals(type)) {
                if (modelManager.getBboxManager() != null) modelManager.getBboxManager().close();
                modelManager.setBboxManager(new ModelManagerBbox(modelManager.getEnv(), bytes));
                modelManager.setBestBboxMeta(selectedModel);
            } else {
                if (modelManager.getPointsManager() != null) modelManager.getPointsManager().close();
                modelManager.setPointsManager(new ModelManagerPoints(modelManager.getEnv(), bytes));
                modelManager.setBestPointsMeta(selectedModel);
            }

            loadCurrentUsed();
            showAlert("Успех", "Модель применена: " + type + " V" + selectedModel.get("version").getAsString());
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось применить модель: " + e.getMessage());
        }
    }

    @FXML private void handleTypeDef() { System.out.println("Тип определения"); }
    @FXML private void handleModelType() { System.out.println("Тип модели"); }
    @FXML private void handleUsers() { System.out.println("Пользователи"); }

    @FXML
    private void handleLogout() {
        try { modelManager.close(); } catch (Exception ignored) {}
        context.switchScene("Login-view.fxml");
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}