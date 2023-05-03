package open.gl.debug;

import open.gl.EulerCamera;
import open.gl.Utilities;
import open.gl.shaders.WorldShader;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL30.*;

public class DebugRenderer {

//    private int vao;
//    private int vbo;
//    private int vboi;

    private static boolean shouldDepthTest = false;
    private static  ArrayList<DebugInstance> instances = new ArrayList<>();
    private static ArrayList<Integer> vaos = new ArrayList<Integer>();
    private static ArrayList<Integer> vbos = new ArrayList<Integer>();
    private static ArrayList<Integer> vbois = new ArrayList<Integer>();

    public DebugRenderer(){

    }

    public static DebugInstance add(Vector3f start, Vector3f end){

        DebugInstance instance = new DebugInstance();
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int vboi = glGenBuffers();

        instances.add(instance);
        vaos.add(vao);
        vbos.add(vbo);
        vbois.add(vboi);
        float[] verts = {
                start.x, start.y, start.z,
                end.x, end.y, end.z
        };

        int[] indices = {
                0, 1
        };

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, Utilities.flip(verts), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);


        glBindVertexArray(0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboi);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Utilities.flip(indices), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        return instance;
    }

    public static DebugInstance[] addCubeRender(Vector3f min, Vector3f max){

        int i = 0;
        DebugInstance[] instanceList = new DebugInstance[12];

        //front face
        instanceList[i++] = DebugRenderer.add(new Vector3f(min.x, max.y, min.z), new Vector3f(max.x, max.y, min.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(max.x, max.y, min.z), new Vector3f(max.x, min.y, min.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(max.x, min.y, min.z), new Vector3f(min.x, min.y, min.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(min.x, min.y, min.z), new Vector3f(min.x, max.y, min.z));

        //back face
        instanceList[i++] = DebugRenderer.add(new Vector3f(min.x, max.y, max.z), new Vector3f(max.x, max.y, max.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(max.x, max.y, max.z), new Vector3f(max.x, min.y, max.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(max.x, min.y, max.z), new Vector3f(min.x, min.y, max.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(min.x, min.y, max.z), new Vector3f(min.x, max.y, max.z));

        //sides
        instanceList[i++] = DebugRenderer.add(new Vector3f(max.x, max.y, min.z), new Vector3f(max.x, max.y, max.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(max.x, min.y, min.z), new Vector3f(max.x, min.y, max.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(min.x, max.y, min.z), new Vector3f(min.x, max.y, max.z));
        instanceList[i++] = DebugRenderer.add(new Vector3f(min.x, min.y, min.z), new Vector3f(min.x, min.y, max.z));

        return instanceList;
    }

    public static void enableDepthTesting(){
        DebugRenderer.shouldDepthTest = true;
    }

    public static void disableDepthTesting(){
        DebugRenderer.shouldDepthTest = false;
    }

    public static void render(WorldShader shader, EulerCamera camera){
        for(int i = 0; i < vaos.size(); i++){
            if(!DebugRenderer.shouldDepthTest){
                glDisable(GL_DEPTH_TEST);
            }
            glBindVertexArray(vaos.get(i));
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glEnableVertexAttribArray(2);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbois.get(i));

            {
//                Vector3f position = new Vector3f();
//                Vector3f rotation = new Vector3f();
//                Vector3f scale = new Vector3f(1f, 1f, 1f);
                Matrix4f transformationMatrix = createTransformationMatrix(instances.get(i).position, instances.get(i).rotation, instances.get(i).scale);
                shader.loadMatrix4f(shader.getModel(), transformationMatrix);
                shader.setColor(instances.get(i).getColor());

                glLineWidth(2);
                glDrawElements(GL_LINES, 2, GL_UNSIGNED_INT, 0);

                //reset color
                shader.setColor(new Vector3f(1));
            }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glDisableVertexAttribArray(2);
            glBindVertexArray(0);
            glEnable(GL_DEPTH_TEST);
        }
    }

    private static Matrix4f createTransformationMatrix(Vector3f translation, Vector3f rotation, Vector3f scale) {
        Matrix4f matrix = new Matrix4f();
        matrix.identity();
        matrix.translate(translation, matrix);
        matrix.rotateX((float) Math.toRadians(rotation.x), matrix);
        matrix.rotateY((float) Math.toRadians(rotation.y), matrix);
        matrix.rotateZ((float) Math.toRadians(rotation.z), matrix);
        matrix.scale(scale, matrix);

        return matrix;
    }

    public static void cleanup(){
        for(int i = 0; i < vaos.size(); i++){
            glDeleteVertexArrays(vaos.get(i));
        }
        for(int i = 0; i < vbos.size(); i++){
            glDeleteBuffers(vbos.get(i));
        }
        for(int i = 0; i < vbois.size(); i++){
            glDeleteBuffers(vbois.get(i));
        }
    }
}
