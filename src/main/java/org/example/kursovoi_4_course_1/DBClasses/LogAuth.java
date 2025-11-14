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

@Getter
@Setter
@NoArgsConstructor
public class LogAuth {
    private Integer id;
    private OffsetDateTime timeAuth;
    private Long userId;
    private String ipAddress;
    private Boolean success;

    private transient Gson gson = new Gson();  // Transient to avoid serialization issues
    private transient HttpClient client = HttpClient.newHttpClient();

    public static List<LogAuth> parseLogAuthsArray(String jsonArray) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> maps = gson.fromJson(jsonArray, listType);

        List<LogAuth> logAuths = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            logAuths.add(fromMap(m));
        }
        return logAuths;
    }

    private static LogAuth fromMap(Map<String, Object> m) {
        LogAuth la = new LogAuth();

        if (m.get("id") != null) la.id = ((Number) m.get("id")).intValue();
        if (m.get("timeAuth") != null) la.timeAuth = OffsetDateTime.parse((String) m.get("timeAuth"));

        Object userObj = m.get("user");
        if (userObj instanceof Map<?, ?> userMap) {
            if (userMap.get("id") != null) {
                la.userId = ((Number) userMap.get("id")).longValue();
            }
        }

        la.ipAddress = (String) m.get("ipAddress");
        la.success = (Boolean) m.get("success");

        return la;
    }

    protected HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("LogAuth/sendRequest: HTTP Status Code: " + response.statusCode());
                System.out.println("LogAuth/sendRequest: Response Body: " + response.body());
                throw new RuntimeException("Request failed with status: " + response.statusCode());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при отправке запроса: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
