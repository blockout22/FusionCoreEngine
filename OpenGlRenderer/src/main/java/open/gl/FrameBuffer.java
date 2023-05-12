package open.gl;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class FrameBuffer {

    private boolean depthOnly;

//    int vao;
//    int vbo;
    int rbo;

    int frameBuffer;
    int textureColorbuffer;

    //depth
    int depthMapFBO;
    int depthMap;

    int textureWidth;
    int textureHeight;

    public FrameBuffer(int width, int height, boolean depthOnly)
    {
        this(width, height, width, height, depthOnly);
    }

    public FrameBuffer(int width, int height, int textureWidth, int textureHeight, boolean depthOnly) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        if(!depthOnly) {
            this.depthOnly = depthOnly;
            frameBuffer = glGenFramebuffers();
            rbo = glGenRenderbuffers();

            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

            textureColorbuffer = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureColorbuffer);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, NULL);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
            glBindTexture(GL_TEXTURE_2D, 0);

            //bind texture to frame buffer
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureColorbuffer, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, rbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);
        }else{
            //depth
            depthMap = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, depthMap);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, textureWidth, textureHeight, 0 , GL_DEPTH_COMPONENT, GL_FLOAT, NULL);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            depthMapFBO = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            //bind texture to frame buffer
//            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);
//            glBindRenderbuffer(GL_RENDERBUFFER, rbo);
//            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);
        }

        //Create Render Buffer



        //check if Frame buffer successfully completed
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE){
            System.out.println("Error: FrameBuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void updateSize(int width, int height){
        // Update the texture color buffer
        glBindTexture(GL_TEXTURE_2D, textureColorbuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, textureWidth, textureHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, NULL);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Update the render buffer
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public void bind() {
        if(!depthOnly) {
            glViewport(0, 0, textureWidth, textureHeight);
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        }else{
            glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        }
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getTextureId(){
        if(!depthOnly) {
            return textureColorbuffer;
        }else{
            return depthMap;
        }
    }

    public void cleanup(){
        if(!depthOnly) {
            glDeleteTextures(textureColorbuffer);
            glDeleteFramebuffers(frameBuffer);
        }else{
            glDeleteTextures(depthMap);
            glDeleteFramebuffers(depthMapFBO);
        }
        glDeleteRenderbuffers(rbo);
    }

    public ByteBuffer readPixels(int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4); // Assuming RGBA format
        if(!depthOnly) {
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        }else{
            glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        }
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return buffer;
    }

    public void saveImage(ByteBuffer buffer, int width, int height, String filePath) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (x + width * y) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                image.setRGB(x, height - y - 1, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        try {
            ImageIO.write(image, "PNG", new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

