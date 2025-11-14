package org.example.kursovoi_4_course_1.Controllers;

import ai.onnxruntime.OrtException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.DBClasses.Model;
import org.example.kursovoi_4_course_1.DBClasses.ModelType;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;
import org.example.kursovoi_4_course_1.InnerClasses.ModelManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AdminModelsController extends Controller {
    private final Context context = Context.getInstance();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private ModelManager modelManager;

    @FXML private ImageView logoImageView;
    @FXML private TableView<Model> modelTable;
    @FXML private Button editButton;
    @FXML private Button addButton;

    @FXML private AnchorPane sideDrawer;
    @FXML private Button toggleButton;
    @FXML private Button typeDefButton;
    @FXML private Button modelTypeButton;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;

    private boolean drawerOpen = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadIcons(logoImageView, 180);
        try {
            modelManager = new ModelManager();
        } catch (OrtException e) {
            showAlert("Ошибка", "Не удалось инициализировать ModelManager: " + e.getMessage());
            modelTable.setPlaceholder(new Label("Ошибка загрузки"));
            initializeTable(new ArrayList<>());  // Empty table on error
            return;
        }
        modelTable.setPlaceholder(new Label("Пожалуйста, подождите загрузку данных"));

        modelManager.asyncInit()
                .thenAccept(ignored -> Platform.runLater(() -> {
                    List<Model> models = getModelsFromServer();  // Now cache is populated
                    initializeTable(models != null ? models : new ArrayList<>());  // Safe call
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showAlert("Ошибка загрузки", "Не удалось загрузить модели: " + ex.getMessage());
                        modelTable.setPlaceholder(new Label("Ошибка загрузки"));
                        initializeTable(new ArrayList<>());  // Fallback empty
                    });
                    return null;
                });
    }

    private List<Model> getModelsFromServer() {
        try {
            List<JsonObject> allInfo = modelManager.getAllModelsInfo();
            // If cache empty, fallback to async fetch (but since called post-init, shouldn't be)
            if (allInfo.isEmpty()) {
                CompletableFuture<List<JsonObject>> future = modelManager.fetchAllInfoAsync(false);
                allInfo = future.get();  // Block briefly if needed (rare)
            }
            List<Model> models = new ArrayList<>();
            for (JsonObject info : allInfo) {
                Model model = Model.fromJsonObject(info);
                if (model.getId() != null) {
                    models.add(model);
                }
            }
            return models;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void refreshTable() {
        List<Model> models = getModelsFromServer();
        if (models != null) {
            initializeTable(models);
        } else {
            showAlert("Ошибка", "Не удалось обновить список моделей.");
            initializeTable(new ArrayList<>());
        }
    }

    private void initializeTable(List<Model> models) {
        ObservableList<Model> observableModels = FXCollections.observableArrayList(models);

        modelTable.getColumns().clear();

        TableColumn<Model, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(60);

        TableColumn<Model, ModelType> typeColumn = new TableColumn<>("Тип");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(100);

        TableColumn<Model, Short> versionColumn = new TableColumn<>("Версия");
        versionColumn.setCellValueFactory(new PropertyValueFactory<>("version"));
        versionColumn.setPrefWidth(80);

        TableColumn<Model, Float> lossColumn = new TableColumn<>("Потери");
        lossColumn.setCellValueFactory(new PropertyValueFactory<>("loss"));
        lossColumn.setPrefWidth(80);

        TableColumn<Model, String> commentColumn = new TableColumn<>("Комментарий");
        commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));
        commentColumn.setPrefWidth(150);

        TableColumn<Model, Long> sizeColumn = new TableColumn<>("Размер");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeColumn.setPrefWidth(100);

        TableColumn<Model, String> createdColumn = new TableColumn<>("Дата создания");
        createdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getCreatedAt() != null
                                ? cellData.getValue().getCreatedAt().toLocalDate().toString()
                                : ""
                ));
        createdColumn.setPrefWidth(120);

        modelTable.getColumns().addAll(idColumn, typeColumn, versionColumn, lossColumn, commentColumn, sizeColumn, createdColumn);
        modelTable.setItems(observableModels);

        if (observableModels.isEmpty()) {
            modelTable.setPlaceholder(new Label("Нет записей в таблице"));
        } else {
            modelTable.setPlaceholder(null);
        }
    }

    @FXML
    private void buttonEdit(ActionEvent event) {
        Model selected = modelTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите модель для редактирования.");
            return;
        }

        Dialog<Model> dialog = new Dialog<>();
        dialog.setTitle("Редактирование модели");
        dialog.setHeaderText("Измените метаданные модели: " + selected.getId());
        ButtonType okButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        ComboBox<ModelType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(ModelType.values());
        typeCombo.setValue(selected.getType());

        TextField versionField = new TextField(String.valueOf(selected.getVersion()));
        TextField lossField = new TextField(String.valueOf(selected.getLoss()));
        TextField sizeField = new TextField(String.valueOf(selected.getSize()));
        TextArea commentArea = new TextArea(selected.getComment());
        commentArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Тип:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Версия:"), 0, 1);
        grid.add(versionField, 1, 1);
        grid.add(new Label("Потери:"), 0, 2);
        grid.add(lossField, 1, 2);
        grid.add(new Label("Размер (байты):"), 0, 3);
        grid.add(sizeField, 1, 3);
        grid.add(new Label("Комментарий:"), 0, 4);
        grid.add(commentArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dButton -> {
            if (dButton == okButtonType) {
                try {
                    selected.setType(typeCombo.getValue());
                    selected.setVersion(Short.parseShort(versionField.getText().trim()));
                    selected.setLoss(Float.parseFloat(lossField.getText().trim()));
                    selected.setSize(Long.parseLong(sizeField.getText().trim()));
                    selected.setComment(commentArea.getText().trim());
                    return selected;
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Неверный формат чисел. Проверьте версию (short), потери (float), размер (long).");
                    return null;
                }
            }
            return null;
        });

        Optional<Model> result = dialog.showAndWait();
        if (result.isPresent()) {
            boolean success = updateModelOnServer(result.get());
            if (success) {
                refreshTable();  // Reload table after update
                showAlert("Успех", "Модель обновлена.");
            } else {
                showAlert("Ошибка", "Не удалось обновить модель.");
            }
        }
    }

    private boolean updateModelOnServer(Model model) {
        String apiUrl = "http://localhost:8080/models/" + model.getId();
        Map<String, Object> body = new HashMap<>();
        body.put("type", model.getType().name());
        body.put("version", model.getVersion());
        body.put("loss", model.getLoss());
        body.put("comment", model.getComment());
        body.put("size", model.getSize());
        String json = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void buttonAdd(ActionEvent event) {
        Dialog<Model> dialog = new Dialog<>();
        dialog.setTitle("Добавление модели");
        dialog.setHeaderText("Введите метаданные новой модели (файл загружайте отдельно через ModelManager)");
        ButtonType okButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        ComboBox<ModelType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(ModelType.values());

        TextField versionField = new TextField("1");
        TextField lossField = new TextField("0.05");
        TextField sizeField = new TextField("0");
        TextArea commentArea = new TextArea();
        commentArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Тип:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Версия:"), 0, 1);
        grid.add(versionField, 1, 1);
        grid.add(new Label("Потери:"), 0, 2);
        grid.add(lossField, 1, 2);
        grid.add(new Label("Размер (байты):"), 0, 3);
        grid.add(sizeField, 1, 3);
        grid.add(new Label("Комментарий:"), 0, 4);
        grid.add(commentArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dButton -> {
            if (dButton == okButtonType && typeCombo.getValue() != null) {
                try {
                    Model newModel = new Model();
                    newModel.setType(typeCombo.getValue());
                    newModel.setVersion(Short.parseShort(versionField.getText().trim()));
                    newModel.setLoss(Float.parseFloat(lossField.getText().trim()));
                    newModel.setSize(Long.parseLong(sizeField.getText().trim()));
                    newModel.setComment(commentArea.getText().trim());
                    newModel.setModelData(new byte[0]);  // Empty for metadata only
                    newModel.setCreatedAt(OffsetDateTime.now());
                    return newModel;
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Неверный формат чисел. Проверьте версию (short), потери (float), размер (long).");
                    return null;  // Don't close dialog
                }
            }
            return null;
        });

        Optional<Model> result = dialog.showAndWait();
        if (result.isPresent()) {
            boolean success = addModelOnServer(result.get());
            if (success) {
                refreshTable();  // Reload table after add
                showAlert("Успех", "Метаданные модели добавлены.");
            } else {
                showAlert("Ошибка", "Не удалось добавить модель.");
            }
        }
    }

    private boolean addModelOnServer(Model model) {
        String apiUrl = "http://localhost:8080/models";
        Map<String, Object> body = new HashMap<>();
        body.put("type", model.getType().name());
        body.put("version", model.getVersion());
        body.put("loss", model.getLoss());
        body.put("comment", model.getComment());
        body.put("size", model.getSize());
        body.put("modelData", "");  // Empty Base64 for metadata
        String json = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        // Close resources before leaving (shutdown executor, close sub-managers, env)
        if (modelManager != null) {
            try {
                modelManager.close();
            } catch (Exception e) {
                e.printStackTrace();  // Log but don't block navigation
            }
        }
        context.switchScene("Admin-Home-view.fxml");
    }

    @FXML
    private void handleToggleDrawer() {
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
    private void handleTypeBoxButton(ActionEvent event) {
        context.switchScene("RegAdmin-Type-Display-view.fxml");
    }

    @FXML
    private void handleTypeModelButton(ActionEvent event) {
        context.switchScene("RegAdmin-Model-choose-view.fxml");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        context.switchScene("AdminUsersView.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (modelManager != null) {
            try {
                modelManager.close();
            } catch (Exception ignored) {}
        }
        if (context.getIsAdminLogin()) {
            context.setIsAdminLogin(false);
            context.switchScene("Bbox-view.fxml");
        } else {
            context.switchScene("Login-view.fxml");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
