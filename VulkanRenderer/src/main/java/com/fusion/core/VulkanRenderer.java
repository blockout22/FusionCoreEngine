package com.fusion.core;

import com.fusion.core.engine.Debug;
import com.fusion.core.engine.renderer.Renderer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer extends Renderer {

    private static final boolean USE_STAGING_BUFFER = false;

    private static final int DEMO_TEXTURE_COUNT    = 1;
    private static final int VERTEX_BUFFER_BIND_ID = 0;

    private TextureObject[] textures = new TextureObject[DEMO_TEXTURE_COUNT];
//    private Vertices vertices = new Vertices();

    private GlfwWindow window;

    private VulkanInstance instance;
    private VkDevice device;
    private VkQueue queue;
    private VkCommandBuffer draw_cmd;
    private VkCommandBuffer setup_cmd;

    private Depth depth = new Depth();

    private int   width          = 300;
    private int   height         = 300;
    private float depthStencil   = 1.0f;
    private float depthIncrement = -0.01f;
    private int swapchainImageCount;
    private int format;
    private int color_space;
    private int current_buffer;
    private int graphics_queue_node_index;
    private long surface;
    private long swapchain;
    private long cmd_pool;
    private long desc_layout;
    private long pipeline_layout;
    private long render_pass;
    private long pipeline;
    private long desc_pool;
    private long desc_set;
    private long indexBuffer;

    private LongBuffer framebuffers;
//    private VulkanFramebuffer framebuffer;

    private VulkanShader vertexShader, fragmentShader;

    private SwapchainBuffers[] buffers;
    private final IntBuffer     ip = memAllocInt(1);
    private final LongBuffer lp = memAllocLong(1);
    private final PointerBuffer pp = memAllocPointer(1);
    public VkPipelineVertexInputStateCreateInfo vi = VkPipelineVertexInputStateCreateInfo.calloc();

    public VkVertexInputBindingDescription.Buffer   vi_bindings = VkVertexInputBindingDescription.calloc(1);
    public VkVertexInputAttributeDescription.Buffer vi_attrs    = VkVertexInputAttributeDescription.calloc(2);

    private ArrayList<VulkanMesh> meshList = new ArrayList<>();
    private VulkanMesh vulkanMesh, vulkanMesh2;

    public VulkanRenderer(GlfwWindow  window){
        this.window = window;
        for (int i = 0; i < textures.length; i++) {
            textures[i] = new TextureObject();
        }
    }

    @Override
    public void init() {
        Debug.logInfo("Creating Vulkan instance");
        instance = new VulkanInstance();
        createSwapChain();
        prepare();
    }

    private void createDevice(){
        try(MemoryStack stack = stackPush()){
            VkDeviceQueueCreateInfo.Buffer queue = VkDeviceQueueCreateInfo.malloc(1, stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(graphics_queue_node_index)
                    .pQueuePriorities(stack.floats(0.0f));

            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);
            if(instance.getGpuFeatures().shaderClipDistance()){
                features.shaderClipDistance(true);
            }
            instance.getExtensionNames().flip();
            VkDeviceCreateInfo device = VkDeviceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queue)
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(instance.getExtensionNames())
                    .pEnabledFeatures(features);

            VulkanUtils.check(vkCreateDevice(instance.getGpu(), device, null, pp));

            this.device = new VkDevice(pp.get(0), instance.getGpu(), device);
        }
    }

    private void createSwapChain(){
        glfwCreateWindowSurface(instance.get(), window.getWindowID(), null, lp);
        surface = lp.get(0);

        try(MemoryStack stack = stackPush()){
            IntBuffer supportsPresent = stack.mallocInt(instance.getQueueProps().capacity());
            int graphicsQueueNodeIndex;
            int presentQueueNodeIndex;
            for (int i = 0; i < supportsPresent.capacity(); i++) {
                supportsPresent.position(i);
                vkGetPhysicalDeviceSurfaceSupportKHR(instance.getGpu(), i, surface, supportsPresent);
            }

            graphicsQueueNodeIndex = Integer.MAX_VALUE;
            presentQueueNodeIndex = Integer.MAX_VALUE;
            for (int i = 0; i < supportsPresent.capacity(); i++) {
                if((instance.getQueueProps().get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0){
                    if (graphicsQueueNodeIndex == Integer.MAX_VALUE) {
                        graphicsQueueNodeIndex = i;
                    }

                    if(supportsPresent.get(i) == VK_TRUE){
                        graphicsQueueNodeIndex = i;
                        presentQueueNodeIndex = i;
                        break;
                    }
                }
            }
            if(presentQueueNodeIndex == Integer.MAX_VALUE){
                for (int i = 0; i < supportsPresent.capacity(); i++) {
                    if(supportsPresent.get(i) == VK_TRUE){
                        presentQueueNodeIndex = i;
                        break;
                    }
                }
            }

            if(graphicsQueueNodeIndex == Integer.MAX_VALUE || presentQueueNodeIndex == Integer.MAX_VALUE){
                throw new IllegalStateException("could not find a graphics and present queue");
            }

            if(graphicsQueueNodeIndex != presentQueueNodeIndex){
                throw new IllegalStateException("could not find a common graphics and present queue");
            }

            graphics_queue_node_index = graphicsQueueNodeIndex;

            createDevice();

            vkGetDeviceQueue(device, graphics_queue_node_index, 0, pp);
            queue = new VkQueue(pp.get(0), device);
            VulkanUtils.check(vkGetPhysicalDeviceSurfaceFormatsKHR(instance.getGpu(), surface, ip, null));

            VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.malloc(ip.get(0), stack);
            VulkanUtils.check(vkGetPhysicalDeviceSurfaceFormatsKHR(instance.getGpu(), surface, ip, surfFormats));

            if(ip.get(0) == 1 && surfFormats.get(0).format() == VK_FORMAT_UNDEFINED){
                format = VK_FORMAT_B8G8R8A8_UNORM;
            }else{
                assert ip.get(0) >= 1;
                format = surfFormats.get(0).format();
            }
            color_space = surfFormats.get(0).colorSpace();

            vkGetPhysicalDeviceMemoryProperties(instance.getGpu(), VulkanUtils.memory_properties);
        }

    }

    private void prepare(){
        try(MemoryStack stack = stackPush()){
            VkCommandPoolCreateInfo cmd_pool_info = VkCommandPoolCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(graphics_queue_node_index);

            VulkanUtils.check(vkCreateCommandPool(device, cmd_pool_info, null, lp));

            cmd_pool = lp.get(0);

            VkCommandBufferAllocateInfo cmd = VkCommandBufferAllocateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .commandPool(cmd_pool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            VulkanUtils.check(vkAllocateCommandBuffers(device, cmd, pp));
        }

        draw_cmd = new VkCommandBuffer(pp.get(0), device);

        prepareBuffer();
        prepareDepth();
        prepareTextures();
        prepareVertices();
        prepareDescriptorLayout();
        prepareRenderPass();
        vertexShader = new VulkanShader(device, new File("E:\\Github\\FusionCoreEngine\\Base\\Assets\\Vulkan\\Vert.glsl"), VK_SHADER_STAGE_VERTEX_BIT);
        fragmentShader = new VulkanShader(device, new File("E:\\Github\\FusionCoreEngine\\Base\\Assets\\Vulkan\\Frag.glsl"), VK_SHADER_STAGE_FRAGMENT_BIT);
        preparePipeline();

        prepareDescriptorPool();
        prepareDescriptorSet();

        prepareFrameBuffers();
//        framebuffer = new VulkanFramebuffer(device, width, height, render_pass);
    }

    private void prepareBuffer() {
        long oldSwapChain = swapchain;

        try(MemoryStack stack = stackPush()){
            VkSurfaceCapabilitiesKHR surfCapabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            VulkanUtils.check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(instance.getGpu(), surface, surfCapabilities));

            VulkanUtils.check(vkGetPhysicalDeviceSurfacePresentModesKHR(instance.getGpu(), surface, ip, null));

            IntBuffer presentModes = stack.mallocInt(ip.get(0));
            VulkanUtils.check(vkGetPhysicalDeviceSurfacePresentModesKHR(instance.getGpu(), surface, ip, presentModes));

            VkExtent2D swapchainExtent = VkExtent2D.malloc(stack);

            if(surfCapabilities.currentExtent().width() == 0xFFFFFFFF){
                swapchainExtent.width(width);
                swapchainExtent.height(height);

                if(swapchainExtent.width() < surfCapabilities.minImageExtent().width()){
                    swapchainExtent.width(surfCapabilities.minImageExtent().width());
                }else if (swapchainExtent.width() > surfCapabilities.maxImageExtent().width()) {
                    swapchainExtent.width(surfCapabilities.maxImageExtent().width());
                }

                if (swapchainExtent.height() < surfCapabilities.minImageExtent().height()) {
                    swapchainExtent.height(surfCapabilities.minImageExtent().height());
                } else if (swapchainExtent.height() > surfCapabilities.maxImageExtent().height()) {
                    swapchainExtent.height(surfCapabilities.maxImageExtent().height());
                }
            }else{
                swapchainExtent.set(surfCapabilities.currentExtent());
                width = surfCapabilities.currentExtent().width();
                height = surfCapabilities.currentExtent().height();
            }

            int swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR;

            int desiredNumOfSwapchainImages = surfCapabilities.minImageCount();

            if((surfCapabilities.maxImageCount() > 0) && (desiredNumOfSwapchainImages > surfCapabilities.maxImageCount())){
                desiredNumOfSwapchainImages = surfCapabilities.maxImageCount();
            }

            int preTransform;
            if((surfCapabilities.supportedTransforms() & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0){
                preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
            }else{
                preTransform = surfCapabilities.currentTransform();
            }

            VkSwapchainCreateInfoKHR swapchain = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType$Default()
                    .surface(surface)
                    .minImageCount(desiredNumOfSwapchainImages)
                    .imageFormat(format)
                    .imageColorSpace(color_space)
                    .imageExtent(swapchainExtent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(preTransform)
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(swapchainPresentMode)
                    .clipped(true)
                    .oldSwapchain(oldSwapChain);

            VulkanUtils.check(vkCreateSwapchainKHR(device, swapchain, null, lp));
            this.swapchain = lp.get(0);

            if(oldSwapChain != VK_NULL_HANDLE){
                vkDestroySwapchainKHR(device, oldSwapChain, null);
            }

            VulkanUtils.check(vkGetSwapchainImagesKHR(device, this.swapchain, ip, null));
            swapchainImageCount = ip.get(0);

            LongBuffer swapchainImages = stack.mallocLong(swapchainImageCount);
            VulkanUtils.check(vkGetSwapchainImagesKHR(device, this.swapchain, ip, swapchainImages));

            buffers = new SwapchainBuffers[swapchainImageCount];

            for (int i = 0; i < swapchainImageCount; i++) {
                buffers[i] = new SwapchainBuffers();
                buffers[i].image = swapchainImages.get(i);

                VkImageViewCreateInfo color_attachment_view = VkImageViewCreateInfo.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .image(buffers[i].image)
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(format)
                        .components(it -> it
                                .r(VK_COMPONENT_SWIZZLE_R)
                                .g(VK_COMPONENT_SWIZZLE_G)
                                .b(VK_COMPONENT_SWIZZLE_B)
                                .a(VK_COMPONENT_SWIZZLE_A))
                        .subresourceRange(it -> it
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0)
                                .levelCount(1)
                                .baseArrayLayer(0)
                                .layerCount(1));

                VulkanUtils.check(vkCreateImageView(device, color_attachment_view, null, lp));
                buffers[i].view = lp.get(0);
            }

            current_buffer = 0;
        }
    }

    private void prepareDepth(){
        depth.format = VK_FORMAT_D16_UNORM;

        try(MemoryStack stack = stackPush()){
            VkImageCreateInfo image = VkImageCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(depth.format)
                    .extent(it -> it
                            .width(width)
                            .height(height)
                            .depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);

            VulkanUtils.check(vkCreateImage(device, image, null, lp));
            depth.image = lp.get(0);

            VkMemoryRequirements mem_reqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, depth.image, mem_reqs);

            VkMemoryAllocateInfo mem_alloc = VkMemoryAllocateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .allocationSize(mem_reqs.size())
                    .memoryTypeIndex(0);

            boolean pass = VulkanUtils.memory_type_from_properties(mem_reqs.memoryTypeBits(), 0, mem_alloc);
            assert (pass);

            VulkanUtils.check(vkAllocateMemory(device, mem_alloc, null, lp));
            depth.mem = lp.get(0);

            VulkanUtils.check(vkBindImageMemory(device, depth.image, depth.mem, 0));

            demo_set_image_layout(depth.image, VK_IMAGE_ASPECT_DEPTH_BIT, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, 0);

            VkImageViewCreateInfo view = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .image(depth.image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(depth.format)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            VulkanUtils.check(vkCreateImageView(device, view, null, lp));
            depth.view = lp.get(0);

        }
    }

    private void prepareTextures(){
        int tex_format = VK_FORMAT_B8G8R8A8_UNORM;

        int[][] tex_colors = {{0xffff0000, 0xff00ff00}};

        try(MemoryStack stack = stackPush()){
            VkFormatProperties props = VkFormatProperties.malloc(stack);
            vkGetPhysicalDeviceFormatProperties(instance.getGpu(), tex_format, props);

            for (int i = 0; i < DEMO_TEXTURE_COUNT; i++) {
                if((props.linearTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) != 0 && !USE_STAGING_BUFFER){
                    demo_prepare_texture_image(
                            tex_colors[i], textures[i], VK_IMAGE_TILING_LINEAR,
                            VK_IMAGE_USAGE_SAMPLED_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
                }else if((props.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) != 0){
                    TextureObject staging_texture = new TextureObject();

                    demo_prepare_texture_image(
                            tex_colors[i], staging_texture, VK_IMAGE_TILING_LINEAR,
                            VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

                    demo_prepare_texture_image(
                            tex_colors[i], textures[i],
                            VK_IMAGE_TILING_OPTIMAL,
                            (VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT),
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

                    demo_set_image_layout(staging_texture.image,
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            staging_texture.imageLayout,
                            VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                            0);

                    demo_set_image_layout(textures[i].image,
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            textures[i].imageLayout,
                            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                            0);

                    VkImageCopy.Buffer copy_region = VkImageCopy.malloc(1, stack)
                            .srcSubresource(it -> it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                    .mipLevel(0)
                                    .baseArrayLayer(0)
                                    .layerCount(1))
                            .srcOffset(it -> it
                                    .x(0)
                                    .y(0)
                                    .z(0))
                            .dstSubresource(it -> it
                                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                    .mipLevel(0)
                                    .baseArrayLayer(0)
                                    .layerCount(1))
                            .dstOffset(it -> it
                                    .x(0)
                                    .y(0)
                                    .z(0))
                            .extent(it -> it
                                    .width(staging_texture.tex_width)
                                    .height(staging_texture.tex_height)
                                    .depth(1));

                    vkCmdCopyImage(setup_cmd, staging_texture.image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, textures[i].image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copy_region);

                    demo_set_image_layout(textures[i].image,
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                            textures[i].imageLayout,
                            0);

                    demo_flush_init_cmd();

                    demo_destroy_texture_image(staging_texture);
                }else{
                    throw new IllegalStateException("No support for B8G8R8A8_UNORM  as texture image format");
                }

                VkSamplerCreateInfo sampler = VkSamplerCreateInfo.calloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .magFilter(VK_FILTER_NEAREST)
                        .minFilter(VK_FILTER_NEAREST)
                        .mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
                        .addressModeU(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
                        .addressModeV(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
                        .addressModeW(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
                        .mipLodBias(0.0f)
                        .anisotropyEnable(false)
                        .maxAnisotropy(1)
                        .compareOp(VK_COMPARE_OP_NEVER)
                        .minLod(0.0f)
                        .maxLod(0.0f)
                        .borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE)
                        .unnormalizedCoordinates(false);

                VulkanUtils.check(vkCreateSampler(device, sampler, null, lp));
                textures[i].sampler = lp.get(0);

                VkImageViewCreateInfo view = VkImageViewCreateInfo.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .image(VK_NULL_HANDLE)
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(tex_format)
                        .flags(0)
                        .components(it -> it
                                .r(VK_COMPONENT_SWIZZLE_R)
                                .g(VK_COMPONENT_SWIZZLE_G)
                                .b(VK_COMPONENT_SWIZZLE_B)
                                .a(VK_COMPONENT_SWIZZLE_A))
                        .subresourceRange(it -> it.
                                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0)
                                .levelCount(1)
                                .baseArrayLayer(0)
                                .layerCount(1));

                view.image(textures[i].image);
                VulkanUtils.check(vkCreateImageView(device, view, null, lp));
                textures[i].view = lp.get(0);
            }
        }
    }

    private void prepareVertices(){
        // Vertex buffer 1
        float[] vertices1 = {
                /* position */
                -0.5f, -0.5f, 0.5f, // bottom left
                0.0f, -0.5f, 0.05f, // bottom right
                -0.8f, 0.0f, 0.1f, // top center
        };
        float[] texCoords1 = {
                /* texcoord */
                0.0f, 0.0f, // bottom left
                1.0f, 0.0f, // bottom right
                0.5f, 1.0f, // top center
        };
        int[] indices1 = {
                0, 1, 2
        };


// Vertex buffer 2
        float[] vertices2 = {
                /* position */
                0.1f, -0.5f, 0.05f,
                0.6f, -0.5f, 0.05f,
                0.35f, 0.0f, 0.1f,
        };
        float[] texCoords2 = {
                /* texcoord */
                0.0f, 0.0f,
                0.5f, 0.0f,
                0.25f, 0.5f,
        };

        float[][] vb2 = {
                /*      position            texcoord */
                {0.1f, -0.5f, 0.05f, 0.0f, 0.0f},  // Bottom left corner
                {0.6f, -0.5f, 0.05f, 1.0f, 0.0f},  // Bottom right corner
                {0.6f,  0.0f, 0.05f, 1.0f, 1.0f},  // Top right corner

                {0.1f, -0.5f, 0.05f, 0.0f, 0.0f},  // Bottom left corner
                {0.6f,  0.0f, 0.05f, 1.0f, 1.0f},  // Top right corner
                {0.1f,  0.0f, 0.05f, 0.0f, 1.0f},  // Top left corner
        };

        // Vertex buffer for square
        float[] squareVertices = {
                /* position */
                0.1f, -0.5f, 0.05f,  // Bottom left corner
                0.6f, -0.5f, 0.05f,  // Bottom right corner
                0.6f,  0.0f, 0.05f,  // Top right corner
                0.1f, -0.5f, 0.05f,  // Bottom left corner
                0.6f,  0.0f, 0.05f,  // Top right corner
                0.1f,  0.0f, 0.05f,  // Top left corner
        };
        float[] squareTexCoords = {
                /* texcoord */
                0.0f, 0.0f, // Bottom left corner
                1.0f, 0.0f, // Bottom right corner
                1.0f, 1.0f, // Top right corner
                0.0f, 0.0f, // Bottom left corner
                1.0f, 1.0f, // Top right corner
                0.0f, 1.0f, // Top left corner
        };
        int[] squareIndices = {
                0, 1, 2,    // First triangle
                3, 4, 5     // Second triangle
        };




        vulkanMesh = new VulkanMesh(device, vertices1, texCoords1, indices1);
        vulkanMesh2 = new VulkanMesh(device, squareVertices, squareTexCoords, squareIndices);

        meshList.add(vulkanMesh);
        meshList.add(vulkanMesh2);

        //sets up the pipeline for 3d rendering
        vi
                .sType$Default()
                .pNext(NULL)
                .pVertexBindingDescriptions(vi_bindings)
                .pVertexAttributeDescriptions(vi_attrs);
//
        System.out.println(vb2[0].length * 4);
        vi_bindings.get(0)
                .binding(VERTEX_BUFFER_BIND_ID)
                // Stride is calculated by the sum of the values of each attribute multiplied by the byte size.
                // For example, if you have vertices, normals, and texture coordinates, each taking 3, 3, and 2 values respectively,
                // the stride would be calculated as: (vertices + normals + texCoords) * byteSize = (3 + 3 + 2) * 4
                .stride((3 + 2) * 4)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        vi_attrs.get(0)
                .binding(VERTEX_BUFFER_BIND_ID)
                //sets the attribute location
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
    }

    private void prepareDescriptorLayout(){
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutCreateInfo descriptor_layout = VkDescriptorSetLayoutCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pBindings(
                            VkDescriptorSetLayoutBinding.calloc(1, stack)
                                    .binding(0)
                                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                                    .descriptorCount(DEMO_TEXTURE_COUNT)
                                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    );

            LongBuffer layouts = stack.mallocLong(1);
            VulkanUtils.check(vkCreateDescriptorSetLayout(device, descriptor_layout, null, layouts));
            desc_layout = layouts.get(0);

            VkPipelineLayoutCreateInfo pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .pSetLayouts(layouts);

            VulkanUtils.check(vkCreatePipelineLayout(device, pPipelineLayoutCreateInfo, null, lp));
            pipeline_layout = lp.get(0);
        }
    }

    private void prepareRenderPass(){
        try(MemoryStack stack = stackPush()){
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.malloc(2, stack);
            attachments.get(0)
                    .flags(0)
                    .format(format)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            attachments.get(1)
                    .flags(0)
                    .format(depth.format)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(
                            VkAttachmentReference.malloc(1, stack)
                                    .attachment(0)
                                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    )
                    .pDepthStencilAttachment(
                            VkAttachmentReference.malloc(stack)
                                    .attachment(1)
                                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                    );

            VkRenderPassCreateInfo rp_info = VkRenderPassCreateInfo.calloc(stack)
                    .sType$Default()
                    .pAttachments(attachments)
                    .pSubpasses(subpass);

            VulkanUtils.check(vkCreateRenderPass(device, rp_info, null, lp));
            render_pass = lp.get(0);
        }
    }

    private void preparePipeline(){
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
                    .module(vert_shader_module = vertexShader.get())//demo_prepare_shader_module(VulkanUtils.vertShaderCode))
                    .pName(main);
            shaderStages.get(1)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(frag_shader_module = fragmentShader.get())
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

    private void prepareDescriptorPool(){
        try(MemoryStack stack = stackPush()){
            VkDescriptorPoolCreateInfo descriptor_pool = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .maxSets(1)
                    .pPoolSizes(
                            VkDescriptorPoolSize.malloc(1, stack)
                                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                                    .descriptorCount(DEMO_TEXTURE_COUNT)
                    );

            VulkanUtils.check(vkCreateDescriptorPool(device, descriptor_pool, null, lp));
            desc_pool = lp.get(0);
        }
    }

    private void prepareDescriptorSet(){
        try(MemoryStack stack = stackPush()){
            LongBuffer layouts = stack.longs(desc_layout);
            VkDescriptorSetAllocateInfo alloc_info = VkDescriptorSetAllocateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .descriptorPool(desc_pool)
                    .pSetLayouts(layouts);

            VulkanUtils.check(vkAllocateDescriptorSets(device, alloc_info, lp));
            desc_set = lp.get(0);

            VkDescriptorImageInfo.Buffer tex_descs = VkDescriptorImageInfo.calloc(DEMO_TEXTURE_COUNT, stack);

            for (int i = 0; i < DEMO_TEXTURE_COUNT; i++) {
                tex_descs.get(i)
                        .sampler(textures[i].sampler)
                        .imageView(textures[i].view)
                        .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }

            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack)
                    .sType$Default()
                    .dstSet(desc_set)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(tex_descs.remaining())
                    .pImageInfo(tex_descs);

            vkUpdateDescriptorSets(device, write, null);
        }
    }

    private void prepareFrameBuffers(){
        try(MemoryStack stack = stackPush()){
            LongBuffer attachments = stack.longs(0, depth.view);

            VkFramebufferCreateInfo fb_info = VkFramebufferCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .renderPass(render_pass)
                    .pAttachments(attachments)
                    .width(width)
                    .height(height)
                    .layers(1);

            framebuffers = memAllocLong(swapchainImageCount);

            for (int i = 0; i < swapchainImageCount; i++) {
                attachments.put(0, buffers[i].view);
                VulkanUtils.check(vkCreateFramebuffer(device, fb_info, null, lp));
                framebuffers.put(i, lp.get(0));
            }
        }
    }

//    private long demo_prepare_shader_module(byte[] code) {
//        try (MemoryStack stack = stackPush()) {
//            ByteBuffer pCode = memAlloc(code.length).put(code);
//            pCode.flip();
//
//            VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.malloc(stack)
//                    .sType$Default()
//                    .pNext(NULL)
//                    .flags(0)
//                    .pCode(pCode);
//
//            VulkanUtils.check(vkCreateShaderModule(device, moduleCreateInfo, null, lp));
//
//            memFree(pCode);
//
//            return lp.get(0);
//        }
//    }

    private void demo_destroy_texture_image(TextureObject tex_obj) {
        /* clean up staging resources */
        vkDestroyImage(device, tex_obj.image, null);
        vkFreeMemory(device, tex_obj.mem, null);
    }

    private void demo_flush_init_cmd() {
        if (setup_cmd == null) {
            return;
        }

        VulkanUtils.check(vkEndCommandBuffer(setup_cmd));

        try (MemoryStack stack = stackPush()) {
            VkSubmitInfo submit_info = VkSubmitInfo.calloc(stack)
                    .sType$Default()
                    .pCommandBuffers(pp.put(0, setup_cmd));

            VulkanUtils.check(vkQueueSubmit(queue, submit_info, VK_NULL_HANDLE));
        }

        VulkanUtils.check(vkQueueWaitIdle(queue));

        vkFreeCommandBuffers(device, cmd_pool, pp);
        setup_cmd = null;
    }

    private void demo_prepare_texture_image(
            int[] tex_colors,
            TextureObject tex_obj, int tiling,
            int usage, int required_props
    ) {
        int tex_format = VK_FORMAT_B8G8R8A8_UNORM;

        int tex_width  = 2;
        int tex_height = 2;

        boolean pass;

        tex_obj.tex_width = tex_width;
        tex_obj.tex_height = tex_height;

        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo image_create_info = VkImageCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(tex_format)
                    .extent(it -> it
                            .width(tex_width)
                            .height(tex_height)
                            .depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(tiling)
                    .usage(usage)
                    .flags(0)
                    .initialLayout(VK_IMAGE_LAYOUT_PREINITIALIZED);

            VulkanUtils.check(vkCreateImage(device, image_create_info, null, lp));
            tex_obj.image = lp.get(0);

            VkMemoryRequirements mem_reqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, tex_obj.image, mem_reqs);
            VkMemoryAllocateInfo mem_alloc = VkMemoryAllocateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .allocationSize(mem_reqs.size())
                    .memoryTypeIndex(0);
            pass = VulkanUtils.memory_type_from_properties(mem_reqs.memoryTypeBits(), required_props, mem_alloc);
            assert (pass);

            /* allocate memory */
            VulkanUtils.check(vkAllocateMemory(device, mem_alloc, null, lp));
            tex_obj.mem = lp.get(0);

            /* bind memory */
            VulkanUtils.check(vkBindImageMemory(device, tex_obj.image, tex_obj.mem, 0));

            if ((required_props & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
                VkImageSubresource subres = VkImageSubresource.malloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(0)
                        .arrayLayer(0);

                VkSubresourceLayout layout = VkSubresourceLayout.malloc(stack);
                vkGetImageSubresourceLayout(device, tex_obj.image, subres, layout);

                VulkanUtils.check(vkMapMemory(device, tex_obj.mem, 0, mem_alloc.allocationSize(), 0, pp));

                for (int y = 0; y < tex_height; y++) {
                    IntBuffer row = memIntBuffer(pp.get(0) + layout.rowPitch() * y, tex_width);
                    for (int x = 0; x < tex_width; x++) {
                        row.put(x, tex_colors[(x & 1) ^ (y & 1)]);
                    }
                }

                vkUnmapMemory(device, tex_obj.mem);
            }

            tex_obj.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            demo_set_image_layout(tex_obj.image, VK_IMAGE_ASPECT_COLOR_BIT, VK_IMAGE_LAYOUT_PREINITIALIZED, tex_obj.imageLayout, VK_ACCESS_HOST_WRITE_BIT);
            /* setting the image layout does not reference the actual memory so no need
             * to add a mem ref */
        }
    }

    private void demo_set_image_layout(long image, int aspectMask, int old_image_layout, int new_image_layout, int srcAccessMask) {
        try (MemoryStack stack = stackPush()) {
            if (setup_cmd == null) {
                VkCommandBufferAllocateInfo cmd = VkCommandBufferAllocateInfo.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .commandPool(cmd_pool)
                        .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                        .commandBufferCount(1);

                VulkanUtils.check(vkAllocateCommandBuffers(device, cmd, pp));
                setup_cmd = new VkCommandBuffer(pp.get(0), device);

                VkCommandBufferBeginInfo cmd_buf_info = VkCommandBufferBeginInfo.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .pInheritanceInfo(null);
                VulkanUtils.check(vkBeginCommandBuffer(setup_cmd, cmd_buf_info));
            }

            VkImageMemoryBarrier.Buffer image_memory_barrier = VkImageMemoryBarrier.malloc(1, stack)
                    .sType$Default()
                    .pNext(NULL)
                    .srcAccessMask(srcAccessMask)
                    .dstAccessMask(0)
                    .oldLayout(old_image_layout)
                    .newLayout(new_image_layout)
                    .srcQueueFamilyIndex(0)
                    .dstQueueFamilyIndex(0)
                    .image(image)
                    .subresourceRange(it -> it
                            .aspectMask(aspectMask)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            int src_stages  = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            int dest_stages = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;

            if (srcAccessMask == VK_ACCESS_HOST_WRITE_BIT) {
                src_stages = VK_PIPELINE_STAGE_HOST_BIT;
            }

            switch (new_image_layout) {
                case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL:
                    image_memory_barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                    break;
                case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL:
                    image_memory_barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                    dest_stages = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
                    break;
                case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL:
                    /* Make sure any Copy or CPU writes to image are flushed */
                    image_memory_barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_INPUT_ATTACHMENT_READ_BIT);
                    dest_stages = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
                    break;
                case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
                    image_memory_barrier.srcAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                    /* Make sure anything that was copying from this image has completed */
                    image_memory_barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                    dest_stages = VK_PIPELINE_STAGE_TRANSFER_BIT;
                    break;
                case VK_IMAGE_LAYOUT_PRESENT_SRC_KHR:
                    image_memory_barrier.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                    break;
            }


            vkCmdPipelineBarrier(setup_cmd, src_stages, dest_stages, 0, null, null, image_memory_barrier);
        }
    }


    public void demoDraw(){
        try(MemoryStack stack = stackPush()){
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0);

            VulkanUtils.check(vkCreateSemaphore(device, semaphoreCreateInfo, null, lp));
            long imageAcquiredSemaphore = lp.get(0);

            VulkanUtils.check(vkCreateSemaphore(device, semaphoreCreateInfo, null, lp));
            long drawCompleteSemaphore = lp.get(0);

            int err = vkAcquireNextImageKHR(device, swapchain, ~0L,
                    imageAcquiredSemaphore,
                    NULL, // TODO: Show use of fence
                    ip);

            if(err == VK_ERROR_OUT_OF_DATE_KHR){
                demoDraw();
                vkDestroySemaphore(device, drawCompleteSemaphore, null);
                vkDestroySemaphore(device, imageAcquiredSemaphore, null);
                return;
            }else if(err == VK_SUBOPTIMAL_KHR){

            }else{
                VulkanUtils.check(err);
            }
            current_buffer = ip.get(0);

            demo_flush_init_cmd();

            draw_build_cmd();
            LongBuffer lp2 = stack.mallocLong(1);
            VkSubmitInfo submitInfo = VkSubmitInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(lp.put(0, imageAcquiredSemaphore))
                    .pWaitDstStageMask(ip.put(0, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT))
                    .pCommandBuffers(pp.put(0, draw_cmd))
                    .pSignalSemaphores(lp2.put(0, drawCompleteSemaphore));

            VulkanUtils.check(vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE));

            VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .pWaitSemaphores(lp2)
                    .swapchainCount(1)
                    .pSwapchains(lp.put(0, swapchain))
                    .pImageIndices(ip.put(0, current_buffer));

            err = vkQueuePresentKHR(queue, present);
            if(err == VK_ERROR_OUT_OF_DATE_KHR){

            }else if(err == VK_SUBOPTIMAL_KHR){

            }else{
                VulkanUtils.check(err);
            }

            VulkanUtils.check(vkQueueWaitIdle(queue));

            vkDestroySemaphore(device, drawCompleteSemaphore, null);
            vkDestroySemaphore(device, imageAcquiredSemaphore, null);
        }
    }

    private void draw_build_cmd(){
        try(MemoryStack stack = stackPush()){
            VkCommandBufferBeginInfo cmd_buf_info = VkCommandBufferBeginInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pInheritanceInfo(null);

            VulkanUtils.check(vkBeginCommandBuffer(draw_cmd, cmd_buf_info));

            //sets the clear color of the screen
            VkClearValue.Buffer clear_values = VkClearValue.malloc(2, stack);
            clear_values.get(0).color()
                    .float32(0, 0.2f)
                    .float32(1, 0.2f)
                    .float32(2, 0.2f)
                    .float32(3, 0.2f);
            clear_values.get(1).depthStencil()
                    .depth(depthStencil)
                    .stencil(0);

            VkRenderPassBeginInfo rp_begin = VkRenderPassBeginInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .renderPass(render_pass)
                    .framebuffer(framebuffers.get(current_buffer))
                    .renderArea(ra -> ra
                            .offset(it -> it
                                    .x(0)
                                    .y(0))
                            .extent(it -> it
                                    .width(width)
                                    .height(height)))
                    .pClearValues(clear_values);

            VkImageMemoryBarrier.Buffer image_memory_barrier = VkImageMemoryBarrier.malloc(1, stack)
                    .sType$Default()
                    .pNext(NULL)
                    .srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(buffers[current_buffer].image)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            vkCmdPipelineBarrier(draw_cmd, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, image_memory_barrier);
            vkCmdBeginRenderPass(draw_cmd, rp_begin, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(draw_cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

            lp.put(0, desc_set);
            vkCmdBindDescriptorSets(draw_cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout, 0, lp, null);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .height(height)
                    .width(width)
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            vkCmdSetViewport(draw_cmd, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .extent(it -> it
                            .width(width)
                            .height(height))
                    .offset(it -> it
                            .x(0)
                            .y(0));
            vkCmdSetScissor(draw_cmd, 0, scissor);

            lp.put(0, 0);
            {
//            LongBuffer pBuffers = stack.longs(vertices.buf);
                for (int i = 0; i < meshList.size(); i++) {
                    VulkanMesh mesh = meshList.get(i);
                    LongBuffer pBuffers = stack.longs(mesh.vertexBuffer);
                    vkCmdBindVertexBuffers(draw_cmd, VERTEX_BUFFER_BIND_ID, pBuffers, lp);
                    //draw without indices
//                    vkCmdDraw(draw_cmd, mesh.verticesCount, 1, 0, 0);

                    //draw with indices
                    vkCmdBindIndexBuffer(draw_cmd, mesh.indexBuffer, 0, VK_INDEX_TYPE_UINT32);
                    vkCmdDrawIndexed(draw_cmd, mesh.indices.length, 1, 0, 0, 0);
                }
//                LongBuffer pBuffers = stack.longs(vulkanMesh.buf);
//                vkCmdBindVertexBuffers(draw_cmd, VERTEX_BUFFER_BIND_ID, pBuffers, lp);
//
//                vkCmdDraw(draw_cmd, vulkanMesh.verticesCount, 1, 0, 0);
//
//                pBuffers = stack.longs(vulkanMesh2.buf);
//                vkCmdBindVertexBuffers(draw_cmd, VERTEX_BUFFER_BIND_ID, pBuffers, lp);
//
//                vkCmdDraw(draw_cmd, vulkanMesh2.verticesCount, 1, 0, 0);
            }

            vkCmdEndRenderPass(draw_cmd);

            VkImageMemoryBarrier.Buffer prePresentBarrier = VkImageMemoryBarrier.malloc(1, stack)
                    .sType$Default()
                    .pNext(NULL)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                    .oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(buffers[current_buffer].image)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            vkCmdPipelineBarrier(draw_cmd, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, null, null, prePresentBarrier);

            VulkanUtils.check(vkEndCommandBuffer(draw_cmd));
        }
    }

    @Override
    public void update() {
        demoDraw();
    }

    @Override
    public void close() {
        //destroy order matters, e.g. you can't destroy the device if the argument takes in a device

        for (int i = 0; i < swapchainImageCount; i++) {
            vkDestroyFramebuffer(device, framebuffers.get(i), null);
        }
        memFree(framebuffers);
//        framebuffer.cleanup();
        vkDestroyDescriptorPool(device, desc_pool, null);

        if (setup_cmd != null) {
            vkFreeCommandBuffers(device, cmd_pool, setup_cmd);
            setup_cmd = null;
        }

        vkFreeCommandBuffers(device, cmd_pool, draw_cmd);
        vkDestroyCommandPool(device, cmd_pool, null);

        vkDestroyPipeline(device, pipeline, null);
        vkDestroyRenderPass(device, render_pass, null);
        vkDestroyPipelineLayout(device, pipeline_layout, null);
        vkDestroyDescriptorSetLayout(device, desc_layout, null);

//        vkDestroyBuffer(device, vertices.buf, null);
//        vkFreeMemory(device, vertices.mem, null);
        vi.free();
        vi_bindings.free();
        vi_attrs.free();
        vulkanMesh.cleanup();
        vulkanMesh2.cleanup();

        for (int i = 0; i < DEMO_TEXTURE_COUNT; i++) {
            vkDestroyImageView(device, textures[i].view, null);
            vkDestroyImage(device, textures[i].image, null);
            vkFreeMemory(device, textures[i].mem, null);
            vkDestroySampler(device, textures[i].sampler, null);
        }

        for (int i = 0; i < swapchainImageCount; i++) {
            vkDestroyImageView(device, buffers[i].view, null);
        }

        vkDestroyImageView(device, depth.view, null);
        vkDestroyImage(device, depth.image, null);
        vkFreeMemory(device, depth.mem, null);

        vkDestroySwapchainKHR(device, swapchain, null);

//        if(msg_callback != NULL){
//            vkDestroyDebugUtilsMessengerEXT(instance, msg_callback, null);
//        }
        vkDestroyDevice(device, null);
        vkDestroySurfaceKHR(instance.get(), surface, null);
//        vkDestroyInstance(instance, null);
        instance.cleanup();
//        dbgFunc.free();

//        gpu_features.free();
//        gpu_props.free();
//        queue_props.free();

//        memFree(extension_names);

        memFree(pp);
        memFree(lp);
        memFree(ip);

//        memFree(EXT_debug_utils);
//        memFree(KHR_swapchain);
    }

    private static class SwapchainBuffers {
        long            image;
        VkCommandBuffer cmd;
        long            view;
    }

    private static class Depth {
        int format;

        long image;
        long mem;
        long view;
    }

    private static class TextureObject {
        long sampler;

        long image;
        int  imageLayout;

        long mem;
        long view;
        int  tex_width, tex_height;
    }

//    private static class Vertices {
//        long buf;
//        long mem;
//
//        VkPipelineVertexInputStateCreateInfo     vi          = VkPipelineVertexInputStateCreateInfo.calloc();
//        VkVertexInputBindingDescription.Buffer   vi_bindings = VkVertexInputBindingDescription.calloc(1);
//        VkVertexInputAttributeDescription.Buffer vi_attrs    = VkVertexInputAttributeDescription.calloc(2);
//    }

    @Override
    public boolean isOpenGL() {
        return false;
    }
}
