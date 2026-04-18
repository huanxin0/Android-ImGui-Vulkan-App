package com.Neko.YouXi;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;
import com.Neko.YouXi.AppConfig;
import com.Neko.YouXi.ImGuiViewService;
import com.Neko.YouXi.JniRoot;
import com.Neko.YouXi.R;
import com.Neko.YouXi.Root.ImGuiAidlService;
import com.Neko.YouXi.Tools;
import com.Neko.YouXi.UI.AboutFragment;
import com.Neko.YouXi.UI.HomeFragment;
import com.Neko.YouXi.UI.SettingsFragment;

public class MainActivity extends Activity {

    private static MainActivity instance;
    private DrawerLayout drawerLayout;
    private FrameLayout contentFrame;

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
        
        if (savedInstanceState == null) {
            switchToFragment(new HomeFragment());
        }
    }

    private View createMainLayout() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setLayoutParams(new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT));
        drawerLayout.setBackgroundColor(Color.WHITE);
        drawerLayout.setScrimColor(Color.parseColor("#99000000"));

        contentFrame = new FrameLayout(this);
        contentFrame.setId(View.generateViewId());
        contentFrame.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        drawerLayout.addView(contentFrame);

        LinearLayout navView = createNavigationView();
        DrawerLayout.LayoutParams navParams = new DrawerLayout.LayoutParams(dpToPx(280), DrawerLayout.LayoutParams.MATCH_PARENT, Gravity.START);
        navView.setLayoutParams(navParams);
        drawerLayout.addView(navView);

        return drawerLayout;
    }

    private LinearLayout createNavigationView() {
        LinearLayout navView = new LinearLayout(this);
        navView.setOrientation(LinearLayout.VERTICAL);
        navView.setBackgroundColor(Color.WHITE);
        navView.setElevation(dpToPx(16));

        LinearLayout navHeader = createNavHeader();
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(160));
        navHeader.setLayoutParams(headerParams);
        navView.addView(navHeader);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        navView.addView(divider);

        addNavItemWithoutIcon(navView, "首页");
        addNavItemWithoutIcon(navView, "设置");
        addNavItemWithoutIcon(navView, "关于");

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        navView.addView(spacer);

        return navView;
    }

    private LinearLayout createNavHeader() {
        LinearLayout headerContainer = new LinearLayout(this);
        headerContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        headerContainer.setOrientation(LinearLayout.VERTICAL);
        
        ImageView backgroundImage = new ImageView(this);
        backgroundImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImage.setImageResource(R.drawable.ic_launcher);
        headerContainer.addView(backgroundImage);
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24));
        content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        ImageView avatar = new ImageView(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(70), dpToPx(70)));
        avatar.setImageResource(R.drawable.ic_launcher);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            avatar.setClipToOutline(true);
        }
        content.addView(avatar);
        
        TextView appName = new TextView(this);
        appName.setText("莜汐");
        appName.setTextSize(20);
        appName.setTextColor(Color.WHITE);
        appName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        appName.setPadding(0, dpToPx(12), 0, 0);
        content.addView(appName);
        
        TextView version = new TextView(this);
        version.setText("v1.0.0");
        version.setTextSize(12);
        version.setTextColor(Color.parseColor("#CCFFFFFF"));
        content.addView(version);
        
        headerContainer.addView(content);
        
        return headerContainer;
    }

    private void addNavItemWithoutIcon(LinearLayout parent, String title) {
        LinearLayout item = new LinearLayout(this);
        item.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52)));
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dpToPx(20), 0, dpToPx(16), 0);
        item.setClickable(true);
        item.setFocusable(true);
        
        item.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setBackgroundColor(Color.parseColor("#E8E8E8"));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setBackgroundColor(Color.TRANSPARENT);
                    break;
            }
            return false;
        });
        
        item.setOnClickListener(v -> {
            switch (title) {
                case "首页":
                    switchToFragment(new HomeFragment());
                    break;
                case "设置":
                    switchToFragment(new SettingsFragment());
                    break;
                case "关于":
                    switchToFragment(new AboutFragment());
                    break;
            }
            drawerLayout.closeDrawer(Gravity.START);
        });

        TextView text = new TextView(this);
        text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        text.setText(title);
        text.setTextSize(16);
        text.setTextColor(Color.parseColor("#1C1B1F"));
        text.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(text);

        parent.addView(item);
    }

    private void switchToFragment(Fragment fragment) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(contentFrame.getId(), fragment);
        transaction.commit();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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