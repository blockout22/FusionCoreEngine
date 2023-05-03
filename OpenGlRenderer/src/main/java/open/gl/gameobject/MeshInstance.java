package open.gl.gameobject;

import open.gl.Transform;
import open.gl.shaders.Material;

public class MeshInstance extends Transform {

    private final Mesh mesh;
    public Material material = new Material();

    public MeshInstance(Mesh mesh){
        this.mesh = mesh;
    }

    public Mesh getMesh() {
        return mesh;
    }
}
