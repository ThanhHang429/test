package com.eventapp.app.ui.organizer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Dock điều hướng của khu Nhà tổ chức — cùng cấu trúc với dock khu Người dùng
 * (viên gradient trượt theo tab) nhưng dùng tông xanh eucalyptus.
 */
public class OrgBottomNavView extends FrameLayout {

    public static final int TAB_DASHBOARD = 0;
    public static final int TAB_EVENTS = 1;
    public static final int TAB_CHECKIN = 2;
    public static final int TAB_ORDERS = 3;
    public static final int TAB_SETTINGS = 4;

    private static final int TAB_COUNT = 5;
    private static final int[] TAB_IDS = {
            R.id.tabOrgDashboard, R.id.tabOrgEvents, R.id.tabOrgCheckin,
            R.id.tabOrgOrders, R.id.tabOrgSettings
    };
    private static final int[] ICON_IDS = {
            R.id.iconOrgDashboard, R.id.iconOrgEvents, R.id.iconOrgCheckin,
            R.id.iconOrgOrders, R.id.iconOrgSettings
    };
    private static final int[] LABEL_IDS = {
            R.id.labelOrgDashboard, R.id.labelOrgEvents, R.id.labelOrgCheckin,
            R.id.labelOrgOrders, R.id.labelOrgSettings
    };

    private final List<View> tabs = new ArrayList<>(TAB_COUNT);
    private final List<ImageView> icons = new ArrayList<>(TAB_COUNT);
    private final List<TextView> labels = new ArrayList<>(TAB_COUNT);

    private View pill;
    private int selectedIndex = -1;
    private boolean needsPillPosition = true;
    private OnTabSelectedListener listener;

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    public OrgBottomNavView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public OrgBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OrgBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_org_bottom_nav, this, true);
        pill = findViewById(R.id.navPill);

        for (int i = 0; i < TAB_COUNT; i++) {
            View tab = findViewById(TAB_IDS[i]);
            ImageView icon = findViewById(ICON_IDS[i]);
            TextView label = findViewById(LABEL_IDS[i]);
            tabs.add(tab);
            icons.add(icon);
            labels.add(label);

            final int index = i;
            tab.setOnClickListener(v -> {
                if (index == selectedIndex) {
                    return;
                }
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                selectTab(index);
            });
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void selectTab(int index) {
        selectTab(index, true);
    }

    public void selectTab(int index, boolean animate) {
        if (index < 0 || index >= TAB_COUNT || index == selectedIndex) {
            return;
        }
        int previous = selectedIndex;
        selectedIndex = index;

        for (int i = 0; i < TAB_COUNT; i++) {
            applyTabState(i, i == index);
        }
        movePill(index, animate && previous >= 0);

        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    private void applyTabState(int index, boolean active) {
        View tab = tabs.get(index);
        ImageView icon = icons.get(index);
        TextView label = labels.get(index);
        int color = ContextCompat.getColor(getContext(),
                active ? R.color.white : R.color.org_text_dim);

        tab.setSelected(active);
        icon.setImageTintList(ColorStateList.valueOf(color));
        label.setTextColor(color);

        float scale = active ? 1.06f : 1f;
        icon.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
    }

    private void movePill(int index, boolean animate) {
        View tab = tabs.get(index);
        int slotWidth = tab.getWidth();
        if (slotWidth <= 0) {
            needsPillPosition = true;
            return;
        }
        needsPillPosition = false;

        int pillWidth = Math.max(0, slotWidth - dp(8));
        ViewGroup.LayoutParams params = pill.getLayoutParams();
        if (params.width != pillWidth) {
            params.width = pillWidth;
            pill.setLayoutParams(params);
        }

        float targetX = tab.getLeft() + dp(4);
        if (animate) {
            pill.animate()
                    .translationX(targetX)
                    .setDuration(260)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .start();
        } else {
            pill.animate().cancel();
            pill.setTranslationX(targetX);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int available = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = dp(480);
        if (available > maxWidth) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (selectedIndex >= 0 && needsPillPosition) {
            movePill(selectedIndex, false);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
