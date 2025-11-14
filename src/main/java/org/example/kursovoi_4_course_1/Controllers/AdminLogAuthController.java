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
import javafx.scene.image.ImageView;
import org.example.kursovoi_4_course_1.DBClasses.User;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminLogAuthController extends Controller implements Initializable {

    private final Context context = Context.getInstance();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML private ImageView logoImageView;
    @FXML private TableView<LogEntry> logTable;
    @FXML private Button backButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadIcons(logoImageView, 180);
        setupColumns();
        logTable.setPlaceholder(new Label("Загрузка лога авторизаций..."));
        loadLogsAsync();
    }

    private void setupColumns() {
        TableColumn<LogEntry, String> timeCol = new TableColumn<>("Время");
        timeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().timeAuth != null ? cd.getValue().timeAuth : ""));
        timeCol.setPrefWidth(170);

        TableColumn<LogEntry, String> loginCol = new TableColumn<>("Login");
        loginCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().user != null && cd.getValue().user.getLogin() != null ? cd.getValue().user.getLogin() : ""
        ));
        loginCol.setPrefWidth(140);

        TableColumn<LogEntry, String> userIdCol = new TableColumn<>("User ID");
        userIdCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().user != null && cd.getValue().user.getId() != null ? cd.getValue().user.getId().toString() : ""
        ));
        userIdCol.setPrefWidth(100);

        TableColumn<LogEntry, String> ipCol = new TableColumn<>("IP Адрес");
        ipCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().ipAddress != null ? cd.getValue().ipAddress : ""));
        ipCol.setPrefWidth(140);

        TableColumn<LogEntry, String> successCol = new TableColumn<>("Успех");
        successCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().success ? "Да" : "Нет"));
        successCol.setPrefWidth(80);

        logTable.getColumns().setAll(timeCol, loginCol, userIdCol, ipCol, successCol);
    }

    private void loadLogsAsync() {
        String apiUrl = "http://localhost:8080/log-auths";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                        return gson.fromJson(response.body(), listType);
                    }
                    return null;
                })
                .thenAccept(logMapsObj -> Platform.runLater(() -> {
                    if (logMapsObj == null) {
                        logTable.setPlaceholder(new Label("Ошибка загрузки лога"));
                        logTable.setItems(FXCollections.observableArrayList(new ArrayList<>()));
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> logMaps = (List<Map<String, Object>>) (List<?>) logMapsObj;

                    if (logMaps.isEmpty()) {
                        logTable.setPlaceholder(new Label("Нет записей в логе авторизаций"));
                        logTable.setItems(FXCollections.observableArrayList(new ArrayList<>()));
                        return;
                    }

                    List<LogEntry> entries = new ArrayList<>(logMaps.size());
                    for (Map<String, Object> logMap : logMaps) {
                        try {
                            String timeAuth = (String) logMap.get("timeAuth");
                            String ip = (String) logMap.get("ipAddress");
                            boolean success = false;
                            Object succ = logMap.get("success");
                            if (succ instanceof Boolean) success = (Boolean) succ;
                            else if (succ instanceof String) success = "true".equalsIgnoreCase((String) succ);

                            @SuppressWarnings("unchecked")
                            Map<String, Object> userMap = (Map<String, Object>) logMap.get("user");
                            User user = null;
                            if (userMap != null) {
                                user = User.fromMap(userMap); // Парсим как в login()
                            }

                            entries.add(new LogEntry(timeAuth, user, ip, success));
                        } catch (Exception e) {
                            System.err.println("Parse item error: " + e.getMessage());
                        }
                    }

                    logTable.setItems(FXCollections.observableArrayList(entries));
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        logTable.setPlaceholder(new Label("Ошибка: " + ex.getMessage()));
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить логи: " + ex.getMessage());
                    });
                    return null;
                });
    }

    public static class LogEntry {
        public final String timeAuth;
        public final User user;
        public final String ipAddress;
        public final boolean success;

        public LogEntry(String timeAuth, User user, String ipAddress, boolean success) {
            this.timeAuth = timeAuth;
            this.user = user;
            this.ipAddress = ipAddress;
            this.success = success;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        context.switchScene("Admin-Home-view.fxml");
    }
}
