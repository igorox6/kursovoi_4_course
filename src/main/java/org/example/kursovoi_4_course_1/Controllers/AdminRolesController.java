package org.example.kursovoi_4_course_1.Controllers;

import com.google.gson.Gson;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
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
import org.example.kursovoi_4_course_1.DBClasses.Role;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URL;
import java.util.*;

public class AdminRolesController extends Controller {
    private final Context context = Context.getInstance();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML private ImageView logoImageView;
    @FXML private TableView<Role> roleTable;
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
        loadIcons(logoImageView, 180);  // Assuming this method exists in base or utility
        List<Role> roles = getRolesFromServer();
        if (roles != null) {
            initializeTable(roles);
        } else {
            showAlert("Ошибка", "Не удалось загрузить роли из API.");
            initializeTable(new ArrayList<>());
        }
    }

    private List<Role> getRolesFromServer() {
        String apiUrl = "http://localhost:8080/roles";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.err.println("HTTP Error: " + response.statusCode() + " - " + response.body());
                return null;
            }
            return Role.parseRolesArray(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initializeTable(List<Role> roles) {
        ObservableList<Role> observableRoles = FXCollections.observableArrayList(roles);

        roleTable.getColumns().clear();

        TableColumn<Role, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(60);

        TableColumn<Role, String> nameColumn = new TableColumn<>("Название");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);

        TableColumn<Role, String> descriptionColumn = new TableColumn<>("Описание");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setPrefWidth(400);

        roleTable.getColumns().addAll(idColumn, nameColumn, descriptionColumn);
        roleTable.setItems(observableRoles);
    }

    @FXML
    private void buttonEdit(ActionEvent event) {
        Role selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите роль для редактирования.");
            return;
        }

        Dialog<Role> dialog = new Dialog<>();
        dialog.setTitle("Редактирование роли");
        dialog.setHeaderText("Измените данные роли: " + selected.getName());
        ButtonType ok = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField nameField = new TextField(selected.getName());
        TextArea descriptionArea = new TextArea(selected.getDescription());
        descriptionArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dButton -> {
            if (dButton == ok) {
                selected.setName(nameField.getText().trim());
                selected.setDescription(descriptionArea.getText());
                return selected;
            }
            return null;
        });

        Optional<Role> result = dialog.showAndWait();
        if (result.isPresent()) {
            boolean success = updateRoleOnServer(result.get());
            if (success) {
                List<Role> updatedRoles = getRolesFromServer();
                if (updatedRoles != null) {
                    initializeTable(updatedRoles);
                }
                showAlert("Успех", "Роль обновлена.");
            } else {
                showAlert("Ошибка", "Не удалось обновить роль.");
            }
        }
    }

    private boolean updateRoleOnServer(Role role) {
        String apiUrl = "http://localhost:8080/roles/" + role.getId();
        Map<String, String> body = new HashMap<>();
        body.put("name", role.getName());
        body.put("description", role.getDescription());
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
        Dialog<Role> dialog = new Dialog<>();
        dialog.setTitle("Добавление роли");
        dialog.setHeaderText("Введите данные новой роли");
        ButtonType ok = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField nameField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Название (уникальное):"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dButton -> {
            if (dButton == ok && !nameField.getText().trim().isEmpty()) {
                Role newRole = new Role();
                newRole.setName(nameField.getText().trim());
                newRole.setDescription(descriptionArea.getText());
                return newRole;
            }
            return null;
        });

        Optional<Role> result = dialog.showAndWait();
        if (result.isPresent()) {
            boolean success = addRoleOnServer(result.get());
            if (success) {
                List<Role> updatedRoles = getRolesFromServer();
                if (updatedRoles != null) {
                    initializeTable(updatedRoles);
                }
                showAlert("Успех", "Роль добавлена.");
            } else {
                showAlert("Ошибка", "Не удалось добавить роль.");
            }
        }
    }

    private boolean addRoleOnServer(Role role) {
        String apiUrl = "http://localhost:8080/roles";
        Map<String, String> body = new HashMap<>();
        body.put("name", role.getName());
        body.put("description", role.getDescription());
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
