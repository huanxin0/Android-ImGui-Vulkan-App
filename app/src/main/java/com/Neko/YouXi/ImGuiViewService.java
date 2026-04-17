package com.Neko.YouXi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.Root.ImGuiAidlService;
import android.graphics.Color;

public class ImGuiViewService {
    private static WindowManager manager;
    private static TouchWindowInfo[] touchWindows = new TouchWindowInfo[32]; // 预分配足够空间
    private static int touchWindowCount = 0;

    // 窗口信息类
    public static class WindowInfo {
        public final int windowId;
        public final Rect rect;

        public WindowInfo(int windowId, Rect rect) {
            this.windowId = windowId;
            this.rect = rect;
        }
    }

    // 触摸窗口信息类
    public static class TouchWindowInfo {
        public int windowId;
        public View touchView;
        public WindowManager.LayoutParams params;
        public boolean inUse; // 标记是否在使用中
        public long lastUpdateTime; // 最后更新时间

        public TouchWindowInfo() {
            // 默认构造函数
        }
    }

    // 上次的窗口信息，用于检测变化
    private static WindowInfo[] previousWindows = null;
    private static long lastUpdateTime = 0;
    private static final long MIN_UPDATE_INTERVAL = 300;
    private static Activity currentActivity;
    private static boolean isRootMode = false;

    public static void showFloatWindow(final Activity Activity) {
		currentActivity = Activity;
		manager = (WindowManager) Activity.getSystemService(Context.WINDOW_SERVICE);
		isRootMode = AppConfig.app_Operation_mode_root;

		// Android 12 以下，保持原有逻辑
		final WindowManager.LayoutParams window_Params = new WindowManager.LayoutParams();
		window_Params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
			View.SYSTEM_UI_FLAG_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

		window_Params.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
			WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		window_Params.gravity = Gravity.TOP | Gravity.LEFT;
		window_Params.format = PixelFormat.TRANSPARENT;
		window_Params.width = WindowManager.LayoutParams.MATCH_PARENT;
		window_Params.height = WindowManager.LayoutParams.MATCH_PARENT;
		window_Params.flags = 
			WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
			WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
			WindowManager.LayoutParams.FLAG_FULLSCREEN |
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
			WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR |
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

		// 低版本不需要强制设置透明度（原代码没有，保持没有）

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			window_Params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}

		if (AppConfig.防录屏) {
			RecorderFakeUtils.setFakeRecorderWindowLayoutParams(window_Params);
		}

		if (AppConfig.兼容模式) {
            final ImGuiTextureView MyTV = new ImGuiTextureView(Activity);
            window_Params.token = MyTV.getApplicationWindowToken();
            manager.addView(MyTV, window_Params);

            // 启动多窗口触摸更新
            updateMultiTouchWindows();
        } else {
            final ImGuiSurfaceView MySV = new ImGuiSurfaceView(Activity);
            window_Params.token = MySV.getApplicationWindowToken();
            manager.addView(MySV, window_Params);

            // 启动多窗口触摸更新
            updateMultiTouchWindows();
        }
	}

    // 多窗口触摸更新
    private static void updateMultiTouchWindows() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					try {
						// 获取窗口ID和矩形数据
						int[] windowIds;
						int[] windowRects;

						if (isRootMode) {
							windowIds = ImGuiAidlService.GetIPC().getWindowsId();
							windowRects = ImGuiAidlService.GetIPC().getWindowsRect();
						} else {
							windowIds = JNI.getWindowsId();
							windowRects = JNI.getWindowsRect();
						}

						processWindowData(windowIds, windowRects);
					} catch (Exception e) {
						e.printStackTrace();
					}
					handler.postDelayed(this, AppConfig.触摸更新频率); // 60 FPS
				}
			}, 17);
    }

    // 处理窗口数据
    private static void processWindowData(int[] windowIds, int[] windowRects) {
        // 如果JNI返回null，清空触摸窗口
        if (windowIds == null || windowRects == null) {
            if (previousWindows != null) {
                previousWindows = null;
                hideAllTouchWindows();
            }
            return;
        }

        // 计算有效窗口数量
        int validWindowCount = Math.min(windowIds.length, windowRects.length / 4);

        if (validWindowCount > 0) {
            // 创建有效的窗口信息数组
            WindowInfo[] currentWindows = new WindowInfo[validWindowCount];
            int currentIndex = 0;

            for (int i = 0; i < validWindowCount; i++) {
                int baseIndex = i * 4;
                int left = windowRects[baseIndex];
                int top = windowRects[baseIndex + 1];
                int right = windowRects[baseIndex + 2];
                int bottom = windowRects[baseIndex + 3];

                // 验证矩形有效性
                if (right > left && bottom > top && 
                    (right - left) > 1 && (bottom - top) > 1) { // 过滤过小的窗口
                    Rect rect = new Rect(left, top, right, bottom);
                    currentWindows[currentIndex++] = new WindowInfo(windowIds[i], rect);
                }
            }

            // 调整实际有效窗口数量
            if (currentIndex < validWindowCount) {
                WindowInfo[] temp = new WindowInfo[currentIndex];
                System.arraycopy(currentWindows, 0, temp, 0, currentIndex);
                currentWindows = temp;
                validWindowCount = currentIndex;
            }

            long currentTime = System.currentTimeMillis();

            // 检查是否需要更新
            boolean needsUpdate = previousWindows == null ||
                currentTime - lastUpdateTime > MIN_UPDATE_INTERVAL ||
                previousWindows.length != validWindowCount;

            if (needsUpdate) {
                lastUpdateTime = currentTime;
                previousWindows = cloneWindowInfos(currentWindows);

                updateTouchWindows(currentWindows);
            } else if (previousWindows != null) {
                // 即使不需要完全更新，也检查窗口位置是否变化
                updateWindowPositions(currentWindows);
            }
        } else {
            if (previousWindows != null) {
                previousWindows = null;
                hideAllTouchWindows();
            }
        }
    }

    // 更新触摸窗口（避免移除和重新创建）
    private static void updateTouchWindows(WindowInfo[] windowInfos) {
        // 第一步：标记所有现有窗口为未使用
        for (int i = 0; i < touchWindowCount; i++) {
            touchWindows[i].inUse = false;
        }

        // 第二步：更新或创建窗口
        for (WindowInfo windowInfo : windowInfos) {
            if (windowInfo == null) continue;

            TouchWindowInfo existingWindow = findTouchWindowById(windowInfo.windowId);

            if (existingWindow != null) {
                // 更新现有窗口
                updateWindowLayout(existingWindow, windowInfo.rect);
                existingWindow.inUse = true;
                existingWindow.lastUpdateTime = System.currentTimeMillis();
            } else {
                // 创建新窗口
                TouchWindowInfo newWindow = createTouchWindow(windowInfo);
                if (newWindow != null) {
                    newWindow.inUse = true;
                    newWindow.lastUpdateTime = System.currentTimeMillis();

                    // 添加到数组
                    if (touchWindowCount < touchWindows.length) {
                        touchWindows[touchWindowCount++] = newWindow;
                    } else {
                        // 需要扩容
                        TouchWindowInfo[] newArray = new TouchWindowInfo[touchWindows.length * 2];
                        System.arraycopy(touchWindows, 0, newArray, 0, touchWindows.length);
                        touchWindows = newArray;
                        touchWindows[touchWindowCount++] = newWindow;
                    }
                }
            }
        }

        // 第三步：移除长时间未使用的窗口（而不是立即移除）
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < touchWindowCount; i++) {
            TouchWindowInfo window = touchWindows[i];
            if (!window.inUse && (currentTime - window.lastUpdateTime > 1000)) { // 1秒后移除
                removeTouchWindow(i);
                i--; // 因为数组元素被移动了
            }
        }
    }

    // 更新窗口位置（优化版，避免频繁更新）
    private static void updateWindowPositions(WindowInfo[] windowInfos) {
        long currentTime = System.currentTimeMillis();

        for (WindowInfo windowInfo : windowInfos) {
            if (windowInfo == null) continue;

            TouchWindowInfo existingWindow = findTouchWindowById(windowInfo.windowId);
            if (existingWindow != null) {
                // 检查位置是否真的需要更新（减少不必要的updateViewLayout调用）
                if (Math.abs(existingWindow.params.x - windowInfo.rect.left) > 2 ||
                    Math.abs(existingWindow.params.y - windowInfo.rect.top) > 2 ||
                    Math.abs(existingWindow.params.width - windowInfo.rect.width()) > 2 ||
                    Math.abs(existingWindow.params.height - windowInfo.rect.height()) > 2) {

                    updateWindowLayout(existingWindow, windowInfo.rect);
                    existingWindow.lastUpdateTime = currentTime;
                }
            }
        }
    }

    // 创建触摸窗口
    private static TouchWindowInfo createTouchWindow(WindowInfo windowInfo) {
        if (currentActivity == null) return null;

        View touchView = new View(currentActivity);
        touchView.setBackgroundColor(0x00000000); // 你的颜色

        touchView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					float rawX = event.getRawX();
					float rawY = event.getRawY();
					int action = event.getActionMasked();

					try {
						switch (action) {
							case MotionEvent.ACTION_DOWN:
							case MotionEvent.ACTION_POINTER_DOWN:
								if (isRootMode) {
									ImGuiAidlService.GetIPC().MotionEventClick(true, rawX, rawY);
								} else {
									JNI.MotionEventClick(true, rawX, rawY);
								}
								break;
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_POINTER_UP:
							case MotionEvent.ACTION_CANCEL:
								if (isRootMode) {
									ImGuiAidlService.GetIPC().MotionEventClick(false, rawX, rawY);
								} else {
									JNI.MotionEventClick(false, rawX, rawY);
								}
								break;
							case MotionEvent.ACTION_MOVE:
								if (isRootMode) {
									ImGuiAidlService.GetIPC().MotionEventClick(true, rawX, rawY);
								} else {
									JNI.MotionEventClick(true, rawX, rawY);
								}
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					return true;
				}});

        WindowManager.LayoutParams touchParams = new WindowManager.LayoutParams();
        touchParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        touchParams.gravity = Gravity.TOP | Gravity.LEFT;
        touchParams.format = PixelFormat.TRANSPARENT;
        touchParams.width = windowInfo.rect.width();
        touchParams.height = windowInfo.rect.height();
        touchParams.x = windowInfo.rect.left;
        touchParams.y = windowInfo.rect.top;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            touchParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        touchParams.flags = 
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        if (AppConfig.防录屏) {
            RecorderFakeUtils.setFakeRecorderWindowLayoutParams(touchParams);
        }

        try {
            manager.addView(touchView, touchParams);

            TouchWindowInfo touchWindowInfo = new TouchWindowInfo();
            touchWindowInfo.windowId = windowInfo.windowId;
            touchWindowInfo.touchView = touchView;
            touchWindowInfo.params = touchParams;
            touchWindowInfo.inUse = true;
            touchWindowInfo.lastUpdateTime = System.currentTimeMillis();

            return touchWindowInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 更新窗口布局
    private static void updateWindowLayout(TouchWindowInfo touchWindowInfo, Rect rect) {
        if (touchWindowInfo.params.x != rect.left || 
            touchWindowInfo.params.y != rect.top ||
            touchWindowInfo.params.width != rect.width() || 
            touchWindowInfo.params.height != rect.height()) {

            touchWindowInfo.params.x = rect.left;
            touchWindowInfo.params.y = rect.top;
            touchWindowInfo.params.width = rect.width();
            touchWindowInfo.params.height = rect.height();

            try {
                manager.updateViewLayout(touchWindowInfo.touchView, touchWindowInfo.params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 隐藏所有触摸窗口（而不是移除）
    private static void hideAllTouchWindows() {
        for (int i = 0; i < touchWindowCount; i++) {
            TouchWindowInfo window = touchWindows[i];
            window.inUse = false;
            window.lastUpdateTime = System.currentTimeMillis();
        }
    }

    // 移除触摸窗口
    private static void removeTouchWindow(int index) {
        TouchWindowInfo window = touchWindows[index];
        try {
            manager.removeView(window.touchView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 从数组中移除
        for (int i = index; i < touchWindowCount - 1; i++) {
            touchWindows[i] = touchWindows[i + 1];
        }
        touchWindows[--touchWindowCount] = null;
    }

    // 根据窗口ID查找触摸窗口
    private static TouchWindowInfo findTouchWindowById(int windowId) {
        for (int i = 0; i < touchWindowCount; i++) {
            if (touchWindows[i].windowId == windowId) {
                return touchWindows[i];
            }
        }
        return null;
    }

    // 克隆窗口信息数组
    private static WindowInfo[] cloneWindowInfos(WindowInfo[] source) {
        if (source == null) return null;
        WindowInfo[] clone = new WindowInfo[source.length];
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                clone[i] = new WindowInfo(source[i].windowId, new Rect(source[i].rect));
            }
        }
        return clone;
    }

    // 清理方法，在Activity销毁时调用
    public static void cleanup() {
        for (int i = 0; i < touchWindowCount; i++) {
            try {
                manager.removeView(touchWindows[i].touchView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        touchWindows = new TouchWindowInfo[32];
        touchWindowCount = 0;
        previousWindows = null;
        currentActivity = null;
    }
}



