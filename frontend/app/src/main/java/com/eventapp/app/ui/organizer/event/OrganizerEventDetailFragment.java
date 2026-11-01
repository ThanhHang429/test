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
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.fake.SampleDashboardData;
import com.eventapp.app.ui.model.DashboardStatusLabels;
import com.eventapp.app.ui.model.DashboardUiModel;
import com.eventapp.app.ui.organizer.report.AiInsightFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

/**
 * FR-O04 detail screen for one event owned by the organizer. FR-O27 lives here as the
 * "Phân tích" tab: the tab hosts {@link AiInsightFragment} in embedded mode so the analysis keeps
 * a single implementation and never becomes a screen of its own.
 */
public class OrganizerEventDetailFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";
    public static final String ARG_EVENT_TITLE = "arg_event_title";
    public static final String ARG_TAB = "arg_tab";

    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_ANALYSIS = 1;

    private static final String TAG_ANALYSIS = "oed_analysis";
    private static final String STATE_TAB = "oed_selected_tab";

    private TabLayout tabs;
    private View cardSummary;
    private View overviewScroll;
    private View analysisContainer;
    private TextView titleView;
    private TextView codeView;
    private TextView revenueView;
    private TextView registrationsView;
    private StatusChip reviewChip;
    private StatusChip lifecycleChip;

    private long eventId = -1L;
    private String eventTitle;
    private Integer selectedTab;

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
        eventId = args == null ? -1L : args.getLong(ARG_EVENT_ID, -1L);
        eventTitle = args == null ? null : args.getString(ARG_EVENT_TITLE);
        // Quay lại từ màn con phải giữ đúng tab đang xem. Fragment có thể được dựng lại từ state
        // (savedInstanceState) hoặc chỉ dựng lại view trên cùng instance (field còn giá trị).
        int startTab;
        if (savedInstanceState != null) {
            startTab = savedInstanceState.getInt(STATE_TAB, TAB_OVERVIEW);
        } else if (selectedTab != null) {
            startTab = selectedTab;
        } else {
            startTab = args == null ? TAB_OVERVIEW : args.getInt(ARG_TAB, TAB_OVERVIEW);
        }

        bindViews(view);
        setupToolbar();
        setupHeader();
        setupOverview();
        setupShortcuts();
        setupTabs(startTab);
    }

    private void bindViews(View view) {
        tabs = view.findViewById(R.id.tabsOed);
        cardSummary = view.findViewById(R.id.cardOedSummary);
        overviewScroll = view.findViewById(R.id.scrollOedOverview);
        analysisContainer = view.findViewById(R.id.containerOedAnalysis);
        titleView = view.findViewById(R.id.tvOedTitle);
        codeView = view.findViewById(R.id.tvOedCode);
        revenueView = view.findViewById(R.id.tvOedRevenue);
        registrationsView = view.findViewById(R.id.tvOedRegistrations);
        reviewChip = view.findViewById(R.id.chipOedReview);
        lifecycleChip = view.findViewById(R.id.chipOedLifecycle);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnOedBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        SectionHeaderView manageHeader = requireView().findViewById(R.id.headerOedManage);
        manageHeader.setTitle(R.string.hc_oed_manage_title);
        manageHeader.setSubtitle(R.string.hc_oed_manage_subtitle);
        manageHeader.showAction(false);
    }

    private void setupHeader() {
        DashboardUiModel.TopEvent event = SampleDashboardData.findTopEvent(eventId);
        if ((eventTitle == null || eventTitle.isEmpty()) && event != null) {
            eventTitle = event.title;
        }
        titleView.setText(eventTitle == null || eventTitle.isEmpty()
                ? getString(R.string.hc_oed_title)
                : eventTitle);
    }

    /** Overview shows the aggregates of this event; a missing fixture hides the card. */
    private void setupOverview() {
        DashboardUiModel.TopEvent event = SampleDashboardData.findTopEvent(eventId);
        cardSummary.setVisibility(event == null ? View.GONE : View.VISIBLE);
        if (event == null) {
            return;
        }
        codeView.setText(getString(R.string.hc_oed_code, event.id));
        revenueView.setText(PriceTextView.formatVnd(event.verifiedRevenue));
        registrationsView.setText(getString(R.string.hc_oed_registrations_value,
                event.registrations));
        reviewChip.setStatus(DashboardStatusLabels.reviewLabel(requireContext(),
                event.reviewStage), DashboardStatusLabels.reviewTone(event.reviewStage));
        lifecycleChip.setStatus(DashboardStatusLabels.lifecycleLabel(requireContext(),
                event.lifecycleStage), DashboardStatusLabels.lifecycleTone(event.lifecycleStage));
    }

    private void setupShortcuts() {
        requireView().findViewById(R.id.rowOedTickets).setOnClickListener(v ->
                navigate(R.id.ticket_type_listFragment));
        requireView().findViewById(R.id.rowOedAttendees).setOnClickListener(v ->
                navigate(R.id.registration_listFragment));
        requireView().findViewById(R.id.rowOedOrders).setOnClickListener(v ->
                navigate(R.id.order_listFragment));
        requireView().findViewById(R.id.rowOedCheckin).setOnClickListener(v ->
                navigate(R.id.show_checkin_qrFragment));
        requireView().findViewById(R.id.rowOedReport).setOnClickListener(v ->
                navigate(R.id.event_reportFragment));
        requireView().findViewById(R.id.rowOedHistory).setOnClickListener(v ->
                navigate(R.id.event_historyFragment));
    }

    private void setupTabs(int startTab) {
        tabs.addTab(tabs.newTab().setText(R.string.hc_oed_tab_overview));
        tabs.addTab(tabs.newTab().setText(R.string.hc_oed_tab_analysis));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        int tab = startTab == TAB_ANALYSIS ? TAB_ANALYSIS : TAB_OVERVIEW;
        TabLayout.Tab target = tabs.getTabAt(tab);
        if (target != null) {
            target.select();
        }
        // select() không phát sự kiện khi tab đã được chọn sẵn, nên luôn đồng bộ thủ công.
        showTab(tab);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, selectedTab == null ? TAB_OVERVIEW : selectedTab);
    }

    private void showTab(int position) {
        selectedTab = position == TAB_ANALYSIS ? TAB_ANALYSIS : TAB_OVERVIEW;
        boolean analysis = selectedTab == TAB_ANALYSIS;
        overviewScroll.setVisibility(analysis ? View.GONE : View.VISIBLE);
        analysisContainer.setVisibility(analysis ? View.VISIBLE : View.GONE);
        if (analysis) {
            ensureAnalysis();
        }
    }

    /**
     * Thêm O27 một lần duy nhất. Sau khi xoay màn hình, FragmentManager đã khôi phục child
     * fragment nên chỉ cần nhận ra nó thay vì thêm bản thứ hai.
     */
    private void ensureAnalysis() {
        if (getChildFragmentManager().findFragmentByTag(TAG_ANALYSIS) != null) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong(AiInsightFragment.ARG_EVENT_ID, eventId);
        args.putBoolean(AiInsightFragment.ARG_EMBEDDED, true);
        if (eventTitle != null && !eventTitle.isEmpty()) {
            args.putString(AiInsightFragment.ARG_EVENT_TITLE, eventTitle);
        }
        Fragment analysis = new AiInsightFragment();
        analysis.setArguments(args);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.containerOedAnalysis, analysis, TAG_ANALYSIS)
                .commit();
    }

    private void navigate(int destination) {
        try {
            Navigation.findNavController(requireView()).navigate(destination);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Snackbar.make(requireView(), R.string.hc_coming_soon, Snackbar.LENGTH_SHORT).show();
        }
    }
}
