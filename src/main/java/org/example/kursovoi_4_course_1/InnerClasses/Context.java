package org.example.kursovoi_4_course_1.InnerClasses;

import javafx.application.Application;
import lombok.Getter;
import lombok.Setter;
import org.example.kursovoi_4_course_1.App;
import org.example.kursovoi_4_course_1.DBClasses.User;

import java.util.List;

@Getter
@Setter
public class Context {
    private Application app;
    private List<User> usersAdmin;
    private User user;
    private User adminReg;
    private Object data;
    private Boolean isAdminLogin;
    private static final Context INSTANCE = new Context();


    private Context() {
        isAdminLogin = false;
    }

    public static Context getInstance() {
        return INSTANCE;
    }

    public void switchScene(String fxml) {
        ((App) app).switchScene(fxml);
    }

}
