package com.eventapp.app.ui.organizer.ticket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.PlaceholderContent;

/** FR-O10 — danh sách loại vé; phần xử lý phía sau chưa có nên hiện trạng thái đang xây dựng. */
public class TicketTypeListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return PlaceholderContent.create(inflater, container, R.string.hc_oed_action_tickets);
    }
}
