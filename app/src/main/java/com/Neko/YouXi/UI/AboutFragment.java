package com.Neko.YouXi.UI;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        FrameLayout iconSection = createIconSection();
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        iconParams.weight = 1f;
        iconSection.setLayoutParams(iconParams);
        layout.addView(iconSection);

        LinearLayout infoSection = createInfoSection();
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoParams.setMargins(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));
        infoSection.setLayoutParams(infoParams);
        layout.addView(infoSection);

        TextView descText = createDescText();
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(dpToPx(20), 0, dpToPx(20), dpToPx(20));
        descText.setLayoutParams(descParams);
        layout.addView(descText);

        View bottomSpacer = new View(getActivity());
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        spacerParams.weight = 2f;
        bottomSpacer.setLayoutParams(spacerParams);
        layout.addView(bottomSpacer);

        return layout;
    }

    private FrameLayout createIconSection() {
        FrameLayout container = new FrameLayout(getActivity());
        
        ImageView backgroundImage = new ImageView(getActivity());
        backgroundImage.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImage.setImageResource(com.Neko.YouXi.R.drawable.ic_launcher);
        container.addView(backgroundImage);
        
        View overlay = new View(getActivity());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(Color.parseColor("#33000000"));
        container.addView(overlay);
        
        TextView appName = new TextView(getActivity());
        appName.setText("莜汐");
        appName.setTextSize(28);
        appName.setTextColor(Color.WHITE);
        appName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        FrameLayout.LayoutParams nameParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        nameParams.gravity = Gravity.BOTTOM | Gravity.START;
        nameParams.bottomMargin = dpToPx(24);
        nameParams.leftMargin = dpToPx(24);
        appName.setLayoutParams(nameParams);
        container.addView(appName);
        
        return container;
    }

    private LinearLayout createInfoSection() {
        LinearLayout section = new LinearLayout(getActivity());
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.WHITE);
        section.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dpToPx(16));
            shape.setColor(Color.WHITE);
            section.setBackground(shape);
            section.setElevation(dpToPx(2));
        } else {
            section.setBackgroundColor(Color.WHITE);
        }
        
        LinearLayout developerItem = createClickableInfoItem("开发者", "huanxin0", "https://github.com/huanxin0");
        section.addView(developerItem);
        
        View divider1 = createDivider();
        section.addView(divider1);
        
        LinearLayout sourceItem = createClickableInfoItem("源代码", "GitHub", "https://github.com/huanxin0/Android-ImGui-Vulkan-App");
        section.addView(sourceItem);
        
        return section;
    }

    private LinearLayout createClickableInfoItem(String label, String value, final String url) {
        LinearLayout item = new LinearLayout(getActivity());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0, dpToPx(14), 0, dpToPx(14));
        item.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        item.setClickable(true);
        item.setFocusable(true);
        
        item.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setBackgroundColor(Color.parseColor("#E8E8E8"));
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setBackgroundColor(Color.TRANSPARENT);
                    break;
            }
            return false;
        });
        
        item.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        
        TextView labelText = new TextView(getActivity());
        labelText.setText(label);
        labelText.setTextSize(16);
        labelText.setTextColor(Color.parseColor("#1C1B1F"));
        labelText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        
        LinearLayout valueLayout = new LinearLayout(getActivity());
        valueLayout.setOrientation(LinearLayout.HORIZONTAL);
        valueLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView valueText = new TextView(getActivity());
        valueText.setText(value);
        valueText.setTextSize(14);
        valueText.setTextColor(Color.parseColor("#6750A4"));
        valueText.setPadding(0, 0, dpToPx(8), 0);
        
        TextView arrowIcon = new TextView(getActivity());
        arrowIcon.setText("›");
        arrowIcon.setTextSize(20);
        arrowIcon.setTextColor(Color.parseColor("#999999"));
        
        valueLayout.addView(valueText);
        valueLayout.addView(arrowIcon);
        
        item.addView(labelText);
        item.addView(valueLayout);
        
        return item;
    }
    
    private TextView createDescText() {
        TextView desc = new TextView(getActivity());
        desc.setText("在Android使用vulkan渲染imgui窗口的demo\n如果有好意见或bug请发Issues");
        desc.setTextSize(13);
        desc.setTextColor(Color.parseColor("#666666"));
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(dpToPx(4), 1.3f);
        desc.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dpToPx(12));
            shape.setColor(Color.parseColor("#F0F0F0"));
            desc.setBackground(shape);
        } else {
            desc.setBackgroundColor(Color.parseColor("#F0F0F0"));
        }
        
        return desc;
    }
    
    private View createDivider() {
        View divider = new View(getActivity());
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
        return divider;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}