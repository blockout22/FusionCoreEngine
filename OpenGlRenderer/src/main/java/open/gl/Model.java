package open.gl;

import open.gl.texture.Texture;

public class Model {

    private float[] vertices;
    private float[] texCoords;
    private float[] normals;
    private int[] indices;

    private Texture diffuseTexture;

    public Model(float[] vertices, float[] texCoords, float[] normals, int[] indices) {
        this.vertices = vertices;
        this.texCoords = texCoords;
        this.normals = normals;
        this.indices = indices;
    }

    public void setDiffuseTexture(Texture texture){
        this.diffuseTexture = texture;
    }

    public Texture getDiffuseTexture(){
        return diffuseTexture;
    }

    public float[] getVertices() {
        return vertices;
    }

    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }

    public float[] getTexCoords() {
        return texCoords;
    }

    public void setTexCoords(float[] texCoords) {
        this.texCoords = texCoords;
    }

    public float[] getNormals() {
        return normals;
    }

    public void setNormals(float[] normals) {
        this.normals = normals;
    }

    public int[] getIndices() {
        return indices;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }
}
