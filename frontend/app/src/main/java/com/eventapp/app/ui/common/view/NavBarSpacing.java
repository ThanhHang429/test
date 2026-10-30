package com.eventapp.app.ui.common.view;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.eventapp.app.R;

/**
 * Bottom navigation là overlay nên nó không chiếm chỗ trong layout. Màn hình nào có nội dung
 * chạm đáy (danh sách cuộn, thanh CTA dính đáy) phải tự chừa khoảng bằng chiều cao thanh cộng
 * với margin của thanh — margin này đã bao gồm inset của thanh gesture.
 */
public final class NavBarSpacing {

    private NavBarSpacing() {
    }

    /** Chừa chỗ bằng padding cho vùng cuộn (dùng kèm {@code clipToPadding="false"}). */
    public static void padBottomForNavBar(@NonNull View hostView, @NonNull View target,
                                          int extraDp) {
        applyClearance(hostView, target, extraDp, true);
    }

    /** Chừa chỗ bằng margin cho view neo đáy (thanh CTA, FAB...). */
    public static void marginBottomForNavBar(@NonNull View hostView, @NonNull View target,
                                             int extraDp) {
        applyClearance(hostView, target, extraDp, false);
    }

    private static void applyClearance(View hostView, View target, int extraDp, boolean asPadding) {
        View navBar = hostView.getRootView().findViewById(R.id.nav_bar_container);
        if (navBar == null) {
            return;
        }
        navBar.post(() -> {
            if (!target.isAttachedToWindow()) {
                return;
            }
            ViewGroup.MarginLayoutParams navParams =
                    (ViewGroup.MarginLayoutParams) navBar.getLayoutParams();
            int extra = Math.round(extraDp
                    * target.getResources().getDisplayMetrics().density);
            int clearance = navBar.getHeight() + navParams.bottomMargin + extra;
            if (asPadding) {
                target.setPadding(target.getPaddingLeft(), target.getPaddingTop(),
                        target.getPaddingRight(), clearance);
            } else if (target.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) target.getLayoutParams();
                params.bottomMargin = clearance;
                target.setLayoutParams(params);
            }
        });
    }
}
