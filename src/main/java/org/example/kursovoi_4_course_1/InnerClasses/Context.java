package org.example.kursovoi_4_course_1.InnerClasses;

import javafx.application.Application;
import lombok.Getter;
import lombok.Setter;
import org.example.kursovoi_4_course_1.App;
import org.example.kursovoi_4_course_1.DBClasses.TypeDisplay;
import org.example.kursovoi_4_course_1.DBClasses.User;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;


@Getter
@Setter
public class Context {

    private Application app;
    private List<User> usersAdmin;
    private User user = new User();
    private User adminReg = new User();
    private Object data;
    private Boolean isAdminLogin;
    private static final Context INSTANCE = new Context();
    private TypeDisplay typeDisplay;

    private JsonObject currentBboxModel;
    private JsonObject currentPointsModel;

    private Context() {
        isAdminLogin = false;
        typeDisplay = TypeDisplay.ALL;
        currentBboxModel = null;
        currentPointsModel = null;
    }

    public static Context getInstance() {
        return INSTANCE;
    }

    public void switchScene(String fxml) {
        ((App) app).switchScene(fxml);
    }

    public void setUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.user = user;

        user.loadLocalPreferences();
        TypeDisplay loadedType = TypeDisplay.ALL;

        if (user.getUser_settings() != null) {
            TypeDisplay settingsType = user.getUser_settings().getTypeDisplay();
            if (settingsType != null) {
                loadedType = settingsType;
            }
        }

        this.typeDisplay = loadedType;
    }

}
