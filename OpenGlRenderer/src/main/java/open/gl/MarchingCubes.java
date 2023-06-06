package open.gl;

import org.joml.Vector3f;
import java.util.*;

public class MarchingCubes {

    private long startTime;
    public long timeTaken = 0;
    private int[][][] scalarField;
    float threshold = 0.5f;
    public List<Vector3f> vertices = new ArrayList<>();
    public List<Vector3f> normals = new ArrayList<>();
    public List<Integer> indices = new ArrayList<>();

    public Map<Vector3f, Integer> vertexMap = new HashMap<>();

    int[][] cornerTable = {
            {0, 0, 0},
            {1, 0, 0},
            {1, 0, 1},
            {0, 0, 1},
            {0, 1, 0},
            {1, 1, 0},
            {1, 1, 1},
            {0, 1, 1}
    };

    public MarchingCubes(int[][][] scalarField) {
        startTime = System.currentTimeMillis();
        this.scalarField = scalarField;

        for (int x = 0; x < scalarField.length - 1; x++) {
            for (int y = 0; y < scalarField[0].length - 1; y++) {
                for (int z = 0; z < scalarField[0][0].length - 1; z++) {
                    int[] corners = new int[8];
                    corners[0] = scalarField[x + 0][y + 0][z + 0];
                    corners[1] = scalarField[x + 1][y + 0][z + 0];
                    corners[2] = scalarField[x + 1][y + 0][z + 1];
                    corners[3] = scalarField[x + 0][y + 0][z + 1];
                    corners[4] = scalarField[x + 0][y + 1][z + 0];
                    corners[5] = scalarField[x + 1][y + 1][z + 0];
                    corners[6] = scalarField[x + 1][y + 1][z + 1];
                    corners[7] = scalarField[x + 0][y + 1][z + 1];

                    int binaryState = 0;
                    for (int i = 0; i < 8; i++) {
                        if (corners[i] < threshold) binaryState |= 1 << i;
                    }

                    /* Create the triangle */
                    int[] edgeIndices = MarchingCubesTables.triTable[binaryState];
                    for (int i = 0; i < 16; i += 3) {
                        if (edgeIndices[i] == -1) {
                            break;
                        }

                        int[] triangle = new int[3];
                        for (int j = 0; j < 3; j++) {
                            int edgeIndex = edgeIndices[i + j];
                            int[] edge = MarchingCubesTables.edgeTable[edgeIndex];

                            int[] vertex0 = cornerTable[edge[0]];
                            int[] vertex1 = cornerTable[edge[1]];

                            // Interpolation step (simplified)
                            float t = (float) (threshold - corners[edge[0]]) / (corners[edge[1]] - corners[edge[0]]);
                            Vector3f point1 = new Vector3f(x + vertex0[0], y + vertex0[1], z + vertex0[2]);
                            Vector3f point2 = new Vector3f(x + vertex1[0], y + vertex1[1], z + vertex1[2]);
                            Vector3f vertex = new Vector3f();
                            vertex = point1.lerp(point2, t, vertex);

                            Vector3f key = vertex;
                            if (!vertexMap.containsKey(key)) {
                                vertices.add(vertex);
                                vertexMap.put(key, vertices.size() - 1);
                            }
                            triangle[j] = vertexMap.get(key);
                        }
                        int baseIndex = vertices.size() - 3;
                        indices.add(triangle[0]);
                        indices.add(triangle[1]);
                        indices.add(triangle[2]);

                        Vector3f v1 = new Vector3f(vertices.get(triangle[1]));
                        Vector3f v0 = new Vector3f(vertices.get(triangle[0]));
                        Vector3f edge1 = v1.sub(v0);  // create edge from v1 to v0

                        Vector3f v2 = new Vector3f(vertices.get(triangle[2]));
                        Vector3f edge2 = v2.sub(v0);  // create edge from v2 to v0

                        Vector3f normal = edge1.cross(edge2);
                        normals.add(normal);

//                        indices.add(baseIndex + 0);
//                        indices.add(baseIndex + 1);
//                        indices.add(baseIndex + 2);
                    }
                }
            }
        }
        timeTaken = System.currentTimeMillis() - startTime;
    }

//    private Vector3f interpolate(int x1, int y1, int z1, int value1, int x2, int y2, int z2, int value2) {
//        // If the value equals the threshold at any of the points, return that point
//        if (Math.abs(threshold - value1) < 0.00001f) {
//            return new Vector3f(x1, y1, z1);
//        }
//        if (Math.abs(threshold - value2) < 0.00001f) {
//            return new Vector3f(x2, y2, z2);
//        }
//        if (Math.abs(value1 - value2) < 0.00001f) {
//            return new Vector3f(x1, y1, z1);
//        }
//
//        // Else, linearly interpolate the position of the point
//        float mu = (threshold - value1) / (value2 - value1);
//        float x = x1 + mu * (x2 - x1);
//        float y = y1 + mu * (y2 - y1);
//        float z = z1 + mu * (z2 - z1);
//
//        return new Vector3f(x, y, z);
//    }
}
