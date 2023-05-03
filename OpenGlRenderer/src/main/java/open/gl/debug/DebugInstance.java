package open.gl.debug;

import org.joml.Vector3f;

public class DebugInstance {

    public Vector3f position;
    public Vector3f rotation;
    public Vector3f scale;
    private Vector3f color;

    public DebugInstance(){
        this.position = new Vector3f();
        this.rotation = new Vector3f();
        this.scale = new Vector3f(1f, 1f, 1f);
        this.color = new Vector3f(255, 0, 0);
    }

    public DebugInstance(Vector3f position, Vector3f rotation, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        this.color = new Vector3f(255, 0, 0);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public Vector3f getScale() {
        return scale;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }
}