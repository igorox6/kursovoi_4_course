package org.example.kursovoi_4_course_1.Controllers;

import ai.onnxruntime.OrtException;
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
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.InnerClasses.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.ResourceBundle;

public class RegAdminModelChooseController extends Controller {

    private Context context;
    private ModelManager modelManager;
    private boolean drawerOpen = false;
    private JsonObject selectedModel;

    @FXML private ImageView logoImageView;
    @FXML private Label adminName;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ListView<String> modelListView;
    @FXML private AnchorPane sideDrawer;
    @FXML private Button toggleButton;
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
        loadIcons(logoImageView, 180);

        try {
            modelManager = new ModelManager();
        } catch (OrtException e) {
            System.out.println("Не удалось инициализировать ModelManager: " + e.getMessage());
            return;
        }

        adminName.setText(context.getAdminReg() != null ? context.getAdminReg().getLogin() : "Admin");



        modelListView.setPlaceholder(new Label("Загрузка моделей..."));
        modelManager.asyncRefreshModels()
                .thenRun(() -> Platform.runLater(() -> {
                    typeComboBox.setItems(FXCollections.observableArrayList("FACE_BBOX", "FACE_KEYPOINTS"));
                    if (!typeComboBox.getItems().isEmpty()) {
                        typeComboBox.getSelectionModel().selectFirst();
                    }
                    loadCurrentUsed();

                    typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                        if (newVal != null) loadModelsForType(newVal);
                    });

                    modelListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                        if (newVal != null) updateModelInfo(newVal);
                    });
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showAlert("Ошибка загрузки", "Не удалось загрузить модели: " + ex.getMessage());
                        modelListView.setPlaceholder(new Label("Ошибка загрузки данных"));
                    });
                    return null;
                });
    }

    private void loadCurrentUsed() {
        JsonObject bbox = modelManager.getBestBboxMeta();
        JsonObject points = modelManager.getBestPointsMeta();

        String b = bbox != null ? "V" + bbox.get("version").getAsString() : "—";
        String p = points != null ? "V" + points.get("version").getAsString() : "—";

        currentUsedLabel.setText("Сейчас: BBOX " + b + "  POINTS " + p);
    }

    private void loadModelsForType(String type) {
        List<JsonObject> all = modelManager.getAllModelsInfo();
        if (all == null || all.isEmpty()) {
            modelListView.setItems(FXCollections.observableArrayList());
            modelListView.setPlaceholder(new Label("Нет моделей для типа: " + type));
            return;
        }

        List<JsonObject> filtered = all.stream()
                .filter(m -> m.has("type") && m.get("type").getAsString().equals(type))
                .sorted(Comparator.comparingInt(m -> -m.get("version").getAsInt()))
                .collect(Collectors.toList());

        ObservableList<String> names = FXCollections.observableArrayList();
        for (JsonObject m : filtered) {
            names.add("V" + m.get("version").getAsString() + " - Loss: " + m.get("loss").getAsString());
        }
        modelListView.setItems(names);
        modelListView.setPlaceholder(null);

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

    private void closeResources() {
        if (modelManager != null) {
            try {
                modelManager.close();
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void handleToggleDrawer() {
        if (sideDrawer == null || toggleButton == null) return;

        double distance = 10;
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), sideDrawer);
        FadeTransition fadeTypeDef = new FadeTransition(Duration.millis(200), typeDefButton);
        FadeTransition fadeModelType = new FadeTransition(Duration.millis(200), modelTypeButton);
        FadeTransition fadeUsers = new FadeTransition(Duration.millis(200), usersButton);
        FadeTransition fadeLogout = new FadeTransition(Duration.millis(200), logoutButton);

        if (!drawerOpen) {
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
        modelManager.fetchModelBytesByIdAsync(selectedModel.get("id").getAsInt())
                .thenAccept(bytes -> Platform.runLater(() -> {
                    if (bytes == null || bytes.length == 0) {
                        showAlert("Ошибка", "Не удалось загрузить байты модели.");
                        return;
                    }
                    try {
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
                    } catch (OrtException e) {
                        showAlert("Ошибка", "Не удалось применить модель: " + e.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert("Ошибка", "Не удалось загрузить модель: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void handleTypeBoxButton(ActionEvent event) {
        closeResources();
        context.switchScene("RegAdmin-Type-Display-view.fxml");
    }

    @FXML
    private void handleTypeModelButton(ActionEvent event) {
        closeResources();
        context.switchScene("AdminModelsController-view.fxml");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        closeResources();
        context.switchScene("RegAdmin-Users-Check-view.fxml");
    }

    @FXML
    private void handleLogout() {
        closeResources();
        if (context.getIsAdminLogin()) {
            context.setIsAdminLogin(false);
            context.switchScene("Bbox-view.fxml");
        } else {
            context.switchScene("Login-view.fxml");
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
