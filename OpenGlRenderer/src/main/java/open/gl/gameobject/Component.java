package open.gl.gameobject;

public abstract class Component {

    GameObject gameObject;
    public abstract void update();

    public GameObject getGameObject(){
        return gameObject;
    }

    protected void setGameObject(GameObject gameObject){
        this.gameObject = gameObject;
    }
}
