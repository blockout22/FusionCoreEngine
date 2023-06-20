package com.fusion.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanMesh {

    public VkDevice device;
    public long vertexBuffer;
    public long indexBuffer;
    private long mem;
    private long indexMem;

    private final LongBuffer lp = memAllocLong(1);
    private final PointerBuffer pp = memAllocPointer(1);

    public int verticesCount = 0;

    private VulkanModel model;

    public VulkanMesh(VkDevice device, VulkanModel model) {
        this.device = device;
        this.model = model;
        verticesCount = model.getVertices().length / 3;

        try(MemoryStack stack = stackPush()){
            VkBufferCreateInfo buf_info = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
//                    .size(vb.length * vb[0].length * 4)
                    .size(model.getVertices().length * model.getTexCoords().length * 4)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VulkanUtils.check(vkCreateBuffer(device, buf_info, null, lp));
            vertexBuffer = lp.get(0);

            VkMemoryRequirements mem_reqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, vertexBuffer, mem_reqs);

            VkMemoryAllocateInfo mem_alloc = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default()
                    .allocationSize(mem_reqs.size());
            boolean pass = VulkanUtils.memory_type_from_properties(mem_reqs.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, mem_alloc);
            assert (pass);

            VulkanUtils.check(vkAllocateMemory(device, mem_alloc, null, lp));
            mem = lp.get(0);

            VulkanUtils.check(vkMapMemory(device, mem, 0, mem_alloc.allocationSize(), 0, pp));
            FloatBuffer data = pp.getFloatBuffer(0, ((int)mem_alloc.allocationSize()) >> 2);
            for(int i = 0; i < model.getVertices().length / 3; i++) {
                // Add vertex
                data.put(model.getVertices(), i * 3, 3);

                // Add texture coordinate
                data.put(model.getTexCoords(), i * 2, 2);
            }
            data.flip();
        }

        vkUnmapMemory(device, mem);

        VulkanUtils.check(vkBindBufferMemory(device, vertexBuffer, mem, 0));

        try(MemoryStack stack = stackPush()){
            VkBufferCreateInfo indexBuffInfo = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .size(model.getIndices().length * Integer.BYTES)
                    .usage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VulkanUtils.check(vkCreateBuffer(device, indexBuffInfo, null, lp));
            indexBuffer = lp.get(0);

            VkMemoryRequirements mem_reqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, indexBuffer, mem_reqs);

            VkMemoryAllocateInfo mem_alloc = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default()
                    .allocationSize(mem_reqs.size());

            boolean pass = VulkanUtils.memory_type_from_properties(mem_reqs.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, mem_alloc);
            assert (pass);

            VulkanUtils.check(vkAllocateMemory(device, mem_alloc, null, lp));
            indexMem = lp.get(0);

            VulkanUtils.check(vkMapMemory(device, indexMem, 0, mem_alloc.allocationSize(), 0, pp));
            IntBuffer data = pp.getIntBuffer(0, model.getIndices().length);
            data.put(model.getIndices()).flip();

            vkUnmapMemory(device, indexMem);

            VulkanUtils.check(vkBindBufferMemory(device, indexBuffer, indexMem, 0));
        }
    }

    public VulkanModel getModel(){
        return model;
    }

    public void cleanup()
    {
        vkDestroyBuffer(device, vertexBuffer, null);
        vkDestroyBuffer(device, indexBuffer, null);
        vkFreeMemory(device, mem, null);
        vkFreeMemory(device, indexMem, null);

        memFree(pp);
        memFree(lp);
    }
}
