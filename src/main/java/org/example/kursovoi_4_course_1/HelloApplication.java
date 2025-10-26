package org.example.kursovoi_4_course_1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.Interfaces.InitializableController;

import java.io.IOException;

public class HelloApplication extends Application {
    private Stage primaryStage;
    private StackPane rootLayout;
    private Context context;
    @Getter
    private Object currentController;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        context = Context.getInstance();
        this.primaryStage = stage;
        rootLayout = new StackPane();
        primaryStage.setTitle("Hello!");
        primaryStage.setScene(new Scene(rootLayout,800,450));

        switchScene("Login-view.fxml");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
    public void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newScene = loader.load();

            Object controller = loader.getController();

            if (controller instanceof InitializableController) {
                ((InitializableController) controller).setMainApp(this, context);
            } else {
                System.out.println("Контроллер не реализует InitializableController: " + controller.getClass().getName());
            }

            rootLayout.getChildren().setAll(newScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void switchSceneWithObject(String fxmlFile, Object data) {
        try {
            System.out.println("Switching to: " + fxmlFile + " with data: " + (data != null ? data.toString() : "null"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newScene = loader.load();

            currentController = loader.getController();
            System.out.println("Loaded controller: " + (currentController != null ? currentController.getClass().getName() : "null"));

            if (currentController instanceof InitializableController) {
                ((InitializableController) currentController).setMainAppWithObject(this, context, data);
            } else {
                System.out.println("Контроллер не реализует InitializableController: " + (currentController != null ? currentController.getClass().getName() : "null"));
            }

            rootLayout.getChildren().setAll(newScene);
        } catch (IOException e) {
            System.out.println("Ошибка при переходе к сцене: " + e.getMessage());
            e.printStackTrace();
        }
    }
}