package open.gl.physics;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.IDebugDraw;
import com.bulletphysics.linearmath.Transform;
import open.gl.gameobject.PhysicsComponent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.vecmath.Quat4f;


public class PhysicsWorld {

    javax.vecmath.Vector3f rayStartWorld = new javax.vecmath.Vector3f();
    javax.vecmath.Vector3f  rayEndWorld = new javax.vecmath.Vector3f();
    CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(rayStartWorld, rayEndWorld);

    private final DiscreteDynamicsWorld dynamicsWorld;

    public PhysicsWorld() {
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        DbvtBroadphase broadphase = new DbvtBroadphase();
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        this.dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

        dynamicsWorld.setGravity(new javax.vecmath.Vector3f(0, -9.81f, 0));
        PhysicsDebugDraw debugDrawer = new PhysicsDebugDraw();
//        dynamicsWorld.setDebugDrawer(debugDrawer);
    }

    public void debugDraw(){
        dynamicsWorld.debugDrawWorld();
    }

    public javax.vecmath.Vector3f toPhysicsVector(Vector3f vector3f) {
        return new javax.vecmath.Vector3f(vector3f.x, vector3f.y, vector3f.z);
    }

    public static Vector3f toJomlVector(javax.vecmath.Vector3f vector3f){
        return new Vector3f(vector3f.x, vector3f.y, vector3f.z);
    }

    public float[] toFloatArray(javax.vecmath.Vector3f vector3f) {
        return new float[]{vector3f.x, vector3f.y, vector3f.z};
    }

    public void raycast(Vector3f start, Vector3f end, HitResults hitResults){
        rayCallback.closestHitFraction = 1f;
        rayCallback.collisionObject = null;

        rayStartWorld.set(start.x, start.y, start.z);
        rayEndWorld.set(end.x, end.y, end.z);

        dynamicsWorld.rayTest(rayStartWorld, rayEndWorld, rayCallback);

        if(rayCallback.hasHit()){
            CollisionObject hitObject = rayCallback.collisionObject;
            PhysicsComponent hitPhysicsComponent = (PhysicsComponent) hitObject.getUserPointer();
            hitResults.hitComponent = hitPhysicsComponent;
        }else{
            hitResults.hitComponent = null;
        }
    }

    public void cursorRaycast(double cursorPositionX, double cursorPositionY, int windowWidth, int windowHeight, Matrix4f projectionMatrix, Matrix4f viewMatrix, HitResults hitResults){
        float x  = (float)(2 * cursorPositionX / windowWidth - 1.0);
        float y = (float)(1.0 - 2.0 * cursorPositionY / windowHeight);

        Vector4f startPointNDC = new Vector4f(x, y, -1.0f, 1.0f);
        Vector4f endPointNDC = new Vector4f(x, y, 1.0f, 1.0f);

        //screen to world
        Matrix4f inverseProjectionViewMatrix = new Matrix4f();
        projectionMatrix.mul(viewMatrix, inverseProjectionViewMatrix).invert();

        Vector4f startPointWorld = new Vector4f();
        Vector4f endPointWorld = new Vector4f();
        inverseProjectionViewMatrix.transform(startPointNDC, startPointWorld);
        inverseProjectionViewMatrix.transform(endPointNDC, endPointWorld);

        startPointWorld.div(startPointWorld.w);
        endPointWorld.div(endPointWorld.w);

        Vector3f rayStart = new Vector3f(startPointWorld.x, startPointWorld.y, startPointWorld.z);
        Vector3f rayEnd = new Vector3f(endPointWorld.x, endPointWorld.y, endPointWorld.z);
        raycast(rayStart, rayEnd, hitResults);
    }

    public void update(float deltaTime) {
        dynamicsWorld.stepSimulation(deltaTime);
    }

    public void addRigidBody(RigidBody rigidBody) {
        dynamicsWorld.addRigidBody(rigidBody);
    }

    public RigidBody addShapeToWorld(CollisionShape shape, float mass, Quaternionf quaternionf, Vector3f position, float scale){
        javax.vecmath.Vector3f intertia =  toPhysicsVector(new Vector3f());
        Quat4f rot = new Quat4f(quaternionf.x, quaternionf.y, quaternionf.z, quaternionf.w);
        javax.vecmath.Vector3f pos = new javax.vecmath.Vector3f(position.x, position.y, position.z);

        shape.calculateLocalInertia(mass, intertia);
        RigidBodyConstructionInfo constructionInfo = new RigidBodyConstructionInfo(
                mass,
                new DefaultMotionState(new Transform(new javax.vecmath.Matrix4f(rot, pos, scale))),
                shape,
                intertia
        );

        RigidBody rigidBody = new RigidBody(constructionInfo);
        addRigidBody(rigidBody);

        return rigidBody;
    }

    public void removeRigidBody(RigidBody rigidBody) {
        dynamicsWorld.removeRigidBody(rigidBody);
    }


    public class PhysicsDebugDraw extends IDebugDraw {
        @Override
        public void drawLine(javax.vecmath.Vector3f from, javax.vecmath.Vector3f to, javax.vecmath.Vector3f color) {
        }

        @Override
        public void drawContactPoint(javax.vecmath.Vector3f pointOnB, javax.vecmath.Vector3f normalOnB, float distance, int lifeTime, javax.vecmath.Vector3f color) {
        }

        @Override
        public void reportErrorWarning(String s) {

        }

        @Override
        public void draw3dText(javax.vecmath.Vector3f vector3f, String s) {

        }

        @Override
        public void setDebugMode(int i) {
        }

        @Override
        public int getDebugMode() {
            return DebugDrawModes.DRAW_WIREFRAME;
        }
    }
}
