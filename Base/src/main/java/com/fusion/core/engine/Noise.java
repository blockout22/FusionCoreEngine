package com.fusion.core.engine;

public class Noise {
    private static final int PERSISTENCE = 1;
    private static final int OCTAVES = 4;
    private static final float AMPLITUDE = 1;
    private static final int FREQUENCY = 1;
    public static float[][] generatePerlinNoise(int width, int height, float amplitude, int frequency, int octaves, float persistence) {
        float[][] noiseMap = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                noiseMap[x][y] = getPerlinNoise(x, y, amplitude, frequency, octaves, persistence);
            }
        }
        return noiseMap;
    }

    public static float getPerlinNoise(float x, float y, float amplitude, int frequency, int octaves, float persistence) {
        float total = 0;

        for (int i = 0; i < OCTAVES; i++) {
            total += interpolateNoise(x * frequency, y * frequency) * amplitude;

            amplitude *= PERSISTENCE;
            frequency *= 2;
        }

        return total;
    }

    private static float interpolateNoise(float x, float y) {
        int integerX = (int) x;
        float fractionalX = x - integerX;

        int integerY = (int) y;
        float fractionalY = y - integerY;

        float v1 = smoothNoise(integerX, integerY);
        float v2 = smoothNoise(integerX + 1, integerY);
        float v3 = smoothNoise(integerX, integerY + 1);
        float v4 = smoothNoise(integerX + 1, integerY + 1);

        float i1 = interpolate(v1, v2, fractionalX);
        float i2 = interpolate(v3, v4, fractionalX);

        return interpolate(i1, i2, fractionalY);
    }

    private static float smoothNoise(int x, int y) {
        float corners = (getNoise(x-1, y-1) + getNoise(x+1, y-1) + getNoise(x-1, y+1) + getNoise(x+1, y+1)) / 16;
        float sides = (getNoise(x-1, y) + getNoise(x+1, y) + getNoise(x, y-1) + getNoise(x, y+1)) / 8;
        float center = getNoise(x, y) / 4;

        return corners + sides + center;
    }

    private static float interpolate(float a, float b, float x) {
        float ft = x * 3.1415927f;
        float f = (1 - (float)Math.cos(ft)) * 0.5f;

        return a * (1 - f) + b * f;
    }

    private static float getNoise(int x, int y) {
        int n = x + y * 57;
        n = (n << 13) ^ n;
        return (1 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }


}
