package com.Neko.YouXi;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.Root.ImGuiAidlService;

public class ImGuiSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    public ImGuiSurfaceView(Context context) {
        super(context);

        getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);
        setBackgroundColor(0x00000000);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        Log.d("ImGuiSurfaceView","surfaceCreated");
		holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

		if (holder.getSurface() != null) {

			try {
				if (AppConfig.app_Operation_mode_root) {
					ImGuiAidlService.GetIPC().Init(holder.getSurface());
				} else {
					JNI.Init(holder.getSurface());
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		try {
			if (AppConfig.app_Operation_mode_root) {
				ImGuiAidlService.GetIPC().OnSurfaceChanged(width, height);
			} else {
				JNI.OnSurfaceChanged(width, height);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
