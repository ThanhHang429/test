package com.eventapp.app.ui.common.view;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Chừa khoảng trống ở cuối vùng cuộn cho dock điều hướng trong suốt và thanh điều hướng hệ thống.
 *
 * <p>Dock cao 68dp, cách đáy màn hình 12dp và cần thêm 8dp thoáng, nên nội dung phải có padding
 * tối thiểu 88dp cộng với inset của thanh điều hướng. Padding gốc trong layout (nếu có) được giữ
 * nguyên và cộng thêm, nhờ vậy các RecyclerView có sẵn padding hai bên vẫn giữ đúng gutter.</p>
 */
public final class NavContentInset {

    /** Dock 68dp + lề dưới 12dp + khoảng thoáng 8dp. */
    public static final int DOCK_CLEARANCE_DP = 88;

    private NavContentInset() {
    }

    /**
     * Áp dụng cho một view cuộn (NestedScrollView/RecyclerView) để item cuối vẫn cuộn lên trên
     * dock trong suốt thay vì bị dock che.
     */
    public static void apply(@NonNull ViewGroup scrollView) {
        final int baseBottom = scrollView.getPaddingBottom();
        final int clearance = Math.round(DOCK_CLEARANCE_DP
                * scrollView.getResources().getDisplayMetrics().density);

        // Nội dung phải được vẽ cả trong vùng padding để nhìn xuyên qua dock.
        scrollView.setClipToPadding(false);

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottom = baseBottom + clearance + bars.bottom;
            if (view.getPaddingBottom() != bottom) {
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getPaddingRight(), bottom);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scrollView);
    }
}
