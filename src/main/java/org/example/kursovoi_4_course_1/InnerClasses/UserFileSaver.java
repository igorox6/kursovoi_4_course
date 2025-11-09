package org.example.kursovoi_4_course_1.InnerClasses;
import org.example.kursovoi_4_course_1.DBClasses.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class UserFileSaver {

    private static final DateTimeFormatter FN_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private UserFileSaver() {}

    public static Path saveToDocuments(User user) {
        if (user == null) throw new IllegalArgumentException("user == null");
        String login = user.getLogin() == null ? "unknown" : user.getLogin();
        String password = user.getPassword() == null ? "" : user.getPassword();

        OffsetDateTime ts = user.getCreated_at() == null ? OffsetDateTime.now() : user.getCreated_at();
        String timestamp = ts.format(FN_TS);

        String fileName = String.format("user_%s_%s.txt", login, timestamp);

        Path dir = getDocumentsNewUsersDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку для сохранения: " + dir, e);
        }

        Path file = dir.resolve(fileName);

        List<String> lines = List.of(
                String.format("Логин: %s", login),
                String.format("Пароль: %s", password)
        );

        try {
            Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            System.out.println("Данные пользователя сохранены: " + file.toAbsolutePath());
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать файл: " + file, e);
        }
    }


    private static Path getDocumentsNewUsersDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Documents", "newUsers");
    }


    // Удобный метод, если хочешь сохранить просто пару login/password без User
    public static Path saveCredentials(String login, String password) {
        User u = new User();
        u.setLogin(login);
        u.setPassword(password);
        u.setCreated_at(OffsetDateTime.now());
        return saveToDocuments(u);
    }
}
