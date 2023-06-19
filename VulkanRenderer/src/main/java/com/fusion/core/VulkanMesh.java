package com.fusion.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanMesh {

    public VkDevice device;
    public long buf;
    public long mem;

    private final LongBuffer lp = memAllocLong(1);
    private final PointerBuffer pp = memAllocPointer(1);


    public VulkanMesh(VkDevice device, float[] vertices, float[] texCoords) {
        this.device = device;

        try(MemoryStack stack = stackPush()){
            VkBufferCreateInfo buf_info = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
//                    .size(vb.length * vb[0].length * 4)
                    .size(vertices.length * texCoords.length * 4)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VulkanUtils.check(vkCreateBuffer(device, buf_info, null, lp));
            buf = lp.get(0);

            VkMemoryRequirements mem_reqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, buf, mem_reqs);

            VkMemoryAllocateInfo mem_alloc = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default()
                    .allocationSize(mem_reqs.size());
            boolean pass = VulkanUtils.memory_type_from_properties(mem_reqs.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, mem_alloc);
            assert (pass);

            VulkanUtils.check(vkAllocateMemory(device, mem_alloc, null, lp));
            mem = lp.get(0);

            VulkanUtils.check(vkMapMemory(device, mem, 0, mem_alloc.allocationSize(), 0, pp));
            FloatBuffer data = pp.getFloatBuffer(0, ((int)mem_alloc.allocationSize()) >> 2);
            for(int i = 0; i < vertices.length / 3; i++) {
                // Add vertex
                data.put(vertices, i * 3, 3);

                // Add texture coordinate
                data.put(texCoords, i * 2, 2);
            }
            data.flip();
//            data.put(vb[0]).put(vb[1]).put(vb[2]).flip();
        }

        vkUnmapMemory(device, mem);

        VulkanUtils.check(vkBindBufferMemory(device, buf, mem, 0));
    }

    public void cleanup()
    {
        vkDestroyBuffer(device, buf, null);
        vkFreeMemory(device, mem, null);

        memFree(pp);
        memFree(lp);
    }
}
