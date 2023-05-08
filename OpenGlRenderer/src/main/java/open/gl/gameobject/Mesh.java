package open.gl.gameobject;

import open.gl.Model;
import open.gl.Transform;
import open.gl.Utilities;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;

import static org.lwjgl.opengl.GL30.*;

public class Mesh {

    private Model model;
    private int vao;
    private int vbo;
    private int vboTexture;
    private int fbo;
    private int vbon;
    private int vboi;

    private int indicesSize;
    private boolean isModel = false;
    private boolean wireframeMode = false;
//    private Material material;

    private Matrix4f transformationMatrix = new Matrix4f();

    public Mesh(Model model){
        this.model = model;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        vboTexture = glGenBuffers();
        fbo = glGenBuffers();
        vbon = glGenBuffers();
        vboi = glGenBuffers();

        add(model.getVertices(), model.getTexCoords(), model.getNormals(), model.getIndices());
    }

    public Model getModel() {
        return model;
    }

    public Matrix4f createTransformationMatrix(Transform transform){
        transformationMatrix.identity();
        Vector3f position = transform.getPosition();
        transformationMatrix.translate(position);
        Quaternionf rotation = transform.getRotation();
        transformationMatrix.rotate(rotation);
        org.joml.Vector3f scale = transform.getScale();
        transformationMatrix.scale(scale);

        return transformationMatrix;
    }

    private void add(float[] vertices, float[] texCoords, float[] normals, int[] indices) {
//        this.texCoords = texCoords;
        if (isModel) {
            System.out.println("Something tried overriding model coords");
            return;
        }
        indicesSize = indices.length;
        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL_ARRAY_BUFFER, Utilities.flip(vertices), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glBindBuffer(GL_ARRAY_BUFFER, vboTexture);
        glBufferData(GL_ARRAY_BUFFER, Utilities.flip(texCoords), GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glBindBuffer(GL_ARRAY_BUFFER, vbon);
        glBufferData(GL_ARRAY_BUFFER, Utilities.flip(normals), GL_STATIC_DRAW);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);


        glBindVertexArray(0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboi);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Utilities.flip(indices), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void enable() {
        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboi);
    }

    public void render(MeshInstance instance){
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, instance.material.diffuse);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, instance.material.specular);
        if(wireframeMode) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glDrawElements(GL_LINE_LOOP, indicesSize, GL_UNSIGNED_INT, 0);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else {
            glDrawElements(GL_TRIANGLES, indicesSize, GL_UNSIGNED_INT, 0);
        }
    }

    public void disable() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
//        glDisableVertexAttribArray(3);
        glBindVertexArray(0);
    }

    public void cleanup()
    {
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glDeleteBuffers(vbo);
        glDeleteBuffers(vboTexture);
        glDeleteBuffers(fbo);
        glDeleteBuffers(vboi);
        glDeleteVertexArrays(vao);
    }


}
