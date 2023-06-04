package open.gl;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class TerrainGenerator {

    public int sizeX;
    public int sizeZ;

    List<Vector3f> vertices = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();

    public TerrainGenerator(float[][] map){
        this.sizeX = map.length;
        this.sizeZ = map[0].length;

        //create vertices and assign indice
        for (int z = 0; z < sizeZ; z++) {
            for (int x = 0; x < sizeX; x++) {
                float height = 0;
                vertices.add(new Vector3f(x, map[x][z], z));
            }
        }

        for (int z = 0; z < sizeZ - 1; z++) {
            for (int x = 0; x < sizeX - 1; x++) {
                int topLeft = (z * sizeX) + x;
                int topRight = topLeft + 1;
                int botomLeft = ((z + 1) * sizeX) + x;
                int bottomRight = botomLeft + 1;

                indices.add(topLeft);
                indices.add(botomLeft);
                indices.add(topRight);

                indices.add(topRight);
                indices.add(botomLeft);
                indices.add(bottomRight);
            }
        }
    }

    public class Point{
        public Vector3f position;
        public int vertexID = -1;

        public Point(Vector3f position, int vertexID) {
            this.position = position;
            this.vertexID = vertexID;
        }
    }
}
