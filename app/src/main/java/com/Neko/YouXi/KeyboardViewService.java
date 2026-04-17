package com.Neko.YouXi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import com.Neko.YouXi.KeyboardViewClass;

/**
 * @Author 雾璃
 * @Date 2026/01/25 02:29
 */
public class KeyboardViewService extends Service {
	
	private static KeyboardViewService instance;
    public static WindowManager manager;
    private static WindowManager.LayoutParams containerViewParams;
    public static KeyboardViewClass keyboardView;
    public static final String TAG = "KeyboardViewService";

    public static Context context;
	

    /**
     * 启动悬浮窗服务
     */
    public static void showFloat(Context context) {
        if (instance == null) {
            Intent intent = new Intent(context, KeyboardViewService.class);
            context.startService(intent);
            Log.d(TAG, "服务启动");
        }
    }

    /**
     * 关闭悬浮窗服务
     */
    public static void hideFloat(Context context) {
        Intent intent = new Intent(context, KeyboardViewService.class);
        context.stopService(intent);
        if (instance != null) {
            instance.hide();
        }
    }

    //用于绑定AlguiService组件和其他组件之间的交互 返回null表示不支持绑定
    @Override
    public IBinder onBind(Intent Intent) {
        return null;
    }
    //创建后台服务
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
		instance = this;
        Log.d(TAG, "服务创建");
        initWindowManager();
    }

    //后台服务销毁
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
	
	private void hide() {
        instance = null;
        manager = null;
        stopSelf();
    }

    /**
     * 初始化 WindowManager
     */
    private void initWindowManager() {
        if (manager == null) {
            manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Log.d(TAG, "初始化 WindowManager");
        }
    }

    public static void addKeyboardView() {
        if (keyboardView == null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						// 使用完全限定名确保调用正确的构造函数
						keyboardView = new KeyboardViewClass(instance);
					}
				});
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						// 同样，这里调用自定义KeyboardView的removeView方法
						keyboardView.removeView();
					}
				});
        }
    }
	
	
	public static void showKeyboardView() {
		if (keyboardView == null) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						keyboardView = new KeyboardViewClass(instance);
					}
				});
		}
	}

	public static void hideKeyboardView() {
		if (keyboardView != null) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						keyboardView.removeView();
					}
				});
		}
	}

}
