package open.gl;

import com.fusion.core.engine.Debug;
import open.gl.texture.Texture;
import open.gl.texture.TextureLoader;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class ModelLoader {

    public static AIScene loadAiScene(String path){
        AIScene scene = aiImportFile(path, aiProcess_JoinIdenticalVertices | aiProcess_Triangulate | aiProcess_FixInfacingNormals);

        if(scene == null){
            throw new RuntimeException("Failed to load model: " + aiGetErrorString());
        }

        return scene;
    }
    
    public static List<Model> loadModels(String path){
        AIScene scene = loadAiScene(path);
        PointerBuffer materials = scene.mMaterials();

        List<Model> models = new ArrayList<>();

        for (int i = 0; i < scene.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(i));

            int materialIndex = aiMesh.mMaterialIndex();

            AIMaterial material = AIMaterial.create(materials.get(materialIndex));
            try(MemoryStack stack  = MemoryStack.stackPush()) {
                PointerBuffer pMaterialProperty = material.mProperties();

                for (int j = 0; j < pMaterialProperty.capacity(); j++) {
                    AIMaterialProperty prop = AIMaterialProperty.create(pMaterialProperty.get(j));

//                    Debug.logError(prop.mKey().dataString());
//                    Debug.logError(prop.mData().getFloat(0));
                }
            }

            float[] vertices = new float[aiMesh.mNumVertices() * 3];
            float[] normals = new float[aiMesh.mNumVertices() * 3];
            float[] texCoords = new float[aiMesh.mNumVertices() * 2];
            int[] indices = new int[aiMesh.mNumFaces() * 3];

            AIVector3D.Buffer aiVertices = aiMesh.mVertices();
            AIVector3D.Buffer aiNormals = aiMesh.mNormals();
            AIVector3D.Buffer aiTexCoords = aiMesh.mTextureCoords(0);

            for (int j = 0; j < aiMesh.mNumVertices(); j++) {
                AIVector3D vertex = aiVertices.get(j);
                AIVector3D normal = aiNormals.get(j);

                vertices[j * 3] = vertex.x();
                vertices[j * 3 + 1] = vertex.y();
                vertices[j * 3 + 2] = vertex.z();

                normals[j * 3] = normal.x();
                normals[j * 3 + 1] = normal.y();
                normals[j * 3 + 2] = normal.z();

                if(aiTexCoords != null){
                    AIVector3D texCoord = aiTexCoords.get(j);
                    texCoords[j * 2] = texCoord.x();
                    texCoords[j * 2 + 1] = texCoord.y();
                }else{
                    texCoords[j * 2] = 0;
                    texCoords[j * 2 + 1] = 0;
                }
            }

            AIFace.Buffer aiFaces = aiMesh.mFaces();
            for (int j = 0; j < aiFaces.capacity(); j++) {
                AIFace aiFace = aiFaces.get(j);
                IntBuffer faceIndices = aiFace.mIndices();

                indices[j * 3] = faceIndices.get(0);
                indices[j * 3 + 1] = faceIndices.get(1);
                indices[j * 3 + 2] = faceIndices.get(2);
            }

            Model model = new Model(vertices, texCoords, normals, indices);
            processMaterialProperties(new File(path).getParent(), model, material);

            models.add(model);

//            aiVertices.free();
//            aiNormals.free();
//            aiTexCoords.free();
//            aiMesh.free();
//            aiFaces.free();
        }

//        scene.free();
        return models;
    }

    private static void processMaterialProperties(String currentPath, Model model, AIMaterial material) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pMaterialProperty = stack.mallocPointer(1);

            // Diffuse color
            ByteBuffer diffuseKey = stack.ASCII(AI_MATKEY_COLOR_DIFFUSE, true);
            if (aiGetMaterialProperty(material, diffuseKey, aiTextureType_NONE, 0, pMaterialProperty) == aiReturn_SUCCESS) {
                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperty.get(0));
                FloatBuffer colorData = materialProperty.mData().asFloatBuffer();
                colorData.rewind();
                int numComponents = materialProperty.mDataLength() / Float.BYTES;
                for (int i = 0; i < numComponents; i++) {
//                    Debug.logError("Color: " + colorData.get(i));
                }
//                System.out.printf("Diffuse color: (%.2f, %.2f, %.2f)\n", colorData.get(0), colorData.get(1), colorData.get(2));
            }

            // Specular color
            ByteBuffer specularKey = stack.ASCII(AI_MATKEY_COLOR_SPECULAR, true);
            if (aiGetMaterialProperty(material, specularKey, aiTextureType_NONE, 0, pMaterialProperty) == aiReturn_SUCCESS) {
                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperty.get(0));
                FloatBuffer colorData = materialProperty.mData().asFloatBuffer();
                colorData.rewind();
//                System.out.printf("Specular color: (%.2f, %.2f, %.2f, %.2f)\n", colorData.get(0), colorData.get(1), colorData.get(2), colorData.get(3));
            }

            // Opacity
            ByteBuffer opacityKey = stack.ASCII(AI_MATKEY_OPACITY, true);
            if (aiGetMaterialProperty(material, opacityKey, aiTextureType_NONE, 0, pMaterialProperty) == aiReturn_SUCCESS) {
                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperty.get(0));
                FloatBuffer opacityData = materialProperty.mData().asFloatBuffer();
                opacityData.rewind();
//                System.out.printf("Opacity: %.2f\n", opacityData.get(0));
            }

            // Diffuse texture
            ByteBuffer diffuseTextureKey = stack.ASCII(_AI_MATKEY_TEXTURE_BASE, true);
            if (aiGetMaterialProperty(material, diffuseTextureKey, aiTextureType_DIFFUSE, 0, pMaterialProperty) == aiReturn_SUCCESS) {
                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperty.get(0));
                ByteBuffer texturePathBuffer = materialProperty.mData();

                String texturePath = MemoryUtil.memUTF8(texturePathBuffer);
                texturePath = texturePath.replaceAll("[^\\x20-\\x7E]", "");
                File file = new File(currentPath + File.separator + texturePath);
                if(file.exists()) {
                    Texture texture = TextureLoader.loadTexture(file.getAbsolutePath());
                    model.setDiffuseTexture(texture);
                }
                Debug.logInfo("Diffuse texture path: " +  texturePath);
            }

            //specular texture
            if (aiGetMaterialProperty(material, diffuseTextureKey, aiTextureType_SPECULAR, 0, pMaterialProperty) == aiReturn_SUCCESS) {
                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperty.get(0));
                ByteBuffer texturePathBuffer = materialProperty.mData();

                String texturePath = MemoryUtil.memUTF8(texturePathBuffer);
                texturePath = texturePath.replaceAll("[^\\x20-\\x7E]", "");
                File file = new File(currentPath + File.separator + texturePath);
                if(file.exists()) {
                    Texture texture = TextureLoader.loadTexture(file.getAbsolutePath());
                    model.setSpecularTexture(texture);
                }
                Debug.logInfo("Specular texture path: " +  texturePath);
            }

            // Add other material properties as needed
        }
    }
}
