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
import org.example.kursovoi_4_course_1.DBClasses.LogRegionalAdminChange;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ResourceBundle;

public class AdminLogRegionalAdminChangeController extends Controller implements Initializable {

    private final Context context = Context.getInstance();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private List<LogRegionalAdminChange> logList = null;

    @FXML private ImageView logoImageView;
    @FXML private TableView<LogRegionalAdminChange> logTable;
    @FXML private Button backButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadIcons(logoImageView, 180);
        logTable.setPlaceholder(new Label("Загрузка лога изменений..."));

        loadLogsAsync();
    }

    private void loadLogsAsync() {
        String apiUrl = "http://localhost:8080/logs/admin-changes";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Type listType = new TypeToken<List<LogRegionalAdminChange>>() {}.getType();
                        return gson.fromJson(response.body(), listType);
                    }
                    return null;
                })
                .thenAccept(logs -> Platform.runLater(() -> {
                    logList = logs != null ? (List<LogRegionalAdminChange>) logs : null;
                    if (logList != null && !logList.isEmpty()) {
                        initializeTable(logList);
                    } else {
                        logTable.setPlaceholder(new Label("Нет записей в логе изменений"));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        logTable.setPlaceholder(new Label("Ошибка загрузки лога"));
                        System.err.println("Load log error: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void initializeTable(List<LogRegionalAdminChange> logs) {
        if (logs.isEmpty()) return;

        logTable.getColumns().clear();

        TableColumn<LogRegionalAdminChange, String> timeColumn = new TableColumn<>("Время");
        timeColumn.setCellValueFactory(cellData -> {
            LogRegionalAdminChange item = cellData.getValue();
            return item.getTimeChange() != null ? new SimpleStringProperty(item.getTimeChange().toString()) : null;
        });
        timeColumn.setPrefWidth(150);
        timeColumn.setSortable(true);

        TableColumn<LogRegionalAdminChange, Long> userIdColumn = new TableColumn<>("User ID");
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        userIdColumn.setPrefWidth(100);

        TableColumn<LogRegionalAdminChange, Integer> fromModelColumn = new TableColumn<>("From Model ID");
        fromModelColumn.setCellValueFactory(new PropertyValueFactory<>("changeFromModelId"));
        fromModelColumn.setPrefWidth(120);

        TableColumn<LogRegionalAdminChange, Integer> toModelColumn = new TableColumn<>("To Model ID");
        toModelColumn.setCellValueFactory(new PropertyValueFactory<>("changeToModelId"));
        toModelColumn.setPrefWidth(120);

        logTable.getColumns().addAll(timeColumn, userIdColumn, fromModelColumn, toModelColumn);
        logTable.setItems(FXCollections.observableArrayList(logs));
        logTable.getSortOrder().add(timeColumn);
        logTable.setPlaceholder(null);
    }

    private void closeResources() {
        // No-op
    }

    @FXML
    private void handleBack(ActionEvent event) {
        closeResources();
        context.switchScene("Admin-Home-view.fxml");
    }
}
