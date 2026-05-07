package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Setter
public class ModelManager {

    private static final String API_BASE_URL = "http://localhost:8080/models";
    private static final Gson GSON = new Gson();

    // ======= Локальные модели =======
    private static final String LOCAL_MODELS_DIR = "models";
    private static final String LOCAL_BBOX_FILE = "face_bbox_model_3.onnx";
    private static final String LOCAL_POINTS_FILE = "face_landmarks_1.onnx";
    private static final String LOCAL_POINTS_DATA_FILE = "face_landmarks_1.onnx.data";

    // Если true — грузим из папки models/, если false — из API
    private static final boolean USE_LOCAL_MODELS = true;
    // =================================

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
        this.allModelsInfo = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
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
        if (USE_LOCAL_MODELS) {
            return loadFromLocalFiles();
        }
        return loadFromApi();
    }

    // ==================== ЗАГРУЗКА ИЗ ЛОКАЛЬНЫХ ФАЙЛОВ ====================

    private CompletableFuture<Void> loadFromLocalFiles() {
        return CompletableFuture.runAsync(() -> {
            System.out.println("Loading models from local folder: " + LOCAL_MODELS_DIR + "/");

            // --- BBOX ---
            Path bboxPath = findModelFile(LOCAL_BBOX_FILE);
            if (bboxPath != null) {
                try {
                    bboxManager = new ModelManagerBbox(env, bboxPath);
                    long size = Files.size(bboxPath);
                    System.out.println("[OK] Bbox loaded: " + bboxPath + " (" + size + " bytes)");
                } catch (Exception e) {
                    bboxManager = null;
                    System.err.println("[FAIL] Bbox load error: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                bboxManager = null;
                System.err.println("[WARN] Bbox file not found: " + LOCAL_BBOX_FILE);
            }

            // --- POINTS ---
            Path pointsPath = findModelFile(LOCAL_POINTS_FILE);
            if (pointsPath != null) {
                try {
                    checkExternalDataFile(pointsPath);
                    pointsManager = new ModelManagerPoints(env, pointsPath);
                    long size = Files.size(pointsPath);
                    System.out.println("[OK] Points loaded: " + pointsPath + " (" + size + " bytes)");
                } catch (Exception e) {
                    pointsManager = null;
                    System.err.println("[FAIL] Points load error: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                pointsManager = null;
                System.err.println("[WARN] Points file not found: " + LOCAL_POINTS_FILE);
            }

            System.out.println("Local model loading complete.");
        }, executor);
    }

    private void checkExternalDataFile(Path pointsPath) {
        Path dir = pointsPath.toAbsolutePath().getParent();
        if (dir == null) return;

        Path dataPath = dir.resolve(LOCAL_POINTS_DATA_FILE);
        if (!Files.exists(dataPath) || !Files.isRegularFile(dataPath)) {
            System.err.println("[WARN] External data file not found near points model: " + dataPath);
            System.err.println("[WARN] If points model was exported with external data, it will not load without .onnx.data file.");
        } else {
            System.out.println("[OK] Points external data found: " + dataPath);
        }
    }

    /**
     * Ищет файл модели в нескольких местах:
     * 1. models/filename — рядом с jar/рабочей директорией
     * 2. src/main/resources/models/filename — для запуска из IDE
     * 3. filename — в текущей директории
     */
    private Path findModelFile(String filename) {
        Path[] candidates = {
                Paths.get(LOCAL_MODELS_DIR, filename),
                Paths.get("src", "main", "resources", "models", filename),
                Paths.get(filename),
        };

        for (Path p : candidates) {
            if (Files.exists(p) && Files.isRegularFile(p)) {
                return p.toAbsolutePath();
            }
        }

        try {
            var url = getClass().getClassLoader().getResource("models/" + filename);
            if (url != null) {
                Path p = Paths.get(url.toURI());
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    return p.toAbsolutePath();
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    // ==================== ЗАГРУЗКА ИЗ API ====================

    private CompletableFuture<Void> loadFromApi() {
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
                                            Path modelPath = saveModelToLocalFile(bestBboxMeta, bytes);
                                            bboxManager = new ModelManagerBbox(env, modelPath);
                                            System.out.println("Loaded BBOX model ID: " + bestBboxMeta.get("id"));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }));
                    }

                    if (bestPointsMeta != null) {
                        loads.add(fetchModelBytesByIdAsync(bestPointsMeta.get("id").getAsInt())
                                .thenAccept(bytes -> {
                                    if (bytes != null && bytes.length > 0) {
                                        try {
                                            Path modelPath = saveModelToLocalFile(bestPointsMeta, bytes);
                                            pointsManager = new ModelManagerPoints(env, modelPath);
                                            System.out.println("Loaded POINTS model ID: " + bestPointsMeta.get("id"));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }));
                    }

                    return CompletableFuture.allOf(loads.toArray(new CompletableFuture[0]));
                })
                .thenRun(() -> System.out.println("Models refreshed from API"))
                .exceptionally(ex -> {
                    System.err.println("Error in refreshModels: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private Path saveModelToLocalFile(JsonObject meta, byte[] bytes) throws IOException {
        Files.createDirectories(Paths.get(LOCAL_MODELS_DIR));

        String type = meta.has("type") ? meta.get("type").getAsString() : "MODEL";
        int id = meta.has("id") ? meta.get("id").getAsInt() : 0;
        int version = meta.has("version") ? meta.get("version").getAsInt() : 0;

        String filename = id + "_" + type + "_v" + version + ".onnx";
        Path path = Paths.get(LOCAL_MODELS_DIR, filename).toAbsolutePath();

        Files.write(path, bytes);

        return path;
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
                    throw new IOException("Failed to fetch models info: " + response.statusCode());
                }

                JsonArray modelsArray = JsonParser.parseString(response.body()).getAsJsonArray();
                List<JsonObject> list = new ArrayList<>();

                for (JsonElement element : modelsArray) {
                    if (element.isJsonObject()) {
                        list.add(element.getAsJsonObject());
                    }
                }

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
            throw new RuntimeException(e);
        }
    }

    public JsonObject findBestByType(String type) {
        if (allModelsInfo == null || allModelsInfo.isEmpty()) return null;

        return allModelsInfo.stream()
                .filter(m -> m.has("type") && m.get("type").getAsString().equals(type))
                .max(java.util.Comparator
                        .comparingInt((JsonObject m) -> m.has("version") ? m.get("version").getAsInt() : Integer.MIN_VALUE)
                        .thenComparingDouble((JsonObject m) -> m.has("loss") ? -m.get("loss").getAsDouble() : Double.MAX_VALUE))
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
                    throw new IOException("Failed to fetch model ID " + id);
                }

                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();

                if (!obj.has("modelData") || obj.get("modelData").isJsonNull()) {
                    return new byte[0];
                }

                return Base64.getDecoder().decode(obj.get("modelData").getAsString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public byte[] fetchModelBytesById(int id) throws IOException, InterruptedException {
        try {
            return fetchModelBytesByIdAsync(id).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> uploadModelAsync(String type, String path, short version, float loss, String comment) {
        return CompletableFuture.runAsync(() -> {
            try {
                File modelFile = new File(path);

                if (!modelFile.exists()) {
                    throw new IOException("File not found: " + path);
                }

                byte[] modelData;

                try (InputStream is = new FileInputStream(modelFile)) {
                    modelData = is.readAllBytes();
                }

                Map<String, Object> dto = new HashMap<>();
                dto.put("type", type);
                dto.put("version", version);
                dto.put("loss", loss);
                dto.put("modelData", Base64.getEncoder().encodeToString(modelData));
                dto.put("comment", comment);
                dto.put("size", modelData.length);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/add"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(dto), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IOException("Upload failed: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void uploadModel(String type, String path, short version, float loss, String comment) throws IOException, InterruptedException {
        try {
            uploadModelAsync(type, path, version, loss, comment).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<JsonObject> getAllModelsInfo() {
        return new ArrayList<>(allModelsInfo);
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
}