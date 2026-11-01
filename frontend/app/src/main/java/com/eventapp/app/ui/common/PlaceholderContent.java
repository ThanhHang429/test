package com.eventapp.app.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.EmptyStateView;

/**
 * Nội dung thay thế cho các màn hình đã có đích điều hướng nhưng chưa được triển khai.
 *
 * <p>Không để trắng màn hình: người dùng bấm vào tab mà thấy trang trống sẽ tưởng app lỗi. Màn
 * giữ trạng thái EMPTY theo {@code 03-state-display.md} và nói rõ là chức năng đang xây dựng.</p>
 */
public final class PlaceholderContent {

    private PlaceholderContent() {
    }

    @NonNull
    public static View create(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @StringRes int titleRes) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        EmptyStateView empty = view.findViewById(R.id.emptyPlaceholder);
        empty.setIcon(R.drawable.ic_ds_info);
        empty.setTitle(titleRes);
        empty.setMessage(R.string.hc_placeholder_message);
        return view;
    }
}
