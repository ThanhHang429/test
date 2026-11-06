package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.eventapp.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight bar chart used by O03/O27/A20 blocks. Avoids a charting dependency and keeps
 * the visual language consistent with the design tokens.
 */
public class SimpleBarChartView extends View {

    public static class Bar {
        public final String label;
        public final float value;

        public Bar(String label, float value) {
            this.label = label == null ? "" : label;
            this.value = Math.max(0f, value);
        }
    }

    private final List<Bar> bars = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private int barColor;
    private int highlightColor;
    private int highlightIndex = -1;
    private float maxValue;
    private boolean showValues = true;
    private CharSequence emptyText;

    public SimpleBarChartView(Context context) {
        this(context, null);
    }

    public SimpleBarChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        barColor = ContextCompat.getColor(context, R.color.ds_chart_bar);
        highlightColor = ContextCompat.getColor(context, R.color.ds_chart_error);
        emptyText = context.getString(R.string.hc_chart_empty);

        barPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.ds_chart_grid));
        gridPaint.setStrokeWidth(dp(1));

        labelPaint.setColor(ContextCompat.getColor(context, R.color.ds_text_muted));
        labelPaint.setTextSize(sp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(android.graphics.Typeface.create(
                ResourcesCompat.getFont(getContext(), R.font.be_vietnam_pro),
                android.graphics.Typeface.NORMAL));

        valuePaint.setColor(ContextCompat.getColor(context, R.color.ds_text));
        valuePaint.setTextSize(sp(10));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(android.graphics.Typeface.create(
                ResourcesCompat.getFont(getContext(), R.font.plus_jakarta_sans),
                android.graphics.Typeface.BOLD));

        emptyPaint.setColor(ContextCompat.getColor(context, R.color.ds_text_muted));
        emptyPaint.setTextSize(sp(12));
        emptyPaint.setTextAlign(Paint.Align.CENTER);

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        updateContentDescription();
    }

    public void setData(@Nullable List<Bar> data) {
        bars.clear();
        if (data != null) {
            bars.addAll(data);
        }
        updateContentDescription();
        invalidate();
    }

    public void setData(@Nullable float[] values, @Nullable String[] labels) {
        bars.clear();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                String label = labels != null && i < labels.length ? labels[i] : "";
                bars.add(new Bar(label, values[i]));
            }
        }
        updateContentDescription();
        invalidate();
    }

    public void clear() {
        setData((List<Bar>) null);
    }

    /** Fixed scale for comparing charts. Set 0 or less to auto-scale to the largest bar. */
    public void setMaxValue(float maxValue) {
        this.maxValue = Math.max(0f, maxValue);
        invalidate();
    }

    /** Paints one bar with the error/highlight color, e.g. the peak hour in A20. */
    public void setHighlightIndex(int index) {
        this.highlightIndex = index;
        invalidate();
    }

    public void setBarColor(@ColorInt int color) {
        barColor = color;
        invalidate();
    }

    public void setHighlightColor(@ColorInt int color) {
        highlightColor = color;
        invalidate();
    }

    public void setShowValues(boolean showValues) {
        this.showValues = showValues;
        invalidate();
    }

    public void setEmptyText(@Nullable CharSequence text) {
        emptyText = text;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.max(getSuggestedMinimumWidth(), (int) dp(240));
        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            height = (int) dp(180);
        }
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (bars.isEmpty()) {
            canvas.drawText(emptyText == null ? "" : emptyText.toString(),
                    width / 2f, height / 2f - (emptyPaint.ascent() + emptyPaint.descent()) / 2f,
                    emptyPaint);
            return;
        }

        float left = dp(8);
        float right = width - dp(8);
        float top = showValues ? dp(22) : dp(12);
        float bottom = height - dp(30);
        float chartHeight = bottom - top;
        if (chartHeight <= 0 || right <= left) {
            return;
        }

        float scaleMax = maxValue;
        if (scaleMax <= 0f) {
            for (Bar bar : bars) {
                scaleMax = Math.max(scaleMax, bar.value);
            }
        }
        if (scaleMax <= 0f) {
            scaleMax = 1f;
        }

        // Grid: top, middle and baseline.
        for (int i = 0; i <= 2; i++) {
            float y = top + chartHeight * i / 2f;
            canvas.drawLine(left, y, right, y, gridPaint);
        }

        float slot = (right - left) / bars.size();
        float barWidth = Math.min(dp(26), slot * 0.62f);
        int labelStep = bars.size() <= 8 ? 1 : (int) Math.ceil(bars.size() / 8.0);
        boolean valuesFit = showValues && bars.size() <= 12;
        float radius = Math.min(dp(4), barWidth / 2f);

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            float centerX = left + slot * i + slot / 2f;
            float barHeight = chartHeight * (bar.value / scaleMax);
            barRect.set(centerX - barWidth / 2f, bottom - barHeight,
                    centerX + barWidth / 2f, bottom);
            barPaint.setColor(i == highlightIndex ? highlightColor : barColor);
            canvas.drawRoundRect(barRect, radius, radius, barPaint);

            if (valuesFit && bar.value > 0f) {
                float valueY = Math.max(top, bottom - barHeight - dp(4));
                canvas.drawText(formatCompact(bar.value), centerX, valueY, valuePaint);
            }

            if (i % labelStep == 0) {
                CharSequence label = TextUtils.ellipsize(bar.label, labelPaint,
                        Math.max(dp(12), slot), TextUtils.TruncateAt.END);
                canvas.drawText(label.toString(), centerX, height - dp(10), labelPaint);
            }
        }
    }

    private void updateContentDescription() {
        if (bars.isEmpty()) {
            setContentDescription(getContext().getString(R.string.hc_chart_empty));
            return;
        }
        Bar peak = bars.get(0);
        float total = 0f;
        for (Bar bar : bars) {
            total += bar.value;
            if (bar.value > peak.value) {
                peak = bar;
            }
        }
        setContentDescription(getContext().getString(R.string.hc_chart_content_description)
                + ". " + getContext().getString(R.string.hc_chart_peak,
                peak.label, formatCompact(peak.value))
                + ". " + getContext().getString(R.string.hc_chart_total, formatCompact(total)));
    }

    private String formatCompact(float value) {
        if (value >= 1_000_000_000f) {
            return trim(value / 1_000_000_000f) + " tỷ";
        }
        if (value >= 1_000_000f) {
            return trim(value / 1_000_000f) + " tr";
        }
        if (value >= 1_000f) {
            return trim(value / 1_000f) + "k";
        }
        if (value == Math.round(value)) {
            return String.valueOf((int) value);
        }
        return trim(value);
    }

    private String trim(float value) {
        if (value == Math.round(value)) {
            return String.valueOf(Math.round(value));
        }
        return String.format(new Locale("vi", "VN"), "%.1f", value);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
