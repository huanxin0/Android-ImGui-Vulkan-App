#include "imgui.h"
#include "字体.h"
#include "imgui_internal.h"
#include "Includes.h"
#include <vector>
#include <cstdlib>
#include <ctime>
#include <cmath>

float px = 1080;
float py = 1920;

// 雪花结构（圆形粒子）
struct SnowFlake {
    float x, y;
    float speed;
    float size;
    float baseSize;     // 基础大小
    int colorR, colorG, colorB;
    float angle;        // 飘落摆动角度
};

std::vector<SnowFlake> g_snowFlakes;
bool g_snowInitialized = false;

// 获取随机颜色（彩色亮色系）
void GetRandomColor(int& r, int& g, int& b) {
    int colorType = rand() % 7;
    switch(colorType) {
        case 0: r = 255; g = 80 + rand() % 100; b = 80 + rand() % 100; break;   // 红/粉
        case 1: r = 80 + rand() % 100; g = 255; b = 80 + rand() % 100; break;   // 绿
        case 2: r = 80 + rand() % 100; g = 80 + rand() % 100; b = 255; break;   // 蓝
        case 3: r = 255; g = 200 + rand() % 55; b = 80 + rand() % 100; break;   // 橙/黄
        case 4: r = 255; g = 80 + rand() % 100; b = 255; break;                 // 紫
        case 5: r = 80 + rand() % 100; g = 255; b = 255; break;                 // 青
        default: r = 220 + rand() % 35; g = 220 + rand() % 35; b = 220 + rand() % 35; break; // 白/银
    }
}

// 初始化雪花
void InitSnow(int count = 250, int screenWidth = 1080, int screenHeight = 1920) {
    g_snowFlakes.resize(count);
    for (auto& flake : g_snowFlakes) {
        flake.x = rand() % screenWidth;
        flake.y = rand() % screenHeight;
        flake.speed = 40 + rand() % 120;       // 速度 40~160 像素/秒
        flake.baseSize = 8 + rand() % 15;      // 基础大小 3~13 像素
        flake.size = flake.baseSize;
        flake.angle = (rand() % 360) * 3.14159f / 180.0f;  // 随机摆动角度
        GetRandomColor(flake.colorR, flake.colorG, flake.colorB);
    }
    g_snowInitialized = true;
}

// 在窗口内绘制彩色圆形雪花
void DrawSnowInWindow(ImDrawList* drawList, int windowWidth, int windowHeight, const ImVec2& windowPos) {
    if (!g_snowInitialized) {
        srand((unsigned int)time(nullptr));
        InitSnow(250, windowWidth, windowHeight);
        return;
    }
    
    float deltaTime = ImGui::GetIO().DeltaTime;
    // 限制 deltaTime 防止突变
    if (deltaTime > 0.033f) deltaTime = 0.033f;
    
    // ========== 方案1：平滑滤波（增加空间感） ==========
    static float smoothBeat = 0.0f;
    static float lastBeat = 0.0f;
    
    float rawBeat = g_BeatIntensity;
    
    // 指数移动平均（EMA）- 让变化更平滑
    smoothBeat = smoothBeat * 0.7f + rawBeat * 0.3f;
    
    // 峰值保持效果（让节奏感更强但不闪烁）
    if (rawBeat > lastBeat) {
        // 上升时快速响应
        smoothBeat = rawBeat;
    } else {
        // 下降时缓慢衰减
        smoothBeat = smoothBeat * 0.92f;
    }
    
    lastBeat = rawBeat;
    
    // 限制范围
    if (smoothBeat < 0) smoothBeat = 0;
    if (smoothBeat > 1) smoothBeat = 1;
    
    // 基础缩放（整体节奏影响）
    float basePulseScale = 1.0f + smoothBeat * 0.9f;
    // ========== 方案1结束 ==========
    
    // 更新所有雪花
    for (auto& flake : g_snowFlakes) {
        // 更新 Y 坐标（下落）
        flake.y += flake.speed * deltaTime;
        
        // 轻微左右摆动（飘落效果）
        flake.x += sinf(flake.angle) * 30 * deltaTime;
        flake.angle += 1.5f * deltaTime;
        
        // 超出窗口底部就重置到顶部
        if (flake.y > windowHeight) {
            flake.y = 0;
            flake.x = rand() % windowWidth;
            flake.speed = 40 + rand() % 120;
            flake.baseSize = 8 + rand() % 15;
            GetRandomColor(flake.colorR, flake.colorG, flake.colorB);
        }
        
        // 超出左右边界则反向
        if (flake.x < -20) flake.x = windowWidth + 20;
        if (flake.x > windowWidth + 20) flake.x = -20;
        
        // ========== 空间感：每个雪花独立的反应系数 ==========
        // 根据雪花的基础大小和速度，计算个性化系数
        static float individualFactor[250] = {0};  // 存储每个雪花的个性系数
        static bool factorInit = false;
        
        if (!factorInit) {
            for (int i = 0; i < 250; i++) {
                // 系数范围 0.5 ~ 1.5，让雪花反应各不相同
                individualFactor[i] = 0.5f + (rand() % 100) / 100.0f;
            }
            factorInit = true;
        }
        
        // 个性系数（每个雪花不同）
        float personality = individualFactor[&flake - &g_snowFlakes[0]];
        
        // 空间深度系数：Y坐标越靠下（越近）反应越大，越靠上（越远）反应越小
        float depthFactor = 0.6f + (flake.y / windowHeight) * 0.8f;
        
        // 综合系数
        float combinedFactor = personality * depthFactor;
        
        // 不同雪花响应速度不同（快的先变大，慢的后变大）
        static float snowState[250] = {0};  // 存储每个雪花的当前状态
        int idx = &flake - &g_snowFlakes[0];
        
        // 根据个性系数决定响应速度（系数越大响应越快）
        float responseSpeed = 0.3f + personality * 0.5f;
        float decaySpeed = 0.85f + personality * 0.1f;
        
        // 目标缩放值
        float targetScale = 1.0f + (basePulseScale - 1.0f) * combinedFactor;
        
        // 平滑过渡到目标值（不同雪花速度不同）
        snowState[idx] = snowState[idx] * (1.0f - responseSpeed) + targetScale * responseSpeed;
        
        // 下降时也按个性速度衰减
        if (targetScale < snowState[idx]) {
            snowState[idx] = snowState[idx] * decaySpeed + targetScale * (1.0f - decaySpeed);
        }
        
        float finalScale = snowState[idx];
        
        // 限制范围
        if (finalScale < 0.8f) finalScale = 0.8f;
        if (finalScale > 2.2f) finalScale = 2.2f;
        // ========== 空间感结束 ==========
        
        // 根据音乐节奏动态改变大小
        float dynamicSize = flake.baseSize * finalScale;
        
        // 根据速度决定透明度（固定透明度，不再随节奏变化）
        float alpha = 120 + (flake.speed - 40) / 120 * 100;
        if (alpha > 220) alpha = 220;
        if (alpha < 100) alpha = 100;
        
        // 绘制圆形雪花
        ImVec2 center = ImVec2(windowPos.x + flake.x, windowPos.y + flake.y);
        float radius = dynamicSize * 0.8f;
        
        // 主圆形
        drawList->AddCircleFilled(center, radius,
            IM_COL32(flake.colorR, flake.colorG, flake.colorB, (int)alpha));
    }
}

// 重新初始化雪花（窗口大小变化时调用）
void ResizeSnow(int windowWidth, int windowHeight) {
    if (g_snowInitialized) {
        for (auto& flake : g_snowFlakes) {
            if (flake.x > windowWidth) flake.x = rand() % windowWidth;
            if (flake.y > windowHeight) flake.y = rand() % windowHeight;
        }
    }
}

void YouXiStyle()
{
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
    io.IniFilename = nullptr;

    ImGui::StyleColorsDark();          // 先基于 Dark
    ImGuiStyle& style = ImGui::GetStyle();
    style.ScaleAllSizes(2.0f);         // 你原来的缩放，不动

    ImVec4* colors = style.Colors;

    /* ================= 基础背景 ================= */
    colors[ImGuiCol_WindowBg]        = ImVec4(0.09f, 0.11f, 0.17f, 1.00f); // 深蓝黑
    colors[ImGuiCol_ChildBg]         = ImVec4(0.12f, 0.14f, 0.20f, 0.50f); // 子窗口
    colors[ImGuiCol_PopupBg]         = ImVec4(0.10f, 0.12f, 0.18f, 1.00f); // 下拉框

    /* ================= 标题栏 ================= */
    colors[ImGuiCol_TitleBg]         = ImVec4(0.13f, 0.16f, 0.24f, 1.00f);
    colors[ImGuiCol_TitleBgActive]   = ImVec4(0.14f, 0.16f, 0.25f, 1.00f); // 科技蓝
    colors[ImGuiCol_TitleBgCollapsed]= ImVec4(0.10f, 0.12f, 0.18f, 1.00f);

    /* ================= 边框 ================= */
    colors[ImGuiCol_Border]          = ImVec4(0.20f, 0.45f, 0.85f, 0.35f);
    colors[ImGuiCol_BorderShadow]    = ImVec4(0.00f, 0.00f, 0.00f, 0.00f);

    /* ================= 文字 ================= */
    colors[ImGuiCol_Text]            = ImVec4(1.00f, 1.00f, 1.00f, 1.00f);
    colors[ImGuiCol_TextDisabled]    = ImVec4(0.55f, 0.60f, 0.70f, 1.00f);

    /* ================= 按钮 ================= */
    colors[ImGuiCol_Button]          = ImVec4(0.18f, 0.22f, 0.32f, 1.00f);
    colors[ImGuiCol_ButtonHovered]   = ImVec4(0.25f, 0.50f, 0.90f, 1.00f); // 亮蓝
    colors[ImGuiCol_ButtonActive]   = ImVec4(0.15f, 0.35f, 0.70f, 1.00f); // 深蓝

    /* ================= 输入框 / 滑块 ================= */
    colors[ImGuiCol_FrameBg]         = ImVec4(0.14f, 0.16f, 0.22f, 1.00f);
    colors[ImGuiCol_FrameBgHovered]  = ImVec4(0.20f, 0.45f, 0.85f, 0.40f);
    colors[ImGuiCol_FrameBgActive]  = ImVec4(0.20f, 0.45f, 0.85f, 0.60f);

    colors[ImGuiCol_SliderGrab]      = ImVec4(0.25f, 0.50f, 0.90f, 1.00f);
    colors[ImGuiCol_SliderGrabActive]= ImVec4(0.30f, 0.60f, 1.00f, 1.00f);

    /* ================= 列表 / 下拉 ================= */
    colors[ImGuiCol_Header]          = ImVec4(0.25f, 0.50f, 0.90f, 0.35f);
    colors[ImGuiCol_HeaderHovered]   = ImVec4(0.25f, 0.50f, 0.90f, 0.60f);
    colors[ImGuiCol_HeaderActive]    = ImVec4(0.25f, 0.50f, 0.90f, 0.80f);

    /* ================= Tab ================= */
    colors[ImGuiCol_Tab]             = ImVec4(0.13f, 0.16f, 0.24f, 1.00f);
    colors[ImGuiCol_TabHovered]     = ImVec4(0.25f, 0.50f, 0.90f, 0.50f);
    colors[ImGuiCol_TabActive]       = ImVec4(0.20f, 0.45f, 0.85f, 1.00f);

    /* ================= Checkbox / Radio ================= */
    colors[ImGuiCol_CheckMark]       = ImVec4(0.25f, 0.50f, 0.90f, 1.00f);

    /* ================= 滚动条 ================= */
    colors[ImGuiCol_ScrollbarBg]    = ImVec4(0.08f, 0.10f, 0.15f, 1.00f);
    colors[ImGuiCol_ScrollbarGrab]  = ImVec4(0.25f, 0.50f, 0.90f, 0.40f);
    colors[ImGuiCol_ScrollbarGrabHovered]
                                    = ImVec4(0.25f, 0.50f, 0.90f, 0.60f);

    /* ================= 圆角 & 细节 ================= */
    style.WindowRounding   = 10.0f;
    style.ChildRounding    = 10.0f;
    style.FrameRounding    = 8.0f;
    style.PopupRounding    = 8.0f;
    style.ScrollbarRounding= 6.0f;

    style.WindowPadding    = ImVec2(16, 16);
    style.ItemSpacing      = ImVec2(12, 10);

    /* ================= 字体 ================= */
    io.Fonts->AddFontFromMemoryTTF(
        (void*)font_data,
        font_size,
        26.0f,
        nullptr,
        io.Fonts->GetGlyphRangesChineseFull()
    );
}

// 修改 YouXiMenu 中的调用
void YouXiMenu() {
	
	
    CheckImGuiTextInput();
    ProcessAndroidInputToImGui();
    
    ImGui::SetNextWindowPos({20, 20}, ImGuiCond_FirstUseEver);
    ImGui::SetNextWindowSize({1050, 700}, ImGuiCond_FirstUseEver);
    
    float fps = ImGui::GetIO().Framerate;
    
    // 创建带背景的窗口
    ImGui::Begin("莜汐-By雾璃梦汐", nullptr, ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoScrollbar);
    
    ImDrawList* drawList = ImGui::GetWindowDrawList();
    ImVec2 windowPos = ImGui::GetWindowPos();
    ImVec2 windowSize = ImGui::GetWindowSize();
    
    // 保存当前裁剪区域，并设置窗口裁剪区域
    drawList->PushClipRect(windowPos, ImVec2(windowPos.x + windowSize.x, windowPos.y + windowSize.y), true);
    
    // 在窗口内绘制彩色圆形雪花（跟随音乐节奏）
    DrawSnowInWindow(drawList, (int)windowSize.x, (int)windowSize.y, windowPos);
    
    // 恢复裁剪区域
    drawList->PopClipRect();
    
    static bool 主页 = true;
    static bool 音乐 = false;
	static bool 设置 = false;
    
    ImGui::BeginChild("左面板", ImVec2(110, 0), false);
    
    if (ImGui::Button("主页",ImVec2(110,80))) {
        主页 = true;
        音乐 = false;
		设置 = false;
    }
    if (ImGui::Button("音乐",ImVec2(110,80))) {
        主页 = false;
        音乐 = true;
		设置 = false;
    }
	if (ImGui::Button("设置",ImVec2(110,80))) {
        主页 = false;
        音乐 = false;
		设置 = true;
    }
    
    ImGui::EndChild();
    ImGui::SameLine();
    ImGui::BeginChild("右面板", ImVec2(900, 0), false, ImGuiWindowFlags_NoScrollbar);
    if (主页) {
        // UI 内容（会显示在下雪背景上方）
        ImGui::Text("Resolution: %.1fx%.1f", px, py);
        ImGui::Text("平均帧率：%.2f ms/FPS", 1000.0f / fps);
        ImGui::Text("当前帧率：%.1f FPS", fps);
        
        bool static menu = false;
        ImGui::Checkbox("ShowDemoWindow",&menu);
        if(menu) {
            ImGui::ShowDemoWindow(&menu);
        }
    }
    
    if (音乐) {
        音乐窗口();
    }
	
	if (设置) {
		
		static int playMode = 0;  // 0=顺序, 1=随机, 2=循环单曲, 3=循环列表
		
		ImGui::SeparatorText("音乐列表播放设置");
    	ImGui::RadioButton("顺序播放", &playMode, 0);
		ImGui::SameLine();
    	ImGui::RadioButton("随机播放", &playMode, 1);
		ImGui::SameLine();
    	ImGui::RadioButton("循环单曲", &playMode, 2);
		ImGui::SameLine();
    	ImGui::RadioButton("循环列表", &playMode, 3);
    
    	// 根据选择更新 Playlist
    	switch(playMode) {
        	case 0: g_Playlist.shuffle = false; g_Playlist.repeat = false; break;
        	case 1: g_Playlist.shuffle = true;  g_Playlist.repeat = false; break;
        	case 2: g_Playlist.shuffle = false; g_Playlist.repeat = true; break;
        	case 3: g_Playlist.shuffle = true;  g_Playlist.repeat = true; break;
    	}
		
		
		
		
	}
	
    ImGui::EndChild();
    ImGui::End();
}

