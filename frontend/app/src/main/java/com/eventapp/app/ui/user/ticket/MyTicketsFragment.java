package com.eventapp.app.ui.user.ticket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.adapter.TicketAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.model.TicketUiModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;

/**
 * FR-U14 + FR-U18 gộp thành một tab "Vé của tôi" với hai tab con:
 * "Sắp diễn ra" (vé còn hiệu lực, kiêm lịch sự kiện sắp tới) và "Đã qua".
 */
public class MyTicketsFragment extends Fragment {

    private static final int TAB_UPCOMING = 0;
    private static final int TAB_PAST = 1;

    private MyTicketsViewModel viewModel;
    private TicketAdapter adapter;
    private RecyclerView list;
    private TabLayout tabs;
    private EmptyStateView emptyState;

    private List<TicketUiModel> upcomingTickets = Collections.emptyList();
    private List<TicketUiModel> pastTickets = Collections.emptyList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_ticket_my_tickets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyTicketsViewModel.class);

        tabs = view.findViewById(R.id.tabTickets);
        list = view.findViewById(R.id.rvTickets);
        emptyState = view.findViewById(R.id.emptyTickets);

        adapter = new TicketAdapter();
        adapter.setOnTicketClickListener(ticket -> showMessage(R.string.hc_coming_soon));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        view.findViewById(R.id.btnTicketsNotifications).setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.notification_listFragment));

        view.findViewById(R.id.btnTicketsMyReviews).setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.my_reviewsFragment));

        emptyState.setActionText(R.string.hc_tickets_empty_action);
        emptyState.setOnActionClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.homeFragment));

        setupTabs();
        observeData();
        NavBarSpacing.padBottomForNavBar(requireView(), list, 16);
    }

    private void setupTabs() {
        tabs.addTab(tabs.newTab().setText(R.string.hc_tickets_tab_upcoming));
        tabs.addTab(tabs.newTab().setText(R.string.hc_tickets_tab_past));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                renderTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                list.smoothScrollToPosition(0);
            }
        });
        renderTab(TAB_UPCOMING);
    }

    private void observeData() {
        viewModel.getUpcomingTickets().observe(getViewLifecycleOwner(), tickets -> {
            upcomingTickets = tickets == null ? Collections.emptyList() : tickets;
            if (selectedTab() == TAB_UPCOMING) {
                renderTab(TAB_UPCOMING);
            }
        });
        viewModel.getPastTickets().observe(getViewLifecycleOwner(), tickets -> {
            pastTickets = tickets == null ? Collections.emptyList() : tickets;
            if (selectedTab() == TAB_PAST) {
                renderTab(TAB_PAST);
            }
        });
    }

    private int selectedTab() {
        return tabs.getSelectedTabPosition() == TabLayout.Tab.INVALID_POSITION
                ? TAB_UPCOMING
                : tabs.getSelectedTabPosition();
    }

    private void renderTab(int position) {
        boolean upcoming = position == TAB_UPCOMING;
        List<TicketUiModel> tickets = upcoming ? upcomingTickets : pastTickets;
        adapter.submitList(tickets);

        boolean empty = tickets.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            emptyState.setTitle(upcoming
                    ? R.string.hc_tickets_empty_upcoming_title
                    : R.string.hc_tickets_empty_past_title);
            emptyState.setMessage(upcoming
                    ? R.string.hc_tickets_empty_upcoming_message
                    : R.string.hc_tickets_empty_past_message);
        } else {
            list.scrollToPosition(0);
        }
    }

    /** Snackbar neo lên thanh điều hướng nổi để không che mất các mục chính. */
    private void showMessage(int messageRes) {
        Snackbar snackbar = Snackbar.make(requireView(), messageRes, Snackbar.LENGTH_SHORT);
        View navBar = requireActivity().findViewById(R.id.nav_bar_container);
        if (navBar != null) {
            snackbar.setAnchorView(navBar);
        }
        snackbar.show();
    }
}
