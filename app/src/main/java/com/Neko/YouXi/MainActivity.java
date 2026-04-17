package com.Neko.YouXi;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.Neko.YouXi.Tools;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.JniRoot;
import com.Neko.YouXi.ImGuiViewService;
import com.Neko.YouXi.Root.ImGuiAidlService;

public class MainActivity extends Activity {

    private static MainActivity instance;
    private EditText keyEditText;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        
        setFullScreen();
        存储权限();
        Tools.申请悬浮窗权限(this);

        Tools.申请ROOT();
        if (Tools.检测ROOT()) {
            AppConfig.app_Operation_mode_root = true;
        }

        setContentView(createMainLayout());

        int refreshRate = (int) getMaxSupportedRefreshRate(this);
        try {
            if (AppConfig.app_Operation_mode_root) {
                ImGuiAidlService.GetIPC().SetTargetFPS(refreshRate);
            } else {
                JNI.SetTargetFPS(refreshRate);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private View createMainLayout() {
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT));
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setPadding(20, 20, 20, 20);
        rootLayout.setBackgroundColor(Color.WHITE);

        CardView iconCard = new CardView(this);
        iconCard.setLayoutParams(new CardView.LayoutParams(
            CardView.LayoutParams.WRAP_CONTENT,
            CardView.LayoutParams.WRAP_CONTENT));
        iconCard.setRadius(15);
        iconCard.setCardElevation(0);
        
        ImageView iconImage = new ImageView(this);
        iconImage.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        iconImage.setImageResource(com.Neko.YouXi.R.drawable.ic_launcher);
        iconCard.addView(iconImage);
        rootLayout.addView(iconCard);

        TextView titleText = new TextView(this);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        titleText.setText("莜汐");
        titleText.setTextSize(24);
        titleText.setTypeface(Typeface.MONOSPACE);
        titleText.setGravity(Gravity.CENTER);
        ((LinearLayout.LayoutParams) titleText.getLayoutParams()).topMargin = 5;
        rootLayout.addView(titleText);

        TextInputLayout textInputLayout = new TextInputLayout(this);
        LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        inputLayoutParams.topMargin = 20;
        textInputLayout.setLayoutParams(inputLayoutParams);
        textInputLayout.setHint("请输入卡密");
        textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);

        TextInputEditText textInputEditText = new TextInputEditText(this);
        textInputEditText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        textInputEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        textInputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        textInputEditText.setBackground(null);

        keyEditText = textInputEditText;
        textInputLayout.addView(textInputEditText);
        rootLayout.addView(textInputLayout);

        MaterialButton verifyButton = new MaterialButton(this);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = 20;
        verifyButton.setLayoutParams(buttonParams);
        verifyButton.setText("开启");
        verifyButton.setOnClickListener(v -> onVerifyKey());
        rootLayout.addView(verifyButton);

        return rootLayout;
    }

    private void onVerifyKey() {
        String key = keyEditText != null ? keyEditText.getText().toString() : "";
        if (key.isEmpty()) {
            Toast.makeText(this, "请输入卡密", Toast.LENGTH_SHORT).show();
        } else {
            KeyboardViewService.showFloat(MainActivity.this);
            ImGuiViewService.showFloatWindow(MainActivity.this);
        }
    }

    private float getMaxSupportedRefreshRate(Context context) {
        Display display = context.getDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
            float maxRate = 0;
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getRefreshRate() > maxRate) {
                    maxRate = mode.getRefreshRate();
                }
            }
            return maxRate;
        }
        return 60.0f;
    }

    private void 存储权限() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean isGranted = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            
            if (!isGranted) {
                requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 102);
            }
        }
    }

    private void setFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setFullScreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setFullScreen();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImGuiViewService.cleanup();
    }

    static {
        System.loadLibrary("youxi");
    }

    public static class AIDLConnection implements ServiceConnection {
        public static JniRoot iTestService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iTestService = JniRoot.Stub.asInterface(service);
            ImGuiAidlService.InItIPC(iTestService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    }
}