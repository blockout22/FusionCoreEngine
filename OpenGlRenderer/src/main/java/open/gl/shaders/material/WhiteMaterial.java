package open.gl.shaders.material;

import com.fusion.core.engine.Global;
import open.gl.shaders.OpenGlShader;
import open.gl.texture.TextureLoader;

import java.io.File;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.*;

public class WhiteMaterial extends Material{

    public int diffuse = TextureLoader.loadTexture(Global.getAssetDir().toString() + File.separator + "OpenGL" + File.separator + "white.png").getID();
    public int specular = TextureLoader.loadTexture(Global.getAssetDir().toString() + File.separator + "OpenGL" + File.separator + "container2_specular.png").getID();
    public float shininess = 0f;

    public WhiteMaterial(OpenGlShader shader) {
        super(shader);
    }

    @Override
    public void bind() {
        shader.loadInt(shader.getUniformLocation("material.diffuse"), 0);
        shader.loadInt(shader.getUniformLocation("material.specular"), 1);
        shader.loadFloat(shader.getUniformLocation("material.shininess"), shininess);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, diffuse);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, specular);
    }
}
