package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;

/**
 * Дочерний класс для BBOX-модели.
 * Инициализируется байтами модели, использует общий OrtEnvironment.
 */
public class ModelManagerBbox {

    private final OrtSession session;
    private final OrtEnvironment env;  // Добавляем поле для env

    public ModelManagerBbox(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    public float[] predict(BufferedImage image) throws OrtException {
        // Preprocess: grayscale, resize to 64x64
        BufferedImage gray = new BufferedImage(64, 64, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, 64, 64, null);
        g.dispose();

        // To tensor [1,1,64,64]
        FloatBuffer buffer = FloatBuffer.allocate(1 * 1 * 64 * 64);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int pixel = gray.getRGB(x, y);
                float val = ((pixel & 0xFF) / 255.0f - 0.5f) / 0.5f;  // Normalize [-1,1]
                buffer.put(val);
            }
        }
        buffer.rewind();

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, new long[]{1, 1, 64, 64});  // Используем env

        HashMap<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input", inputTensor);

        OrtSession.Result result = session.run(inputs);
        float[][] output = (float[][]) result.get(0).getValue();
        return output[0];  // [x,y,w,h] normalized
    }

    public void close() throws OrtException {
        session.close();
    }
}