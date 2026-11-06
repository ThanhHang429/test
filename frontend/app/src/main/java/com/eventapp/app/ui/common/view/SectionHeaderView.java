package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.eventapp.app.R;

/**
 * Section heading with optional subtitle, leading icon and a trailing "see all" action.
 */
public class SectionHeaderView extends LinearLayout {

    private ImageView iconView;
    private TextView titleView;
    private TextView subtitleView;
    private LinearLayout actionContainer;
    private TextView actionView;
    private ImageView chevronView;

    public SectionHeaderView(Context context) {
        this(context, null);
    }

    public SectionHeaderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SectionHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(dp(48));
        setPadding(0, dp(4), 0, dp(4));
        LayoutInflater.from(context).inflate(R.layout.view_ds_section_header, this, true);
        iconView = findViewById(R.id.ivDsSectionIcon);
        titleView = findViewById(R.id.tvDsSectionTitle);
        subtitleView = findViewById(R.id.tvDsSectionSubtitle);
        actionContainer = findViewById(R.id.layoutDsSectionAction);
        actionView = findViewById(R.id.tvDsSectionAction);
        chevronView = findViewById(R.id.ivDsSectionChevron);
    }

    public void setTitle(@StringRes int titleRes) {
        setTitle(getContext().getString(titleRes));
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
    }

    public void setSubtitle(@StringRes int subtitleRes) {
        setSubtitle(getContext().getString(subtitleRes));
    }

    public void setSubtitle(CharSequence subtitle) {
        boolean visible = subtitle != null && subtitle.length() > 0;
        subtitleView.setText(subtitle);
        subtitleView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setIcon(@DrawableRes int drawableRes) {
        iconView.setImageResource(drawableRes);
        iconView.setVisibility(VISIBLE);
    }

    public void hideIcon() {
        iconView.setVisibility(GONE);
    }

    public void setActionText(@StringRes int textRes) {
        setActionText(getContext().getString(textRes));
    }

    public void setActionText(CharSequence text) {
        actionView.setText(text);
        boolean visible = text != null && text.length() > 0;
        actionContainer.setVisibility(visible ? VISIBLE : GONE);
    }

    public void showAction(boolean show) {
        actionContainer.setVisibility(show ? VISIBLE : GONE);
    }

    public void setChevronVisible(boolean visible) {
        chevronView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setOnActionClickListener(@Nullable OnClickListener listener) {
        actionContainer.setOnClickListener(listener);
        actionContainer.setClickable(listener != null);
    }

    public View getActionContainer() {
        return actionContainer;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
