package open.gl;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera extends Transform{

    private float FOV;
    private float z_near;
    private float z_far;

    private Matrix4f projectionMatrix;

    public Camera(float FOV, float z_near, float z_far) {
        this.FOV = FOV;
        this.z_near = z_near;
        this.z_far = z_far;

        createProjectionMatrix(800, 600);
    }

    public void lookAt(Vector3f target){
        Vector3f cameraDirection = new Vector3f();
        getPosition().sub(target, cameraDirection).normalize();

        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f cameraRight = new Vector3f();
        up.cross(cameraDirection, cameraRight).normalize();

        Vector3f cameraUp = new Vector3f();
        cameraDirection.cross(cameraRight, cameraUp).normalize();

        Quaternionf rotation = new Quaternionf();
        rotation.lookAlong(cameraDirection.negate(), cameraUp);

        setRotation(rotation);
    }

    public void createProjectionMatrix(int width, int height){
        float aspectRatio = (float) width / height;
//        float y_scale = 1f / (float) Math.tan(Math.toRadians(FOV / 2f)) * aspectRatio;
//        float x_scale = y_scale / aspectRatio;
//        float frustum_length = z_far - z_near;
//
        projectionMatrix = new Matrix4f();
        projectionMatrix.identity();
        projectionMatrix.perspective(70, aspectRatio, z_near, z_far, false, projectionMatrix);
//        projectionMatrix.ortho(800, 0, 600, 0, z_near, z_far);


    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
