package org.example.kursovoi_4_course_1.Controllers;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.Interfaces.InitializableController;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements InitializableController,Initializable {
    private Application app;
    private Context context;

    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @Override
    public void setMainApp(Application app, Context context) {
        this.app = app;
        this.context = context;
    }

    @Override
    public void setMainAppWithObject(Application app, Context context, Object data) {
        this.app = app;
        this.context = context;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}