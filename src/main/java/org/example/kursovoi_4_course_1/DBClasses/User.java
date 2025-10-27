package org.example.kursovoi_4_course_1.DBClasses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
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
    private Gson gson;
    private HttpClient client;

    public User(){
        role = RoleType.USER;
        gson = new Gson();
        client = HttpClient.newHttpClient();
    }

    //TODO добавить чтение предпочитаемого отображения (настройки) из файла
    public Map<String, Object> login (){
        String url = "http://localhost:8080/users/login";
        if (login == null || password == null) {
            throw new RuntimeException("Login and password are required");
        }
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("login", login);
        requestBody.put("password", password);
        String json = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        System.out.println("POST request: " + json);
        Map<String, Object> result = new HashMap<>();
        try{
            HttpResponse<String> response = sendRequest(request);
            Map<String, Object> responseBody = (gson.fromJson(response.body(), Map.class));

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

}
