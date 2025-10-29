package org.example.kursovoi_4_course_1.Controllers;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.example.kursovoi_4_course_1.DBClasses.RoleType;
import org.example.kursovoi_4_course_1.DBClasses.User;
import org.example.kursovoi_4_course_1.InnerClasses.BufferedImageTranscoder;
import org.example.kursovoi_4_course_1.InnerClasses.Context;
import org.example.kursovoi_4_course_1.InnerClasses.Controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class LoginController extends Controller {

    private Application app;
    private Context context;

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ImageView logoImageView;

    @FXML
    private Button backButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        this.context = Context.getInstance();
        this.app = context.getApp();
        loadIcons(logoImageView,180);
        //TODO НЕ РАБОТАЕТ
        if (context != null) {
            if (context.getIsAdminLogin()){ // авторизация после обычного пользователя, включаем возможность вернуться назад
                backButton.setVisible(true);
            }
        }

    }




    @FXML
    protected void handleLogin() {
        //TODO добавление в логи авторизации
        User user = new User();

        user.setLogin(loginField.getText());
        user.setPassword(passwordField.getText());

        Map<String, Object> result = user.login();



        if (result.get("status") == "success") {
            if (user.getRole() == RoleType.ADMIN) { // логин за админа от приложения

            }
            else if (user.getRole() == RoleType.REGADMIN){ // логин за админа от компании
                context.setAdminReg(user);
            }

            else if (user.getRole() == RoleType.USER){
                context.setUser(user);
                context.switchScene("Bbox-view.fxml");
            }
            else {
                //Ошибка
            }
        }
        else{
            System.out.println("не успэх");
        }


    }
    @FXML
    protected void getBack(){
        System.out.println(1);
    }


}