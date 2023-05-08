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
import com.fusion.core.engine.renderer.Renderer;
import com.fusion.core.engine.window.Window;
import open.gl.debug.DebugRenderer;
import open.gl.gameobject.GameObject;
import open.gl.physics.HitResults;
import open.gl.physics.PhysicsWorld;
import open.gl.shaders.DepthShader;
import open.gl.shaders.QuadShader;
import open.gl.shaders.WorldShader;
import open.gl.shaders.lights.DirLight;
import open.gl.shaders.lights.PointLight;
import open.gl.texture.Texture;
import open.gl.texture.TextureLoader;
import org.joml.*;
import org.lwjgl.opengl.GL;

import java.lang.Math;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL13.*;

public class OpenGlRenderer extends Renderer {

    private static int drawCall = 0;
    private static int lastDrawCallCount = 0;
    private static int triangleCount = 0;
    private static int lastTriangleCount = 0;
    private Window window;

    public EulerCamera eulerCamera;
    private CameraController cameraController;

    public WorldShader shader;
    private DepthShader depthShader;

    private Texture white;

    private ArrayList<GameObject> gameObjects = new ArrayList<>();

    private FrameBuffer depthFrameBuffer, finalFrameBuffer;
    private QuadShader quadShader;
    private ScreenQuad depthQuad, finalQuad;

    //Lights
    public DirLight dirLlight = new DirLight();
    public PointLight pointLight = new PointLight();
    private Vector3f center = new Vector3f(0.0f, 0.0f, 0.0f);
    private Matrix4f lightSpaceMatrix = new Matrix4f();

    public OpenGlRenderer(Window window) {
        this.window = window;
        this.window.addResizeListener((width, height) -> {
            depthFrameBuffer.updateSize(width, height);
            finalFrameBuffer.updateSize(width, height);
            glViewport(0, 0, width, height);
            eulerCamera.createProjectionMatrix(width, height);
            System.out.println("Update");
        });
    }

    @Override
    public void init() {
        Debug.logInfo("OpenGl Renderer Init");
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        eulerCamera = new EulerCamera(window.getWidth(), window.getHeight(), 70, 0.1f, 1000f);
        eulerCamera.setPosition(new Vector3f(0, 0, 0));

        cameraController = new CameraController(eulerCamera);

        shader = new WorldShader();
        shader.bind();
        shader.loadMatrix4f(shader.getProjection(), eulerCamera.getProjectionMatrix());

        depthShader = new DepthShader();
        depthShader.bind();
        depthShader.loadMatrix4f(depthShader.getProjection(), eulerCamera.getProjectionMatrix());

        //Load Textures
        white = TextureLoader.loadTexture("E:\\Github\\FusionCoreEngine\\OpenGlRenderer\\src\\main\\resources\\white.png");

        Debug.logInfo(window.getWidth() + " : " + window.getHeight());
        //width x height = 1920 x 1080
        depthFrameBuffer = new FrameBuffer(window.getWidth(), window.getHeight(), false);
        quadShader = new QuadShader();
        depthQuad = new ScreenQuad(new Vector2f(.5f, .5f), new Vector2f(.3f, .3f));

        finalFrameBuffer = new FrameBuffer(window.getWidth(), window.getHeight(), false);
        finalQuad = new ScreenQuad(new Vector2f(0f, 0f), new Vector2f(1f, 1f));

        //Lights
        dirLlight.direction.set(1, 1, 1);
        dirLlight.ambient.set(.1f, .1f, .1f);
        dirLlight.diffuse.set(0.5f, 0.5f, 0.5f);
        dirLlight.specular.set(1, 1, 1);

        pointLight.position.set(10, 15, -5);
        pointLight.ambient.set(0.2f, 0.2f, 0.2f);
        pointLight.diffuse.set(0.5f, 0.5f, 0.5f);
        pointLight.specular.set(1f, 1f, 1f);
        pointLight.constant = 1.0f;
        pointLight.linear = 0.09f;
        pointLight.quadratic = 0.032f;

        FpsCounter.addCallback((fps) -> {
            Debug.logInfo("FPS: " + fps);
        });
    }

    public void addGameObject(GameObject gameObject){
        gameObjects.add(gameObject);
    }

    @Override
    public void update() {
        drawCall = 0;
        glEnable(GL_CULL_FACE);
        cameraController.update();

        renderDepthScene();
        renderScene();

        int textureUnit = 0;
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        quadShader.bind();
        {
            glDisable(GL_DEPTH_TEST);

            //if more texture banks are used in the mesh renderer increase this to the next bank
            quadShader.loadInt(quadShader.getUniformLocation("screenTexture"), 2);
            glActiveTexture(GL_TEXTURE2);
//            quadShader.loadFrameBufferTexture(textureUnit, finalFrameBuffer.getTextureId());
            glBindTexture(GL_TEXTURE_2D, finalFrameBuffer.getTextureId());
            finalQuad.render();
            quadShader.loadInt(quadShader.getUniformLocation("screenTexture"), 3);
            glActiveTexture(GL_TEXTURE3);
            quadShader.loadFrameBufferTexture(textureUnit, depthFrameBuffer.getTextureId());
            depthQuad.render();
        }
        quadShader.unbind();

        FpsCounter.update();

        lastDrawCallCount = drawCall;
        lastTriangleCount = triangleCount;
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

    private void renderDepthScene(){
        depthFrameBuffer.bind();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        depthShader.bind();
        {
            //calculate light Matrix
            float distanceFromCenter = 200f;
            Matrix4f lightView = dirLlight.getLightViewMatrix(center, distanceFromCenter);
            // Calculate the light projection matrix
            float near = 1.0f;
            float far = 500.0f;
            float aspectRatio = (float) window.getWidth() / (float) window.getHeight();
            float size = 50f;
            Matrix4f lightProjection = dirLlight.getLightProjectionMatrix(-size, size, -size, size, near, far);

            lightSpaceMatrix = lightProjection.mul(lightView, lightSpaceMatrix);

            depthShader.loadMatrix4f(depthShader.getLightSpaceMatrix(), lightSpaceMatrix);

            depthShader.loadViewMatrix(eulerCamera);

            for (int i = 0; i < gameObjects.size(); i++) {
                GameObject go = gameObjects.get(i);
                go.render(depthShader, "model");
            }
        }
        depthShader.unbind();
        depthFrameBuffer.unbind();
    }

    double angle = 0.0;
    private void renderScene(){
        triangleCount = 0;
        finalFrameBuffer.bind();
        glEnable(GL_DEPTH_TEST);
        glCullFace(GL_BACK);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        shader.bind();
        {
            shader.loadMatrix4f(shader.getProjection(), eulerCamera.getProjectionMatrix());
//
            shader.loadViewMatrix(eulerCamera);
            shader.loadVector3f(shader.getUniformLocation("viewPos"), eulerCamera.getPosition());
            shader.updateDepthMap(depthFrameBuffer.getTextureId());

            //Lights
            angle += 0.01;
//            pointLight.position.set((Math.cos(angle) * 10), 5.0f, (Math.sin(angle) * 10));
            dirLlight.direction.add((float) Math.cos(angle), 1f, (float) Math.sin(angle)).normalize();
            if(angle >= Math.PI){
                Random r = new Random();
                dirLlight.ambient.set(r.nextFloat(), r.nextFloat(), r.nextFloat());
                dirLlight.diffuse.set(r.nextFloat(), r.nextFloat(), r.nextFloat());
                dirLlight.specular.set(r.nextFloat(), r.nextFloat(), r.nextFloat());
                angle = -Math.PI;
            }
            shader.updateDirLight(dirLlight);


            //calculate light Matrix
//            Vector3f center = new Vector3f(0.0f, 0.0f, 0.0f);
            float distanceFromCenter = 200f;
            Matrix4f lightView = dirLlight.getLightViewMatrix(center, distanceFromCenter);
            // Calculate the light projection matrix
            float near = 1.0f;
            float far = 500.0f;
            float aspectRatio = (float) window.getWidth() / (float) window.getHeight();
            float size = 50f;
            Matrix4f lightProjection = dirLlight.getLightProjectionMatrix(-size, size, -size, size, near, far);

//            Matrix4f lightSpaceMatrix = new Matrix4f();
            lightSpaceMatrix = lightProjection.mul(lightView, lightSpaceMatrix);

            shader.loadMatrix4f(shader.getUniformLocation("lightSpaceMatrix"), lightSpaceMatrix);
            shader.loadInt(shader.getUniformLocation("shadowMap"), 4);
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, depthFrameBuffer.getTextureId());

            shader.loadVector3f(shader.getUniformLocation("light.position"), pointLight.position);
            shader.loadVector3f(shader.getUniformLocation("light.ambient"), pointLight.ambient);
            shader.loadVector3f(shader.getUniformLocation("light.diffuse"), pointLight.diffuse);
            shader.loadVector3f(shader.getUniformLocation("light.specular"), pointLight.specular);
            shader.loadFloat(shader.getUniformLocation("light.constant"), pointLight.constant);
            shader.loadFloat(shader.getUniformLocation("light.linear"), pointLight.linear);
            shader.loadFloat(shader.getUniformLocation("light.quadratic"), pointLight.quadratic);

            shader.loadFloat(shader.getUniformLocation("gamma"), 2.2f);


            for (int i = 0; i < gameObjects.size(); i++) {
                GameObject go = gameObjects.get(i);
                go.render(shader, "model");
            }

            DebugRenderer.render(shader, eulerCamera);
        }
        shader.unbind();
        finalFrameBuffer.unbind();
    }

    @Override
    public void close() {
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.cleanup();
        }

        DebugRenderer.cleanup();

        depthFrameBuffer.cleanup();
        finalFrameBuffer.cleanup();
        quadShader.cleanup();
        depthQuad.cleanup();

       for(Texture  texture : TextureLoader.texturePool.values()){
           texture.cleanup();
       }

        white.cleanup();
        shader.cleanup();
    }

    public ArrayList<GameObject> getGameObjects() {
        return gameObjects;
    }
}
