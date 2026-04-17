package com.Neko.YouXi;

interface JniRoot {
    void Init(in Surface surface);
    void MotionEventClick(boolean down, float PosX, float PosY);
    void OnSurfaceChanged(int width, int height);
    void SetTargetFPS(int fps);
    
    int[] getWindowsRect();
    int[] getWindowsId();
    
    void UpdateInputText(String text);
    void DeleteInputText();
    void showKeyboard(boolean show);
}
