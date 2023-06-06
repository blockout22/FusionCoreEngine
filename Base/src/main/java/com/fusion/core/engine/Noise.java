package com.fusion.core.engine;

public class Noise {
    private static final int PERSISTENCE = 1;
    private static final int OCTAVES = 4;
    private static final float AMPLITUDE = 1;
    private static final int FREQUENCY = 1;

    public static float[][][] generatePerlinNoise3D(int width, int height, int depth, float amplitude, int frequency, int octaves, float persistence)
    {
        float[][][] noiseMap = new float[width][height][depth];
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    noiseMap[x][y][z] = getPerlinNoise(x, y, z, amplitude, frequency, octaves, persistence);
                }
            }
        }
        return noiseMap;
    }

    public static float getPerlinNoise(float x, float y, float z, float amplitude, int frequency, int octaves, float persistence) {
        float total = 0;

        for (int i = 0; i < OCTAVES; i++) {
            total += interpolateNoise3D(x * frequency, y * frequency, z * frequency) * amplitude;

            amplitude *= PERSISTENCE;
            frequency *= 2;
        }

        return total;
    }

    private static float interpolateNoise3D(float x, float y, float z) {
        int integerX = (int) x;
        float fractionalX = x - integerX;

        int integerY = (int) y;
        float fractionalY = y - integerY;

        int integerZ = (int) z;
        float fractionalZ = z - integerZ;

        // interpolate between 8 corners of the cube
        float v1 = smoothNoise(integerX, integerY, integerZ);
        float v2 = smoothNoise(integerX + 1, integerY, integerZ);
        float v3 = smoothNoise(integerX, integerY + 1, integerZ);
        float v4 = smoothNoise(integerX + 1, integerY + 1, integerZ);
        float v5 = smoothNoise(integerX, integerY, integerZ + 1);
        float v6 = smoothNoise(integerX + 1, integerY, integerZ + 1);
        float v7 = smoothNoise(integerX, integerY + 1, integerZ + 1);
        float v8 = smoothNoise(integerX + 1, integerY + 1, integerZ + 1);

        float i1 = interpolate(v1, v2, fractionalX);
        float i2 = interpolate(v3, v4, fractionalX);
        float i3 = interpolate(v5, v6, fractionalX);
        float i4 = interpolate(v7, v8, fractionalX);

        float i5 = interpolate(i1, i2, fractionalY);
        float i6 = interpolate(i3, i4, fractionalY);

        return interpolate(i5, i6, fractionalZ);
    }

    private static float smoothNoise(int x, int y, int z) {
        // Extend smoothNoise to 3D. Here I just average all 26 surrounding points and the point itself
        float sum = 0;
        float count = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    sum += getNoise(x + dx, y + dy, z + dz);
                    count++;
                }
        return sum / count;
    }

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

    private static float getNoise(int x, int y, int z) {
        int n = x + y * 57 + z * 131;
        n = (n << 13) ^ n;
        return (1 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }

    private static float getNoise(int x, int y) {
        int n = x + y * 57;
        n = (n << 13) ^ n;
        return (1 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }
}
