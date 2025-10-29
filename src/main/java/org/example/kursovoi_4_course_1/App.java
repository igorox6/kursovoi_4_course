package org.example.kursovoi_4_course_1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import org.example.kursovoi_4_course_1.InnerClasses.Context;

import java.io.IOException;

public class App extends Application {
    private Stage primaryStage;
    private StackPane rootLayout;
    private Context context;
    @Getter
    private Object currentController;

    @Override
    public void start(Stage stage) throws IOException {


        context = Context.getInstance();
        context.setApp(this);

        this.primaryStage = stage;
        rootLayout = new StackPane();
        primaryStage.setTitle("Hello!");
        primaryStage.setScene(new Scene(rootLayout, 800, 450));

        switchScene("Bbox-view.fxml");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newScene = loader.load();

            rootLayout.getChildren().setAll(newScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}