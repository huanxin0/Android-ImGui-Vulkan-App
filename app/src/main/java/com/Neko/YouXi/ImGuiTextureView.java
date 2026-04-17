package com.Neko.YouXi;

import androidx.annotation.NonNull;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import com.Neko.YouXi.JNI;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.Root.ImGuiAidlService;

public class ImGuiTextureView extends TextureView implements TextureView.SurfaceTextureListener {
	
    public ImGuiTextureView(Context context) {
        super(context);
        this.setOpaque(true);
        setSurfaceTextureListener(this);
    }
   
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        try {
            if (AppConfig.app_Operation_mode_root) {
                ImGuiAidlService.GetIPC().OnSurfaceChanged(width, height);
                ImGuiAidlService.GetIPC().Init(new Surface(surface));
            } else {
                JNI.OnSurfaceChanged(width, height);
                JNI.Init(new Surface(surface));            
            }
       } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d("ImGuiTextureView", "surfaceCreated_");
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        Log.e("NDK-java", "onSurfaceTextureDestroyed"); //结束
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        //Log.e("NDK-java", "onSurfaceTextureUpdated");
    }
    
    
}



