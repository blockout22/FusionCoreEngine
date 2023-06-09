package open.gl;

import org.joml.Vector3f;
import java.util.*;

public class MarchingCubes {

    private long startTime;
    public long timeTaken = 0;
    private float[][][] scalarField;
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

    public MarchingCubes(float[][][] scalarField){
        this(scalarField, 0.5f);
    }

    public MarchingCubes(float[][][] scalarField, float threshold) {
        startTime = System.currentTimeMillis();
        this.scalarField = scalarField;

        for (int x = 0; x < scalarField.length - 1; x++) {
            for (int y = 0; y < scalarField[0].length - 1; y++) {
                for (int z = 0; z < scalarField[0][0].length - 1; z++) {
                    float[] corners = new float[8];
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
                            float t = (threshold - corners[edge[0]]) / (corners[edge[1]] - corners[edge[0]]);
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
//
                        Vector3f normal = edge1.cross(edge2);
                        normals.add(normal);
                    }
                }
            }
        }

//        for (int i = 0; i < vertices.size(); i++) {
//            normals.add(new Vector3f(0));
//        }
//
//        for (int i = 0; i < indices.size(); i+= 3) {
//            Vector3f v1 = vertices.get(indices.get(i));
//            Vector3f v2 = vertices.get(indices.get(i + 1));
//            Vector3f v3 = vertices.get(indices.get(i + 2));
//
//            Vector3f edge1 = v2.sub(v1, new Vector3f());
//            Vector3f edge2 = v3.sub(v1, new Vector3f());
//            Vector3f normal = edge1.cross(edge2, new Vector3f());
//
//            normals.get(indices.get(i)).set(normal);
//            normals.get(indices.get(i + 1)).set(normal);
//            normals.get(indices.get(i + 2)).set(normal);
//        }
        timeTaken = System.currentTimeMillis() - startTime;
    }

    public static float[][][] generateClouds(int size, float blobHeight, float blobWidth, float blobDepth, int numBlobs, float cloudThreshold) {
        float[][][] scalarField = new float[size][size][size];

        Random rand = new Random();

        // Create a number of "blobs" within the scalar field
        for (int i = 0; i < numBlobs; i++) {
            // Randomly position the center of the blob
            int blobX = rand.nextInt(size);
            int blobY = rand.nextInt(size);
            int blobZ = rand.nextInt(size);

            // Fill in the scalar field with the density function
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    for (int z = 0; z < size; z++) {
                        // Calculate the density at this point (an ellipsoid)
                        float dx = (x - blobX) / blobWidth;
                        float dy = (y - blobY) / blobHeight;
                        float dz = (z - blobZ) / blobDepth;
                        float distanceSquared = dx*dx + dy*dy + dz*dz;
                        float density = 1 / (distanceSquared + 0.001f);  // Add a small constant to avoid division by zero
                        scalarField[x][y][z] += density;
                    }
                }
            }
        }

        // Apply the cloud threshold
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if (scalarField[x][y][z] > cloudThreshold) {
                        scalarField[x][y][z] = 0;
                    } else {
                        scalarField[x][y][z] = 1;  // Not a cloud
                    }
                }
            }
        }

        return scalarField;
    }
}
