package com.fusion.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.NVRayTracing.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;

public class VulkanShader {

//    private long module;

    private static final int VERTEX_BUFFER_BIND_ID = 0;

    private VkDevice device;
    private long pipeline;

    private long vertexShader;
    private long fragmentShader;

    private final LongBuffer lp = memAllocLong(1);

    public VkPipelineVertexInputStateCreateInfo vi = VkPipelineVertexInputStateCreateInfo.calloc();
    public VkVertexInputBindingDescription.Buffer   vi_bindings = VkVertexInputBindingDescription.calloc(1);
    public VkVertexInputAttributeDescription.Buffer vi_attrs    = VkVertexInputAttributeDescription.calloc(2);

    public VulkanShader(VkDevice device, File vertexSource, File fragmentSource)
    {
        this(device, loadFile(vertexSource.getAbsolutePath()), loadFile(fragmentSource.getAbsolutePath()));
    }

    public VulkanShader(VkDevice device, String vertexSource, String fragmentSource) {
        this.device = device;
        vertexShader = loadShader(vertexSource, VK_SHADER_STAGE_VERTEX_BIT);
        fragmentShader = loadShader(fragmentSource, VK_SHADER_STAGE_FRAGMENT_BIT);
    }

    public void updateUniform(float r, float g, float b){
        try(MemoryStack stack = stackPush()){

        }
    }

    private void prepareDescriptorLayout(){
    }

    private void preparePipeline(long pipeline_layout, long render_pass){
        long vert_shader_module;
        long frag_shader_module;
        long pipelineCache;

        try(MemoryStack stack = stackPush()){
            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack);

            ByteBuffer main = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            shaderStages.get(0)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vert_shader_module = vertexShader)//demo_prepare_shader_module(VulkanUtils.vertShaderCode))
                    .pName(main);
            shaderStages.get(1)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(frag_shader_module = fragmentShader)
                    .pName(main);

            VkPipelineDepthStencilStateCreateInfo ds = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false)
                    .back(it -> it
                            .failOp(VK_STENCIL_OP_KEEP)
                            .passOp(VK_STENCIL_OP_KEEP)
                            .compareOp(VK_COMPARE_OP_ALWAYS));
            ds.front(ds.back());

            pipeline
                    .sType$Default()
                    .pStages(shaderStages)
//                    .pVertexInputState(vertices.vi)
                    //uses the 3d setup for the pipeline
                    .pVertexInputState(vi)
                    .pInputAssemblyState(
                            VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                                    .sType$Default()
                                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST))
                    .pViewportState(
                            VkPipelineViewportStateCreateInfo.calloc(stack)
                                    .sType$Default()
                                    .viewportCount(1)
                                    .scissorCount(1))
                    .pRasterizationState(
                            VkPipelineRasterizationStateCreateInfo.calloc(stack)
                                    .sType$Default()
                                    .polygonMode(VK_POLYGON_MODE_FILL)
                                    .cullMode(VK_CULL_MODE_BACK_BIT)
                                    .frontFace(VK_FRONT_FACE_CLOCKWISE)
                                    .depthClampEnable(false)
                                    .rasterizerDiscardEnable(false)
                                    .depthBiasEnable(false)
                                    .lineWidth(1.0f))
                    .pMultisampleState(
                            VkPipelineMultisampleStateCreateInfo.calloc(stack)
                                    .sType$Default()
                                    .pSampleMask(null)
                                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT))
                    .pDepthStencilState(ds)
                    .pColorBlendState(
                            VkPipelineColorBlendStateCreateInfo.calloc(stack)
                                    .sType$Default()
                                    .pAttachments(
                                            VkPipelineColorBlendAttachmentState.calloc(1, stack)
                                                    .colorWriteMask(0xf)
                                                    .blendEnable(false)
                                    ))
                    .pDynamicState(
                            VkPipelineDynamicStateCreateInfo.calloc(stack)
                                    .sType$Default()
                                    .pDynamicStates(stack.ints(
                                            VK_DYNAMIC_STATE_VIEWPORT,
                                            VK_DYNAMIC_STATE_SCISSOR
                                    )))
                    .layout(pipeline_layout)
                    .renderPass(render_pass);

            VkPipelineCacheCreateInfo pipelineCacheCI = VkPipelineCacheCreateInfo.calloc(stack).sType$Default();

            VulkanUtils.check(vkCreatePipelineCache(device, pipelineCacheCI, null, lp));
            pipelineCache = lp.get(0);

            VulkanUtils.check(vkCreateGraphicsPipelines(device, pipelineCache, pipeline, null, lp));
            this.pipeline = lp.get(0);

            vkDestroyPipelineCache(device, pipelineCache, null);

            vkDestroyShaderModule(device, frag_shader_module, null);
            vkDestroyShaderModule(device, vert_shader_module, null);
        }
    }

    public void initShader(long pipeline_layout, long render_pass) {
        vi
                .sType$Default()
                .pNext(NULL)
                .pVertexBindingDescriptions(vi_bindings)
                .pVertexAttributeDescriptions(vi_attrs);
//
        vi_bindings.get(0)
                .binding(VERTEX_BUFFER_BIND_ID)
                // Stride is calculated by the sum of the values of each attribute multiplied by the byte size.
                // For example, if you have vertices, normals, and texture coordinates, each taking 3, 3, and 2 values respectively,
                // the stride would be calculated as: (vertices + normals + texCoords) * byteSize = (3 + 3 + 2) * 4
                .stride((3 + 2) * 4)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        vi_attrs.get(0)
                .binding(VERTEX_BUFFER_BIND_ID)
                //sets the attribute location e.g. if you want to use the data from this you can use the following in your shader code:-
                //layout(location = 0) in vec3 aPos;
                //in this case location equals the location (0) values set below vec3 is for format (VK_FORMAT_R32G32B32_SFLOAT) and aPos can be any variable name you choose
                .location(0)
                //VK_FORMAT_R32G32B32_SFLOAT = RGB which sets location 0 to 3d coords
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(0);

        vi_attrs.get(1)
                .binding(VERTEX_BUFFER_BIND_ID)
                //sets the attribute location
                .location(1)
                //VK_FORMAT_R32G32_SFLOAT = RG which sets location 1 to 2d coords
                .format(VK_FORMAT_R32G32_SFLOAT)
                .offset(4 * 3);
        preparePipeline(pipeline_layout, render_pass);
    }

    private long loadShader(String source, int vulkanStage){
        long module = -1;
        try(MemoryStack stack = stackPush())
        {
            long compiler = shaderc_compiler_initialize();
            long result = shaderc_compile_into_spv(compiler, source, vulkanStageToShadercKind(vulkanStage), "shader.glsl", "main", NULL);
            shaderc_compiler_release(compiler);

            if(shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success){
                throw new RuntimeException("Failed to compile shader: " + shaderc_result_get_error_message(result));
            }

            long resultBytes = shaderc_result_get_length(result);
            ByteBuffer spirvBinary = Shaderc.shaderc_result_get_bytes(result).slice(0, (int)resultBytes);

            module = createShaderModule(device, spirvBinary);

            shaderc_result_release(result);
        }

        return module;
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

    public int getBuffer(){
        return VERTEX_BUFFER_BIND_ID;
    }

    public long getPipeline(){
        return pipeline;
    }

    public void cleanup(){
        vkDestroyPipeline(device, pipeline, null);
        vi.free();
        vi_bindings.free();
        vi_attrs.free();
    }
}
