package org.example.kursovoi_4_course_1.Controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;


import java.net.URL;
import java.util.ResourceBundle;


public class AdminHomeController extends Controller {
    private Context context;


    @FXML
    private ImageView logoImageView;


    @FXML
    private Button authLogBtn;


    @FXML
    private Button adminChangesBtn;


    @FXML
    private Button modelsBtn;


    @FXML
    private Button rolesBtn;


    @FXML
    private Button userSettingsBtn;


    @FXML
    private Button usersBtn;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.context = Context.getInstance();
        loadIcons(logoImageView, 180);

    }


    @FXML
    protected void handleAuthLog() {
        context.switchScene("Admin-LogAuth-view.fxml");
    }


    @FXML
    protected void handleAdminChangesLog() {
        context.switchScene("Admin-LogRegionalAdminChange-view.fxml");
    }

    @FXML
    protected void handleModels() {
        context.switchScene("Admin-Models-view.fxml");
    }


    @FXML
    protected void handleRoles() {
        context.switchScene("Admin-Roles-view.fxml");
    }


    @FXML
    protected void handleUserSettings() {
        context.switchScene("Admin-UserSettings-view.fxml");
    }


    @FXML
    protected void handleUsers() {
        context.switchScene("Admin-Users-view.fxml");
    }

    @FXML
    public void handleExit() {
        context.switchScene("Login-view.fxml");
    }
}