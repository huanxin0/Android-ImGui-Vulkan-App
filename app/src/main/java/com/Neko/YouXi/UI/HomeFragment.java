package com.Neko.YouXi.UI;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.Neko.YouXi.KeyboardViewService;
import com.Neko.YouXi.ImGuiViewService;
import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        ImageView icon = new ImageView(getActivity());
        icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(120), dpToPx(120)));
        icon.setImageResource(com.Neko.YouXi.R.drawable.ic_launcher);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layout.addView(icon);

        TextView welcomeText = new TextView(getActivity());
        welcomeText.setText("欢迎使用莜汐");
        welcomeText.setTextSize(24);
        welcomeText.setTextColor(Color.parseColor("#1C1B1F"));
        welcomeText.setGravity(Gravity.CENTER);
        welcomeText.setPadding(0, dpToPx(16), 0, dpToPx(8));
        layout.addView(welcomeText);

        TextView descText = new TextView(getActivity());
        descText.setText("辅助工具");
        descText.setTextSize(16);
        descText.setTextColor(Color.parseColor("#49454F"));
        descText.setGravity(Gravity.CENTER);
        descText.setPadding(0, 0, 0, dpToPx(32));
        layout.addView(descText);

        MaterialButton startButton = new MaterialButton(getActivity());
        startButton.setText("开启");
        startButton.setCornerRadius(dpToPx(28));
        startButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6750A4")));
        startButton.setTextColor(Color.WHITE);
        startButton.setOnClickListener(v -> {
            KeyboardViewService.showFloat(getActivity());
            ImGuiViewService.showFloatWindow(getActivity());
        });
        layout.addView(startButton);

        return layout;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}