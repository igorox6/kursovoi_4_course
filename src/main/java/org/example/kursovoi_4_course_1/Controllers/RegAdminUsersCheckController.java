package org.example.kursovoi_4_course_1.Controllers;

import com.google.gson.Gson;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.example.kursovoi_4_course_1.DBClasses.RoleType;
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

import static org.example.kursovoi_4_course_1.InnerClasses.RandomUserGenerator.generateLogin;
import static org.example.kursovoi_4_course_1.InnerClasses.RandomUserGenerator.generatePassword;
import static org.example.kursovoi_4_course_1.InnerClasses.UserFileSaver.saveToDocuments;

public class RegAdminUsersCheckController extends Controller {

    private Context context;
    private final ObservableList<User> items = FXCollections.observableArrayList();
    private HttpClient client;

    @FXML private ImageView logoImageView;
    @FXML private Label adminName;

    @FXML private TableView<User> userTable;
    @FXML private Button editButton;
    @FXML private Button addButton;

    // drawer related
    @FXML private AnchorPane sideDrawer;
    @FXML private Button toggleButton;
    @FXML private Button typeDefButton;
    @FXML private Button modelTypeButton;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;

    private boolean drawerOpen = false;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();
        loadIcons(logoImageView,180);
        if (context.getAdminReg() != null) {
            adminName.setText(context.getAdminReg().getLogin());
        }
        else{adminName.setText("Admin");}
        client = HttpClient.newHttpClient();

        context.setUsersAdmin(getUsers());
        initializeTable();
    }

    private void initializeTable(){

        try {
            List<User> users = context.getUsersAdmin();
            ObservableList<User> observableUsers = FXCollections.observableArrayList(users);

            // Очищаем старые колонки, если они были
            userTable.getColumns().clear();

            // Создаём и настраиваем колонки
            TableColumn<User, Long> idColumn = new TableColumn<>("ID");
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

            TableColumn<User, String> loginColumn = new TableColumn<>("Логин");
            loginColumn.setCellValueFactory(new PropertyValueFactory<>("login"));

            TableColumn<User, String> nameColumn = new TableColumn<>("Имя");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<User, String> secondNameColumn = new TableColumn<>("Фамилия");
            secondNameColumn.setCellValueFactory(new PropertyValueFactory<>("second_name"));

            TableColumn<User, String> patronymicNameColumn = new TableColumn<>("Отчество");
            patronymicNameColumn.setCellValueFactory(new PropertyValueFactory<>("patronymic_name"));

            TableColumn<User, Boolean> activeColumn = new TableColumn<>("Активен");
            activeColumn.setCellValueFactory(new PropertyValueFactory<>("is_active"));

            TableColumn<User, String> createdColumn = new TableColumn<>("Создан");
            createdColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getCreated_at() != null
                                    ? cellData.getValue().getCreated_at().toLocalDate().toString()
                                    : ""
                    ));
            TableColumn<User, String> updatedColumn = new TableColumn<>("Обновлена");
            updatedColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getUpdated_at() != null
                                    ? cellData.getValue().getUpdated_at().toLocalDate().toString()
                                    : ""
                    ));
            TableColumn<User, String> lastLoginColumn = new TableColumn<>("Последний логин");
            lastLoginColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getLast_login() != null
                                    ? cellData.getValue().getLast_login().toLocalDate().toString()
                                    : ""
                    ));
            // 4️⃣ Добавляем колонки в таблицу
            userTable.getColumns().addAll(
                    idColumn, loginColumn, nameColumn, secondNameColumn, patronymicNameColumn,
                     activeColumn, createdColumn,updatedColumn,lastLoginColumn
            );

            // 5️⃣ Заполняем таблицу данными
            userTable.setItems(observableUsers);

        } catch (Exception e) {
            System.out.println("Ошибка при инициализации таблицы: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<User> getUsers() {
        String url = "http://localhost:8080/users";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            String body = (sendRequest(request)).body();

            return( User.parseUsersArray(body));
        } catch (Exception e) {
            System.out.println("Ошибка при получении пользователей: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    protected HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("RegAdminUsersCheck/sendRequest: HTTP Status Code: " + response.statusCode());
                System.out.println("RegAdminUsersCheck/sendRequest: Response Body: " + response.body());
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

    public void buttonEdit(ActionEvent actionEvent) {
    }

    public void buttonAdd(ActionEvent actionEvent) {

        String pwd = generatePassword();
        String login =generateLogin();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Добавление нового пользователя");
        alert.setHeaderText("Добавить пользователя с полями\n" + "Логин: "+login + "\nПароль: "+pwd);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.OK) {
            User newUser = new User();
            newUser.setLogin(login);
            newUser.setPassword(pwd);
            newUser.setIs_active(true);
            newUser.setCreated_at(OffsetDateTime.now());
            newUser.setUpdated_at(OffsetDateTime.now());
            newUser.setId((long) ((context.getUsersAdmin()).size() - 1));

            try{
                AddUser(newUser);
                saveToDocuments(newUser);
                Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
                alert1.setTitle("Успешно!");
                alert1.setHeaderText("Пользователь успешно добавлен!\n Информация о пользователе лежит в файле Documents/newUsers/");
                alert1.showAndWait();
                (context.getUsersAdmin()).add(newUser);
                initializeTable();
            }
            catch (IllegalArgumentException e){
                Alert alert1 = new Alert(Alert.AlertType.ERROR);
                alert1.setTitle("Ошибка");
                alert1.setHeaderText("Логин или пароль пустой");
                alert1.showAndWait();
            }
            catch(Exception e){
                Alert alert1 = new Alert(Alert.AlertType.ERROR);
                alert1.setTitle("Ошибка");
                alert1.setHeaderText("Случилась неизвестная ошибка");
                alert1.showAndWait();
            }


        }
        else{
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
            alert1.setTitle("Успешно!");
            alert1.setHeaderText("Пользователь не добавлен");
            alert1.showAndWait();
        }
    }

    protected Boolean AddUser(User newUser) {
        String url = "http://localhost:8080/users/register";
        String login = newUser.getLogin();
        String password = newUser.getPassword();

        if (login == null || password == null) {
            throw new IllegalArgumentException("Login and password are required");
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("login", login);
        requestBody.put("password", password);
        String json = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();


        Map<String, Object> result = new HashMap<>();
        try {
            HttpResponse<String> response = sendRequest(request);
            Map<String, Object> responseBody = gson.fromJson(response.body(), Map.class);

            Number status = (Number) responseBody.get("status");
            if (status != null && status.intValue() == 200) {
                return true;
            } else {
                return false;
            }
        } catch (RuntimeException e) {
            result.put("status", "error");
            result.put("message", "Ошибка при регистрации: " + e.getMessage());
            return false;
        }

    }

    public void handleLogout(ActionEvent actionEvent) {
        if (context.getIsAdminLogin()){
            context.setIsAdminLogin(false);
            context.switchScene("Bbox-view.fxml");
        }
        else{
            context.switchScene("Login-view.fxml");
        }
    }

    public void handleTypeBoxButton(ActionEvent actionEvent) {
        context.switchScene("RegAdmin-Type-Display-view.fxml");
    }

    public void handleTypeModelButton(ActionEvent actionEvent) {
        context.switchScene("RegAdmin-Model-choose-view.fxml");
    }

}
