package open.gl;

import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.fusion.core.GlfwInput;
import com.fusion.core.GlfwKey;
import com.fusion.core.Time;
import com.fusion.core.engine.Debug;
import com.fusion.core.engine.Global;
import com.fusion.core.engine.renderer.Renderer;
import com.fusion.core.engine.window.Window;
import open.gl.debug.DebugRenderer;
import open.gl.gameobject.GameObject;
import open.gl.gameobject.Mesh;
import open.gl.gameobject.MeshInstance;
import open.gl.physics.HitResults;
import open.gl.physics.PhysicsWorld;
import open.gl.shaders.DepthShader;
import open.gl.shaders.OpenGlShader;
import open.gl.shaders.QuadShader;
import open.gl.shaders.WorldShader;
import open.gl.shaders.lights.DirLight;
import open.gl.shaders.lights.PointLight;
import open.gl.texture.Texture;
import open.gl.texture.TextureLoader;
import org.joml.*;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL13.*;

public class OpenGlRenderer extends Renderer {

    private static int drawCall = 0;
    private static int lastDrawCallCount = 0;
    private static int triangleCount = 0;
    private static int lastTriangleCount = 0;
    private Window window;

    private Texture white;

    private ArrayList<GameObject> gameObjects = new ArrayList<>();

    public OpenGlRenderer(Window window) {
        this.window = window;
    }

    @Override
    public void init() {
        Debug.logInfo("OpenGl Renderer Init");
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        //Load Textures
//        white = TextureLoader.loadTexture("E:\\Github\\FusionCoreEngine\\OpenGlRenderer\\src\\main\\resources\\white.png");
        white = TextureLoader.loadTexture(Global.getAssetDir().toString() + File.separator + "OpenGL" + File.separator + "white.png");

        Debug.logInfo(window.getWidth() + " : " + window.getHeight());
    }

    public void addGameObject(GameObject gameObject){
        gameObjects.add(gameObject);
    }

    public void render(OpenGlShader shader, Map<Mesh, List<MeshInstance>> instances, WhileRendering whileRendering){
        whileRendering.ShaderBeforeBind();
        shader.bind();
        {
            whileRendering.ShaderAfterBind();
            if(instances != null){
                for (Map.Entry<Mesh, List<MeshInstance>> entry : instances.entrySet()) {
                    Mesh mesh = entry.getKey();
                    whileRendering.MeshBeforeBind(mesh);
                    mesh.enable();
                    whileRendering.MeshAfterBind(mesh);
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        MeshInstance instance = entry.getValue().get(i);
                        whileRendering.MeshInstanceUpdate(mesh, instance);
                        drawCall++;
                        mesh.render(instance, instance.material.getShader() != shader);
                    }
                    whileRendering.MeshBeforeUnbind(mesh);
                    mesh.disable();
                    whileRendering.MeshAfterUnbind(mesh);
                }
            }
        }
        whileRendering.ShaderBeforeUnbind();
        shader.unbind();
        whileRendering.ShaderAfterUnbind();
    }

    @Override
    public void update() {
        //TODO draw calls are being set to 0 then nothing happens to increase this in the middle of this update
        lastDrawCallCount = drawCall;
        lastTriangleCount = triangleCount;
        drawCall = 0;

        FpsCounter.update();
    }

    public static void addDrawCall(){
        drawCall++;
    }

    public static int getDrawCalls(){
        return lastDrawCallCount;
    }

    public static void addTriangleCount(int triangleCount) {
        OpenGlRenderer.triangleCount += triangleCount;
    }

    public static int getTriangleCount(){
        return lastTriangleCount;
    }

    @Override
    public void close() {
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.cleanup();
        }

        DebugRenderer.cleanup();

       for(Texture  texture : TextureLoader.texturePool.values()){
           texture.cleanup();
       }

        white.cleanup();
    }

    public ArrayList<GameObject> getGameObjects() {
        return gameObjects;
    }
}
