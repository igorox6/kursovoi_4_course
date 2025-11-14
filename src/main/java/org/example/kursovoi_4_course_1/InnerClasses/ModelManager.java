package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


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
    private List<JsonObject> allModelsInfo;
    private final HttpClient httpClient;
    private final ExecutorService executor;


    public ModelManager() throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.httpClient = HttpClient.newHttpClient();
        this.allModelsInfo = new ArrayList<>();  // Empty cache initially
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);  // Daemon to not block JVM shutdown
            return t;
        });
        this.bboxManager = null;
        this.pointsManager = null;
        this.bestBboxMeta = null;
        this.bestPointsMeta = null;
    }


    public CompletableFuture<Void> asyncInit() {
        return asyncRefreshModels();
    }


    public CompletableFuture<Void> asyncRefreshModels() {
        return fetchAllInfoAsync(false)
                .thenCompose(infos -> {
                    allModelsInfo = infos;
                    bestBboxMeta = findBestByType("FACE_BBOX");
                    bestPointsMeta = findBestByType("FACE_KEYPOINTS");

                    List<CompletableFuture<Void>> loads = new ArrayList<>();
                    if (bestBboxMeta != null) {
                        loads.add(fetchModelBytesByIdAsync(bestBboxMeta.get("id").getAsInt())
                                .thenAccept(bytes -> {
                                    if (bytes != null && bytes.length > 0) {
                                        try {
                                            bboxManager = new ModelManagerBbox(env, bytes);
                                        } catch (OrtException e) {
                                            throw new RuntimeException(e);
                                        }
                                        System.out.println("Loaded BBOX model ID: " + bestBboxMeta.get("id"));
                                    } else {
                                        System.err.println("Failed to load BBOX bytes");
                                    }
                                }));
                    } else {
                        System.err.println("No best BBOX model found");
                    }

                    if (bestPointsMeta != null) {
                        loads.add(fetchModelBytesByIdAsync(bestPointsMeta.get("id").getAsInt())
                                .thenAccept(bytes -> {
                                    if (bytes != null && bytes.length > 0) {
                                        try {
                                            pointsManager = new ModelManagerPoints(env, bytes);
                                        } catch (OrtException e) {
                                            throw new RuntimeException(e);
                                        }
                                        System.out.println("Loaded POINTS model ID: " + bestPointsMeta.get("id"));
                                    } else {
                                        System.err.println("Failed to load POINTS bytes");
                                    }
                                }));
                    } else {
                        System.err.println("No best POINTS model found");
                    }

                    return CompletableFuture.allOf(loads.toArray(new CompletableFuture[0]));
                })
                .thenRun(() -> System.out.println("Models refreshed successfully"))
                .exceptionally(ex -> {
                    System.err.println("Error in refreshModels: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }


    public void refreshModelsSync() throws IOException, InterruptedException, OrtException {
        try {
            asyncRefreshModels().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh models synchronously", e);
        }
    }


    public CompletableFuture<List<JsonObject>> fetchAllInfoAsync(boolean allModels) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = allModels ? "/infoAll" : "/info";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + endpoint))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("Failed to fetch models info: " + response.statusCode() + " - " + response.body());
                }

                JsonArray modelsArray = JsonParser.parseString(response.body()).getAsJsonArray();
                List<JsonObject> list = new ArrayList<>();
                for (JsonElement element : modelsArray) {
                    if (element.isJsonObject()) {
                        list.add(element.getAsJsonObject());
                    }
                }
                System.out.println("Fetched " + list.size() + " models info from " + endpoint);
                return list;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to fetchAllInfo: " + e.getMessage(), e);
            }
        }, executor);
    }


    public List<JsonObject> fetchAllInfo(boolean allModels) throws IOException, InterruptedException {
        try {
            return fetchAllInfoAsync(allModels).get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof InterruptedException) {
                    throw (InterruptedException) cause;
                }
            }
            throw new RuntimeException(e);
        }
    }


    public JsonObject findBestByType(String type) {
        if (allModelsInfo == null || allModelsInfo.isEmpty()) {
            return null;
        }
        return allModelsInfo.stream()
                .filter((JsonObject m) -> m.has("type") && m.get("type").getAsString().equals(type))
                .max(Comparator.comparingInt((JsonObject m) -> {
                    if (m.has("version") && m.get("version").isJsonPrimitive()) {
                        return m.get("version").getAsInt();
                    }
                    return Integer.MIN_VALUE;
                }).thenComparingDouble((JsonObject m) -> {
                    if (m.has("loss") && m.get("loss").isJsonPrimitive()) {
                        return -m.get("loss").getAsDouble();
                    }
                    return Double.MAX_VALUE;
                }))
                .orElse(null);
    }


    public CompletableFuture<byte[]> fetchModelBytesByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/" + id))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("Failed to fetch model by ID " + id + ": " + response.statusCode() + " - " + response.body());
                }

                JsonObject modelObj = JsonParser.parseString(response.body()).getAsJsonObject();
                if (!modelObj.has("modelData") || modelObj.get("modelData").isJsonNull()) {
                    return new byte[0];
                }
                String base64Data = modelObj.get("modelData").getAsString();
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                System.out.println("Fetched model bytes for ID " + id + ": " + bytes.length + " bytes");
                return bytes;
            } catch (IOException | InterruptedException e) {
                System.err.println("Error fetching bytes for ID " + id + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }


    public byte[] fetchModelBytesById(int id) throws IOException, InterruptedException {
        try {
            return fetchModelBytesByIdAsync(id).get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof InterruptedException) {
                    throw (InterruptedException) cause;
                }
            }
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> uploadModelAsync(String type, String path, short version, float loss, String comment) {
        return CompletableFuture.runAsync(() -> {
            try {
                File modelFile = new File(path);
                if (!modelFile.exists() || !modelFile.isFile()) {
                    throw new IOException("Model file not found: " + path);
                }

                byte[] modelData;
                try (InputStream inputStream = new FileInputStream(modelFile)) {
                    modelData = inputStream.readAllBytes();
                }

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

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/add"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("Upload failed: " + response.statusCode() + " - " + response.body());
                }
                System.out.println("Model uploaded successfully: " + type);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Upload failed: " + e.getMessage(), e);
            }
        }, executor);
    }


    public void uploadModel(String type, String path, short version, float loss, String comment) throws IOException, InterruptedException {
        try {
            uploadModelAsync(type, path, version, loss, comment).get();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof InterruptedException) {
                    throw (InterruptedException) cause;
                }
            }
            throw new RuntimeException(e);
        }
    }


    public List<JsonObject> getAllModelsInfo() {
        return new ArrayList<>(allModelsInfo); // Копия для безопасности
    }

    public void close() throws OrtException {
        if (bboxManager != null) {
            bboxManager.close();
            bboxManager = null;
        }
        if (pointsManager != null) {
            pointsManager.close();
            pointsManager = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        env.close();
    }


    public static void main(String[] args) throws IOException, OrtException, InterruptedException {
        ModelManager mm = new ModelManager();
        try {
            String path = "C:\\Users\\igorox6\\Documents\\java_prog\\kursovoi_4_course_1\\src\\main\\resources\\models\\10_11_1_points.onnx";
            mm.uploadModel("FACE_KEYPOINTS", path, (short) 4, (float) 0.00112, "");
        } finally {
            mm.close();
        }
    }
}
