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
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.user.adapter.TicketAdapter;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoTicket;

import java.util.List;

/** Tab Vé của tôi: lọc theo trạng thái và mở chi tiết vé kèm mã QR. */
public class TicketsFragment extends Fragment {

    private FragmentUserTicketsBinding binding;
    private TicketAdapter adapter;
    private DemoTicket.Status filter = DemoTicket.Status.UPCOMING;

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
        adapter = new TicketAdapter(ticket ->
                TicketDetailActivity.start(requireContext(), ticket.code));
        binding.rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTickets.setAdapter(adapter);

        binding.chipUpcoming.setSelected(true);
        binding.chipUpcoming.setOnClickListener(v -> select(DemoTicket.Status.UPCOMING));
        binding.chipUsed.setOnClickListener(v -> select(DemoTicket.Status.USED));
        binding.chipCancelled.setOnClickListener(v -> select(DemoTicket.Status.CANCELLED));
        binding.btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof UserHostActivity) {
                ((UserHostActivity) getActivity()).openTab(UserBottomNavView.TAB_DISCOVERY);
            }
        });

        reload();
    }

    private void select(DemoTicket.Status status) {
        filter = status;
        binding.chipUpcoming.setSelected(status == DemoTicket.Status.UPCOMING);
        binding.chipUsed.setSelected(status == DemoTicket.Status.USED);
        binding.chipCancelled.setSelected(status == DemoTicket.Status.CANCELLED);
        reload();
    }

    private void reload() {
        if (binding == null) {
            return;
        }
        DemoContent content = DemoContent.get();
        binding.tvTicketsSub.setText(getString(R.string.user_tickets_sub,
                content.ticketsByStatusCount(DemoTicket.Status.UPCOMING),
                content.ticketsByStatusCount(DemoTicket.Status.USED)));
        binding.chipUpcoming.setText(getString(R.string.user_filter_upcoming)
                + " (" + content.ticketsByStatusCount(DemoTicket.Status.UPCOMING) + ")");
        binding.chipUsed.setText(getString(R.string.user_filter_used)
                + " (" + content.ticketsByStatusCount(DemoTicket.Status.USED) + ")");
        binding.chipCancelled.setText(getString(R.string.user_filter_cancelled)
                + " (" + content.ticketsByStatusCount(DemoTicket.Status.CANCELLED) + ")");

        List<DemoTicket> tickets = content.tickets(filter);
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
