package com.Neko.YouXi;

/**
 * @Author 雾璃
 * @Date 2026/04/12 13:03
 */
import android.view.Surface;

public class JNI {
    
    public static final String TAG = "JNI";
    
    public static native void Init(Surface surface);
    public static native void MotionEventClick(boolean down, float PosX, float PosY);
    public static native int[] getWindowsRect();
    public static native int[] getWindowsId();
    public static native void OnSurfaceChanged(int width, int height);
    public static native void SetTargetFPS(int fps);
	
	public static native void UpdateInputText(String text);
    public static native void DeleteInputText();
    public static native void showKeyboard(boolean show);
    
}
