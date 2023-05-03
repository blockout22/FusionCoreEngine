package open.gl.shaders.lights;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class PointLight {

    public Vector3f position = new Vector3f();
    public Vector3f ambient = new Vector3f();
    public Vector3f diffuse = new Vector3f();
    public Vector3f specular = new Vector3f();
    public float constant = 1.0f;
    public float linear = 1.0f;
    public float quadratic = 0.0f;

    public Matrix4f getLightViewMatrix() {
        Matrix4f lightView = new Matrix4f();
        lightView.lookAt(position, new Vector3f(0, 1, 0).sub(position), new Vector3f(0, 1, 0));

//        Vector3f target = new Vector3f(position).add(new Vector3f(0, 0, -0.1f));
//        lightView.lookAt(position, target, new Vector3f(0, 1, 0));
        return lightView;
    }


    public Matrix4f getLightProjectionMatrix(float near, float far, float aspectRatio) {
        Matrix4f lightProjection = new Matrix4f();
        lightProjection.identity();
//        lightProjection.ortho(-10, 10, -10, 10, 0.1f, 150, false);
        lightProjection.perspective((float) Math.toRadians(90), aspectRatio, near, far);
        return lightProjection;
    }
}
