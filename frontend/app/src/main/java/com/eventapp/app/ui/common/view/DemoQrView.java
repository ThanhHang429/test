package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;

/**
 * Mã QR minh họa cho luồng thanh toán/soát vé demo.
 *
 * <p>Pattern được sinh ổn định từ {@link #setPayload(String)} nên không cần ảnh bitmap và không
 * đại diện cho một mã thanh toán thật. Khi nối cổng thanh toán, thay view này bằng ảnh QR do
 * backend trả về.</p>
 */
public class DemoQrView extends View {

    private static final int MODULES = 29;
    private static final int FINDER = 7;

    private final Paint modulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint quietPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path checkPath = new Path();

    private String payload = "";
    private int moduleColor;
    private int accentColor;
    private int quietColor;

    public DemoQrView(Context context) {
        this(context, null);
    }

    public DemoQrView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DemoQrView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        moduleColor = ContextCompat.getColor(context, R.color.ds_text);
        accentColor = ContextCompat.getColor(context, R.color.ds_primary);
        quietColor = Color.WHITE;
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setPayload(@Nullable String value) {
        payload = value == null ? "" : value;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = Math.round(220 * getResources().getDisplayMetrics().density);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int quietZone = Math.max(4, Math.min(width, height) / 18);
        int available = Math.min(width, height) - quietZone * 2;
        int cell = Math.max(1, available / MODULES);
        int qrSize = cell * MODULES;
        int left = (width - qrSize) / 2;
        int top = (height - qrSize) / 2;

        modulePaint.setColor(moduleColor);
        quietPaint.setColor(quietColor);
        int seed = payload.hashCode();

        for (int y = 0; y < MODULES; y++) {
            for (int x = 0; x < MODULES; x++) {
                if (inFinder(x, y)) {
                    continue;
                }
                int hash = seed ^ (x * 73856093) ^ (y * 19349663);
                if (((hash >>> 4) & 1) == 0) {
                    continue;
                }
                canvas.drawRect(left + x * cell, top + y * cell,
                        left + (x + 1) * cell, top + (y + 1) * cell, modulePaint);
            }
        }

        drawFinder(canvas, left, top, 0, 0, cell);
        drawFinder(canvas, left, top, MODULES - FINDER, 0, cell);
        drawFinder(canvas, left, top, 0, MODULES - FINDER, cell);
        drawCenterBadge(canvas, left, top, qrSize, cell);
    }

    private void drawFinder(Canvas canvas, int left, int top, int x, int y, int cell) {
        modulePaint.setColor(moduleColor);
        canvas.drawRect(left + x * cell, top + y * cell,
                left + (x + FINDER) * cell, top + (y + FINDER) * cell, modulePaint);
        quietPaint.setColor(quietColor);
        canvas.drawRect(left + (x + 1) * cell, top + (y + 1) * cell,
                left + (x + FINDER - 1) * cell, top + (y + FINDER - 1) * cell, quietPaint);
        modulePaint.setColor(moduleColor);
        canvas.drawRect(left + (x + 2) * cell, top + (y + 2) * cell,
                left + (x + FINDER - 2) * cell, top + (y + FINDER - 2) * cell, modulePaint);
    }

    private void drawCenterBadge(Canvas canvas, int left, int top, int qrSize, int cell) {
        int centerX = left + qrSize / 2;
        int centerY = top + qrSize / 2;

        quietPaint.setColor(quietColor);
        canvas.drawCircle(centerX, centerY, cell * 7.2f, quietPaint);

        badgePaint.setColor(accentColor);
        canvas.drawCircle(centerX, centerY, cell * 5.4f, badgePaint);

        checkPath.reset();
        checkPath.moveTo(centerX - cell * 2.3f, centerY);
        checkPath.lineTo(centerX - cell * 0.6f, centerY + cell * 1.7f);
        checkPath.lineTo(centerX + cell * 2.4f, centerY - cell * 1.5f);
        checkPaint.setStrokeWidth(Math.max(2f, cell * 0.7f));
        checkPaint.setColor(quietColor);
        canvas.drawPath(checkPath, checkPaint);
    }

    private boolean inFinder(int x, int y) {
        return x < FINDER && y < FINDER
                || x >= MODULES - FINDER && y < FINDER
                || x < FINDER && y >= MODULES - FINDER;
    }
}
