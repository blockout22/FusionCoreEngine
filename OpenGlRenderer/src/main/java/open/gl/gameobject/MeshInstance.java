package open.gl.gameobject;

import open.gl.Transform;
import open.gl.shaders.material.Material;

public class MeshInstance extends Transform {

    private final Mesh mesh;
    public Material material;

    public MeshInstance(Mesh mesh, Material material){
        this.mesh = mesh;
        this.material = material;
    }

    public Mesh getMesh() {
        return mesh;
    }
}
