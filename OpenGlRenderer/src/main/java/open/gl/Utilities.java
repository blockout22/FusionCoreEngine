package open.gl;

import open.gl.shaders.lights.DirLight;
import open.gl.texture.TextureLoader;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Utilities {

    public static IntBuffer flip(int[] data) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();

        return buffer;
    }

    public static FloatBuffer flip(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();

        return buffer;
    }

    public static BufferedImage loadImage(String fileName) throws IOException {
        InputStream in = null;
        BufferedImage image;
        File file = new File(fileName);

        if(!file.exists()) {
            in = TextureLoader.class.getResourceAsStream("/" + fileName);
            //if filename starts with http ... try to load from URL if image isn't found in file system
            if(fileName.startsWith("http")){
                URL url = new URL(fileName);
                String urlFileName = url.getFile().substring(url.getFile().lastIndexOf("/") + 1);

                //check if saved image already exists
                File urlFile = new File(urlFileName);
                if(!urlFile.exists()) {
                    image = ImageIO.read(url);
                    ImageIO.write(image, "png", new FileOutputStream(urlFile));
                }else{
                    image = ImageIO.read(urlFile);
                }

            }else {
                image = ImageIO.read(in);
            }
        }else{
            image = ImageIO.read(file);
        }


        if(image == null){
            throw new IOException("Image Not Found!");
        }

        if (in != null) {
            in.close();
        }
        return image;
    }

    public static FloatBuffer flipFloatBuffer(Matrix4f matrix, FloatBuffer dest){
        dest.put(matrix.m00());
        dest.put(matrix.m01());
        dest.put(matrix.m02());
        dest.put(matrix.m03());

        dest.put(matrix.m10());
        dest.put(matrix.m11());
        dest.put(matrix.m12());
        dest.put(matrix.m13());

        dest.put(matrix.m20());
        dest.put(matrix.m21());
        dest.put(matrix.m22());
        dest.put(matrix.m23());

        dest.put(matrix.m30());
        dest.put(matrix.m31());
        dest.put(matrix.m32());
        dest.put(matrix.m33());

        dest.flip();

        return dest;
    }

    public static FloatBuffer createFlippedFloatBuffer(Matrix4f matrix){
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

        flipFloatBuffer(matrix, buffer);

        return buffer;
    }
}
