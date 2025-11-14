package org.example.kursovoi_4_course_1.Controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.DBClasses.User;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.kursovoi_4_course_1.InnerClasses.RandomUserGenerator.generateLogin;
import static org.example.kursovoi_4_course_1.InnerClasses.RandomUserGenerator.generatePassword;
import static org.example.kursovoi_4_course_1.InnerClasses.UserFileSaver.saveToDocuments;


public class AdminUsersController extends Controller {

    private Context context;
    private HttpClient client;

    @FXML private ImageView logoImageView;
    @FXML private Label adminName;

    @FXML private TableView<User> userTable;
    @FXML private Button editButton;
    @FXML private Button addButton;

    @FXML private AnchorPane sideDrawer;
    @FXML private Button toggleButton;
    @FXML private Button typeDefButton;
    @FXML private Button modelTypeButton;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;

    private boolean drawerOpen = false;

    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();
        loadIcons(logoImageView, 180);


        client = HttpClient.newHttpClient();

        editButton.setDisable(true);
        addButton.setDisable(true);

        loadUsersInBackground();
    }

    private void loadUsersInBackground() {
        Task<List<User>> loadTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return getUsersFromServer();
            }
        };

        loadTask.setOnSucceeded(workerStateEvent -> {
            List<User> users = loadTask.getValue();
            if (users == null) users = new ArrayList<>();
            context.setUsersAdmin(users);
            initializeTable();   // заполнит таблицу и включит кнопки
            editButton.setDisable(false);
            addButton.setDisable(false);
        });

        loadTask.setOnFailed(workerStateEvent -> {
            Throwable ex = loadTask.getException();
            ex.printStackTrace();
            Platform.runLater(() -> {
                showAlert("Ошибка", "Не удалось загрузить пользователей: " + (ex != null ? ex.getMessage() : "<unknown>"));

                context.setUsersAdmin(new ArrayList<>());
                initializeTable();
            });
        });

        executor.submit(loadTask);
    }

    private List<User> getUsersFromServer() {
        String url = "http://localhost:8080/users";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP error: " + response.statusCode() + " body: " + response.body());
            }
            return User.parseUsersArray(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeTable() {
        try {
            List<User> users = context.getUsersAdmin();
            if (users == null) users = new ArrayList<>();
            ObservableList<User> observableUsers = FXCollections.observableArrayList(users);

            userTable.getColumns().clear();

            TableColumn<User, Long> idColumn = new TableColumn<>("ID");
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            idColumn.setPrefWidth(60);

            TableColumn<User, String> loginColumn = new TableColumn<>("Логин");
            loginColumn.setCellValueFactory(new PropertyValueFactory<>("login"));
            loginColumn.setPrefWidth(140);

            TableColumn<User, String> nameColumn = new TableColumn<>("Имя");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameColumn.setPrefWidth(120);

            TableColumn<User, String> secondNameColumn = new TableColumn<>("Фамилия");
            secondNameColumn.setCellValueFactory(new PropertyValueFactory<>("second_name"));
            secondNameColumn.setPrefWidth(120);

            TableColumn<User, String> patronymicNameColumn = new TableColumn<>("Отчество");
            patronymicNameColumn.setCellValueFactory(new PropertyValueFactory<>("patronymic_name"));
            patronymicNameColumn.setPrefWidth(120);

            TableColumn<User, Boolean> activeColumn = new TableColumn<>("Активен");
            activeColumn.setCellValueFactory(new PropertyValueFactory<>("is_active"));
            activeColumn.setPrefWidth(80);

            TableColumn<User, String> createdColumn = new TableColumn<>("Создан");
            createdColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getCreated_at() != null
                                    ? cellData.getValue().getCreated_at().toLocalDate().toString()
                                    : ""
                    ));
            createdColumn.setPrefWidth(100);

            TableColumn<User, String> updatedColumn = new TableColumn<>("Обновлена");
            updatedColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getUpdated_at() != null
                                    ? cellData.getValue().getUpdated_at().toLocalDate().toString()
                                    : ""
                    ));
            updatedColumn.setPrefWidth(100);

            TableColumn<User, String> lastLoginColumn = new TableColumn<>("Последний логин");
            lastLoginColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getLast_login() != null
                                    ? cellData.getValue().getLast_login().toLocalDate().toString()
                                    : ""
                    ));
            lastLoginColumn.setPrefWidth(120);

            userTable.getColumns().addAll(
                    idColumn, loginColumn, nameColumn, secondNameColumn, patronymicNameColumn,
                    activeColumn, createdColumn, updatedColumn, lastLoginColumn
            );

            userTable.setItems(observableUsers);

        } catch (Exception e) {
            System.out.println("Ошибка при инициализации таблицы: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("AdminUsers/sendRequest: HTTP Status Code: " + response.statusCode());
                System.out.println("AdminUsers/sendRequest: Response Body: " + response.body());
                throw new RuntimeException("Request failed with status: " + response.statusCode());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при отправке запроса: " + e.getMessage());
            throw new RuntimeException(e);
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

    public void buttonEdit(ActionEvent actionEvent) {
        User sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Предупреждение", "Выберите пользователя для редактирования.");
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Редактирование пользователя");
        dialog.setHeaderText("Измените данные пользователя: " + sel.getLogin());

        ButtonType ok = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField nameField = new TextField(sel.getName());
        TextField secondNameField = new TextField(sel.getSecond_name());
        TextField patronymicField = new TextField(sel.getPatronymic_name());
        CheckBox activeCheck = new CheckBox("Активен");
        activeCheck.setSelected(Boolean.TRUE.equals(sel.getIs_active()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Имя:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Фамилия:"), 0, 1);
        grid.add(secondNameField, 1, 1);
        grid.add(new Label("Отчество:"), 0, 2);
        grid.add(patronymicField, 1, 2);
        grid.add(activeCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ok) {
                sel.setName(nameField.getText());
                sel.setSecond_name(secondNameField.getText());
                sel.setPatronymic_name(patronymicField.getText());
                sel.setIs_active(activeCheck.isSelected());
                sel.setUpdated_at(OffsetDateTime.now());
                return sel;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(updatedUser -> {
            Task<Boolean> updateTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    String url = "http://localhost:8080/users/" + updatedUser.getId();
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", updatedUser.getName());
                    body.put("secondName", updatedUser.getSecond_name());
                    body.put("patronymicName", updatedUser.getPatronymic_name());
                    body.put("isActive", updatedUser.getIs_active());
                    String json = gson.toJson(body);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> resp = sendRequest(request);
                    return resp.statusCode() >= 200 && resp.statusCode() < 300;
                }
            };

            updateTask.setOnSucceeded(ev -> {
                Boolean okRes = updateTask.getValue();
                if (okRes) {
                    initializeTable();
                    showAlert("Успех", "Пользователь сохранён.");
                } else {
                    showAlert("Ошибка", "Не удалось сохранить пользователя.");
                }
            });

            updateTask.setOnFailed(ev -> {
                Throwable ex = updateTask.getException();
                showAlert("Ошибка", "Ошибка при сохранении: " + (ex != null ? ex.getMessage() : "<unknown>"));
            });

            executor.submit(updateTask);
        });
    }

    public void buttonAdd(ActionEvent actionEvent) {
        String pwd = generatePassword();
        String login = generateLogin();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Добавление нового пользователя");
        confirm.setHeaderText("Добавить пользователя с полями\nЛогин: " + login + "\nПароль: " + pwd);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) {
            showAlert("Отмена", "Пользователь не добавлен");
            return;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("login", login);
        requestBody.put("password", pwd);

        Task<Boolean> addTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                String url = "http://localhost:8080/users/register";
                String json = gson.toJson(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> resp = sendRequest(request);
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    try {
                        Map map = gson.fromJson(resp.body(), Map.class);
                        if (map != null && map.get("user") instanceof Map) {
                            Map userMap = (Map) map.get("user");
                            User u = User.parseUsersArray(gson.toJson(Collections.singletonList(userMap))).get(0);
                            context.getUsersAdmin().add(u);
                            saveToDocuments(u);
                            return true;
                        }
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        };

        addTask.setOnSucceeded(ev -> {
            if (addTask.getValue()) {
                initializeTable();
                showAlert("Успех", "Пользователь добавлен. Информация сохранена в Documents/newUsers/");
            } else {
                showAlert("Ошибка", "Не удалось добавить пользователя.");
            }
        });

        addTask.setOnFailed(ev -> {
            Throwable ex = addTask.getException();
            showAlert("Ошибка", "Ошибка при добавлении пользователя: " + (ex != null ? ex.getMessage() : "<unknown>"));
        });

        executor.submit(addTask);
    }


    @FXML
    private void handleBack(ActionEvent actionEvent) {
        Context.getInstance().switchScene("Admin-Home-view.fxml");
    }

    public void handleTypeBoxButton(ActionEvent actionEvent) {
        Context.getInstance().switchScene("RegAdmin-Type-Display-view.fxml");
    }

    public void handleTypeModelButton(ActionEvent actionEvent) {
        Context.getInstance().switchScene("RegAdmin-Model-choose-view.fxml");
    }

    public void handleLogout(ActionEvent actionEvent) {
        try {
            executor.shutdownNow();
        } catch (Exception ignored) {}
        if (context.getIsAdminLogin()){
            context.setIsAdminLogin(false);
            context.switchScene("Bbox-view.fxml");
        }
        else{
            context.switchScene("Login-view.fxml");
        }
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
