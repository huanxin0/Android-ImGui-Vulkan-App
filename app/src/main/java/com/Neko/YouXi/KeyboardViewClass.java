package com.Neko.YouXi;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.JNI;
import com.Neko.YouXi.Root.ImGuiAidlService;

public class KeyboardViewClass {

    public static final String TAG = "KeyboardView";
	
	private static final WindowManager.LayoutParams keyboardViewParams = new WindowManager.LayoutParams();
	private final EditText editText;
	private final WindowManager windowManager;

	public KeyboardViewClass(Context context) {
        editText = new EditText(context) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();
                    try {
                        if (AppConfig.app_Operation_mode_root) {
                            if (keyCode == KeyEvent.KEYCODE_DEL) {
                                ImGuiAidlService.GetIPC().DeleteInputText();
                                return true; // 消费事件，防止EditText自身处理
                            }

                            // 处理回车键
                            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                                ImGuiAidlService.GetIPC().UpdateInputText("\n");
                                return true; // 消费事件
                            }
                        } else {
                            if (keyCode == KeyEvent.KEYCODE_DEL) {
                                JNI.DeleteInputText();
                                return true; // 消费事件，防止EditText自身处理
                            }

                            // 处理回车键
                            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                                JNI.UpdateInputText("\n");
                                return true; // 消费事件
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    
                }
                return super.dispatchKeyEvent(event);
            }
        };

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        setupWindowParams();
        windowManager.addView(editText, keyboardViewParams);
        toggleKeyboard(true);

        editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// 使用更精确的文本变化检测
					if (count > 0) {
						// 有新增文本
						CharSequence addedText = s.subSequence(start, start + count);
						String addedString = addedText.toString();
						if (!addedString.isEmpty()) {
                            try {
                            	if (AppConfig.app_Operation_mode_root) {
                                    ImGuiAidlService.GetIPC().UpdateInputText(addedString);
                                } else {
                                    JNI.UpdateInputText(addedString);
                                }
                            } catch(RemoteException err) {
                            	err.printStackTrace();
                            }
						}
					}
				}

				@Override
				public void afterTextChanged(Editable editable) {
				}
			});
    }

    private void setupWindowParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyboardViewParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                keyboardViewParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        } else {
            keyboardViewParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        keyboardViewParams.format = PixelFormat.RGBA_8888;
        keyboardViewParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        keyboardViewParams.gravity = Gravity.LEFT | Gravity.TOP;
        keyboardViewParams.width = 1;
        keyboardViewParams.height = 1;
    }

    public void toggleKeyboard(final boolean open) {
        new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					if (open) {
						imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
					} else {
						imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
					}
				}
			}, 300);
    }

    public void removeView() {
        if (editText.getParent() != null) {
            toggleKeyboard(false);
            windowManager.removeView(editText);
            KeyboardViewService.keyboardView = null;
        }
    }
}
