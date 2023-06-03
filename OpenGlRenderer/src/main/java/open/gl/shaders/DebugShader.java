package open.gl.shaders;

import open.gl.PerspectiveCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DebugShader extends OpenGlShader{

    private int model;
    private int view;
    private int projection;

    private Matrix4f viewMatrix = new Matrix4f();
    private Vector3f negCamPos = new Vector3f();

    public DebugShader() {
        super("shaders/worldVertexShader.glsl", "shaders/DebugFragment.glsl");

        bindAttribLocation(0, "aPos");
        bindAttribLocation(1, "aTexCoord");
        bindAttribLocation(2, "aNormal");
        linkAndValidate();

        model = getUniformLocation("model");
        view = getUniformLocation("view");
        projection = getUniformLocation("projection");
    }

    public void setColor(Vector3f color){
        loadVector3f(getUniformLocation("objectColor"), color);
    }

    public void loadViewMatrix(PerspectiveCamera camera) {
        Matrix4f matrix = createQuaternionCameraViewMatrix(camera);
        loadMatrix4f(getView(), matrix);
    }

    public Matrix4f createQuaternionCameraViewMatrix(PerspectiveCamera camera){
        viewMatrix.identity();

        // Use the camera's orientation quaternion to create the rotation part of the view matrix
        viewMatrix.rotate(camera.getOrientation());

        Vector3f camPos = camera.getPosition();
        negCamPos.set(-camPos.x, -camPos.y, - camPos.z);
        viewMatrix.translate(negCamPos, viewMatrix);
        return viewMatrix;
    }

    public int getModel() {
        return model;
    }

    public int getView() {
        return view;
    }

    public int getProjection() {
        return projection;
    }
}
