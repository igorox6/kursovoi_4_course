package org.example.kursovoi_4_course_1.InnerClasses;

import javafx.application.Application;
import lombok.Getter;
import lombok.Setter;
import org.example.kursovoi_4_course_1.App;
import org.example.kursovoi_4_course_1.DBClasses.User;

@Getter
@Setter
public class Context {
    private Application app;
    private User user;
    private User adminReg;
    private Object data;
    private Boolean isAdminLogin = false;
    private static final Context INSTANCE = new Context();


    private Context() {}

    public static Context getInstance() {
        return INSTANCE;
    }

    public void switchScene(String fxml) {
        ((App) app).switchScene(fxml);
    }

}
