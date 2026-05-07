package org.example.kursovoi_4_course_1.DBClasses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;            // ← подправь пакет если нужно
import org.example.kursovoi_4_course_1.DBClasses.TypeDisplay;
import org.example.kursovoi_4_course_1.DBClasses.UserSettings;
import org.example.kursovoi_4_course_1.InnerClasses.Crypt;

@Getter
@Setter
@NoArgsConstructor
public class User {
    private Long id;
    private String login;
    private String password;
    private Integer role_id;
    private Boolean is_active;
    private String name;
    private String second_name;
    private String patronymic_name;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
    private OffsetDateTime last_login;

    private UserSettings user_settings;
    private RoleType role;
    private Gson gson = new Gson();
    private HttpClient client = HttpClient.newHttpClient();

    public static List<User> parseUsersArray(String jsonArray) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> maps = gson.fromJson(jsonArray, listType);

        List<User> users = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            users.add(fromMap(m));
        }
        return users;
    }

    public static User fromMap(Map<String, Object> m) {
        User u = new User();

        if (m.get("id") != null) u.id = ((Number) m.get("id")).longValue();
        u.login = (String) m.get("login");
        u.password = (String) m.get("passwordHash");
        u.is_active = (Boolean) m.get("isActive");
        u.name = (String) m.get("name");
        u.second_name = (String) m.get("secondName");
        u.patronymic_name = (String) m.get("patronymicName");

        Object roleObj = m.get("role");
        if (roleObj instanceof Map<?, ?> roleMap) {
            Object roleName = roleMap.get("name");
            if (roleName != null)
                u.role = RoleType.valueOf(roleName.toString().toUpperCase());
        }

        if (m.get("createdAt") != null) u.created_at = OffsetDateTime.parse((String) m.get("createdAt"));
        if (m.get("updatedAt") != null) u.updated_at = OffsetDateTime.parse((String) m.get("updatedAt"));
        if (m.get("lastLogin") != null) u.last_login = OffsetDateTime.parse((String) m.get("lastLogin"));

        return u;
    }

    public Map<String, Object> login (){
        String ipAddress = "";
        try {
            HttpClient ipClient = HttpClient.newHttpClient();
            HttpRequest ipRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .GET()
                    .build();
            HttpResponse<String> ipResponse = ipClient.send(ipRequest, HttpResponse.BodyHandlers.ofString());
            if (ipResponse.statusCode() == 200) {
                ipAddress = ipResponse.body().trim();
            }

        } catch (Exception e) {
            System.err.println("IP fetch error: " + e.getMessage());
            ipAddress = "";
        }

        String url = "http://localhost:8080/users/login";
        if (login == null || password == null) {
            throw new RuntimeException("Login and password are required");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("login", login);
        requestBody.put("password", password);
        if (!ipAddress.isEmpty()) {
            requestBody.put("ipAddress", ipAddress);
        }

        String json = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        System.out.println("POST /users/login body: " + json);

        Map<String, Object> result = new HashMap<>();
        try {
            HttpResponse<String> response = sendRequest(request);
            Map<String, Object> responseBody = gson.fromJson(response.body(), Map.class);

            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");

            this.id = ((Number) userMap.get("id")).longValue();
            this.login = (String) userMap.get("login");
            String roleName = (String) ((Map<String, Object>) userMap.get("role")).get("name");
            this.role = RoleType.valueOf(roleName.toUpperCase());
            this.password = (String) userMap.get("passwordHash");
            this.is_active = (Boolean) userMap.get("isActive");
            this.name = (String) userMap.get("name");
            this.second_name = (String) userMap.get("secondName");
            this.patronymic_name = (String) userMap.get("patronymicName");
            this.created_at = OffsetDateTime.parse((String) userMap.get("createdAt"));
            this.updated_at = OffsetDateTime.parse((String) userMap.get("updatedAt"));
            if (userMap.get("lastLogin") != null)
                this.last_login = OffsetDateTime.parse((String) userMap.get("lastLogin"));

            result.put("status", "success");
            result.put("id", this.id);
            result.put("login", this.login);
            result.put("role", this.role);
            result.put("is_active", this.is_active);
            result.put("name", this.name);
            result.put("second_name", this.second_name);
            result.put("patronymic_name", this.patronymic_name);
            result.put("created_at", this.created_at);
            result.put("updated_at", this.updated_at);
            result.put("last_login", this.last_login);
        } catch (RuntimeException e) {
            result.put("status", "error");
            result.put("message", "Ошибка при входе: " + e.getMessage());
        }


        return result;
    }


    protected HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("User/sendRequest: HTTP Status Code: " + response.statusCode());
                System.out.println("User/sendRequest: Response Body: " + response.body());
                throw new RuntimeException("Request failed with status: " + response.statusCode());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при отправке запроса: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Path getPreferencesFilePath() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home") + "/AppData/Roaming"; // fallback
        }
        return Paths.get(appData, "Local", "VisionMark", "Preferences", "preferences.txt");
    }
    public void loadLocalPreferences() {
        if (this.id == null || this.id <= 0) {
            return;
        }

        Path file = getPreferencesFilePath();
        if (!Files.exists(file)) {
            return;
        }

        String userIdStr = this.id.toString();

        try {
            List<String> lines = Files.readAllLines(file);

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("=")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }

                String storedId = parts[0].trim();
                String encrypted = parts[1].trim();

                if (storedId.equals(userIdStr)) {
                    String decrypted = Crypt.decrypt(encrypted);
                    if (decrypted.isEmpty()) {
                        return;
                    }

                    try {
                        TypeDisplay typeDisplay = TypeDisplay.valueOf(decrypted.toUpperCase());

                        if (this.user_settings == null) {
                            this.user_settings = new UserSettings();
                        }

                        this.user_settings.setUserId(this.id);
                        this.user_settings.setTypeDisplay(typeDisplay);
                        this.user_settings.setUpdatedAt(OffsetDateTime.now());

                        // Если в будущем захочешь сохранять и id моделей — здесь же их можно читать
                        // пока оставляем заглушки или null
                        // this.user_settings.setModelBboxId(...);
                        // this.user_settings.setModelKeypointsId(...);

                        System.out.println("Локальные настройки загружены для user " + this.id +
                                ": typeDisplay = " + typeDisplay);

                    } catch (IllegalArgumentException e) {
                        System.err.println("Некорректный TypeDisplay в файле для user " + this.id +
                                ": " + decrypted);
                    }
                    return; // нашли → больше не ищем
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка чтения локальных предпочтений: " + e.getMessage());
        }
    }

    /**
     * Сохраняет текущее значение TypeDisplay в локальный файл (дозапись / обновление строки)
     *
     * Используется, когда пользователь меняет тип отображения в интерфейсе
     */
    public void saveTypeDisplayToLocalPreferences(TypeDisplay typeDisplay) {
        if (this.id == null || this.id <= 0) {
            System.err.println("Нельзя сохранить: user.id не установлен");
            return;
        }

        if (typeDisplay == null) {
            typeDisplay = TypeDisplay.ALL; // дефолт
        }

        String userIdStr = this.id.toString();
        String encrypted = Crypt.encrypt(typeDisplay.name());

        Path file = getPreferencesFilePath();
        Path parentDir = file.getParent();

        try {
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            List<String> lines = Files.exists(file) ? Files.readAllLines(file) : new ArrayList<>();
            StringBuilder updatedContent = new StringBuilder();
            boolean found = false;

            for (String line : lines) {
                if (line.trim().startsWith(userIdStr + "=")) {
                    updatedContent.append(userIdStr).append("=").append(encrypted).append("\n");
                    found = true;
                } else {
                    updatedContent.append(line).append("\n");
                }
            }

            if (!found) {
                updatedContent.append(userIdStr).append("=").append(encrypted).append("\n");
            }

            Files.writeString(file, updatedContent.toString());

            // обновляем и объект в памяти
            if (this.user_settings == null) {
                this.user_settings = new UserSettings();
                this.user_settings.setUserId(this.id);
            }
            this.user_settings.setTypeDisplay(typeDisplay);
            this.user_settings.setUpdatedAt(OffsetDateTime.now());

            System.out.println("Сохранено TypeDisplay = " + typeDisplay + " для user " + this.id);

        } catch (IOException e) {
            System.err.println("Ошибка сохранения локальных предпочтений: " + e.getMessage());
        }
    }
}
