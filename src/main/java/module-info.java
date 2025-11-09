module org.example.kursovoi_4_course_1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires batik.transcoder;
    requires javafx.swing;
    requires webcam.capture;
    requires com.google.gson;
    requires java.net.http;
    requires com.microsoft.onnxruntime;


    opens org.example.kursovoi_4_course_1 to javafx.fxml;
    opens org.example.kursovoi_4_course_1.DBClasses to javafx.base, javafx.fxml;
    exports org.example.kursovoi_4_course_1;
    exports org.example.kursovoi_4_course_1.Controllers;
    opens org.example.kursovoi_4_course_1.Controllers to javafx.fxml;
}