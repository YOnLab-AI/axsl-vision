package com.yonlab.vision.core;

import java.nio.ByteBuffer;

/** Converts BT.601 limited-range YUV420 planes into a caller-owned BGR buffer. */
public final class Yuv420 {
    private Yuv420() { }

    public static void toBgr(ByteBuffer y, int yRowStride, int yPixelStride,
            ByteBuffer u, int uRowStride, int uPixelStride,
            ByteBuffer v, int vRowStride, int vPixelStride,
            int width, int height, byte[] out) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Common image dimensions must be positive");
        }
        long pixels = (long) width * height;
        if (pixels > Integer.MAX_VALUE / 3 || out == null || out.length < pixels * 3) {
            throw new IllegalArgumentException("Common BGR output is too short");
        }
        int chromaWidth = (int) (((long) width + 1) / 2);
        int chromaHeight = (int) (((long) height + 1) / 2);
        validatePlane(y, yRowStride, yPixelStride, width, height);
        validatePlane(u, uRowStride, uPixelStride, chromaWidth, chromaHeight);
        validatePlane(v, vRowStride, vPixelStride, chromaWidth, chromaHeight);

        int yBase = y.position(), uBase = u.position(), vBase = v.position();
        int output = 0;
        for (int row = 0; row < height; row++) {
            int yRow = yBase + row * yRowStride;
            int uRow = uBase + (row / 2) * uRowStride;
            int vRow = vBase + (row / 2) * vRowStride;
            for (int col = 0; col < width; col++) {
                int luma = Math.max(0, (y.get(yRow + col * yPixelStride) & 255) - 16);
                int blueDifference = (u.get(uRow + (col / 2) * uPixelStride) & 255) - 128;
                int redDifference = (v.get(vRow + (col / 2) * vPixelStride) & 255) - 128;
                int scaledLuma = 298 * luma;
                out[output++] = clip((scaledLuma + 516 * blueDifference + 128) >> 8);
                out[output++] = clip((scaledLuma - 100 * blueDifference - 208 * redDifference + 128) >> 8);
                out[output++] = clip((scaledLuma + 409 * redDifference + 128) >> 8);
            }
        }
    }

    private static void validatePlane(ByteBuffer plane, int rowStride, int pixelStride,
            int columns, int rows) {
        if (plane == null || rowStride <= 0 || pixelStride <= 0) {
            throw new IllegalArgumentException("Common plane and strides must be valid");
        }
        long rowBytes = (long) (columns - 1) * pixelStride + 1;
        long end = (long) plane.position() + (long) (rows - 1) * rowStride + rowBytes;
        if (rowStride < rowBytes || end > plane.limit()) {
            throw new IllegalArgumentException("Common YUV plane is too short for its strides");
        }
    }

    private static byte clip(int value) {
        return (byte) Math.max(0, Math.min(255, value));
    }
}
