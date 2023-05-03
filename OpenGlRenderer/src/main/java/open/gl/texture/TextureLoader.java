package open.gl.texture;

import open.gl.Utilities;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class TextureLoader {

    public static HashMap<String, Texture> texturePool = new HashMap<>();

    public static Texture loadTexture(String textureFile) {
        Texture pool = texturePool.get(textureFile);

        if(pool != null){
            return pool;
        }

        BufferedImage image = null;
        try {
            image = Utilities.loadImage(textureFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

        for (int h = 0; h < image.getHeight(); h++) {
            for (int w = 0; w < image.getWidth(); w++) {
                int pixel = pixels[h * image.getWidth() + w];

                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        buffer.flip();


        Texture texture = new Texture(image.getWidth(), image.getHeight());
        texture.genTextureID(buffer);

        texturePool.put(textureFile, texture);

        return texture;

    }
}
