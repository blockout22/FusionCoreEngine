package open.gl;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class EulerCamera {

    private Vector3f position = new Vector3f(0, 0, 0);
    protected float pitch = 0;
    protected float yaw = 0;
    protected float roll = 0;
    private float pitch_min = -90;
    private float pitch_max = 90;
    private float FOV;
    private float z_near;
    private float z_far;

    private Matrix4f projectionMatrix;

    public EulerCamera(int width, int height, float fov, float z_near, float z_far){
        this.FOV = fov;
        this.z_near = z_near;
        this.z_far = z_far;

        createProjectionMatrix(width, height);
    }

    public Vector3f getForwardVector()
    {
        Vector3f forward = new Vector3f();
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        float sinPitch = (float) Math.sin(Math.toRadians(pitch));
        float cosYaw = (float) Math.cos(Math.toRadians(yaw));
        float sinYaw = (float) Math.sin(Math.toRadians(yaw));

        forward.x = cosPitch * sinYaw;
        forward.y = -sinPitch;
        forward.z = -cosPitch * cosYaw;

        return forward.normalize();
    }

    protected void createProjectionMatrix(int width, int height) {
        float aspectRatio = (float) width / (float) height;
        projectionMatrix = new Matrix4f();
        projectionMatrix.identity();
        float fovRad = (float) Math.toRadians(70);
        projectionMatrix.perspective(fovRad, aspectRatio, z_near, z_far, false, projectionMatrix);
    }

    public void moveX(float amt) {
        this.getPosition().x += amt;
    }

    public void moveY(float amt) {
        this.getPosition().y += amt;
    }

    public void moveZ(float amt) {
        this.getPosition().z += amt;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setPosition(float x, float y, float z){
        this.position.set(x, y, z);
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getFOV() {
        return FOV;
    }

    public void setFOV(float fOV) {
        FOV = fOV;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
