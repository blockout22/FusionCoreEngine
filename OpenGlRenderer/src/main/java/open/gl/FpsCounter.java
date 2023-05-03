package open.gl;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class FpsCounter {

    private static double lastTime;
    private static int frames;
    private static int fps;

    public interface FpsCallback {
        void onFpsUpdate(int fps);
    }

    private static ArrayList<FpsCallback> callbacks = new ArrayList<>();

    public static void addCallback(FpsCallback callback){
        callbacks.add(callback);
    }

    static {
        lastTime = GLFW.glfwGetTime();
        frames = 0;
        fps = 0;
    }

    public static void update() {
        double currentTime = GLFW.glfwGetTime();
        frames++;

        if (currentTime - lastTime >= 1.0) {
            fps = frames;

            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).onFpsUpdate(fps);
            }

            frames = 0;
            lastTime += 1.0;
        }
    }

    public static int getFPS() {
        return fps;
    }
}

