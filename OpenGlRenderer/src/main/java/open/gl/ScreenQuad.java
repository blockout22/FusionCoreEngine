package open.gl;

import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

public class ScreenQuad {

    private final int vao;
    private final int vbo;
    private Vector2f position;
    private Vector2f size;

    public ScreenQuad() {
        this(new Vector2f(-1, -1), new Vector2f(1, 1));
    }

    public ScreenQuad(Vector2f position, Vector2f size) {
        this.position = position;
        this.size = size;

        float[] vertices = createVertexData();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        glBindVertexArray(0);
    }

    private float[] createVertexData() {
        float x = position.x - ((size.x * 2.0f) / 2.0f);
        float y = position.y - ((size.y * 2.0f) / 2.0f);
        float w = size.x * 2.0f;
        float h = size.y * 2.0f;

        return new float[]{
                // Positions     // Texture Coords
                x,     y + h,    0.0f, 1.0f,
                x,     y,        0.0f, 0.0f,
                x + w, y,        1.0f, 0.0f,

                x + w, y,        1.0f, 0.0f,
                x + w, y + h,    1.0f, 1.0f,
                x,     y + h,    0.0f, 1.0f
        };
    }

    public void render() {
        glBindVertexArray(vao);
        OpenGlRenderer.addDrawCall();
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
