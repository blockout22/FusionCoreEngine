package open.gl.shaders;

import open.gl.PerspectiveCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DepthShader extends OpenGlShader{

    private int model;
    private int view;
    private int projection;
    private int lightSpaceMatrix;

    private Matrix4f viewMatrix = new Matrix4f();
    private Vector3f negCamPos = new Vector3f();

    public DepthShader() {
        super("shaders/DepthVertex.glsl", "shaders/DepthFragment.glsl");

        bindAttribLocation(0, "aPos");
        linkAndValidate();

        model = getUniformLocation("model");
        view = getUniformLocation("view");
        projection = getUniformLocation("projection");
        lightSpaceMatrix = getUniformLocation("lightSpaceMatrix");

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

//    public Matrix4f createEulerCameraViewMatrix(PerspectiveCamera camera){
//        viewMatrix.identity();
//
//        viewMatrix.rotateX((float)Math.toRadians(camera.getPitch()), viewMatrix);
//        viewMatrix.rotateY((float)Math.toRadians(camera.getYaw()), viewMatrix);
//        viewMatrix.rotateZ((float)Math.toRadians(camera.getRoll()), viewMatrix);
//        Vector3f camPos = camera.getPosition();
//        negCamPos.set(-camPos.x, -camPos.y, - camPos.z);
//        viewMatrix.translate(negCamPos, viewMatrix);
//        return viewMatrix;
//    }

    public void loadLightViewMatrix(Matrix4f matrix) {
        loadMatrix4f(view, matrix);
    }

    public Matrix4f createDirectionalLightViewMatrix(Vector3f lightDirection, Vector3f up) {
        viewMatrix.identity();

        // Calculate the orthogonal axes for the light's coordinate system
        Vector3f zAxis = new Vector3f(lightDirection).normalize();
        Vector3f xAxis = new Vector3f();
        up.cross(zAxis, xAxis).normalize();
        Vector3f yAxis = new Vector3f();
        zAxis.cross(xAxis, yAxis);

        // Set the rotation component of the view matrix
        viewMatrix.m00(xAxis.x);
        viewMatrix.m10(xAxis.y);
        viewMatrix.m20(xAxis.z);

        viewMatrix.m01(yAxis.x);
        viewMatrix.m11(yAxis.y);
        viewMatrix.m21(yAxis.z);

        viewMatrix.m02(zAxis.x);
        viewMatrix.m12(zAxis.y);
        viewMatrix.m22(zAxis.z);

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

    public int getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }
}
