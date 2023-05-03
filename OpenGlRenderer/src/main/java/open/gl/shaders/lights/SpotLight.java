package open.gl.shaders.lights;

import org.joml.Vector3f;

public class SpotLight {

    public Vector3f position = new Vector3f();
    public Vector3f direction = new Vector3f();
    public Vector3f ambient = new Vector3f();
    public Vector3f diffuse = new Vector3f();
    public Vector3f specular = new Vector3f();
    public float constant = 1.0f;
    public float linear = 1.0f;
    public float quadratic = 0.0f;
    public float cutOff = 0.0f;
    public float outerCutOff = 0.0f;
}
