package com.eventapp.app.ui.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;

/**
 * Biểu đồ cột đôi 7 ngày cho bảng điều hành: tuần này (teal) và tuần trước (xám).
 * Vẽ trực tiếp bằng Canvas để nhẹ và không phụ thuộc thư viện biểu đồ.
 */
public class AdminBarChartView extends View {

    private static final String[] DAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private float[] current = {5.2f, 6.8f, 5.9f, 8.1f, 7.4f, 9.6f, 8.6f};
    private float[] previous = {4.6f, 5.2f, 5.5f, 6.9f, 6.4f, 7.8f, 7.2f};

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public AdminBarChartView(@NonNull Context context) {
        super(context);
        init();
    }

    public AdminBarChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdminBarChartView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.admin_chart_grid));
        gridPaint.setStrokeWidth(dp(1));

        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.admin_text_faint));
        labelPaint.setTextSize(sp(9));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setSeries(float[] current, float[] previous) {
        if (current != null && current.length == DAYS.length) {
            this.current = current;
        }
        if (previous != null && previous.length == DAYS.length) {
            this.previous = previous;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float labelHeight = sp(16);
        float top = dp(8);
        float bottom = getHeight() - labelHeight;
        float chartHeight = bottom - top;
        if (chartHeight <= 0) {
            return;
        }

        for (int i = 1; i <= 3; i++) {
            float y = bottom - chartHeight * i / 4f;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }

        float max = 1f;
        for (float value : current) {
            max = Math.max(max, value);
        }
        for (float value : previous) {
            max = Math.max(max, value);
        }
        max *= 1.12f;

        float groupWidth = getWidth() / (float) DAYS.length;
        float barWidth = Math.min(dp(9), groupWidth * 0.24f);
        float gap = dp(3);
        float radius = barWidth / 2f;

        int teal = ContextCompat.getColor(getContext(), R.color.admin_accent);
        int tealBright = ContextCompat.getColor(getContext(), R.color.admin_accent_bright);
        int muted = ContextCompat.getColor(getContext(), R.color.admin_chart_bar);

        for (int i = 0; i < DAYS.length; i++) {
            float centerX = groupWidth * i + groupWidth / 2f;

            float prevHeight = chartHeight * previous[i] / max;
            rect.set(centerX - barWidth - gap / 2f, bottom - prevHeight,
                    centerX - gap / 2f, bottom);
            barPaint.setShader(null);
            barPaint.setColor(muted);
            canvas.drawRoundRect(rect, radius, radius, barPaint);

            float curHeight = chartHeight * current[i] / max;
            rect.set(centerX + gap / 2f, bottom - curHeight,
                    centerX + barWidth + gap / 2f, bottom);
            barPaint.setShader(new LinearGradient(0, bottom - curHeight, 0, bottom,
                    tealBright, teal, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, barPaint);
            barPaint.setShader(null);

            canvas.drawText(DAYS[i], centerX, getHeight() - dp(2), labelPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
