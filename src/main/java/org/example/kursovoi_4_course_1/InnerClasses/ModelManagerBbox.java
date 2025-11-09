package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;

/**
 * Дочерний класс для BBOX-модели.
 * Инициализируется байтами модели, использует общий OrtEnvironment.
 * Теперь с ПЛАВНЫМ отслеживанием лица (EMA smoothing).
 */
public class ModelManagerBbox {

    private final OrtSession session;
    private final OrtEnvironment env;

    // Параметры сглаживания
    private final float alpha = 0.7f;  // 0.0 = max smooth, 1.0 = no smooth
    private float[] prevBbox = null;   // Хранит предыдущий bbox для сглаживания

    public ModelManagerBbox(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    /**
     * Предсказывает bbox с плавным сглаживанием.
     * @param image Входное изображение (любого размера)
     * @return float[4] = [x, y, w, h] в пикселях оригинального изображения
     * @throws OrtException
     */
    public float[] predict(BufferedImage image) throws OrtException {
        if (image == null) {
            System.err.println("Input image is null");
            return null;
        }

        // --- Предобработка: grayscale + resize to 64x64 ---
        BufferedImage gray = new BufferedImage(64, 64, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, 64, 64, null);
        g.dispose();

        // --- Создание тензора [1,1,64,64] ---
        FloatBuffer buffer = FloatBuffer.allocate(1 * 1 * 64 * 64);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int pixel = gray.getRGB(x, y);
                float val = ((pixel & 0xFF) / 255.0f - 0.5f) / 0.5f;  // Normalize [-1,1]
                buffer.put(val);
            }
        }
        buffer.rewind();

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, new long[]{1, 1, 64, 64});
        HashMap<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input", inputTensor);

        // --- Инференс ---
        try (OrtSession.Result result = session.run(inputs)) {
            float[][] output = (float[][]) result.get(0).getValue();
            float[] bboxNorm = output[0];  // [x, y, w, h] в [0,1]

            // --- Денормализация в пиксели ---
            int origW = image.getWidth();
            int origH = image.getHeight();
            float[] currBbox = new float[]{
                    bboxNorm[0] * origW,
                    bboxNorm[1] * origH,
                    bboxNorm[2] * origW,
                    bboxNorm[3] * origH
            };

            // --- ПЛАВНОЕ СГЛАЖИВАНИЕ (EMA) ---
            if (prevBbox != null) {
                for (int i = 0; i < 4; i++) {
                    currBbox[i] = alpha * currBbox[i] + (1 - alpha) * prevBbox[i];
                }
            }
            prevBbox = currBbox.clone();

            // --- Логирование (можно убрать) ---
            //System.out.println("Smoothed bbox: " + java.util.Arrays.toString(currBbox));

            return currBbox;
        } finally {
            inputTensor.close();
        }
    }

    public void close() throws OrtException {
        session.close();
    }
}