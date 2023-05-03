package open.gl.shaders;

import com.fusion.core.engine.Debug;
import open.gl.Utilities;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.*;
import java.net.URL;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;

public abstract class OpenGlShader {

    private int program;
    private int vertex;
    private int fragment;

    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
    FloatBuffer buffer = null;

    public OpenGlShader(String vertextShader, String fragmentShader) {
        vertex = loadShader(vertextShader, GL20.GL_VERTEX_SHADER);
        fragment = loadShader(fragmentShader, GL20.GL_FRAGMENT_SHADER);

        createProgram();
    }


    private void createProgram() {
        program = GL20.glCreateProgram();

        //Attach the vertex shader and fragment shader to the program
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
    }

    public void linkAndValidate()
    {
        GL20.glLinkProgram(program);
        GL20.glValidateProgram(program);
    }

    public void bind()
    {
        GL20.glUseProgram(program);
    }

    public static void unbind()
    {
        GL20.glUseProgram(0);
    }

    public void bindAttribLocation(int index, String name){
        GL20.glBindAttribLocation(program, index, name);
    }

    public int getUniformLocation(String uniform){
        return glGetUniformLocation(program, uniform);
    }

    public void loadMatrix4f(int location, Matrix4f matrix){
        if(buffer == null){
            buffer = Utilities.createFlippedFloatBuffer(matrix);
        }else{
            Utilities.flipFloatBuffer(matrix, buffer);
        }

        GL20.glUniformMatrix4fv(location, false, buffer);
    }

    public void loadVector3f(int location, Vector3f vector3f) {
        loadVector3f(location, vector3f.x, vector3f.y, vector3f.z);
    }
    public void loadFloat(int location, float value) {
        GL20.glUniform1f(location, value);
    }

    public void loadInt(int location, int value){
        GL20.glUniform1i(location, value);
    }

    public void loadVector3f(int location, float x, float y, float z){
        GL20.glUniform3f(location, x, y, z);
    }

    private int loadShader(String fileName, int type) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        File file = new File(fileName);
        try {
            InputStream in = null;
//            File file = new File(Utilities.getAssetDir() + fileName);

            if(!file.exists()) {
                URL url = OpenGlShader.class.getResource("/" + fileName);
                in = OpenGlShader.class.getResourceAsStream("/" + fileName);
                br = new BufferedReader(new InputStreamReader(in));
            }else{
                br = new BufferedReader(new FileReader(file));
            }
        }catch (Exception e){
            e.printStackTrace();
            return - 1;
        }

        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }

        int shaderID = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderID, sb);
        GL20.glCompileShader(shaderID);

        if(GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE){
            System.out.println("====================");
            System.out.println(sb);
            System.out.println("====================");
            System.err.println("[" + fileName + "]");
            System.err.println(GL20.glGetShaderInfoLog(shaderID));
            String error = "Shader.class\n" + GL20.glGetShaderInfoLog(shaderID);
            System.err.println(error);
        }


        return shaderID;
    }

    public void cleanup(){
        unbind();
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        GL20.glDeleteProgram(program);
    }
}
