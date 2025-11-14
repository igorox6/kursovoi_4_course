package org.example.kursovoi_4_course_1.DBClasses;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Model {
    private Integer id;
    private ModelType type;
    private Short version;
    private Float loss;
    private byte[] modelData;
    private String comment;
    private Long size;
    private OffsetDateTime createdAt;

    private transient Gson gson = new Gson();
    private transient HttpClient client = HttpClient.newHttpClient();

    public static List<Model> parseModelsArray(String jsonArray) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> maps = gson.fromJson(jsonArray, listType);

        List<Model> models = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            models.add(fromMap(m));
        }
        return models;
    }

    public static Model fromJsonObject(JsonObject obj) {
        Model mod = new Model();

        if (obj.has("id") && !obj.get("id").isJsonNull()) {
            mod.id = obj.get("id").getAsInt();
        }

        if (obj.has("type") && !obj.get("type").isJsonNull()) {
            String typeStr = obj.get("type").getAsString().toUpperCase();
            try {
                if (typeStr.contains("BBOX")) {
                    mod.type = ModelType.FACE_BBOX;
                } else if (typeStr.contains("KEYPOINTS") || typeStr.contains("POINTS")) {
                    mod.type = ModelType.FACE_KEYPOINTS;
                } else {
                    mod.type = ModelType.valueOf(typeStr);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown model type: " + typeStr);
                mod.type = null;
            }
        }

        if (obj.has("version") && !obj.get("version").isJsonNull()) {
            mod.version = obj.get("version").getAsShort();
        }
        if (obj.has("loss") && !obj.get("loss").isJsonNull()) {
            mod.loss = obj.get("loss").getAsFloat();
        }

        if (obj.has("modelData") && !obj.get("modelData").isJsonNull()) {
            String base64Str = obj.get("modelData").getAsString();
            if (base64Str != null && !base64Str.trim().isEmpty()) {
                try {
                    mod.modelData = Base64.getDecoder().decode(base64Str);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid Base64 for modelData (ID: " + mod.id + "): " + e.getMessage());
                    mod.modelData = new byte[0];
                }
            } else {
                mod.modelData = new byte[0];
            }
        } else {
            mod.modelData = new byte[0];
        }

        if (obj.has("comment") && !obj.get("comment").isJsonNull()) {
            mod.comment = obj.get("comment").getAsString();
        }
        if (obj.has("size") && !obj.get("size").isJsonNull()) {
            mod.size = obj.get("size").getAsLong();
        }
        if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
            mod.createdAt = OffsetDateTime.parse(obj.get("createdAt").getAsString());
        }

        return mod;
    }

    private static Model fromMap(Map<String, Object> m) {
        JsonObject obj = new Gson().toJsonTree(m).getAsJsonObject();
        return fromJsonObject(obj);
    }

    protected HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("Model/sendRequest: HTTP Status Code: " + response.statusCode());
                System.out.println("Model/sendRequest: Response Body: " + response.body());
                throw new RuntimeException("Request failed with status: " + response.statusCode());
            }
            return response;
        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при отправке запроса: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

