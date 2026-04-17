#pragma once
#include <vector>
#include <string>

extern float px;
extern float py;
extern void YouXiStyle();
extern void YouXiMenu();

extern void CheckImGuiTextInput();
extern void ProcessAndroidInputToImGui();

extern void 音乐窗口();
extern float g_BeatIntensity;


// 声明结构体类型（如果其他cpp需要知道结构体定义）
struct Song {
    std::string name, id, picurl, singers, jumpurl;
};

enum class PlayState { Idle, Loading, Playing, Paused };

struct Playlist {
    std::vector<Song> songs;
    int currentIndex = -1;
    PlayState state = PlayState::Idle;
    float progress = 0.0f, duration = 0.0f;
    bool shuffle = false, repeat = false;
};

// 声明外部变量（告诉编译器这个变量在其他地方定义）
extern Playlist g_Playlist;
extern std::vector<Song> g_SongList;
extern float g_BeatIntensity;
