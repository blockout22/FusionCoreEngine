package open.gl.shaders.lights;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DirLight {

    public Vector3f direction = new Vector3f(0.0f, 0.0f, 0f);
    public Vector3f ambient = new Vector3f(0.05f, 0.05f, 0.05f);
    public Vector3f diffuse = new Vector3f(0.4f, 0.4f, 0.4f);
    public Vector3f specular = new Vector3f(0.5f, 0.5f, 0.5f);

    public float constant = 1.0f;
    public float linear = 0.09f;
    public float quadratic = 0.032f;
    private Matrix4f lightProjection = new Matrix4f();

    public void updateDirection(Vector3f newDirection) {
        this.direction = new Vector3f(newDirection).normalize();
    }

    public Matrix4f getLightViewMatrix(Vector3f center, float distanceFromCenter) {
        Matrix4f lightView = new Matrix4f();
        Vector3f target = new Vector3f(center);
        Vector3f position = new Vector3f(direction).mul(distanceFromCenter).add(center);
        Vector3f up = Math.abs(direction.y) < 0.999 ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        lightView.lookAt(position, target, up);

        return lightView;
    }


    public Matrix4f getLightProjectionMatrix(float left, float right, float bottom, float top, float near, float far) {
        lightProjection.identity();
        lightProjection.ortho(left, right, bottom, top, near, far);
        return lightProjection;
    }

}
