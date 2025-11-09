package org.example.kursovoi_4_course_1.Controllers;

import com.google.gson.JsonObject;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.InnerClasses.*;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ResourceBundle;

public class RegAdminModelChooseView extends Controller {

    private Context context;
    private ModelManager modelManager;
    private boolean drawerOpen = false;
    private JsonObject selectedModel;

    // UI
    @FXML private ImageView logoImageView;
    @FXML private Label adminName;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ListView<String> modelListView;
    @FXML private AnchorPane sideDrawer;       // animate this (как в Bbox)
    @FXML private Button toggleButton;         // toggle id как в Bbox
    @FXML private Button typeDefButton;
    @FXML private Button modelTypeButton;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;

    @FXML private Label versionLabel;
    @FXML private Label lossLabel;
    @FXML private Label releaseDateLabel;
    @FXML private TextArea commentTextArea;
    @FXML private Label currentUsedLabel;
    @FXML private Button applyButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();

        // Логотип
        loadIcons(logoImageView, 180);

        if (context.getAdminReg() != null) {
            adminName.setText(context.getAdminReg().getLogin());
        }
        else{adminName.setText("Admin");}

        try {
            modelManager = new ModelManager();
            loadCurrentUsed();

            // Типы
            typeComboBox.setItems(FXCollections.observableArrayList("FACE_BBOX", "FACE_KEYPOINTS"));

            // Слушатели
            typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) loadModelsForType(newVal);
            });

            modelListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) updateModelInfo(newVal);
            });

            // Выбрать первый тип по-умолчанию
            if (!typeComboBox.getItems().isEmpty()) typeComboBox.getSelectionModel().selectFirst();

            // Изначально скрыть кнопки drawer (как в FXML, но на всякий случай)
            typeDefButton.setVisible(false);
            typeDefButton.setOpacity(0);
            modelTypeButton.setVisible(false);
            modelTypeButton.setOpacity(0);
            usersButton.setVisible(false);
            usersButton.setOpacity(0);
            logoutButton.setVisible(false);
            logoutButton.setOpacity(0);

        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось загрузить модели: " + e.getMessage());
        }
    }

    private void loadCurrentUsed() {
        JsonObject bbox = modelManager.getBestBboxMeta();
        JsonObject points = modelManager.getBestPointsMeta();

        String b = bbox != null ? "V" + bbox.get("version").getAsString() : "—";
        String p = points != null ? "V" + points.get("version").getAsString() : "—";

        // Сделали короче формат — чтобы точно помещалось
        currentUsedLabel.setText("Сейчас: BBOX " + b + "  POINTS " + p);
    }

    private void loadModelsForType(String type) {
        List<JsonObject> all = modelManager.getAllModelsInfo();
        if (all == null) {
            modelListView.setItems(FXCollections.observableArrayList());
            return;
        }

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
                break;
            }
        }
    }


    @FXML
    private void handleToggleDrawer() {
        double distance = 10;
        TranslateTransition slide = new TranslateTransition(Duration.millis(20), sideDrawer);
        FadeTransition fadeTypeDef = new FadeTransition(Duration.millis(20), typeDefButton);
        FadeTransition fadeModelType = new FadeTransition(Duration.millis(20), modelTypeButton);
        FadeTransition fadeUsers = new FadeTransition(Duration.millis(20), usersButton);
        FadeTransition fadeLogout = new FadeTransition(Duration.millis(20), logoutButton);

        if (!drawerOpen) {
            // выдвигаем внутрь
            slide.setByX(-distance);

            fadeTypeDef.setToValue(1);
            fadeModelType.setToValue(1);
            fadeUsers.setToValue(1);
            fadeLogout.setToValue(1);

            typeDefButton.setVisible(true);
            modelTypeButton.setVisible(true);
            usersButton.setVisible(true);
            logoutButton.setVisible(true);

            toggleButton.setText("→");
            toggleButton.setMaxWidth(170.0);
        } else {
            // задвигаем обратно
            slide.setByX(distance);

            fadeTypeDef.setToValue(0);
            fadeModelType.setToValue(0);
            fadeUsers.setToValue(0);
            fadeLogout.setToValue(0);

            fadeLogout.setOnFinished(e -> {
                typeDefButton.setVisible(false);
                modelTypeButton.setVisible(false);
                usersButton.setVisible(false);
                logoutButton.setVisible(false);
            });

            toggleButton.setText("←");
            toggleButton.setMaxWidth(44.0);
        }

        slide.play();
        fadeTypeDef.play();
        fadeModelType.play();
        fadeUsers.play();
        fadeLogout.play();
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

    public void handleTypeBoxButton(ActionEvent actionEvent) { context.switchScene("RegAdmin-Type-Display-view.fxml");}
    @FXML private void handleUsers() { context.switchScene("RegAdmin-Users-Check-view.fxml"); }

    @FXML
    private void handleLogout() {
        try {
            modelManager.close();
            if (context.getIsAdminLogin()){
                context.setIsAdminLogin(false);
                context.switchScene("Bbox-view.fxml");
            }
            else{
                context.switchScene("Login-view.fxml");
            }
        }
        catch (Exception ignored) {}
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
