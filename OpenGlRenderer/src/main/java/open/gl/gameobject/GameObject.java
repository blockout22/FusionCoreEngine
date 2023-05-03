package open.gl.gameobject;

import open.gl.shaders.OpenGlShader;

import java.util.*;

public class GameObject {

//    private Mesh mesh;
//    private List<MeshInstance> meshInstances = new ArrayList<>();
    private Map<Mesh, List<MeshInstance>> sortedMeshInstances = new HashMap<>();
    private List<Component> components = new ArrayList<>();

//    public GameObject(Mesh mesh) {
//        this.mesh = mesh;
//    }

    public void addInstance(MeshInstance instance){
//        meshInstances.add(instance);

        Mesh mesh = instance.getMesh();
        sortedMeshInstances.putIfAbsent(mesh, new ArrayList<>());
        sortedMeshInstances.get(mesh).add(instance);
    }

    public void removeInstance(MeshInstance instance){
        Mesh mesh = instance.getMesh();
        List<MeshInstance> instances = sortedMeshInstances.get(mesh);
        if(instances != null){
            instances.remove(instance);

            if(instances.isEmpty()){
                sortedMeshInstances.remove(mesh);
            }
        }
    }

    public void addComponent(Component component){
        components.add(component);
        component.setGameObject(this);
    }

    public void removeComponent(Component component){
        components.remove(component);
    }

    public void render(OpenGlShader shader, String modelMatrix){
//        mesh.enable();
//        {
//            for (int i = 0; i < meshInstances.size(); i++) {
//                shader.loadMatrix4f(shader.getUniformLocation(modelMatrix), mesh.createTransformationMatrix(meshInstances.get(i)));
//                mesh.render(meshInstances.get(i));
//            }
//        }
//        mesh.disable();

        for(Map.Entry<Mesh, List<MeshInstance>> entry : sortedMeshInstances.entrySet()){
            Mesh mesh = entry.getKey();
            List<MeshInstance> instances = entry.getValue();

            mesh.enable();

            for(MeshInstance instance : instances){
                shader.loadMatrix4f(shader.getUniformLocation(modelMatrix), mesh.createTransformationMatrix(instance));
                mesh.render(instance);
            }
            mesh.disable();
        }

        for (int i = 0; i < components.size(); i++) {
            components.get(i).update();
        }
    }

    public List<MeshInstance> getMeshInstances() {
        List<MeshInstance> allInstances = new ArrayList<>();
        for (List<MeshInstance> instances : sortedMeshInstances.values()) {
            allInstances.addAll(instances);
        }
        return allInstances;
    }

    public void cleanup(){
        Set<Mesh> meshes = new HashSet<>(sortedMeshInstances.keySet());
        for (Mesh mesh : meshes) {
            mesh.cleanup();
        }
    }
}
