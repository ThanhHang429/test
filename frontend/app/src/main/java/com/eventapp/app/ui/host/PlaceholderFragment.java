package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;

/**
 * Màn hình tạm thời hiển thị trong lúc toàn bộ giao diện được dựng lại.
 * Dựng hoàn toàn bằng code nên không phụ thuộc layout XML nào.
 */
public class PlaceholderFragment extends Fragment {

    private static final int PADDING_DP = 24;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(PADDING_DP);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(requireContext());
        title.setText(R.string.ui_reset_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView message = new TextView(requireContext());
        message.setText(R.string.ui_reset_message);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        root.addView(message, params);

        return root;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
