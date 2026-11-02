package com.eventapp.app.ui.organizer.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * Cửa ngõ cho một sự kiện của organizer: phân tích AI (FR-O27) và phân loại/nhãn (FR-O09).
 *
 * <p>Không hiển thị số liệu tổng hợp ở đây vì dashboard đã có; màn này chỉ điều hướng tới hai
 * tính năng có API thật, tránh lặp lại dữ liệu và tránh hiển thị số liệu giả.</p>
 */
public class OrganizerEventDetailFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";
    public static final String ARG_EVENT_TITLE = "arg_event_title";
    /** Giữ lại để tương thích với dashboard cũ; màn này không còn tab. */
    public static final String ARG_TAB = "arg_tab";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        long eventId = args == null ? -1L : args.getLong(ARG_EVENT_ID, -1L);
        String eventTitle = args == null ? null : args.getString(ARG_EVENT_TITLE);

        view.findViewById(R.id.btnOedBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        TextView title = view.findViewById(R.id.tvOedTitle);
        TextView code = view.findViewById(R.id.tvOedCode);
        title.setText(eventTitle == null || eventTitle.trim().isEmpty()
                ? getString(R.string.org_event_title) : eventTitle);
        code.setText(eventId > 0L
                ? getString(R.string.hc_oed_code, eventId)
                : getString(R.string.org_event_subtitle));

        view.findViewById(R.id.cardOedAnalysis).setOnClickListener(v ->
                open(R.id.ai_insightFragment, eventId, eventTitle));
        view.findViewById(R.id.cardOedClassification).setOnClickListener(v ->
                open(R.id.classificationFragment, eventId, eventTitle));
    }

    private void open(int destination, long eventId, @Nullable String eventTitle) {
        if (eventId <= 0L) {
            Snackbar.make(requireView(), R.string.org_classification_error, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        Bundle args = new Bundle();
        args.putLong("arg_event_id", eventId);
        args.putString("arg_event_title", eventTitle);
        try {
            Navigation.findNavController(requireView()).navigate(destination, args);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Snackbar.make(requireView(), R.string.org_tools_error, Snackbar.LENGTH_SHORT).show();
        }
    }
}
