package com.fusion.core;

import com.fusion.core.engine.Debug;
import com.fusion.core.engine.renderer.Renderer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer extends Renderer {
    private static final boolean VALIDATE = true;

    private VkInstance instance;
    private VkPhysicalDevice gpu;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;

    private final IntBuffer     ip = memAllocInt(1);
    private final LongBuffer lp = memAllocLong(1);
    private final PointerBuffer pp = memAllocPointer(1);
    private PointerBuffer extension_names = memAllocPointer(64);

    private VkQueueFamilyProperties.Buffer queue_props;

    private VkPhysicalDeviceProperties gpu_props    = VkPhysicalDeviceProperties.malloc();
    private VkPhysicalDeviceFeatures   gpu_features = VkPhysicalDeviceFeatures.malloc();

    private final ByteBuffer EXT_debug_utils = memASCII(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    private final ByteBuffer KHR_swapchain   = memASCII(VK_KHR_SWAPCHAIN_EXTENSION_NAME);

    private long msg_callback;

    @Override
    public void init() {
        Debug.logInfo("Creating Vulkan instance");
        craeteInstance();
    }

    private final VkDebugUtilsMessengerCallbackEXT dbgFunc = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                String severity;
                if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
                    severity = "VERBOSE";
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                    severity = "INFO";
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                    severity = "WARNING";
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                    severity = "ERROR";
                } else {
                    severity = "UNKNOWN";
                }

                String type;
                if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
                    type = "GENERAL";
                } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
                    type = "VALIDATION";
                } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
                    type = "PERFORMANCE";
                } else {
                    type = "UNKNOWN";
                }

                VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                System.err.format(
                        "%s %s: [%s]\n\t%s\n",
                        type, severity, data.pMessageIdNameString(), data.pMessageString()

                );

                /*
                 * false indicates that layer should not bail-out of an
                 * API call that had validation failures. This may mean that the
                 * app dies inside the driver due to invalid parameter(s).
                 * That's what would happen without validation layers, so we'll
                 * keep that behavior here.
                 */
                return VK_FALSE;
            }
    );

    private static void check(int errcode) {
        if (errcode != 0) {
            throw new IllegalStateException(String.format("Vulkan error [0x%X]", errcode));
        }
    }

    private void craeteInstance() {
        try(MemoryStack stack = stackPush()) {
            PointerBuffer requiredLayers = null;


            check(vkEnumerateInstanceLayerProperties(ip, null));

            if(VALIDATE) {
                VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(ip.get(0), stack);
                check(vkEnumerateInstanceLayerProperties(ip, availableLayers));
                if (ip.get(0) > 0) {

                    requiredLayers = demo_check_layers(
                            stack, availableLayers,
                            "VK_LAYER_KHRONOS_validation"/*,
                        "VK_LAYER_LUNARG_assistant_layer"*/
                    );

                    if (requiredLayers == null) { // use alternative (deprecated) set of validation layers
                        requiredLayers = demo_check_layers(
                                stack, availableLayers,
                                "VK_LAYER_LUNARG_standard_validation"/*,
                            "VK_LAYER_LUNARG_assistant_layer"*/
                        );
                    }
                    if (requiredLayers == null) { // use alternative (deprecated) set of validation layers
                        requiredLayers = demo_check_layers(
                                stack, availableLayers,
                                "VK_LAYER_GOOGLE_threading",
                                "VK_LAYER_LUNARG_parameter_validation",
                                "VK_LAYER_LUNARG_object_tracker",
                                "VK_LAYER_LUNARG_core_validation",
                                "VK_LAYER_GOOGLE_unique_objects"/*,
                            "VK_LAYER_LUNARG_assistant_layer"*/
                        );
                    }

                }
                if (requiredLayers == null) {
                    throw new IllegalStateException("vkEnumerateInstanceLayerProperties failed to find required validation layer.");
                }
            }

            PointerBuffer required_extensions = glfwGetRequiredInstanceExtensions();
            if (required_extensions == null) {
                throw new IllegalStateException("glfwGetRequiredInstanceExtensions failed to find the platform surface extensions.");
            }

            for (int i = 0; i < required_extensions.capacity(); i++) {
                extension_names.put(required_extensions.get(i));
            }

            check(vkEnumerateInstanceExtensionProperties((String)null, ip, null));

            if (ip.get(0) != 0) {
                VkExtensionProperties.Buffer instance_extensions = VkExtensionProperties.malloc(ip.get(0), stack);
                check(vkEnumerateInstanceExtensionProperties((String)null, ip, instance_extensions));

                for (int i = 0; i < ip.get(0); i++) {
                    instance_extensions.position(i);
                    if (VK_EXT_DEBUG_UTILS_EXTENSION_NAME.equals(instance_extensions.extensionNameString())) {
                        if (VALIDATE) {
                            extension_names.put(EXT_debug_utils);
                        }
                    }
                }
            }

            ByteBuffer APP_SHORT_NAME = stack.UTF8("tri");

            VkApplicationInfo appInfo = VkApplicationInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .pApplicationName(APP_SHORT_NAME)
                    .applicationVersion(0)
                    .pEngineName(APP_SHORT_NAME)
                    .engineVersion(0)
                    .apiVersion(VK.getInstanceVersionSupported());

            Debug.logInfo("Created app info");
            extension_names.flip();

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pApplicationInfo(appInfo)
                    .ppEnabledLayerNames(requiredLayers)
                    .ppEnabledExtensionNames(extension_names);
            extension_names.clear();

            Debug.logInfo("Create CreateInfo");

            VkDebugUtilsMessengerCreateInfoEXT dbgCreateInfo;
            if (VALIDATE) {
                dbgCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .messageSeverity(
                        /*VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |*/
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        )
                        .messageType(
                                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                        )
                        .pfnUserCallback(dbgFunc)
                        .pUserData(NULL);

                createInfo.pNext(dbgCreateInfo.address());
            }

             Debug.logInfo("Create Pointer Info");
             int err = vkCreateInstance(createInfo, null, pp);
            if (err == VK_ERROR_INCOMPATIBLE_DRIVER) {
                throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD).");
            } else if (err == VK_ERROR_EXTENSION_NOT_PRESENT) {
                throw new IllegalStateException("Cannot find a specified extension library. Make sure your layers path is set appropriately.");
            } else if (err != 0) {
                throw new IllegalStateException("vkCreateInstance failed. Do you have a compatible Vulkan installable client driver (ICD) installed?");
            }

            Debug.logInfo("Create Instance");

            instance = new VkInstance(pp.get(0), createInfo);
            Debug.logInfo("Assigned instance");

            check(vkEnumeratePhysicalDevices(instance, ip, null));

            if (ip.get(0) > 0) {
                PointerBuffer physical_devices = stack.mallocPointer(ip.get(0));
                check(vkEnumeratePhysicalDevices(instance, ip, physical_devices));

                /* For demo we just grab the first physical device */
                gpu = new VkPhysicalDevice(physical_devices.get(0), instance);
            } else {
                throw new IllegalStateException("vkEnumeratePhysicalDevices reported zero accessible devices.");
            }

            boolean swapchainExtFound = false;
            check(vkEnumerateDeviceExtensionProperties(gpu, (String)null, ip, null));

            if (ip.get(0) > 0) {
                VkExtensionProperties.Buffer device_extensions = VkExtensionProperties.malloc(ip.get(0), stack);
                check(vkEnumerateDeviceExtensionProperties(gpu, (String)null, ip, device_extensions));

                for (int i = 0; i < ip.get(0); i++) {
                    device_extensions.position(i);
                    if (VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(device_extensions.extensionNameString())) {
                        swapchainExtFound = true;
                        extension_names.put(KHR_swapchain);
                    }
                }
            }

            if (!swapchainExtFound) {
                throw new IllegalStateException("vkEnumerateDeviceExtensionProperties failed to find the " + VK_KHR_SWAPCHAIN_EXTENSION_NAME + " extension.");
            }

            if (VALIDATE) {
                err = vkCreateDebugUtilsMessengerEXT(instance, dbgCreateInfo, null, lp);
                switch (err) {
                    case VK_SUCCESS:
                        msg_callback = lp.get(0);
                        break;
                    case VK_ERROR_OUT_OF_HOST_MEMORY:
                        throw new IllegalStateException("CreateDebugReportCallback: out of host memory");
                    default:
                        throw new IllegalStateException("CreateDebugReportCallback: unknown failure");
                }
            }

            vkGetPhysicalDeviceProperties(gpu, gpu_props);

            vkGetPhysicalDeviceQueueFamilyProperties(gpu, ip, null);

            queue_props = VkQueueFamilyProperties.malloc(ip.get(0));
            vkGetPhysicalDeviceQueueFamilyProperties(gpu, ip, queue_props);
            if (ip.get(0) == 0) {
                throw new IllegalStateException();
            }

            vkGetPhysicalDeviceFeatures(gpu, gpu_features);

            Debug.logInfo("Vendor ID: " + VulkanUtils.vendorIdToString(gpu_props.vendorID()));
            Debug.logInfo("Device Name: " + gpu_props.deviceNameString());
            Debug.logInfo("Device Type: " + VulkanUtils.deviceTypeToString(gpu_props.deviceType()));
            Debug.logInfo("Vulkan Driver Version: " + VulkanUtils.driverVersionToString(gpu_props.driverVersion(), gpu_props.vendorID()));
            Debug.logInfo("Vulkan API Version: " + VulkanUtils.apiVersionToString(gpu_props.apiVersion()));
            Debug.logInfo("Device ID: " + gpu_props.deviceID());

        }
    }

    private static PointerBuffer demo_check_layers(MemoryStack stack, VkLayerProperties.Buffer available, String... layers) {
        PointerBuffer required = stack.mallocPointer(layers.length);
        for (int i = 0; i < layers.length; i++) {
            boolean found = false;

            for (int j = 0; j < available.capacity(); j++) {
                available.position(j);
                if (layers[i].equals(available.layerNameString())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.err.format("Cannot find layer: %s\n", layers[i]);
                return null;
            }

            required.put(i, stack.ASCII(layers[i]));
        }

        return required;
    }

//    private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
//        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
//        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
//        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
//        debugCreateInfo.pfnUserCallback(VulkanRenderer::debugCallback);
//    }

//    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
//
//        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
//
//        System.err.println("Validation layer: " + callbackData.pMessageString());
//
//        return VK_FALSE;
//    }

    @Override
    public void update() {

    }

    @Override
    public void close() {
        if(msg_callback != NULL){
            vkDestroyDebugUtilsMessengerEXT(instance, msg_callback, null);
        }
        vkDestroyInstance(instance, null);
        dbgFunc.free();

        gpu_features.free();
        gpu_props.free();
        queue_props.free();

        memFree(extension_names);

        memFree(pp);
        memFree(lp);
        memFree(ip);

        memFree(EXT_debug_utils);
        memFree(KHR_swapchain);
    }
}
