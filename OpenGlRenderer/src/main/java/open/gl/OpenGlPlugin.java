package open.gl;

import com.fusion.core.engine.CoreEngine;
import com.fusion.core.engine.Debug;
import com.fusion.core.engine.plugin.UnmodifiableString;
import com.fusion.core.engine.plugin.Plugin;
import com.fusion.core.engine.window.Window;

import java.util.Collections;
import java.util.List;

public class OpenGlPlugin implements Plugin {

    private OpenGlRenderer renderer;

    @Override
    public UnmodifiableString setId() {
        return UnmodifiableString.fromString("OpenGL");
    }

    @Override
    public void init(CoreEngine coreEngine) {
        Window window = coreEngine.getWindow();
        renderer = new OpenGlRenderer(window);
        coreEngine.setRenderer(renderer);
    }

    @Override
    public void update() {

    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<String> getDependencies() {
        return Collections.singletonList("GLFW");
    }
}
