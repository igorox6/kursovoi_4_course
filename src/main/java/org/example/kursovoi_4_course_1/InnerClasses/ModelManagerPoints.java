package org.example.kursovoi_4_course_1.InnerClasses;

import ai.onnxruntime.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;


public class ModelManagerPoints {

    private final OrtSession session;
    private final OrtEnvironment env;

    public ModelManagerPoints(OrtEnvironment env, byte[] modelBytes) throws OrtException {
        this.env = env;
        this.session = env.createSession(modelBytes, new OrtSession.SessionOptions());
    }


    public float[] runInference(BufferedImage faceCrop) throws OrtException {
        if (faceCrop == null) {
            System.err.println("Input faceCrop is null");
            return null;
        }

        int targetSize = 96;
        int channels = 1;
        BufferedImage resized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(faceCrop, 0, 0, targetSize, targetSize, null);
        g2.dispose();

        float[] inputData = new float[1 * channels * targetSize * targetSize];
        java.awt.image.Raster raster = resized.getRaster();
        int idx = 0;
        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                int pixel = raster.getSample(x, y, 0);
                float val = ((pixel / 255.0f) - 0.5f) / 0.5f;
                inputData[idx++] = val;
            }
        }

        FloatBuffer buffer = FloatBuffer.wrap(inputData);
        long[] shape = {1, channels, targetSize, targetSize};
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);

        try {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input", inputTensor);

            OrtSession.Result result = session.run(inputs);
            float[][] outputArray = (float[][]) result.get(0).getValue();
            float[] keypoints = outputArray[0];

            return keypoints;
        } finally {
            inputTensor.close();
        }
    }

    public void close() throws OrtException {
        session.close();
    }
}