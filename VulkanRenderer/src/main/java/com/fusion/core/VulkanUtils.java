package com.fusion.core;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanUtils {
    public static String deviceTypeToString(int deviceType) {
        switch (deviceType) {
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
                return "Integrated GPU";
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
                return "Discrete GPU";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
                return "Virtual GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU:
                return "CPU";
            default:
                return "Unknown";
        }
    }

    public static String apiVersionToString(int apiVersion) {
        int major = VK_VERSION_MAJOR(apiVersion);
        int minor = VK_VERSION_MINOR(apiVersion);
        int patch = VK_VERSION_PATCH(apiVersion);

        return major + "." + minor + "." + patch;
    }

    public static String vendorIdToString(int vendorId) {
        // Here you could map the vendor ID to the vendor name, e.g.:
        if (vendorId == 0x1002) {
            return "AMD";
        } else if (vendorId == 0x10DE) {
            return "NVIDIA";
        } else if (vendorId == 0x8086) {
            return "Intel";
        } else {
            return "Unknown Vendor ID: " + vendorId;
        }
    }

    public static String driverVersionToString(int driverVersion, int vendorId) {
        // AMD and Intel use the same scheme
        if (vendorId == 0x1002 || vendorId == 0x8086) {
            int major = driverVersion >> 22;
            int minor = (driverVersion >> 12) & 0x3ff;
            int patch = driverVersion & 0xfff;

            return major + "." + minor + "." + patch;
        }else if (vendorId == 0x10DE) { // Nvidia
            int major = driverVersion >> 22;
            int minor = (driverVersion >> 12) & 0x3ff;
            int secondaryBranch = (driverVersion >> 4) & 0xff;
            int tertiaryBranch = driverVersion & 0xf;

            return major + "." + minor + "." + secondaryBranch + "." + tertiaryBranch;
        }else{
            return String.valueOf(driverVersion);
        }
    }
}
