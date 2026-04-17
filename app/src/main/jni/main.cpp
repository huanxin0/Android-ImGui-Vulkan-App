#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <jni.h>
#include <android/log.h>
#include <thread>
#include <unistd.h>
#include <vector>
#include <cstring>
#include <dlfcn.h>
#include <chrono>
#include <memory>
#include <atomic>
#include <algorithm>

#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>
#include "Includes.h"

// Android Surface 扩展定义
#ifndef VK_KHR_android_surface
#define VK_KHR_ANDROID_SURFACE_EXTENSION_NAME "VK_KHR_android_surface"
typedef VkFlags VkSurfaceKHRCreateFlags;
typedef struct VkAndroidSurfaceCreateInfoKHR {
    VkStructureType sType;
    const void* pNext;
    VkSurfaceKHRCreateFlags flags;
    ANativeWindow* window;
} VkAndroidSurfaceCreateInfoKHR;
typedef VkResult (VKAPI_PTR *PFN_vkCreateAndroidSurfaceKHR)(VkInstance, const VkAndroidSurfaceCreateInfoKHR*, const VkAllocationCallbacks*, VkSurfaceKHR*);
#endif

#ifndef VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR
#define VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR (VkStructureType)1000009000
#endif

#include "imgui.h"
#include "imgui_internal.h"
#include "imgui_impl_android.h"
#include "imgui_impl_vulkan.h"

#define LOG_TAG "ImGuiVulkan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// 前向声明
static bool CreateImageViews();
static bool CreateFramebuffers();

// Vulkan 上下文
struct VulkanContext {
    VkAllocationCallbacks* allocator = nullptr;
    void* vulkanLib = nullptr;
    
    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    uint32_t queueFamily = 0;
    VkQueue queue = VK_NULL_HANDLE;
    
    VkDescriptorPool descriptorPool = VK_NULL_HANDLE;
    VkCommandPool commandPool = VK_NULL_HANDLE;
    VkRenderPass renderPass = VK_NULL_HANDLE;
    VkPipelineCache pipelineCache = VK_NULL_HANDLE;
    
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkSurfaceFormatKHR surfaceFormat;
    VkExtent2D swapchainExtent;
    VkPresentModeKHR presentMode;
    
    std::vector<VkImage> swapchainImages;
    std::vector<VkImageView> swapchainImageViews;
    std::vector<VkFramebuffer> swapchainFramebuffers;
    
    struct PerFrame {
        VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
        VkFence fence = VK_NULL_HANDLE;
        VkSemaphore imageAcquired = VK_NULL_HANDLE;
        VkSemaphore renderComplete = VK_NULL_HANDLE;
    };
    std::vector<PerFrame> frames;
    uint32_t frameIndex = 0;
    
    // ImGui 初始化信息
    ImGui_ImplVulkan_InitInfo imguiInitInfo = {};
    
    bool initialized = false;
    bool needRebuild = false;
};

// 全局变量
static std::unique_ptr<VulkanContext> g_Ctx;
static std::atomic<bool> g_Running{true};
static std::atomic<bool> g_NeedResize{false};
static int g_TargetWidth = 0;
static int g_TargetHeight = 0;
static int g_TargetFPS = 60;
static double g_TargetFrameTime = 1000.0 / 60.0;

// Vulkan 1.1 函数指针
struct VkFunc {
    #define DECL_FUNC(name) PFN_##name name = nullptr
    DECL_FUNC(vkGetInstanceProcAddr);
    DECL_FUNC(vkCreateInstance);
    DECL_FUNC(vkDestroyInstance);
    DECL_FUNC(vkEnumeratePhysicalDevices);
    DECL_FUNC(vkGetPhysicalDeviceProperties);
    DECL_FUNC(vkGetPhysicalDeviceQueueFamilyProperties);
    DECL_FUNC(vkCreateDevice);
    DECL_FUNC(vkDestroyDevice);
    DECL_FUNC(vkGetDeviceQueue);
    DECL_FUNC(vkCreateDescriptorPool);
    DECL_FUNC(vkDestroyDescriptorPool);
    DECL_FUNC(vkCreateRenderPass);
    DECL_FUNC(vkDestroyRenderPass);
    DECL_FUNC(vkCreateCommandPool);
    DECL_FUNC(vkDestroyCommandPool);
    DECL_FUNC(vkAllocateCommandBuffers);
    DECL_FUNC(vkFreeCommandBuffers);
    DECL_FUNC(vkBeginCommandBuffer);
    DECL_FUNC(vkEndCommandBuffer);
    DECL_FUNC(vkResetCommandBuffer);
    DECL_FUNC(vkResetCommandPool);
    DECL_FUNC(vkCmdBeginRenderPass);
    DECL_FUNC(vkCmdEndRenderPass);
    DECL_FUNC(vkQueueSubmit);
    DECL_FUNC(vkQueueWaitIdle);
    DECL_FUNC(vkDeviceWaitIdle);
    DECL_FUNC(vkCreateSwapchainKHR);
    DECL_FUNC(vkDestroySwapchainKHR);
    DECL_FUNC(vkGetSwapchainImagesKHR);
    DECL_FUNC(vkCreateImageView);
    DECL_FUNC(vkDestroyImageView);
    DECL_FUNC(vkCreateFramebuffer);
    DECL_FUNC(vkDestroyFramebuffer);
    DECL_FUNC(vkAcquireNextImageKHR);
    DECL_FUNC(vkGetPhysicalDeviceSurfaceSupportKHR);
    DECL_FUNC(vkGetPhysicalDeviceSurfaceCapabilitiesKHR);
    DECL_FUNC(vkGetPhysicalDeviceSurfaceFormatsKHR);
    DECL_FUNC(vkGetPhysicalDeviceSurfacePresentModesKHR);
    DECL_FUNC(vkDestroySurfaceKHR);
    DECL_FUNC(vkCreateFence);
    DECL_FUNC(vkDestroyFence);
    DECL_FUNC(vkWaitForFences);
    DECL_FUNC(vkResetFences);
    DECL_FUNC(vkCreateSemaphore);
    DECL_FUNC(vkDestroySemaphore);
    DECL_FUNC(vkQueuePresentKHR);
    DECL_FUNC(vkCreateAndroidSurfaceKHR);
    DECL_FUNC(vkCreatePipelineCache);
    DECL_FUNC(vkDestroyPipelineCache);
    #undef DECL_FUNC
};
static VkFunc vk;

// 错误检查
static const char* VkResultToString(VkResult err) {
    switch(err) {
        case VK_SUCCESS: return "SUCCESS";
        case VK_NOT_READY: return "NOT_READY";
        case VK_TIMEOUT: return "TIMEOUT";
        case VK_EVENT_SET: return "EVENT_SET";
        case VK_EVENT_RESET: return "EVENT_RESET";
        case VK_INCOMPLETE: return "INCOMPLETE";
        case VK_ERROR_OUT_OF_HOST_MEMORY: return "OUT_OF_HOST_MEMORY";
        case VK_ERROR_OUT_OF_DEVICE_MEMORY: return "OUT_OF_DEVICE_MEMORY";
        case VK_ERROR_INITIALIZATION_FAILED: return "INITIALIZATION_FAILED";
        case VK_ERROR_DEVICE_LOST: return "DEVICE_LOST";
        case VK_ERROR_MEMORY_MAP_FAILED: return "MEMORY_MAP_FAILED";
        case VK_ERROR_LAYER_NOT_PRESENT: return "LAYER_NOT_PRESENT";
        case VK_ERROR_EXTENSION_NOT_PRESENT: return "EXTENSION_NOT_PRESENT";
        case VK_ERROR_FEATURE_NOT_PRESENT: return "FEATURE_NOT_PRESENT";
        case VK_ERROR_INCOMPATIBLE_DRIVER: return "INCOMPATIBLE_DRIVER";
        case VK_ERROR_TOO_MANY_OBJECTS: return "TOO_MANY_OBJECTS";
        case VK_ERROR_FORMAT_NOT_SUPPORTED: return "FORMAT_NOT_SUPPORTED";
        case VK_ERROR_SURFACE_LOST_KHR: return "SURFACE_LOST_KHR";
        case VK_ERROR_OUT_OF_DATE_KHR: return "OUT_OF_DATE_KHR";
        case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR: return "INCOMPATIBLE_DISPLAY_KHR";
        case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR: return "NATIVE_WINDOW_IN_USE_KHR";
        case VK_SUBOPTIMAL_KHR: return "SUBOPTIMAL_KHR";
        default: return "UNKNOWN";
    }
}

static inline void CheckVk(VkResult err, const char* msg = nullptr) {
    if (err == VK_SUCCESS) return;
    if (msg) LOGE("Vulkan Error: %s [%s]", msg, VkResultToString(err));
    else LOGE("Vulkan Error: %s", VkResultToString(err));
}

static void ImGuiCheckVk(VkResult err) {
    if (err == VK_SUCCESS) return;
    LOGE("ImGui Vulkan Error: %s", VkResultToString(err));
}

// 加载 Vulkan 函数
static bool LoadVulkan() {
    g_Ctx->vulkanLib = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
    if (!g_Ctx->vulkanLib) {
        LOGE("Failed to load libvulkan.so");
        return false;
    }
    
    #define LOAD(name) \
        vk.name = (PFN_##name)dlsym(g_Ctx->vulkanLib, #name); \
        if (!vk.name) { LOGE("Failed to load " #name); return false; }
    
    LOAD(vkGetInstanceProcAddr);
    LOAD(vkCreateInstance);
    LOAD(vkDestroyInstance);
    LOAD(vkEnumeratePhysicalDevices);
    LOAD(vkGetPhysicalDeviceProperties);
    LOAD(vkGetPhysicalDeviceQueueFamilyProperties);
    LOAD(vkCreateDevice);
    LOAD(vkDestroyDevice);
    LOAD(vkGetDeviceQueue);
    LOAD(vkCreateDescriptorPool);
    LOAD(vkDestroyDescriptorPool);
    LOAD(vkCreateRenderPass);
    LOAD(vkDestroyRenderPass);
    LOAD(vkCreateCommandPool);
    LOAD(vkDestroyCommandPool);
    LOAD(vkAllocateCommandBuffers);
    LOAD(vkFreeCommandBuffers);
    LOAD(vkBeginCommandBuffer);
    LOAD(vkEndCommandBuffer);
    LOAD(vkResetCommandBuffer);
    LOAD(vkResetCommandPool);
    LOAD(vkCmdBeginRenderPass);
    LOAD(vkCmdEndRenderPass);
    LOAD(vkQueueSubmit);
    LOAD(vkQueueWaitIdle);
    LOAD(vkDeviceWaitIdle);
    LOAD(vkCreateSwapchainKHR);
    LOAD(vkDestroySwapchainKHR);
    LOAD(vkGetSwapchainImagesKHR);
    LOAD(vkCreateImageView);
    LOAD(vkDestroyImageView);
    LOAD(vkCreateFramebuffer);
    LOAD(vkDestroyFramebuffer);
    LOAD(vkAcquireNextImageKHR);
    LOAD(vkGetPhysicalDeviceSurfaceSupportKHR);
    LOAD(vkGetPhysicalDeviceSurfaceCapabilitiesKHR);
    LOAD(vkGetPhysicalDeviceSurfaceFormatsKHR);
    LOAD(vkGetPhysicalDeviceSurfacePresentModesKHR);
    LOAD(vkDestroySurfaceKHR);
    LOAD(vkCreateFence);
    LOAD(vkDestroyFence);
    LOAD(vkWaitForFences);
    LOAD(vkResetFences);
    LOAD(vkCreateSemaphore);
    LOAD(vkDestroySemaphore);
    LOAD(vkQueuePresentKHR);
    LOAD(vkCreatePipelineCache);
    LOAD(vkDestroyPipelineCache);
    #undef LOAD
    
    return true;
}

static PFN_vkVoidFunction ImGuiLoader(const char* name, void*) {
    return (PFN_vkVoidFunction)dlsym(g_Ctx->vulkanLib, name);
}

// 创建实例
static bool CreateInstance() {
    const char* extensions[] = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };
    
    VkApplicationInfo appInfo = {
        .sType = VK_STRUCTURE_TYPE_APPLICATION_INFO,
        .pApplicationName = "ImGuiVulkan",
        .applicationVersion = VK_MAKE_VERSION(1, 0, 0),
        .pEngineName = "ImGui",
        .engineVersion = VK_MAKE_VERSION(1, 0, 0),
        .apiVersion = VK_API_VERSION_1_1
    };
    
    VkInstanceCreateInfo createInfo = {
        .sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO,
        .pApplicationInfo = &appInfo,
        .enabledExtensionCount = sizeof(extensions) / sizeof(extensions[0]),
        .ppEnabledExtensionNames = extensions
    };
    
    VkResult res = vk.vkCreateInstance(&createInfo, g_Ctx->allocator, &g_Ctx->instance);
    if (res != VK_SUCCESS) {
        CheckVk(res, "CreateInstance");
        return false;
    }
    
    vk.vkCreateAndroidSurfaceKHR = (PFN_vkCreateAndroidSurfaceKHR)vk.vkGetInstanceProcAddr(g_Ctx->instance, "vkCreateAndroidSurfaceKHR");
    vk.vkDestroySurfaceKHR = (PFN_vkDestroySurfaceKHR)vk.vkGetInstanceProcAddr(g_Ctx->instance, "vkDestroySurfaceKHR");
    vk.vkGetPhysicalDeviceSurfaceSupportKHR = (PFN_vkGetPhysicalDeviceSurfaceSupportKHR)vk.vkGetInstanceProcAddr(g_Ctx->instance, "vkGetPhysicalDeviceSurfaceSupportKHR");
    vk.vkGetPhysicalDeviceSurfaceCapabilitiesKHR = (PFN_vkGetPhysicalDeviceSurfaceCapabilitiesKHR)vk.vkGetInstanceProcAddr(g_Ctx->instance, "vkGetPhysicalDeviceSurfaceCapabilitiesKHR");
    vk.vkGetPhysicalDeviceSurfaceFormatsKHR = (PFN_vkGetPhysicalDeviceSurfaceFormatsKHR)vk.vkGetInstanceProcAddr(g_Ctx->instance, "vkGetPhysicalDeviceSurfaceFormatsKHR");
    vk.vkGetPhysicalDeviceSurfacePresentModesKHR = (PFN_vkGetPhysicalDeviceSurfacePresentModesKHR)vk.vkGetInstanceProcAddr(g_Ctx->instance, "vkGetPhysicalDeviceSurfacePresentModesKHR");
    
    LOGI("Vulkan 1.1 instance created");
    return true;
}

// 选择物理设备
static bool SelectPhysicalDevice() {
    uint32_t count = 0;
    vk.vkEnumeratePhysicalDevices(g_Ctx->instance, &count, nullptr);
    if (!count) { LOGE("No Vulkan devices"); return false; }
    
    std::vector<VkPhysicalDevice> devices(count);
    vk.vkEnumeratePhysicalDevices(g_Ctx->instance, &count, devices.data());
    
    for (auto& dev : devices) {
        VkPhysicalDeviceProperties props;
        vk.vkGetPhysicalDeviceProperties(dev, &props);
        
        uint32_t qCount = 0;
        vk.vkGetPhysicalDeviceQueueFamilyProperties(dev, &qCount, nullptr);
        std::vector<VkQueueFamilyProperties> qProps(qCount);
        vk.vkGetPhysicalDeviceQueueFamilyProperties(dev, &qCount, qProps.data());
        
        for (uint32_t i = 0; i < qCount; ++i) {
            if (qProps[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) {
                g_Ctx->physicalDevice = dev;
                g_Ctx->queueFamily = i;
                LOGI("GPU: %s (Queue: %u)", props.deviceName, i);
                return true;
            }
        }
    }
    
    LOGE("No suitable GPU");
    return false;
}

// 创建设备
static bool CreateDevice() {
    float priority = 1.0f;
    VkDeviceQueueCreateInfo qInfo = {
        .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,
        .queueFamilyIndex = g_Ctx->queueFamily,
        .queueCount = 1,
        .pQueuePriorities = &priority
    };
    
    const char* extensions[] = { VK_KHR_SWAPCHAIN_EXTENSION_NAME };
    
    VkDeviceCreateInfo createInfo = {
        .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,
        .queueCreateInfoCount = 1,
        .pQueueCreateInfos = &qInfo,
        .enabledExtensionCount = 1,
        .ppEnabledExtensionNames = extensions
    };
    
    VkResult res = vk.vkCreateDevice(g_Ctx->physicalDevice, &createInfo, g_Ctx->allocator, &g_Ctx->device);
    if (res != VK_SUCCESS) {
        CheckVk(res, "CreateDevice");
        return false;
    }
    
    vk.vkGetDeviceQueue(g_Ctx->device, g_Ctx->queueFamily, 0, &g_Ctx->queue);
    
    VkPipelineCacheCreateInfo cacheInfo = {
        .sType = VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO,
        .flags = 0,
        .initialDataSize = 0,
        .pInitialData = nullptr
    };
    res = vk.vkCreatePipelineCache(g_Ctx->device, &cacheInfo, g_Ctx->allocator, &g_Ctx->pipelineCache);
    if (res != VK_SUCCESS) {
        CheckVk(res, "CreatePipelineCache");
    } else {
        LOGI("Pipeline cache created");
    }
    
    LOGI("Device created (Vulkan 1.1)");
    return true;
}

// 创建 Surface
static bool CreateSurface(ANativeWindow* window) {
    if (!vk.vkCreateAndroidSurfaceKHR) {
        LOGE("vkCreateAndroidSurfaceKHR missing");
        return false;
    }
    
    VkAndroidSurfaceCreateInfoKHR info = {
        .sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR,
        .window = window
    };
    
    VkResult res = vk.vkCreateAndroidSurfaceKHR(g_Ctx->instance, &info, g_Ctx->allocator, &g_Ctx->surface);
    if (res != VK_SUCCESS) {
        CheckVk(res, "CreateSurface");
        return false;
    }
    
    VkBool32 supported = VK_FALSE;
    vk.vkGetPhysicalDeviceSurfaceSupportKHR(g_Ctx->physicalDevice, g_Ctx->queueFamily, g_Ctx->surface, &supported);
    if (!supported) {
        LOGE("Surface not supported");
        return false;
    }
    
    LOGI("Surface created");
    return true;
}

// 选择 Surface 格式
static VkSurfaceFormatKHR ChooseSurfaceFormat(const std::vector<VkSurfaceFormatKHR>& formats) {
    for (const auto& f : formats) {
        if (f.format == VK_FORMAT_B8G8R8A8_UNORM && f.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
            return f;
        if (f.format == VK_FORMAT_R8G8B8A8_UNORM && f.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
            return f;
    }
    return formats[0];
}

// 选择呈现模式
static VkPresentModeKHR ChoosePresentMode(const std::vector<VkPresentModeKHR>& modes) {
    // 优先使用 MAILBOX（低延迟，不锁帧）
    for (auto m : modes) {
        if (m == VK_PRESENT_MODE_MAILBOX_KHR) {
            LOGI("Using MAILBOX present mode (low latency, uncapped FPS)");
            return m;
        }
    }
    
    // 其次使用 IMMEDIATE（立即呈现，可能有撕裂但帧率最高）
    for (auto m : modes) {
        if (m == VK_PRESENT_MODE_IMMEDIATE_KHR) {
            LOGI("Using IMMEDIATE present mode (uncapped FPS, possible tearing)");
            return m;
        }
    }
    
    // 最后才用 FIFO（垂直同步，锁60帧）
    for (auto m : modes) {
        if (m == VK_PRESENT_MODE_FIFO_KHR) {
            LOGI("Using FIFO present mode (VSync ON, capped FPS)");
            return m;
        }
    }
    
    LOGW("No preferred present mode found, using first available");
    return modes[0];
}

// 创建 Swapchain
static bool CreateSwapchain(ANativeWindow* window) {
    VkSurfaceCapabilitiesKHR caps;
    vk.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(g_Ctx->physicalDevice, g_Ctx->surface, &caps);
    
    uint32_t fmtCount;
    vk.vkGetPhysicalDeviceSurfaceFormatsKHR(g_Ctx->physicalDevice, g_Ctx->surface, &fmtCount, nullptr);
    std::vector<VkSurfaceFormatKHR> formats(fmtCount);
    vk.vkGetPhysicalDeviceSurfaceFormatsKHR(g_Ctx->physicalDevice, g_Ctx->surface, &fmtCount, formats.data());
    
    uint32_t modeCount;
    vk.vkGetPhysicalDeviceSurfacePresentModesKHR(g_Ctx->physicalDevice, g_Ctx->surface, &modeCount, nullptr);
    std::vector<VkPresentModeKHR> modes(modeCount);
    vk.vkGetPhysicalDeviceSurfacePresentModesKHR(g_Ctx->physicalDevice, g_Ctx->surface, &modeCount, modes.data());
    
    g_Ctx->surfaceFormat = ChooseSurfaceFormat(formats);
    g_Ctx->presentMode = ChoosePresentMode(modes);
    
    uint32_t imageCount = 2;
    if (imageCount < caps.minImageCount)
        imageCount = caps.minImageCount;
    if (caps.maxImageCount > 0 && imageCount > caps.maxImageCount)
        imageCount = caps.maxImageCount;
    
    int width = g_TargetWidth;
    int height = g_TargetHeight;
    
    if (width <= 0 || height <= 0) {
        width = ANativeWindow_getWidth(window);
        height = ANativeWindow_getHeight(window);
        LOGI("Using ANativeWindow dimensions: %dx%d", width, height);
    } else {
        LOGI("Using Java dimensions from OnSurfaceChanged: %dx%d", width, height);
    }
    
    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return false;
    }
    
    g_Ctx->swapchainExtent.width = (uint32_t)width;
    g_Ctx->swapchainExtent.height = (uint32_t)height;
    
    g_Ctx->swapchainExtent.width = std::max(caps.minImageExtent.width, 
                                             std::min(caps.maxImageExtent.width, 
                                                     g_Ctx->swapchainExtent.width));
    g_Ctx->swapchainExtent.height = std::max(caps.minImageExtent.height, 
                                              std::min(caps.maxImageExtent.height, 
                                                      g_Ctx->swapchainExtent.height));
    
    LOGI("Creating swapchain with final extent: %dx%d", 
         g_Ctx->swapchainExtent.width, g_Ctx->swapchainExtent.height);
    
    VkSwapchainCreateInfoKHR info = {
        .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,
        .surface = g_Ctx->surface,
        .minImageCount = imageCount,
        .imageFormat = g_Ctx->surfaceFormat.format,
        .imageColorSpace = g_Ctx->surfaceFormat.colorSpace,
        .imageExtent = g_Ctx->swapchainExtent,
        .imageArrayLayers = 1,
        .imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
        .imageSharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR,
        .compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
        .presentMode = g_Ctx->presentMode,
        .clipped = VK_TRUE
    };
    
    VkResult res = vk.vkCreateSwapchainKHR(g_Ctx->device, &info, g_Ctx->allocator, &g_Ctx->swapchain);
    if (res != VK_SUCCESS) {
        CheckVk(res, "CreateSwapchain");
        return false;
    }
    
    uint32_t actualCount;
    vk.vkGetSwapchainImagesKHR(g_Ctx->device, g_Ctx->swapchain, &actualCount, nullptr);
    g_Ctx->swapchainImages.resize(actualCount);
    vk.vkGetSwapchainImagesKHR(g_Ctx->device, g_Ctx->swapchain, &actualCount, g_Ctx->swapchainImages.data());
    
    LOGI("Swapchain created: %dx%d, %u images", 
         g_Ctx->swapchainExtent.width, g_Ctx->swapchainExtent.height, actualCount);
    return true;
}

// 清理交换链资源
static void CleanupSwapchain(bool destroySwapchain = true) {
    if (!g_Ctx->device) return;
    
    VkResult err = vk.vkDeviceWaitIdle(g_Ctx->device);
    if (err != VK_SUCCESS) {
        LOGE("DeviceWaitIdle failed: %s", VkResultToString(err));
    }
    
    for (auto& fb : g_Ctx->swapchainFramebuffers)
        if (fb) vk.vkDestroyFramebuffer(g_Ctx->device, fb, g_Ctx->allocator);
    g_Ctx->swapchainFramebuffers.clear();
    
    for (auto& iv : g_Ctx->swapchainImageViews)
        if (iv) vk.vkDestroyImageView(g_Ctx->device, iv, g_Ctx->allocator);
    g_Ctx->swapchainImageViews.clear();
    
    if (destroySwapchain && g_Ctx->swapchain) {
        vk.vkDestroySwapchainKHR(g_Ctx->device, g_Ctx->swapchain, g_Ctx->allocator);
        g_Ctx->swapchain = VK_NULL_HANDLE;
    }
    g_Ctx->swapchainImages.clear();
}

// 重建交换链
static bool RebuildSwapchain(ANativeWindow* window) {
    if (!window) return false;
    
    int w = ANativeWindow_getWidth(window);
    int h = ANativeWindow_getHeight(window);
    if (w <= 0 || h <= 0) return false;
    
    CleanupSwapchain(true);
    
    if (!CreateSwapchain(window)) return false;
    if (!CreateImageViews()) return false;
    if (!CreateFramebuffers()) return false;
    
    LOGI("Swapchain rebuilt");
    return true;
}

// 创建图像视图
static bool CreateImageViews() {
    g_Ctx->swapchainImageViews.resize(g_Ctx->swapchainImages.size());
    
    for (size_t i = 0; i < g_Ctx->swapchainImages.size(); ++i) {
        VkImageViewCreateInfo info = {
            .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
            .image = g_Ctx->swapchainImages[i],
            .viewType = VK_IMAGE_VIEW_TYPE_2D,
            .format = g_Ctx->surfaceFormat.format,
            .components = {VK_COMPONENT_SWIZZLE_IDENTITY},
            .subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1}
        };
        
        if (vk.vkCreateImageView(g_Ctx->device, &info, g_Ctx->allocator, &g_Ctx->swapchainImageViews[i]) != VK_SUCCESS) {
            LOGE("ImageView %zu failed", i);
            return false;
        }
    }
    return true;
}

// 创建渲染通道
static bool CreateRenderPass() {
    VkAttachmentDescription attach = {
        .format = g_Ctx->surfaceFormat.format,
        .samples = VK_SAMPLE_COUNT_1_BIT,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
        .stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE,
        .stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE,
        .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
        .finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
    };
    
    VkAttachmentReference colorRef = {0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL};
    
    VkSubpassDescription subpass = {
        .pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS,
        .colorAttachmentCount = 1,
        .pColorAttachments = &colorRef
    };
    
    VkSubpassDependency dep = {
        .srcSubpass = VK_SUBPASS_EXTERNAL,
        .dstSubpass = 0,
        .srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
        .dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
        .srcAccessMask = 0,
        .dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
    };
    
    VkRenderPassCreateInfo info = {
        .sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO,
        .attachmentCount = 1,
        .pAttachments = &attach,
        .subpassCount = 1,
        .pSubpasses = &subpass,
        .dependencyCount = 1,
        .pDependencies = &dep
    };
    
    return vk.vkCreateRenderPass(g_Ctx->device, &info, g_Ctx->allocator, &g_Ctx->renderPass) == VK_SUCCESS;
}

// 创建帧缓冲
static bool CreateFramebuffers() {
    g_Ctx->swapchainFramebuffers.resize(g_Ctx->swapchainImageViews.size());
    
    for (size_t i = 0; i < g_Ctx->swapchainImageViews.size(); ++i) {
        VkImageView attach[] = {g_Ctx->swapchainImageViews[i]};
        VkFramebufferCreateInfo info = {
            .sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO,
            .renderPass = g_Ctx->renderPass,
            .attachmentCount = 1,
            .pAttachments = attach,
            .width = g_Ctx->swapchainExtent.width,
            .height = g_Ctx->swapchainExtent.height,
            .layers = 1
        };
        
        if (vk.vkCreateFramebuffer(g_Ctx->device, &info, g_Ctx->allocator, &g_Ctx->swapchainFramebuffers[i]) != VK_SUCCESS) {
            return false;
        }
    }
    return true;
}

// 创建描述符池
static bool CreateDescriptorPool() {
    VkDescriptorPoolSize poolSize = {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 100};
    VkDescriptorPoolCreateInfo info = {
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
        .flags = VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT,
        .maxSets = 100,
        .poolSizeCount = 1,
        .pPoolSizes = &poolSize
    };
    return vk.vkCreateDescriptorPool(g_Ctx->device, &info, g_Ctx->allocator, &g_Ctx->descriptorPool) == VK_SUCCESS;
}

// 创建命令池
static bool CreateCommandPool() {
    VkCommandPoolCreateInfo info = {
        .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
        .flags = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT | VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
        .queueFamilyIndex = g_Ctx->queueFamily
    };
    return vk.vkCreateCommandPool(g_Ctx->device, &info, g_Ctx->allocator, &g_Ctx->commandPool) == VK_SUCCESS;
}

// 创建同步对象
static bool CreateSyncObjects() {
    uint32_t count = (uint32_t)g_Ctx->swapchainImages.size();
    g_Ctx->frames.resize(count);
    
    VkCommandBufferAllocateInfo cmdInfo = {
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
        .commandPool = g_Ctx->commandPool,
        .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
        .commandBufferCount = 1
    };
    
    VkFenceCreateInfo fenceInfo = {.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO, .flags = VK_FENCE_CREATE_SIGNALED_BIT};
    VkSemaphoreCreateInfo semaInfo = {.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    
    for (uint32_t i = 0; i < count; ++i) {
        auto& f = g_Ctx->frames[i];
        if (vk.vkAllocateCommandBuffers(g_Ctx->device, &cmdInfo, &f.commandBuffer) != VK_SUCCESS ||
            vk.vkCreateFence(g_Ctx->device, &fenceInfo, g_Ctx->allocator, &f.fence) != VK_SUCCESS ||
            vk.vkCreateSemaphore(g_Ctx->device, &semaInfo, g_Ctx->allocator, &f.imageAcquired) != VK_SUCCESS ||
            vk.vkCreateSemaphore(g_Ctx->device, &semaInfo, g_Ctx->allocator, &f.renderComplete) != VK_SUCCESS) {
            LOGE("Failed to create sync objects for frame %u", i);
            return false;
        }
    }
    return true;
}

// 初始化 ImGui
static bool InitImGui(ANativeWindow* window) {
    if (!ImGui_ImplVulkan_LoadFunctions(VK_API_VERSION_1_1, ImGuiLoader, nullptr)) {
        LOGE("ImGui Vulkan loader failed");
        return false;
    }
    
    IMGUI_CHECKVERSION();
    YouXiStyle();
    ImGui_ImplAndroid_Init(window);
    
    g_Ctx->imguiInitInfo = {};
    g_Ctx->imguiInitInfo.ApiVersion = VK_API_VERSION_1_1;
    g_Ctx->imguiInitInfo.Instance = g_Ctx->instance;
    g_Ctx->imguiInitInfo.PhysicalDevice = g_Ctx->physicalDevice;
    g_Ctx->imguiInitInfo.Device = g_Ctx->device;
    g_Ctx->imguiInitInfo.QueueFamily = g_Ctx->queueFamily;
    g_Ctx->imguiInitInfo.Queue = g_Ctx->queue;
    g_Ctx->imguiInitInfo.DescriptorPoolSize = 100;
    g_Ctx->imguiInitInfo.MinImageCount = std::max(2u, (uint32_t)g_Ctx->swapchainImages.size());
    g_Ctx->imguiInitInfo.ImageCount = (uint32_t)g_Ctx->swapchainImages.size();
    g_Ctx->imguiInitInfo.Allocator = g_Ctx->allocator;
    g_Ctx->imguiInitInfo.CheckVkResultFn = ImGuiCheckVk;
    g_Ctx->imguiInitInfo.PipelineInfoMain.RenderPass = g_Ctx->renderPass;
    g_Ctx->imguiInitInfo.PipelineInfoMain.Subpass = 0;
    g_Ctx->imguiInitInfo.PipelineInfoMain.MSAASamples = VK_SAMPLE_COUNT_1_BIT;
    
    if (!ImGui_ImplVulkan_Init(&g_Ctx->imguiInitInfo)) {
        LOGE("ImGui_ImplVulkan_Init failed");
        return false;
    }
    
    LOGI("ImGui initialized (new API)");
    return true;
}

// 渲染一帧
static bool RenderFrame(ANativeWindow* window, ImVec4& clearColor, float deltaTime, int& frameCount) {
    auto& frame = g_Ctx->frames[g_Ctx->frameIndex];
    
    // 减少超时时间到 8ms，避免等待过久
    VkResult fenceResult = vk.vkWaitForFences(g_Ctx->device, 1, &frame.fence, VK_TRUE, 8000000);
    if (fenceResult == VK_TIMEOUT) {
        LOGW("Fence timeout, skipping frame");
        g_Ctx->frameIndex = (g_Ctx->frameIndex + 1) % g_Ctx->frames.size();
        return false;
    }
    if (fenceResult != VK_SUCCESS) {
        CheckVk(fenceResult, "WaitForFences");
        return false;
    }
    vk.vkResetFences(g_Ctx->device, 1, &frame.fence);
    
    uint32_t imageIndex;
    // 使用 16ms 超时而不是无限等待
    VkResult res = vk.vkAcquireNextImageKHR(g_Ctx->device, g_Ctx->swapchain, 16000000,
                                            frame.imageAcquired, VK_NULL_HANDLE, &imageIndex);
    
    if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
        g_Ctx->needRebuild = true;
        return false;
    }
    if (res == VK_TIMEOUT) {
        LOGW("AcquireNextImage timeout");
        return false;
    }
    if (res != VK_SUCCESS) {
        CheckVk(res, "AcquireNextImage");
        return false;
    }
    
    vk.vkResetCommandBuffer(frame.commandBuffer, 0);
    VkCommandBufferBeginInfo beginInfo = {
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
        .flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
    };
    vk.vkBeginCommandBuffer(frame.commandBuffer, &beginInfo);
    
    VkRenderPassBeginInfo rpBegin = {
        .sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO,
        .renderPass = g_Ctx->renderPass,
        .framebuffer = g_Ctx->swapchainFramebuffers[imageIndex],
        .renderArea = {{0, 0}, g_Ctx->swapchainExtent},
        .clearValueCount = 1
    };
    VkClearValue clearVal;
    memcpy(clearVal.color.float32, &clearColor.x, 16);
    rpBegin.pClearValues = &clearVal;
    
    vk.vkCmdBeginRenderPass(frame.commandBuffer, &rpBegin, VK_SUBPASS_CONTENTS_INLINE);
    
    ImGui_ImplVulkan_NewFrame();
    ImGui_ImplAndroid_NewFrame();
    ImGui::NewFrame();
    
    px = (float)g_Ctx->swapchainExtent.width;
    py = (float)g_Ctx->swapchainExtent.height;
    
    YouXiMenu();
    
    ImGui::Render();
    ImGui_ImplVulkan_RenderDrawData(ImGui::GetDrawData(), frame.commandBuffer);
    
    vk.vkCmdEndRenderPass(frame.commandBuffer);
    vk.vkEndCommandBuffer(frame.commandBuffer);
    
    VkPipelineStageFlags waitStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo submit = {
        .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
        .waitSemaphoreCount = 1,
        .pWaitSemaphores = &frame.imageAcquired,
        .pWaitDstStageMask = &waitStage,
        .commandBufferCount = 1,
        .pCommandBuffers = &frame.commandBuffer,
        .signalSemaphoreCount = 1,
        .pSignalSemaphores = &frame.renderComplete
    };
    
    if (vk.vkQueueSubmit(g_Ctx->queue, 1, &submit, frame.fence) != VK_SUCCESS) {
        LOGE("Queue submit failed");
        return false;
    }
    
    VkPresentInfoKHR present = {
        .sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR,
        .waitSemaphoreCount = 1,
        .pWaitSemaphores = &frame.renderComplete,
        .swapchainCount = 1,
        .pSwapchains = &g_Ctx->swapchain,
        .pImageIndices = &imageIndex
    };
    
    res = vk.vkQueuePresentKHR(g_Ctx->queue, &present);
    if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
        g_Ctx->needRebuild = true;
    } else if (res != VK_SUCCESS) {
        CheckVk(res, "Present");
        return false;
    }
    
    g_Ctx->frameIndex = (g_Ctx->frameIndex + 1) % g_Ctx->frames.size();
    return true;
}

// 主渲染函数
static void RenderLoop(ANativeWindow* window) {
    LOGI("Starting Vulkan 1.1 render loop...");
    
    g_Ctx = std::make_unique<VulkanContext>();
    
    if (!LoadVulkan()) return;
    
    if (!CreateInstance() ||
        !SelectPhysicalDevice() ||
        !CreateDevice() ||
        !CreateSurface(window) ||
        !CreateSwapchain(window) ||
        !CreateImageViews() ||
        !CreateRenderPass() ||
        !CreateFramebuffers() ||
        !CreateCommandPool() ||
        !CreateSyncObjects() ||
        !InitImGui(window)) {
        LOGE("Initialization failed");
        return;
    }
    
    g_Ctx->initialized = true;
    
    auto lastTime = std::chrono::high_resolution_clock::now();
    int frameCount = 0;
    ImVec4 clearColor = {0.0f, 0.0f, 0.0f, 0.0f};
    
    LOGI("Target FPS: %d, Frame time: %.2f ms", g_TargetFPS, g_TargetFrameTime);
    
    auto lastFrameTime = std::chrono::steady_clock::now();
    auto fpsLogTime = std::chrono::steady_clock::now();
    int fpsFrameCount = 0;
    
    // 帧时间统计
    double avgFrameTime = 0;
    double minFrameTime = 1000;
    double maxFrameTime = 0;
    int statFrameCount = 0;
    
    int lastWidth = 0, lastHeight = 0;
    
    while (g_Running) {
        auto frameStart = std::chrono::steady_clock::now();
        
        if (g_NeedResize) {
            g_NeedResize = false;
            int targetW = g_TargetWidth;
            int targetH = g_TargetHeight;
            
            if (targetW > 0 && targetH > 0 && (targetW != lastWidth || targetH != lastHeight)) {
                LOGI("Resizing swapchain to %dx%d", targetW, targetH);
                
                vk.vkDeviceWaitIdle(g_Ctx->device);
                
                CleanupSwapchain(true);
                
                if (!CreateSwapchain(window) ||
                    !CreateImageViews() ||
                    !CreateFramebuffers()) {
                    LOGE("Failed to recreate swapchain after resize");
                    lastWidth = 0;
                    lastHeight = 0;
                    continue;
                }
                
                g_Ctx->imguiInitInfo.ImageCount = (uint32_t)g_Ctx->swapchainImages.size();
                g_Ctx->imguiInitInfo.MinImageCount = std::max(2u, (uint32_t)g_Ctx->swapchainImages.size());
                
                ImGuiIO& io = ImGui::GetIO();
                io.DisplaySize = ImVec2((float)targetW, (float)targetH);
                io.DisplayFramebufferScale = ImVec2(1.0f, 1.0f);
                
                px = (float)targetW;
                py = (float)targetH;
                
                lastWidth = targetW;
                lastHeight = targetH;
                
                LOGI("Resize completed: %dx%d", lastWidth, lastHeight);
            }
        }
        
        // 渲染一帧
        float dt = std::chrono::duration<float>(std::chrono::steady_clock::now() - lastTime).count();
        lastTime = std::chrono::steady_clock::now();
        
        if (g_Ctx->needRebuild) {
            RebuildSwapchain(window);
            g_Ctx->needRebuild = false;
        }
        
        bool renderSuccess = RenderFrame(window, clearColor, dt, frameCount);
        
        auto frameEnd = std::chrono::steady_clock::now();
        double frameTimeMs = std::chrono::duration<double, std::milli>(frameEnd - frameStart).count();
        
        // 统计帧时间
        statFrameCount++;
        avgFrameTime = (avgFrameTime * (statFrameCount - 1) + frameTimeMs) / statFrameCount;
        if (frameTimeMs < minFrameTime) minFrameTime = frameTimeMs;
        if (frameTimeMs > maxFrameTime) maxFrameTime = frameTimeMs;
        
        // FPS 计算和日志
        fpsFrameCount++;
        auto now = std::chrono::steady_clock::now();
        float elapsedMs = std::chrono::duration<float, std::milli>(now - fpsLogTime).count();
        if (elapsedMs >= 1000.0f) {
            float fps = fpsFrameCount * 1000.0f / elapsedMs;
            LOGI("FPS: %.1f/%d | Frame: %.2fms (min:%.2f, max:%.2f, avg:%.2f) | %dx%d", 
                 fps, g_TargetFPS, frameTimeMs, minFrameTime, maxFrameTime, avgFrameTime, lastWidth, lastHeight);
            fpsFrameCount = 0;
            fpsLogTime = now;
            
            // 重置统计
            minFrameTime = 1000;
            maxFrameTime = 0;
        }
        
        // 帧率控制 - 更精确的方法
        double targetMs = g_TargetFrameTime;
        double elapsedSinceLast = std::chrono::duration<double, std::milli>(frameEnd - lastFrameTime).count();
        
        if (elapsedSinceLast < targetMs) {
            double sleepTime = targetMs - elapsedSinceLast;
            if (sleepTime > 0.1) {
                // 睡眠大部分时间，留一点余量
                std::this_thread::sleep_for(std::chrono::microseconds((int64_t)((sleepTime - 0.1) * 1000)));
            }
            // 忙等待精确调整
            auto waitStart = std::chrono::steady_clock::now();
            while (std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - lastFrameTime).count() < targetMs) {
                // 短暂让步，避免100% CPU
                if (std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - waitStart).count() > 0.5) {
                    std::this_thread::yield();
                    waitStart = std::chrono::steady_clock::now();
                }
            }
        }
        
        lastFrameTime = std::chrono::steady_clock::now();
        frameCount++;
        
        if (!renderSuccess) {
            if (g_Ctx->needRebuild) continue;
            // 渲染失败时短暂休眠
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }
    }
    
    vk.vkDeviceWaitIdle(g_Ctx->device);
    
    ImGui_ImplVulkan_Shutdown();
    ImGui_ImplAndroid_Shutdown();
    ImGui::DestroyContext();
    
    for (auto& f : g_Ctx->frames) {
        if (f.fence) vk.vkDestroyFence(g_Ctx->device, f.fence, g_Ctx->allocator);
        if (f.renderComplete) vk.vkDestroySemaphore(g_Ctx->device, f.renderComplete, g_Ctx->allocator);
        if (f.imageAcquired) vk.vkDestroySemaphore(g_Ctx->device, f.imageAcquired, g_Ctx->allocator);
        if (f.commandBuffer) vk.vkFreeCommandBuffers(g_Ctx->device, g_Ctx->commandPool, 1, &f.commandBuffer);
    }
    
    CleanupSwapchain(true);
    
    if (g_Ctx->pipelineCache) vk.vkDestroyPipelineCache(g_Ctx->device, g_Ctx->pipelineCache, g_Ctx->allocator);
    if (g_Ctx->renderPass) vk.vkDestroyRenderPass(g_Ctx->device, g_Ctx->renderPass, g_Ctx->allocator);
    if (g_Ctx->commandPool) vk.vkDestroyCommandPool(g_Ctx->device, g_Ctx->commandPool, g_Ctx->allocator);
    if (g_Ctx->descriptorPool) vk.vkDestroyDescriptorPool(g_Ctx->device, g_Ctx->descriptorPool, g_Ctx->allocator);
    if (g_Ctx->device) vk.vkDestroyDevice(g_Ctx->device, g_Ctx->allocator);
    if (g_Ctx->surface) vk.vkDestroySurfaceKHR(g_Ctx->instance, g_Ctx->surface, g_Ctx->allocator);
    if (g_Ctx->instance) vk.vkDestroyInstance(g_Ctx->instance, g_Ctx->allocator);
    
    if (g_Ctx->vulkanLib) dlclose(g_Ctx->vulkanLib);
    
    g_Ctx.reset();
    LOGI("Shutdown complete");
}





//JNI键盘调用方法
static bool g_TextInputActive = false;
static std::string g_InputTextBuffer;

JavaVM* g_JVM;
// 全局引用，避免每次FindClass
static jclass g_KeyboardViewServiceClass = nullptr;

// 初始化全局引用
void InitGlobalClasses(JNIEnv* env) {
    if (g_KeyboardViewServiceClass == nullptr) {
        jclass localClass = env->FindClass("com/Neko/YouXi/KeyboardViewService");
        if (localClass != nullptr) {
            g_KeyboardViewServiceClass = (jclass)env->NewGlobalRef(localClass);
            env->DeleteLocalRef(localClass);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "ImGui", "Failed to find KeyboardViewService class");
        }
    }
}

// 清理全局引用
void CleanupGlobalClasses(JNIEnv* env) {
    if (g_KeyboardViewServiceClass != nullptr) {
        env->DeleteGlobalRef(g_KeyboardViewServiceClass);
        g_KeyboardViewServiceClass = nullptr;
    }
}

void ShowAndroidKeyboard(bool show) {
    if (g_JVM == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, "ImGui", "JVM is null");
        return;
    }
    
    JNIEnv* env = nullptr;
    int getEnvStat = g_JVM->GetEnv((void**)&env, JNI_VERSION_1_6);
    
    bool attached = false;
    if (getEnvStat == JNI_EDETACHED) {
        if (g_JVM->AttachCurrentThread(&env, nullptr) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, "ImGui", "Failed to attach thread");
            return;
        }
        attached = true;
    } else if (getEnvStat != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "ImGui", "Failed to get JNIEnv");
        return;
    }
    
    // 检查全局类引用是否已初始化
    if (g_KeyboardViewServiceClass == nullptr) {
        InitGlobalClasses(env);
        if (g_KeyboardViewServiceClass == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "ImGui", "KeyboardViewService class not initialized");
            if (attached) {
                g_JVM->DetachCurrentThread();
            }
            return;
        }
    }
    
    // 获取方法ID
    jmethodID methodId = env->GetStaticMethodID(g_KeyboardViewServiceClass, 
                                               "addKeyboardView", 
                                               "()V");
    if (methodId != nullptr) {
        env->CallStaticVoidMethod(g_KeyboardViewServiceClass, methodId);
        __android_log_print(ANDROID_LOG_INFO, "ImGui", "Called addKeyboardView successfully");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "ImGui", "addKeyboardView method not found");
        // 尝试查找show/hide方法
        jmethodID altMethodId = nullptr;
        if (show) {
            altMethodId = env->GetStaticMethodID(g_KeyboardViewServiceClass, 
                                                "showKeyboardView", 
                                                "()V");
        } else {
            altMethodId = env->GetStaticMethodID(g_KeyboardViewServiceClass, 
                                                "hideKeyboardView", 
                                                "()V");
        }
        if (altMethodId != nullptr) {
            env->CallStaticVoidMethod(g_KeyboardViewServiceClass, altMethodId);
        }
    }
    
    if (attached) {
        g_JVM->DetachCurrentThread();
    }
}

// 开始文本输入模式
void StartTextInput() {
    if (!g_TextInputActive) {
        g_TextInputActive = true;
        g_InputTextBuffer.clear();
        ShowAndroidKeyboard(true);
    }
}

// 结束文本输入模式
void StopTextInput() {
    if (g_TextInputActive) {
        g_TextInputActive = false;
        g_InputTextBuffer.clear();
        ShowAndroidKeyboard(false);
    }
}

// 检查 ImGui 输入状态
void CheckImGuiTextInput() {
    ImGuiIO& io = ImGui::GetIO();
    
    static bool wasTextInputActive = false;
    bool isTextInputActive = io.WantTextInput;
    
    if (isTextInputActive && !wasTextInputActive) {
        StartTextInput();
    } else if (!isTextInputActive && wasTextInputActive) {
        StopTextInput();
    }
    
    wasTextInputActive = isTextInputActive;
}

// 处理 Android 输入到 ImGui
void ProcessAndroidInputToImGui() {
    ImGuiIO& io = ImGui::GetIO();
    
    if (!g_InputTextBuffer.empty()) {
        // 将输入的字符添加到 ImGui
        for (char c : g_InputTextBuffer) {
            io.AddInputCharacter(c);
        }
        g_InputTextBuffer.clear();
    }
}







// JNI 入口
extern "C" JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_Init(JNIEnv* env, jobject thiz, jobject surface) {
    if (!surface) {
        LOGE("Surface is null");
        return;
    }
    
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (!window) {
        LOGE("Failed to get ANativeWindow");
        return;
    }
    
    ANativeWindow_acquire(window);
    
    std::thread([window]() {
        RenderLoop(window);
        ANativeWindow_release(window);
    }).detach();
}

extern "C" JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_SetTargetFPS(JNIEnv* env, jobject thiz, jint fps) {
    if (fps > 0 && fps <= 240) {
        g_TargetFPS = fps;
        g_TargetFrameTime = 1000.0 / fps;
        LOGI("Target FPS set to: %d (%.2f ms per frame)", fps, g_TargetFrameTime);
    } else {
        LOGW("Invalid FPS value: %d, using default 60", fps);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_MotionEventClick(JNIEnv * env, jobject thiz, jboolean action, jfloat pos_x, jfloat pos_y) {
    if (!ImGui::GetCurrentContext()) return;
        
    ImGuiIO& io = ImGui::GetIO();
    io.AddMousePosEvent(pos_x, pos_y);
    io.AddMouseButtonEvent(0, action);
}




extern "C" {
    jint JNI_OnLoad(JavaVM *vm, void *reserved) {
        JNIEnv *env;
        g_JVM = vm;
        if (vm->GetEnv((void **) (&env), JNI_VERSION_1_6) != JNI_OK) {
            return -1;
        }
        assert(env != NULL);
        
        // 在JNI_OnLoad中预先初始化全局类引用
        InitGlobalClasses(env);
        
        return JNI_VERSION_1_6;
	}
}

// 添加一个辅助函数，用于判断一个窗口是否为 Child 窗口
static bool IsWindowChild(ImGuiWindow* window) {
    if (!window) return false;
    // 核心逻辑：检查窗口的标志位中是否包含 ImGuiWindowFlags_ChildWindow
    return (window->Flags & ImGuiWindowFlags_ChildWindow) != 0;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_Neko_YouXi_JNI_getWindowsId(JNIEnv* env, jobject thiz) {
    if (!ImGui::GetCurrentContext()) return nullptr;
    
    ImGuiContext& g = *GImGui;
    std::vector<jint> windowIds;

    for (int i = 0; i < g.Windows.Size; i++) {
        ImGuiWindow* window = g.Windows[i];
        // 修改点：加入 !IsWindowChild(window) 条件，排除子窗口
        if (window && window->Active && !window->Hidden && window->WasActive && !IsWindowChild(window)) {
            windowIds.push_back((jint)(intptr_t)window);
        }
    }

    jintArray result = env->NewIntArray(windowIds.size());
    if (result && !windowIds.empty()) {
        env->SetIntArrayRegion(result, 0, windowIds.size(), windowIds.data());
    }
    return result;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_Neko_YouXi_JNI_getWindowsRect(JNIEnv* env, jobject thiz) {
    if (!ImGui::GetCurrentContext()) return nullptr;

    ImGuiContext& g = *GImGui;
    std::vector<jint> windowRects;

    for (int i = 0; i < g.Windows.Size; i++) {
        ImGuiWindow* window = g.Windows[i];
        // 修改点：同样在这里排除子窗口
        if (window && window->Active && !window->Hidden && window->WasActive && !IsWindowChild(window)) {
            int left = (int)window->Pos.x;
            int top = (int)window->Pos.y;
            int right = (int)(window->Pos.x + window->Size.x);
            int bottom = (int)(window->Pos.y + window->Size.y);

            if (right > left && bottom > top) {
                windowRects.push_back(left);
                windowRects.push_back(top);
                windowRects.push_back(right);
                windowRects.push_back(bottom);
            }
        }
    }

    jintArray result = env->NewIntArray(windowRects.size());
    if (result && !windowRects.empty()) {
        env->SetIntArrayRegion(result, 0, windowRects.size(), windowRects.data());
    }
    return result;
}


extern "C" JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_OnSurfaceChanged(JNIEnv* env, jobject thiz, jint width, jint height) {
    LOGI("OnSurfaceChanged received: %dx%d", width, height);
    
    g_TargetWidth = width;
    g_TargetHeight = height;
    
    LOGI("Set target dimensions: %dx%d", g_TargetWidth, g_TargetHeight);
    g_NeedResize = true;
}


extern "C" {

JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_UpdateInputText(JNIEnv* env, jobject thiz, jstring text) {
    const char* utf8Chars = env->GetStringUTFChars(text, nullptr);
        if (!g_TextInputActive) {
            return;
        }
    
        const char* utf8Charsx = env->GetStringUTFChars(text, nullptr);
        if (utf8Chars != nullptr) {
            // 直接传递给 ImGui
            ImGuiIO& io = ImGui::GetIO();
        
            // 方法1: 使用 AddInputCharactersUTF8 (推荐用于中文)
            io.AddInputCharactersUTF8(utf8Charsx);
        
            env->ReleaseStringUTFChars(text, utf8Charsx);
        }
}

JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_DeleteInputText(JNIEnv* env, jobject thiz) {
    if (!g_TextInputActive) {
            return;
        }
    
        ImGuiIO& io = ImGui::GetIO();
    
        // 方法1: 发送退格键事件
        io.AddKeyEvent(ImGuiKey_Backspace, true);
        io.AddKeyEvent(ImGuiKey_Backspace, false);
}

JNIEXPORT void JNICALL
Java_com_Neko_YouXi_JNI_showKeyboard(JNIEnv* env, jobject thiz, jboolean show) {
    ShowAndroidKeyboard(show);
}

} // extern "C"
