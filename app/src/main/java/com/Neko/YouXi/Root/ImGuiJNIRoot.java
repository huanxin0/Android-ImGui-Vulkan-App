package com.Neko.YouXi.Root;

import androidx.annotation.NonNull;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.Surface;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.JNI;
import com.Neko.YouXi.JniRoot;
import com.topjohnwu.superuser.ipc.RootService;

import android.content.Context;
import java.io.File;
import android.widget.Toast;

public class ImGuiJNIRoot extends RootService{
    static {
            if (Process.myUid() == 0) { // 检查是否以 root 权限运行
                
                if (AppConfig.is测试) {
                    System.loadLibrary("youxi");
                } 
                if (!AppConfig.is测试) {
                    System.load(AppConfig.CxxPath);
                }
            }
      }

	
	@Override
	public IBinder onBind(@NonNull Intent intent) {
		return new JniRoot.Stub() {
			
            public void Init(Surface surface) throws RemoteException {
                JNI.Init(surface);
            }
            
			public void OnSurfaceChanged(int width, int height) {
				JNI.OnSurfaceChanged(width, height);
			}
			public void SetTargetFPS(int fps) {
				SetTargetFPS(fps);
			}

            public void MotionEventClick(boolean down, float PosX, float PosY) throws RemoteException {
                JNI.MotionEventClick(down, PosX, PosY);
            }
            
            public int[] getWindowsRect() throws RemoteException {
                return JNI.getWindowsRect();
            }
            
            public int[] getWindowsId() throws RemoteException {
                return JNI.getWindowsId();
            }
            
            public void UpdateInputText(String text) throws RemoteException {
                JNI.UpdateInputText(text);
            }
            
            public void DeleteInputText() throws RemoteException {
                JNI.DeleteInputText();
            }
            
            public void showKeyboard(boolean show) throws RemoteException {
                JNI.showKeyboard(show);
            }
		};
	}
}
