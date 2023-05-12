package open.gl;

import open.gl.gameobject.Mesh;
import open.gl.gameobject.MeshInstance;
import open.gl.shaders.OpenGlShader;

public abstract class WhileRendering {

    private OpenGlShader shader;

    public WhileRendering(OpenGlShader shader){
        this.shader = shader;
    }

    public OpenGlShader getShader() {
        return shader;
    }

    public void ShaderBeforeBind(){}
    public void ShaderAfterBind(){}
    public void MeshBeforeBind(Mesh mesh){}
    public void MeshAfterBind(Mesh mesh){}
    public void MeshInstanceUpdate(Mesh currentMesh, MeshInstance currentInstance){}
    public void MeshBeforeUnbind(Mesh mesh){}
    public void MeshAfterUnbind(Mesh mesh){}
    public void ShaderBeforeUnbind(){}
    public void ShaderAfterUnbind(){}
}

