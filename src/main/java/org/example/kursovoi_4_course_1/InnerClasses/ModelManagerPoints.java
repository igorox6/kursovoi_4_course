package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModelManagerPoints {

    private final OrtSession session;
    private final OrtEnvironment env;

    private static final int TARGET_SIZE = 128;
    private static final int CHANNELS = 3;

    private final float smoothAlpha = 0.5f;
    private float[] prevPoints = null;

    public ModelManagerPoints(OrtEnvironment env, Path modelPath) throws OrtException {
        this.env = env;
        this.session = env.createSession(
                modelPath.toAbsolutePath().toString(),
                new OrtSession.SessionOptions()
        );
    }

    public ModelManagerPoints(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    public float[] runInference(BufferedImage faceCrop) throws OrtException {
        if (faceCrop == null || faceCrop.getWidth() < 5 || faceCrop.getHeight() < 5) {
            return null;
        }

        BufferedImage resized = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(faceCrop, 0, 0, TARGET_SIZE, TARGET_SIZE, null);
        g2.dispose();

        float[] inputData = new float[CHANNELS * TARGET_SIZE * TARGET_SIZE];
        int idx = 0;

        // CHW: сначала R, потом G, потом B
        for (int c = 0; c < CHANNELS; c++) {
            for (int y = 0; y < TARGET_SIZE; y++) {
                for (int x = 0; x < TARGET_SIZE; x++) {
                    int rgb = resized.getRGB(x, y);

                    int channelValue;
                    switch (c) {
                        case 0 -> channelValue = (rgb >> 16) & 0xFF;
                        case 1 -> channelValue = (rgb >> 8) & 0xFF;
                        default -> channelValue = rgb & 0xFF;
                    }

                    inputData[idx++] = channelValue / 255.0f;
                }
            }
        }

        FloatBuffer buffer = FloatBuffer.wrap(inputData);

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, new long[]{1, CHANNELS, TARGET_SIZE, TARGET_SIZE})) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input", inputTensor);

            try (OrtSession.Result result = session.run(inputs)) {
                float[] keypoints = extractPoints(result);

                if (keypoints == null || keypoints.length != 20) {
                    return null;
                }

                for (int i = 0; i < keypoints.length; i++) {
                    keypoints[i] = clamp01(keypoints[i]);
                }

                if (prevPoints != null && prevPoints.length == keypoints.length) {
                    for (int i = 0; i < keypoints.length; i++) {
                        keypoints[i] = smoothAlpha * keypoints[i] + (1 - smoothAlpha) * prevPoints[i];
                    }
                }

                prevPoints = keypoints.clone();

                return keypoints;
            }
        }
    }

    private float[] extractPoints(OrtSession.Result result) throws OrtException {
        Object value = result.get(0).getValue();

        if (value instanceof float[][] arr2d) {
            return arr2d[0];
        }

        if (value instanceof float[] arr1d) {
            return arr1d;
        }

        throw new OrtException("Unsupported points output type: " + value.getClass());
    }

    private float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public void resetSmoothing() {
        prevPoints = null;
    }

    public void close() throws OrtException {
        session.close();
    }
}