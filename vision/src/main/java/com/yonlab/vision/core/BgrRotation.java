package com.yonlab.vision.core;

/**
 * Rotates packed three-channel BGR pixels into a caller-owned buffer, without
 * mirroring or allocating intermediate arrays. For 90/270 degrees the output
 * width is the input height and the output height is the input width.
 */
public final class BgrRotation {
    private BgrRotation() { }

    public static void rotate(byte[] src, int width, int height,
            int clockwiseDegrees, byte[] out) {
        if (width <= 0 || height <= 0 || src == null || out == null) {
            throw new IllegalArgumentException("Common BGR dimensions and buffers must be valid");
        }
        long pixels = (long) width * height;
        if (pixels > Integer.MAX_VALUE / 3) {
            throw new IllegalArgumentException("Common BGR image is too large");
        }
        int bytes = (int) pixels * 3;
        if (src.length < bytes || out.length < bytes) {
            throw new IllegalArgumentException("Common BGR buffer is too short");
        }
        if (clockwiseDegrees != 0 && clockwiseDegrees != 90
                && clockwiseDegrees != 180 && clockwiseDegrees != 270) {
            throw new IllegalArgumentException("Common rotation must be 0, 90, 180, or 270");
        }
        if (clockwiseDegrees == 0) {
            System.arraycopy(src, 0, out, 0, bytes);
            return;
        }
        if (src == out) {
            throw new IllegalArgumentException("Common nonzero rotation requires a separate output");
        }
        int outputWidth = clockwiseDegrees == 180 ? width : height;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int outputCol, outputRow;
                if (clockwiseDegrees == 90) {
                    outputCol = height - 1 - row;
                    outputRow = col;
                } else if (clockwiseDegrees == 180) {
                    outputCol = width - 1 - col;
                    outputRow = height - 1 - row;
                } else {
                    outputCol = row;
                    outputRow = width - 1 - col;
                }
                int source = (row * width + col) * 3;
                int destination = (outputRow * outputWidth + outputCol) * 3;
                out[destination] = src[source];
                out[destination + 1] = src[source + 1];
                out[destination + 2] = src[source + 2];
            }
        }
    }
}
