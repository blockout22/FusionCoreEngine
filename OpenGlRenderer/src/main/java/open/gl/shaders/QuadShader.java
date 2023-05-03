package open.gl.shaders;

import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

public class QuadShader extends OpenGlShader{

    public int textTexture;
    private int framebufferTextureLocation;

    public QuadShader() {
        super("shaders/QuadDisplayVertex.glsl", "shaders/QuadDisplayFragment.glsl");

        bindAttribLocation(0, "aPos");
        bindAttribLocation(1, "aTexCoords");
        linkAndValidate();

        framebufferTextureLocation = getUniformLocation("screenTexture");
//        textTexture = createTestTexture();

//        loadFrameBufferTexture(textTexture);
    }

    private int createTestTexture() {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);

        int[] data = new int[]{
                0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFF000000,
                0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF,
                0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFF000000,
                0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF
        };

        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data).flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 4, 4, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, 0);

        return texture;
    }


    public void loadFrameBufferTexture(int textureUnit, int textureId) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
        loadInt(framebufferTextureLocation, textureUnit);
    }
}
