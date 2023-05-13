package open.gl.shaders;

import open.gl.EulerCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SkyboxShader extends OpenGlShader{

    private int projection;
    private int view;
    private Matrix4f viewMatrix = new Matrix4f();
    private Vector3f negCamPos = new Vector3f();


    public SkyboxShader() {
        super("shaders/SkyboxVertex.glsl", "shaders/SkyboxFragment.glsl");

        bindAttribLocation(0, "aPos");
        linkAndValidate();

        projection = getUniformLocation("projection");
        view = getUniformLocation("view");
    }

    public Matrix4f createViewMatrix(EulerCamera camera){
        viewMatrix.identity();

        viewMatrix.rotateX((float)Math.toRadians(camera.getPitch()), viewMatrix);
        viewMatrix.rotateY((float)Math.toRadians(camera.getYaw()), viewMatrix);
        viewMatrix.rotateZ((float)Math.toRadians(camera.getRoll()), viewMatrix);
        Vector3f camPos = camera.getPosition();
        negCamPos.set(-camPos.x, -camPos.y, - camPos.z);
        viewMatrix.translate(negCamPos, viewMatrix);
        return viewMatrix;
    }

    public int getProjection() {
        return projection;
    }

    public int getView() {
        return view;
    }
}
