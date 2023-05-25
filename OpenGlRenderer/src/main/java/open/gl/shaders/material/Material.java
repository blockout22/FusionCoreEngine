package open.gl.shaders.material;

import open.gl.shaders.OpenGlShader;

public abstract class Material {

    protected OpenGlShader shader;

    public Material(OpenGlShader shader){
        this.shader = shader;
    }

    public abstract void bind();

    public OpenGlShader getShader() {
        return shader;
    }
}
