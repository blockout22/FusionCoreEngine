package open.gl;

import open.gl.shaders.OpenGlShader;
import open.gl.shaders.SkyboxShader;
import open.gl.shaders.lights.DirLight;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL30.*;

public class Skybox {

    float skyboxVertices[] = {
            // positions
            -1.0f,  1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f,  1.0f
    };

    private int vao;
    private int vbo;

    private SkyboxShader shader;

    public Skybox(){
        shader = new SkyboxShader();
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, Utilities.flip(skyboxVertices), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * 4, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glEnableVertexAttribArray(0);
    }

    public void render(PerspectiveCamera camera, DirLight light){
        glDepthFunc(GL_LEQUAL);
        shader.bind();

        OpenGlShader.loadMatrix4f(shader.getProjection(), camera.getProjectionMatrix());
        Matrix4f view = shader.createViewMatrix(camera);
        view.m30(0);
        view.m31(0);
        view.m32(0);
        OpenGlShader.loadMatrix4f(shader.getView(), view);
        OpenGlShader.loadVector3f(shader.getUniformLocation("lightDirection"), light.direction);

        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glDisableVertexAttribArray(0);

        glDepthFunc(GL_LESS);

        OpenGlShader.unbind();
    }

    public void cleanup()
    {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
