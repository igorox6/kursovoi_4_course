package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Родительский класс для управления моделями.
 * Обеспечивает загрузку метаданных из /models/info, выбор лучших моделей по типам,
 * загрузку полных моделей по ID, инициализацию дочерних менеджеров.
 * Также содержит общий метод upload и OrtEnvironment.
 */

@Getter
@Setter
public class ModelManager {

    private static final String API_BASE_URL = "http://localhost:8080/models";
    private static final Gson GSON = new Gson();

    private final OrtEnvironment env;
    private ModelManagerBbox bboxManager;
    private ModelManagerPoints pointsManager;
    private JsonObject bestBboxMeta;
    private JsonObject bestPointsMeta;
    private List<JsonObject> allModelsInfo; // Кэш метаданных из /info

    public ModelManager() throws OrtException, IOException, InterruptedException {
        this.env = OrtEnvironment.getEnvironment();
        refreshModels(); // Загружаем при инициализации
    }

    /**
     * Обновляет все модели: загружает метаданные, выбирает лучшие, загружает байты, инициализирует дочерние.
     */
    public void refreshModels() throws IOException, InterruptedException, OrtException {
        allModelsInfo = fetchAllInfo();
        bestBboxMeta = findBestByType("FACE_BBOX");
        bestPointsMeta = findBestByType("FACE_KEYPOINTS");

        if (bestBboxMeta != null) {
            byte[] bboxBytes = fetchModelBytesById(bestBboxMeta.get("id").getAsInt());
            bboxManager = new ModelManagerBbox(env, bboxBytes);
        }
        if (bestPointsMeta != null) {
            byte[] pointsBytes = fetchModelBytesById(bestPointsMeta.get("id").getAsInt());
            pointsManager = new ModelManagerPoints(env, pointsBytes);
        }
    }

    /**
     * Загружает метаданные всех моделей из /models/info.
     * @return List<JsonObject> с данными (id, type, version, loss, comment, size, createdAt)
     */
    public List<JsonObject> fetchAllInfo() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/info"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch models info: " + response.statusCode());
        }

        JsonArray modelsArray = JsonParser.parseString(response.body()).getAsJsonArray();
        List<JsonObject> list = new ArrayList<>();
        for (JsonElement element : modelsArray) {
            list.add(element.getAsJsonObject());
        }
        return list;
    }

    /**
     * Находит лучшую модель по типу (максимальная версия, при равенстве — минимальный loss).
     * @param type Тип модели (FACE_BBOX или FACE_KEYPOINTS)
     * @return JsonObject метаданных лучшей модели или null, если не найдено
     */
    private JsonObject findBestByType(String type) {
        return allModelsInfo.stream()
                .filter(m -> m.get("type").getAsString().equals(type))
                .max(Comparator.comparingInt((JsonObject m) -> m.get("version").getAsInt())
                        .thenComparingDouble((JsonObject m) -> -m.get("loss").getAsDouble()))
                .orElse(null);
    }

    /**
     * Загружает байты модели по ID из /models/{id}.
     * @param id ID модели
     * @return byte[] modelData
     */
    public byte[] fetchModelBytesById(int id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch model by ID " + id + ": " + response.statusCode());
        }

        JsonObject modelObj = JsonParser.parseString(response.body()).getAsJsonObject();
        String base64Data = modelObj.get("modelData").getAsString();
        return Base64.getDecoder().decode(base64Data);
    }

    /**
     * Загружает модель в базу по пути к файлу.
     * @param type Тип (FACE_BBOX или FACE_KEYPOINTS)
     * @param path Путь к файлу в resources (например, "/models/bbox_model.onnx")
     * @param version Версия
     * @param loss Loss
     * @param comment Комментарий
     */
    public void uploadModel(String type, String path, short version, float loss, String comment) throws IOException, InterruptedException {
        InputStream inputStream = getClass().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("Model file not found in resources: " + path);
        }

        byte[] modelData = inputStream.readAllBytes();
        inputStream.close();

        String base64Encoded = Base64.getEncoder().encodeToString(modelData);
        long size = modelData.length;

        Map<String, Object> dto = new HashMap<>();
        dto.put("type", type);
        dto.put("version", version);
        dto.put("loss", loss);
        dto.put("modelData", base64Encoded);
        dto.put("comment", comment);
        dto.put("size", size);

        String jsonBody = GSON.toJson(dto);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/add"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Upload failed: " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Получает метаданные всех моделей (кэш из /info).
     */
    public List<JsonObject> getAllModelsInfo() {
        return new ArrayList<>(allModelsInfo); // Копия для безопасности
    }


    /**
     * Закрывает ресурсы.
     */
    public void close() throws OrtException {
        if (bboxManager != null) bboxManager.close();
        if (pointsManager != null) pointsManager.close();
        env.close();
    }
}