package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserTicketsBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.user.adapter.TicketAdapter;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.model.TicketUiModel;

import java.util.List;

/** Tab Vé của tôi: lọc theo trạng thái và mở chi tiết vé kèm mã QR. */
public class TicketsFragment extends Fragment {

    private FragmentUserTicketsBinding binding;
    private TicketAdapter adapter;
    private TicketUiModel.Status filter = TicketUiModel.Status.UPCOMING;

    private final Runnable dataObserver = this::reload;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserTicketsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.getRoot());
        adapter = new TicketAdapter(ticket ->
                TicketDetailActivity.start(requireContext(), ticket.code));
        binding.rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTickets.setAdapter(adapter);

        binding.chipUpcoming.setSelected(true);
        binding.chipUpcoming.setOnClickListener(v -> select(TicketUiModel.Status.UPCOMING));
        binding.chipUsed.setOnClickListener(v -> select(TicketUiModel.Status.USED));
        binding.chipCancelled.setOnClickListener(v -> select(TicketUiModel.Status.CANCELLED));
        binding.btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof UserHostActivity) {
                ((UserHostActivity) getActivity()).openTab(UserBottomNavView.TAB_DISCOVERY);
            }
        });

        reload();
    }

    private void select(TicketUiModel.Status status) {
        filter = status;
        binding.chipUpcoming.setSelected(status == TicketUiModel.Status.UPCOMING);
        binding.chipUsed.setSelected(status == TicketUiModel.Status.USED);
        binding.chipCancelled.setSelected(status == TicketUiModel.Status.CANCELLED);
        reload();
    }

    private void reload() {
        if (binding == null) {
            return;
        }
        DemoContent content = DemoContent.get();
        binding.tvTicketsSub.setText(getString(R.string.user_tickets_sub,
                content.ticketsByStatusCount(TicketUiModel.Status.UPCOMING),
                content.ticketsByStatusCount(TicketUiModel.Status.USED)));
        binding.chipUpcoming.setText(getString(R.string.user_filter_upcoming)
                + " (" + content.ticketsByStatusCount(TicketUiModel.Status.UPCOMING) + ")");
        binding.chipUsed.setText(getString(R.string.user_filter_used)
                + " (" + content.ticketsByStatusCount(TicketUiModel.Status.USED) + ")");
        binding.chipCancelled.setText(getString(R.string.user_filter_cancelled)
                + " (" + content.ticketsByStatusCount(TicketUiModel.Status.CANCELLED) + ")");

        List<TicketUiModel> tickets = content.tickets(filter);
        adapter.setItems(tickets);
        boolean empty = tickets.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvTickets.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        DemoContent.get().addListener(dataObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public void onStop() {
        DemoContent.get().removeListener(dataObserver);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
