package open.gl;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform {

    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;

    public Transform() {
        position = new Vector3f(0.0f, 0.0f, 0.0f);
        rotation = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);
        scale = new Vector3f(1.0f, 1.0f, 1.0f);
    }

    public Matrix4f getModelMatrix() {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotate(rotation);
        modelMatrix.scale(scale);
        return modelMatrix;
    }

//    public void bind(Transform transform){
//        this.position = transform.position;
//        this.rotation = transform.rotation;
//        this.scale = transform.scale;
//    }

    // Getters and setters
    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setPosition(float x, float y, float z){
        this.position.set(x, y, z);
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation = rotation;
    }

    public void setRotation(float x, float y, float z, float w){
        rotation.set(x, y, z, w);
    }

    public void setRotationDeg(float pitch, float roll, float yaw) {
        this.rotation.identity().rotateXYZ((float) Math.toRadians(pitch),
                (float) Math.toRadians(roll),
                (float) Math.toRadians(yaw));
    }

    public Vector3f getRotationDeg() {
        Vector3f eulerAngles = new Vector3f();
        this.rotation.getEulerAnglesXYZ(eulerAngles);
        return new Vector3f((float) Math.toDegrees(eulerAngles.x),
                (float) Math.toDegrees(eulerAngles.y),
                (float) Math.toDegrees(eulerAngles.z));
    }


    public Vector3f getScale() {
        return scale;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }
}
