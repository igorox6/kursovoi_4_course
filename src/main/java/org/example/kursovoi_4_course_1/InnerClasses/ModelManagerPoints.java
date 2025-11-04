package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Дочерний класс для POINTS-модели.
 * Инициализируется байтами модели, использует общий OrtEnvironment.
 */
public class ModelManagerPoints {

    private final OrtSession session;
    private final OrtEnvironment env;  // Добавляем поле для env

    public ModelManagerPoints(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    /**
     * Выполняет предсказание ключевых точек на кадре (face_crop).
     */
    public float[] runInference(BufferedImage faceCrop) throws OrtException {
        int targetSize = 96;  // Изменено на 96, как в обучении
        int channels = 3;

        // Преобразуем в grayscale
        BufferedImage gray = new BufferedImage(faceCrop.getWidth(), faceCrop.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(faceCrop, 0, 0, null);
        g.dispose();

        // Ресайз до 96x96
        BufferedImage resized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(gray, 0, 0, targetSize, targetSize, null);
        g2.dispose();

        // Преобразуем в [1,3,96,96] без нормализации (только [0,1], повтор по каналам)
        float[] inputData = new float[1 * channels * targetSize * targetSize];
        java.awt.image.Raster raster = resized.getRaster();
        int idx = 0;
        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                float v = raster.getSample(x, y, 0) / 255.0f;
                for (int c = 0; c < channels; c++) {
                    inputData[idx++] = v;  // Без mean/std
                }
            }
        }

        FloatBuffer buffer = FloatBuffer.wrap(inputData);
        long[] shape = {1, channels, targetSize, targetSize};
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);  // Используем env

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input.1", inputTensor);  // Имя входа модели — "input"

        OrtSession.Result result = session.run(inputs);
        float[][] outputArray = (float[][]) result.get(0).getValue();
        float[] keypoints = outputArray[0];  // 30 значений

        inputTensor.close();
        return keypoints;
    }

    public void close() throws OrtException {
        session.close();
    }
}