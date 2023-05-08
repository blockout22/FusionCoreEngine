package open.gl.gameobject;

import open.gl.Transform;
import open.gl.shaders.Material;

public class MeshInstance extends Transform {

    private final Mesh mesh;
    public Material material = new Material();

    public MeshInstance(Mesh mesh){
        this.mesh = mesh;
        if(mesh.getModel().getDiffuseTexture() != null) {
            material.diffuse = mesh.getModel().getDiffuseTexture().getID();
        }

        if(mesh.getModel().getSpecularTexture() != null){
            material.specular = mesh.getModel().getSpecularTexture().getID();
        }
    }

    public Mesh getMesh() {
        return mesh;
    }
}
