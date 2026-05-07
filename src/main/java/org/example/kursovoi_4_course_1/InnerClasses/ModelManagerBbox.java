package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModelManagerBbox {

    private final OrtSession session;
    private final OrtEnvironment env;

    private static final int INPUT_SIZE = 64;
    private static final float BBOX_PADDING = 0.25f;

    private final float alpha = 0.6f;
    private float[] prevBbox = null;

    public ModelManagerBbox(OrtEnvironment env, Path modelPath) throws OrtException {
        this.env = env;
        this.session = env.createSession(
                modelPath.toAbsolutePath().toString(),
                new OrtSession.SessionOptions()
        );
    }

    public ModelManagerBbox(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }

    public float[] predict(BufferedImage image) throws OrtException {
        if (image == null) return null;

        BufferedImage gray = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g = gray.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, INPUT_SIZE, INPUT_SIZE, null);
        g.dispose();

        FloatBuffer buffer = FloatBuffer.allocate(INPUT_SIZE * INPUT_SIZE);

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = gray.getRGB(x, y) & 0xFF;
                buffer.put(pixel / 255.0f);
            }
        }

        buffer.rewind();

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, new long[]{1, 1, INPUT_SIZE, INPUT_SIZE})) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input", inputTensor);

            try (OrtSession.Result result = session.run(inputs)) {
                float[] bboxNorm = extractBbox(result);

                if (bboxNorm == null || bboxNorm.length != 4) {
                    return null;
                }

                int origW = image.getWidth();
                int origH = image.getHeight();

                float x = clamp01(bboxNorm[0]);
                float y = clamp01(bboxNorm[1]);
                float w = clamp01(bboxNorm[2]);
                float h = clamp01(bboxNorm[3]);

                if (w <= 0.01f || h <= 0.01f) {
                    return null;
                }

                float padW = w * BBOX_PADDING;
                float padH = h * BBOX_PADDING;

                float x1 = Math.max(0.0f, x - padW) * origW;
                float y1 = Math.max(0.0f, y - padH) * origH;
                float x2 = Math.min(1.0f, x + w + padW) * origW;
                float y2 = Math.min(1.0f, y + h + padH) * origH;

                float bw = x2 - x1;
                float bh = y2 - y1;

                if (bw <= 20 || bh <= 20) {
                    return null;
                }

                float[] currBbox = new float[]{x1, y1, bw, bh};

                if (prevBbox != null && prevBbox.length == currBbox.length) {
                    for (int i = 0; i < currBbox.length; i++) {
                        currBbox[i] = alpha * currBbox[i] + (1 - alpha) * prevBbox[i];
                    }
                }

                prevBbox = currBbox.clone();

                return currBbox;
            }
        }
    }

    private float[] extractBbox(OrtSession.Result result) throws OrtException {
        Object value = result.get(0).getValue();

        if (value instanceof float[][] arr2d) {
            return arr2d[0];
        }

        if (value instanceof float[] arr1d) {
            return arr1d;
        }

        throw new OrtException("Unsupported bbox output type: " + value.getClass());
    }

    private float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public void resetSmoothing() {
        prevBbox = null;
    }

    public void close() throws OrtException {
        session.close();
    }
}