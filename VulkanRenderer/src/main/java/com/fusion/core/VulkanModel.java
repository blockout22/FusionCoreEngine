package com.fusion.core;

public class VulkanModel {

    private float[] vertices;
    private float[] texCoords;
    private int[] indices;

    public VulkanModel(float[] vertices, float[] texCoords, int[] indices) {
        this.vertices = vertices;
        this.texCoords = texCoords;
        this.indices = indices;
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

    public int[] getIndices() {
        return indices;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }
}
