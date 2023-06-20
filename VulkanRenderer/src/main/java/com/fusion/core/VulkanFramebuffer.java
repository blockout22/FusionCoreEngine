package com.fusion.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;

public class VulkanFramebuffer {

    private LongBuffer framebuffers;
    private final LongBuffer lp = memAllocLong(1);

    private long view;


    public VulkanFramebuffer(VkDevice device, int width, int height, long render_pass) {
        try(MemoryStack stack = stackPush()){
            LongBuffer attachments = stack.longs(0, view);

            VkFramebufferCreateInfo fb_info = VkFramebufferCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .renderPass(render_pass)
                    .pAttachments(attachments)
                    .width(width)
                    .height(height)
                    .layers(1);

            framebuffers = memAllocLong(0/*swapchainImageCount*/);

            for (int i = 0; i < 1/*swapchainImageCount*/; i++) {
                attachments.put(0, null/*buffers[i].view*/);
                VulkanUtils.check(vkCreateFramebuffer(device, fb_info, null, lp));
                framebuffers.put(i, lp.get(0));
            }
        }
    }

    public long get(int i){
        return framebuffers.get(i);
    }

    public long getView(){
        return view;
    }

    public void cleanup()
    {
        for (int i = 0; i < 0/*swapchainImageCount*/; i++) {
            vkDestroyFramebuffer(null/*device*/, framebuffers.get(i), null);
        }
        memFree(framebuffers);
    }
}
