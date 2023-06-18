package com.fusion.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.NVRayTracing.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;

public class VulkanShader {

    private long module;

    public VulkanShader(VkDevice device, File shaderFile, int vulkanStage)
    {
        this(device, loadFile(shaderFile.getAbsolutePath()), vulkanStage);
    }

    public VulkanShader(VkDevice device, String shaderSource, int vulkanStage) {
        try(MemoryStack stack = stackPush())
        {
            long compiler = shaderc_compiler_initialize();
            long result = shaderc_compile_into_spv(compiler, shaderSource, vulkanStageToShadercKind(vulkanStage), "shader.glsl", "main", NULL);
            shaderc_compiler_release(compiler);

            if(shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success){
                throw new RuntimeException("Failed to compile shader: " + shaderc_result_get_error_message(result));
            }

            long resultBytes = shaderc_result_get_length(result);
            ByteBuffer spirvBinary = Shaderc.shaderc_result_get_bytes(result).slice(0, (int)resultBytes);

            this.module = createShaderModule(device, spirvBinary);

            shaderc_result_release(result);
        }
    }

    private static int vulkanStageToShadercKind(int stage) {
        switch (stage) {
            case VK_SHADER_STAGE_VERTEX_BIT:
                return shaderc_vertex_shader;
            case VK_SHADER_STAGE_FRAGMENT_BIT:
                return shaderc_fragment_shader;
            case VK_SHADER_STAGE_RAYGEN_BIT_NV:
                return shaderc_raygen_shader;
            case VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV:
                return shaderc_closesthit_shader;
            case VK_SHADER_STAGE_MISS_BIT_NV:
                return shaderc_miss_shader;
            case VK_SHADER_STAGE_ANY_HIT_BIT_NV:
                return shaderc_anyhit_shader;
            case VK_SHADER_STAGE_INTERSECTION_BIT_NV:
                return shaderc_intersection_shader;
            case VK_SHADER_STAGE_COMPUTE_BIT:
                return shaderc_compute_shader;
            default:
                throw new IllegalArgumentException("Stage: " + stage);
        }
    }

    private static String loadFile(String shaderFile) {
        StringBuilder sb = new StringBuilder();


        File file = new File(shaderFile);
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            while((line = br.readLine()) != null){
                if(line.trim().startsWith("#include")){
                    String includedFileName = line.trim().split("\"")[1];
                    sb.append(loadFile(includedFileName));
                }else {
                    sb.append(line).append("\n");
                }
            }

            br.close();

            return sb.toString();

        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    private long createShaderModule(VkDevice device, ByteBuffer byteBuffer) {
        try(MemoryStack stack = stackPush()){
            VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pCode(byteBuffer);

            LongBuffer pShaderModule = stack.mallocLong(1);

            VulkanUtils.check(vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule));
            return pShaderModule.get(0);
        }
    }

    public long get(){
        return module;
    }

    public void cleanup(){

    }
}
