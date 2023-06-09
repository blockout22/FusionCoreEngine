package open.gl;

import com.fusion.core.GlfwInput;
import com.fusion.core.GlfwKey;
import com.fusion.core.Time;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;

public class CameraController {

    private PerspectiveCamera camera;

    private boolean mouseGrabbed = false;

    private float sensitivity = 0.07f;

    private Vector2f previousPos = new Vector2f(-1, -1);
    private Vector2f curPos = new Vector2f(0, 0);
    private Vector2f cursorPosResults = new Vector2f();

    private final Vector3f direction = new Vector3f();

    private DoubleBuffer xpos = BufferUtils.createDoubleBuffer(2);
    private  DoubleBuffer ypos = BufferUtils.createDoubleBuffer(1);

    public float SPEED = 1.1f;

    private float accumulatedYaw = 0.0f;
    private float accumulatedPitch = 0.0f;

    public CameraController(PerspectiveCamera camera){
        this.camera = camera;
    }

    public void update(){
        if(GlfwInput.isKeyPressed(GlfwKey.getKeyCode("KEY_F"))){
            if(mouseGrabbed){
                releaseCursor();
            }else{
                grabCursor();
            }
        }

        curPos = getCursorPos();
        if (mouseGrabbed) {
            float dx = (float)(curPos.x - previousPos.x) * sensitivity;
            float dy = (float)(curPos.y - previousPos.y) * sensitivity;

            camera.getOrientation().rotateLocalX((float)Math.toRadians(dy));  // Rotate around local X first
            camera.getOrientation().rotateY((float)Math.toRadians(dx));
            camera.getOrientation().normalize();
//            camera.yaw += dx * sensitivity;
//            camera.pitch += dy * sensitivity;
        }
        previousPos.x = curPos.x;
        previousPos.y = curPos.y;

        if(GlfwInput.isGame()) {
            if (GlfwInput.isKeyDown(GlfwKey.getKeyCode("KEY_W"))) {
                moveForward();
            }
            if (GlfwInput.isKeyDown(GlfwKey.getKeyCode("KEY_S"))) {
                moveBackward();
            }

            if (GlfwInput.isKeyDown(GlfwKey.getKeyCode("KEY_A"))) {
                moveLeft();
            }

            if (GlfwInput.isKeyDown(GlfwKey.getKeyCode("KEY_D"))) {
                moveRight();
            }

            if (GlfwInput.isKeyDown(GlfwKey.getKeyCode("KEY_SPACE"))) {
                moveUp();
            }

            if (GlfwInput.isKeyDown(GlfwKey.getKeyCode("KEY_LEFT_SHIFT"))) {
                moveDown();
            }
        }
    }

    public void grabCursor()
    {
        mouseGrabbed = true;
        GLFW.glfwSetInputMode(GlfwInput.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    public void releaseCursor()
    {
        mouseGrabbed = false;
        GLFW.glfwSetInputMode(GlfwInput.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    public Vector2f getCursorPos()
    {
        xpos.rewind();
        xpos.rewind();
        GLFW.glfwGetCursorPos(GlfwInput.getWindowID(), xpos, ypos);

        double x = xpos.get();
        double y = ypos.get();

        xpos.clear();
        ypos.clear();
        cursorPosResults.set(x, y);
        return cursorPosResults;
    }

    public void moveForward() {
        Quaternionf orientation = camera.getOrientation();
        Quaternionf conjugate = new Quaternionf(orientation).conjugate();

        Vector3f forward = new Vector3f(0, 0, -1);
        conjugate.transform(forward);

        Vector3f motion = new Vector3f(forward);
        motion.mul((float) (SPEED * Time.getDeltaTime()));
        camera.getPosition().add(motion.x, 0, motion.z);
    }


    public void moveBackward() {
        Quaternionf orientation = camera.getOrientation();
        Quaternionf conjugate = new Quaternionf(orientation).conjugate();

        Vector3f forward = new Vector3f(0, 0, 1);
        conjugate.transform(forward);

        Vector3f motion = new Vector3f(forward);
        motion.mul((float) (SPEED * Time.getDeltaTime()));
        camera.getPosition().add(motion.x, 0, motion.z);
    }

    public void moveLeft() {
        Quaternionf orientation = camera.getOrientation();
        Quaternionf conjugate = new Quaternionf(orientation).conjugate();

        Vector3f forward = new Vector3f(-1, 0, 0);
        conjugate.transform(forward);

        Vector3f motion = new Vector3f(forward);
        motion.mul((float) (SPEED * Time.getDeltaTime()));
        camera.getPosition().add(motion.x, 0, motion.z);
    }

    public void moveRight() {
        Quaternionf orientation = camera.getOrientation();
        Quaternionf conjugate = new Quaternionf(orientation).conjugate();

        Vector3f forward = new Vector3f(1, 0, 0);
        conjugate.transform(forward);

        Vector3f motion = new Vector3f(forward);
        motion.mul((float) (SPEED * Time.getDeltaTime()));
        camera.getPosition().add(motion.x, 0, motion.z);
    }

//    public void moveForward(){
//        camera.getPosition().x += Math.sin(camera.getYaw() * Math.PI / 180) * SPEED * Time.getDeltaTime();
//        camera.getPosition().z += -Math.cos(camera.getYaw() * Math.PI / 180) * SPEED * Time.getDeltaTime();
//    }
//
//    public void moveBackward(){
//        camera.getPosition().x -= Math.sin(camera.getYaw() * Math.PI / 180) * SPEED * Time.getDeltaTime();
//        camera.getPosition().z -= -Math.cos(camera.getYaw() * Math.PI / 180) * SPEED * Time.getDeltaTime();
//    }
//
//    public void moveLeft(){
//        camera.getPosition().x += Math.sin((camera.getYaw() - 90) * Math.PI / 180) * SPEED * Time.getDeltaTime();
//        camera.getPosition().z += -Math.cos((camera.getYaw() - 90) * Math.PI / 180) * SPEED * Time.getDeltaTime();
//    }
//
//    public void moveRight(){
//        camera.getPosition().x += Math.sin((camera.getYaw() + 90) * Math.PI / 180) * SPEED * Time.getDeltaTime();
//        camera.getPosition().z += -Math.cos((camera.getYaw() + 90) * Math.PI / 180) * SPEED * Time.getDeltaTime();
//    }

    public void moveUp()
    {
        camera.getPosition().y += SPEED * Time.getDeltaTime();
    }

    public void moveDown(){
        camera.getPosition().y -= SPEED * Time.getDeltaTime();
    }
}
