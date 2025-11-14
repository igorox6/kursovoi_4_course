package org.example.kursovoi_4_course_1.Controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.example.kursovoi_4_course_1.DBClasses.TypeDisplay;
import org.example.kursovoi_4_course_1.DBClasses.UserSettings;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class UserSettingsController extends Controller implements Initializable {

    private final Context context = Context.getInstance();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private List<UserSettings> settingsList = new ArrayList<>();

    @FXML private ImageView logoImageView;
    @FXML private TableView<UserSettings> settingsTable;
    @FXML private Button editButton;
    @FXML private Button backButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadIcons(logoImageView, 180);
        settingsTable.setPlaceholder(new Label("Загрузка настроек..."));

        loadSettingsAsync();

        settingsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            editButton.setDisable(newVal == null);
        });
    }

    private void loadSettingsAsync() {
        String apiUrl = "http://localhost:8080/users/settings";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Type listType = new TypeToken<List<UserSettings>>() {}.getType();
                        return gson.fromJson(response.body(), listType);
                    }
                    return new ArrayList<UserSettings>();
                })
                .thenAccept(settings -> Platform.runLater(() -> {
                    settingsList = settings != null ? settings : new ArrayList<>();
                    if (!settingsList.isEmpty()) {
                        initializeTable(settingsList);
                    } else {
                        settingsTable.setPlaceholder(new Label("Нет записей"));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        settingsTable.setPlaceholder(new Label("Ошибка загрузки настроек"));
                        System.err.println("Load settings error: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void initializeTable(List<UserSettings> settings) {
        if (settings.isEmpty()) return;

        settingsTable.getColumns().clear();

        TableColumn<UserSettings, Long> userIdColumn = new TableColumn<>("User ID");
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        userIdColumn.setPrefWidth(100);

        TableColumn<UserSettings, Integer> bboxIdColumn = new TableColumn<>("BBOX Model ID");
        bboxIdColumn.setCellValueFactory(new PropertyValueFactory<>("modelBboxId"));
        bboxIdColumn.setPrefWidth(120);

        TableColumn<UserSettings, Integer> keypointsIdColumn = new TableColumn<>("Keypoints Model ID");
        keypointsIdColumn.setCellValueFactory(new PropertyValueFactory<>("modelKeypointsId"));
        keypointsIdColumn.setPrefWidth(140);

        TableColumn<UserSettings, TypeDisplay> typeDisplayColumn = new TableColumn<>("Type Display");
        typeDisplayColumn.setCellValueFactory(new PropertyValueFactory<>("typeDisplay"));
        typeDisplayColumn.setPrefWidth(120);

        TableColumn<UserSettings, String> updatedAtColumn = new TableColumn<>("Updated At");
        updatedAtColumn.setCellValueFactory(cellData -> {
            UserSettings item = cellData.getValue();
            return item.getUpdatedAt() != null ? new SimpleStringProperty(item.getUpdatedAt().toString()) : null;
        });
        updatedAtColumn.setPrefWidth(150);

        settingsTable.getColumns().addAll(userIdColumn, bboxIdColumn, keypointsIdColumn, typeDisplayColumn, updatedAtColumn);
        settingsTable.setItems(FXCollections.observableArrayList(settings));
        settingsTable.setPlaceholder(null);
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        UserSettings selected = settingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите настройку.");
            return;
        }

        Dialog<UserSettings> dialog = new Dialog<>();
        dialog.setTitle("Редактирование настроек");
        dialog.setHeaderText("Измените отображение (TypeDisplay):");
        ButtonType okButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        ComboBox<TypeDisplay> typeDisplayCombo = new ComboBox<>(FXCollections.observableArrayList(TypeDisplay.values()));
        typeDisplayCombo.setValue(selected.getTypeDisplay());

        VBox content = new VBox(10, typeDisplayCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dButton -> {
            if (dButton == okButtonType) {
                selected.setTypeDisplay(typeDisplayCombo.getValue());
                return selected;
            }
            return null;
        });

        Optional<UserSettings> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (updateSetting(result.get())) {
                loadSettingsAsync();  // Refresh
                showAlert("Успех", "Настройки обновлены.");
            } else {
                showAlert("Ошибка", "Не удалось обновить.");
            }
        }
    }

    private boolean updateSetting(UserSettings setting) {
        String apiUrl = "http://localhost:8080/users/settings/" + setting.getUserId();  // По userId
        Map<String, Object> body = new HashMap<>();
        body.put("userId", setting.getUserId());
        body.put("typeDisplay", setting.getTypeDisplay().name());
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
            System.err.println("Update error: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        context.switchScene("Admin-Home-view.fxml");
    }


    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
        });
    }
}
