package com.Neko.YouXi.UI;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.Neko.YouXi.AppConfig;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private MaterialSwitch switchCompatibleMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        layout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        TextView title = new TextView(getActivity());
        title.setText("设置");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#1C1B1F"));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dpToPx(20));
        layout.addView(title);

        CardView compatibleCard = createSettingCard();
        
        LinearLayout cardContent = new LinearLayout(getActivity());
        cardContent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(18));
        cardContent.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout textLayout = new LinearLayout(getActivity());
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        textLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView settingTitle = new TextView(getActivity());
        settingTitle.setText("兼容模式");
        settingTitle.setTextSize(16);
        settingTitle.setTextColor(Color.parseColor("#1C1B1F"));
        settingTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        TextView settingDesc = new TextView(getActivity());
        settingDesc.setText("卡屏请打开，但开启流畅度没有不开好");
        settingDesc.setTextSize(13);
        settingDesc.setTextColor(Color.parseColor("#666666"));
        settingDesc.setPadding(0, dpToPx(4), 0, 0);
        
        textLayout.addView(settingTitle);
        textLayout.addView(settingDesc);
        
        switchCompatibleMode = new MaterialSwitch(getActivity());
        switchCompatibleMode.setMinimumWidth(0);
        switchCompatibleMode.setThumbIconDrawable(null);
        switchCompatibleMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppConfig.兼容模式 = isChecked;
            saveSwitchState(isChecked);
        });
        
        cardContent.addView(textLayout);
        cardContent.addView(switchCompatibleMode);
        compatibleCard.addView(cardContent);
        layout.addView(compatibleCard);

        View spacer = new View(getActivity());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(spacer);

        loadSwitchState();

        return layout;
    }

    private CardView createSettingCard() {
        CardView card = new CardView(getActivity());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        card.setContentPadding(0, 0, 0, 0);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dpToPx(2));
        }
        
        return card;
    }

    private void loadSwitchState() {
        SharedPreferences prefs = getActivity().getSharedPreferences("app_settings", getActivity().MODE_PRIVATE);
        boolean compatibleMode = prefs.getBoolean("compatible_mode", false);
        switchCompatibleMode.setChecked(compatibleMode);
        AppConfig.兼容模式 = compatibleMode;
    }

    private void saveSwitchState(boolean value) {
        SharedPreferences prefs = getActivity().getSharedPreferences("app_settings", getActivity().MODE_PRIVATE);
        prefs.edit().putBoolean("compatible_mode", value).apply();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}