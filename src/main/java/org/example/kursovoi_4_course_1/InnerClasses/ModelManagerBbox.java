package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;


public class ModelManagerBbox {

    private final OrtSession session;
    private final OrtEnvironment env;

    private final float alpha = 0.7f;
    private float[] prevBbox = null;

    public ModelManagerBbox(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }


    public float[] predict(BufferedImage image) throws OrtException {
        if (image == null) {
            System.err.println("Input image is null");
            return null;
        }

        BufferedImage gray = new BufferedImage(64, 64, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, 64, 64, null);
        g.dispose();

        FloatBuffer buffer = FloatBuffer.allocate(1 * 1 * 64 * 64);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int pixel = gray.getRGB(x, y);
                float val = ((pixel & 0xFF) / 255.0f - 0.5f) / 0.5f;
                buffer.put(val);
            }
        }
        buffer.rewind();

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, new long[]{1, 1, 64, 64});
        HashMap<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input", inputTensor);

        try (OrtSession.Result result = session.run(inputs)) {
            float[][] output = (float[][]) result.get(0).getValue();
            float[] bboxNorm = output[0];  // [x, y, w, h] в [0,1]

            int origW = image.getWidth();
            int origH = image.getHeight();
            float[] currBbox = new float[]{
                    bboxNorm[0] * origW,
                    bboxNorm[1] * origH,
                    bboxNorm[2] * origW,
                    bboxNorm[3] * origH
            };

            if (prevBbox != null) {
                for (int i = 0; i < 4; i++) {
                    currBbox[i] = alpha * currBbox[i] + (1 - alpha) * prevBbox[i];
                }
            }
            prevBbox = currBbox.clone();

            return currBbox;
        } finally {
            inputTensor.close();
        }
    }

    public void close() throws OrtException {
        session.close();
    }
}