package open.gl.shaders;

import open.gl.EulerCamera;
import open.gl.Transform;
import open.gl.shaders.lights.DirLight;
import open.gl.shaders.lights.PointLight;
import open.gl.shaders.lights.SpotLight;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class WorldShader extends OpenGlShader{

    private int model;
    private int view;
    private int projection;

    private Matrix4f viewMatrix = new Matrix4f();
    private Vector3f negCamPos = new Vector3f();

    //Lights
    private int dirDirection, dirAmbient, dirDiffuse, dirSpecular;
    private int pointPosition, pointAmbient, pointDiffuse, pointSpecular, pointConstant, pointLinear, pointQuadratic;
    private int spotPosition, spotDirection, spotAmbient, spotDiffuse, spotSpecular, spotConstant, spotLinear, spotQuadratic, spotCutOff, spotOuterCutOff;

    private int numPointLights, numSpotLights;

    //shadows
    private int depthMap, lightSpaceMatrix;

    //Materials
    private int Material_diffuse, Material_specular, Material_shininess;

    public WorldShader() {
//        super("shaders/worldVertexShader.glsl", "shaders/worldFragmentShader.glsl");
        super("shaders/worldVertexShader.glsl", "shaders/LightingFragment.glsl");

        bindAttribLocation(0, "aPos");
        bindAttribLocation(1, "aTexCoord");
        bindAttribLocation(2, "aNormal");
        linkAndValidate();

        model = getUniformLocation("model");
        view = getUniformLocation("view");
        projection = getUniformLocation("projection");

        dirDirection = getUniformLocation("dirLight.direction");
        dirAmbient = getUniformLocation("dirLight.ambient");
        dirDiffuse = getUniformLocation("dirLight.diffuse");
        dirSpecular = getUniformLocation("dirLight.specular");

        pointPosition = getUniformLocation("point.position[0]");
        pointAmbient = getUniformLocation("point.ambient[0]");
        pointDiffuse = getUniformLocation("point.diffuse[0]");
        pointSpecular = getUniformLocation("point.specular[0]");
        pointConstant = getUniformLocation("point.constant[0]");
        pointLinear = getUniformLocation("point.linear[0]");
        pointQuadratic = getUniformLocation("point.quadratic[0]");

        spotPosition = getUniformLocation("spot.position");
        spotDirection = getUniformLocation("spot.direction");
        spotAmbient = getUniformLocation("spot.ambient");
        spotDiffuse = getUniformLocation("spot.diffuse");
        spotSpecular = getUniformLocation("spot.specular");
        spotConstant = getUniformLocation("spot.constant");
        spotLinear = getUniformLocation("spot.linear");
        spotQuadratic = getUniformLocation("spot.quadratic");
        spotCutOff = getUniformLocation("spot.cutOff");
        spotOuterCutOff = getUniformLocation("spot.outerCutOff");

        numPointLights = getUniformLocation("numPointLights");
        numSpotLights = getUniformLocation("numSpotLights");

        //Shadows
        depthMap = getUniformLocation("depthMap");
        lightSpaceMatrix = getUniformLocation("lightSpaceMatrix");

        Material_diffuse = getUniformLocation("material.diffuse");
        Material_specular = getUniformLocation("material.specular");
        Material_shininess = getUniformLocation("material.shininess");
    }

    public void updateDepthMap(int map){
        loadInt(depthMap, map);
    }

    public void updateLightSpace(Matrix4f matrix){
        loadMatrix4f(lightSpaceMatrix, matrix);
    }

    public void updateMaterial(Material material){
        loadInt(Material_diffuse, 0);
        loadInt(Material_specular, 1);
        loadFloat(Material_shininess, material.shininess);
    }

    public void setNumberOfLight(int pointLights, int spotLights) {
        loadInt(numPointLights, pointLights);
        loadInt(numSpotLights, spotLights);
    }

    public void updatePointLight(int index, PointLight light){
        String prefix = "pointLights[" + index + "].";
        int positionLoc = getUniformLocation(prefix + "position");
        int ambientLoc = getUniformLocation(prefix + "ambient");
        int diffuseLoc = getUniformLocation(prefix + "diffuse");
        int specularLoc = getUniformLocation(prefix + "specular");
        int constantLoc = getUniformLocation(prefix + "constant");
        int linearLoc = getUniformLocation(prefix + "linear");
        int quadraticLoc = getUniformLocation(prefix + "quadratic");

        loadVector3f(positionLoc, light.position);
        loadVector3f(ambientLoc, light.ambient);
        loadVector3f(diffuseLoc, light.diffuse);
        loadVector3f(specularLoc, light.specular);
        loadFloat(constantLoc, light.constant);
        loadFloat(linearLoc, light.linear);
        loadFloat(quadraticLoc, light.quadratic);
    }

    public void updateSpotLight(SpotLight light){
        loadVector3f(spotPosition, light.position);
        loadVector3f(spotPosition, light.direction);
        loadVector3f(spotPosition, light.ambient);
        loadVector3f(spotPosition, light.diffuse);
        loadVector3f(spotPosition, light.specular);

        loadFloat(spotConstant, light.constant);
        loadFloat(spotLinear, light.linear);
        loadFloat(spotQuadratic, light.quadratic);
        loadFloat(spotCutOff, light.cutOff);
        loadFloat(spotOuterCutOff, light.outerCutOff);
    }

    public void updateDirLight(DirLight light){
        loadVector3f(dirDirection, light.direction);
        loadVector3f(dirAmbient, light.ambient);
        loadVector3f(dirDiffuse, light.diffuse);
        loadVector3f(dirSpecular, light.specular);
    }

    public void loadViewMatrix(Transform transform) {
        Matrix4f matrix = createViewMatrix(transform);
        loadMatrix4f(getView(), matrix);
    }

    public void loadViewMatrix(EulerCamera camera) {
        Matrix4f matrix = createEulerCameraViewMatrix(camera);
        loadMatrix4f(getView(), matrix);
    }

    public Matrix4f createEulerCameraViewMatrix(EulerCamera camera){
        viewMatrix.identity();

        viewMatrix.rotateX((float)Math.toRadians(camera.getPitch()), viewMatrix);
        viewMatrix.rotateY((float)Math.toRadians(camera.getYaw()), viewMatrix);
        viewMatrix.rotateZ((float)Math.toRadians(camera.getRoll()), viewMatrix);
        Vector3f camPos = camera.getPosition();
        negCamPos.set(-camPos.x, -camPos.y, - camPos.z);
        viewMatrix.translate(negCamPos, viewMatrix);
        return viewMatrix;
    }

    private Matrix4f createViewMatrix(Transform transform) {
        Matrix4f vm = new Matrix4f();
        vm.identity();

        Vector3f eulerAngles = transform.getRotationDeg();
        vm.rotateX((float) Math.toRadians(eulerAngles.x), vm);
        vm.rotateY((float) Math.toRadians(eulerAngles.y), vm);
        vm.rotateZ((float) Math.toRadians(eulerAngles.z), vm);

        Vector3f camPos = transform.getPosition();
        Vector3f negCamPos = new Vector3f(-camPos.x, -camPos.y, -camPos.z);
        vm.translate(negCamPos, vm);
        return vm;
    }


//    public void setViewPosition(Vector3f position){
//        loadVector3f(viewPosition, position);
//    }

    public int getModel(){
        return model;
    }

    public int getView(){
        return view;
    }

    public int getProjection(){
        return projection;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public void setColor(Vector3f color) {
        loadVector3f(getUniformLocation("objectColor"), color);
    }
}
