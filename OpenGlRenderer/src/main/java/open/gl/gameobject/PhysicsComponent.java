package open.gl.gameobject;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import open.gl.physics.PhysicsWorld;

import javax.vecmath.Quat4f;

public class PhysicsComponent extends Component {

    private Transform transform = new Transform();
    private Quat4f quat = new Quat4f();

    private RigidBody rigidBody;
    private MeshInstance instance;
    private boolean isBeingManipulated = false;

    public PhysicsComponent(RigidBody rigidBody, MeshInstance instance) {
        this.rigidBody = rigidBody;
        this.instance = instance;
        rigidBody.setUserPointer(this);
    }

    public void setManipulate(boolean manipulate){
        this.isBeingManipulated = manipulate;
    }

    @Override
    public void update() {
        if(!isBeingManipulated) {
            rigidBody.getMotionState().getWorldTransform(transform);
            instance.setPosition(PhysicsWorld.toJomlVector(transform.origin));

            transform.getRotation(quat);
            instance.setRotation(quat.x, quat.y, quat.z, quat.w);
        }
    }

    public RigidBody getRigidBody() {
        return rigidBody;
    }

    public MeshInstance getInstance() {
        return instance;
    }
}
