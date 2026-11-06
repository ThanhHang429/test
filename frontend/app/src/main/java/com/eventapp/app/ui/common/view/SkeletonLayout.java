package com.eventapp.app.ui.common.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.eventapp.app.R;

/**
 * Container for skeleton placeholders. If no children are declared in XML, a default
 * three-line skeleton is inflated. Children pulse while data is loading.
 */
public class SkeletonLayout extends LinearLayout {

    private ValueAnimator animator;
    private boolean autoStart = true;
    private boolean running;

    public SkeletonLayout(Context context) {
        this(context, null);
    }

    public SkeletonLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SkeletonLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setContentDescription(context.getString(R.string.hc_loading_a11y));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() == 0) {
            LayoutInflater.from(getContext()).inflate(R.layout.view_ds_skeleton, this, true);
        }
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public void start() {
        if (running || getChildCount() == 0) {
            return;
        }
        running = true;
        animator = ValueAnimator.ofFloat(0.45f, 1f);
        animator.setDuration(900L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(animation -> setAlpha((float) animation.getAnimatedValue()));
        animator.start();
    }

    public void stop() {
        running = false;
        if (animator != null) {
            animator.cancel();
            animator.removeAllUpdateListeners();
            animator = null;
        }
        setAlpha(1f);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getChildCount() == 0) {
            LayoutInflater.from(getContext()).inflate(R.layout.view_ds_skeleton, this, true);
        }
        if (autoStart) {
            start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != View.VISIBLE) {
            stop();
        } else if (autoStart) {
            start();
        }
    }
}
