package com.eventapp.app.ui.user.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.adapter.EventGalleryAdapter;
import com.eventapp.app.ui.common.adapter.EventTicketAdapter;
import com.eventapp.app.ui.common.adapter.ReviewAdapter;
import com.eventapp.app.ui.common.view.AiBadge;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.OfflineBanner;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.RatingDistributionView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.common.view.StarRatingView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.user.review.ReviewUiModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** FR-U05 event detail with overview, tickets, reviews and policy tabs. */
public class EventDetailFragment extends Fragment {

    public static final String ARG_EVENT_ID = EventListFragment.ARG_EVENT_ID;
    public static final String ARG_EVENT_TITLE = EventListFragment.ARG_EVENT_TITLE;

    private static final String STATE_TAB = "state_detail_tab";

    private EventDetailViewModel viewModel;
    private EventGalleryAdapter galleryAdapter;
    private EventTicketAdapter ticketAdapter;
    private ReviewAdapter reviewAdapter;

    private SwipeRefreshLayout swipeRefresh;
    private View content;
    private SkeletonLayout skeleton;
    private ErrorStateView errorState;
    private EmptyStateView unavailableState;
    private OfflineBanner offlineBanner;
    private TextView partialError;
    private TextView cancelledBanner;
    private TextView completedBanner;
    private ImageView cover;
    private TextView heroCategory;
    private StatusChip heroStatus;
    private TextView galleryCount;
    private TextView title;
    private StarRatingView headerStars;
    private TextView rating;
    private TextView time;
    private TextView location;
    private ChipGroup tags;
    private TextView asOf;
    private AiBadge aiBadge;
    private TabLayout tabLayout;
    private View overviewSection;
    private View ticketsSection;
    private View reviewsSection;
    private View policySection;
    private RecyclerView galleryList;
    private RecyclerView ticketList;
    private RecyclerView reviewList;
    private View ticketError;
    private View reviewError;
    private TextView description;
    private TextView organizerName;
    private TextView organizerMeta;
    private StatusChip organizerVerified;
    private TextView conditions;
    private TextView cancelPolicy;
    private TextView reviewEmpty;
    private TextView ratingBig;
    private TextView reviewCount;
    private RatingDistributionView distribution;
    private StarRatingView summaryStars;
    private View onlineBox;
    private View onlineJoin;
    private View bottomBar;
    private TextView bottomPrice;
    private TextView bottomStatus;
    private View primaryButton;
    private View writeReviewButton;

    private int selectedTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EventDetailViewModel.class);
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_TAB, 0);
        }
        bindViews(view);
        setupToolbar();
        setupTabs();
        setupHeaders();
        setupLists();
        setupActions();
        observe();
        long eventId = getArguments() == null ? -1L : getArguments().getLong(ARG_EVENT_ID, -1L);
        viewModel.load(eventId);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, selectedTab);
    }

    private void bindViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshDetail);
        content = view.findViewById(R.id.layoutDetailContent);
        skeleton = view.findViewById(R.id.skeletonDetail);
        errorState = view.findViewById(R.id.errorDetail);
        unavailableState = view.findViewById(R.id.emptyDetail);
        offlineBanner = view.findViewById(R.id.bannerDetailOffline);
        partialError = view.findViewById(R.id.cardDetailPartialError);
        cancelledBanner = view.findViewById(R.id.cardDetailCancelled);
        completedBanner = view.findViewById(R.id.cardDetailCompleted);
        cover = view.findViewById(R.id.ivDetailCover);
        heroCategory = view.findViewById(R.id.tvDetailHeroCategory);
        heroStatus = view.findViewById(R.id.chipDetailHeroStatus);
        galleryCount = view.findViewById(R.id.tvDetailGalleryCount);
        title = view.findViewById(R.id.tvDetailTitle);
        headerStars = view.findViewById(R.id.ratingDetailStars);
        rating = view.findViewById(R.id.tvDetailRating);
        time = view.findViewById(R.id.tvDetailTime);
        location = view.findViewById(R.id.tvDetailLocation);
        tags = view.findViewById(R.id.chipGroupDetailTags);
        asOf = view.findViewById(R.id.tvDetailAsOf);
        aiBadge = view.findViewById(R.id.aiBadgeDetail);
        tabLayout = view.findViewById(R.id.tabDetail);
        overviewSection = view.findViewById(R.id.sectionDetailOverview);
        ticketsSection = view.findViewById(R.id.sectionDetailTickets);
        reviewsSection = view.findViewById(R.id.sectionDetailReviews);
        policySection = view.findViewById(R.id.sectionDetailPolicy);
        galleryList = view.findViewById(R.id.rvDetailGallery);
        ticketList = view.findViewById(R.id.rvDetailTickets);
        reviewList = view.findViewById(R.id.rvDetailReviews);
        ticketError = view.findViewById(R.id.layoutDetailTicketError);
        reviewError = view.findViewById(R.id.layoutDetailReviewError);
        description = view.findViewById(R.id.tvDetailDescription);
        organizerName = view.findViewById(R.id.tvDetailOrganizerName);
        organizerMeta = view.findViewById(R.id.tvDetailOrganizerMeta);
        organizerVerified = view.findViewById(R.id.chipDetailOrganizerVerified);
        conditions = view.findViewById(R.id.tvDetailConditions);
        cancelPolicy = view.findViewById(R.id.tvDetailCancelPolicy);
        reviewEmpty = view.findViewById(R.id.tvDetailReviewEmpty);
        ratingBig = view.findViewById(R.id.tvDetailRatingBig);
        reviewCount = view.findViewById(R.id.tvDetailReviewCount);
        distribution = view.findViewById(R.id.ratingDistributionDetail);
        summaryStars = view.findViewById(R.id.ratingDetailSummary);
        onlineBox = view.findViewById(R.id.layoutDetailOnline);
        onlineJoin = view.findViewById(R.id.btnDetailOnlineJoin);
        bottomBar = view.findViewById(R.id.layoutDetailBottomBar);
        bottomPrice = view.findViewById(R.id.tvDetailBottomPrice);
        bottomStatus = view.findViewById(R.id.tvDetailBottomStatus);
        primaryButton = view.findViewById(R.id.btnDetailPrimary);
        writeReviewButton = view.findViewById(R.id.btnDetailWriteReview);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnDetailBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        requireView().findViewById(R.id.btnDetailShare).setOnClickListener(v -> shareEvent());
        requireView().findViewById(R.id.btnDetailFavorite).setOnClickListener(v -> {
            viewModel.toggleFavorite();
        });
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                showTab(selectedTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupHeaders() {
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailAbout))
                .setTitle(R.string.hc_detail_about);
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailGallery))
                .setTitle(R.string.hc_detail_gallery);
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailTickets))
                .setTitle(R.string.hc_detail_ticket_title);
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailTickets))
                .setSubtitle(R.string.hc_detail_ticket_subtitle);
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailReviews))
                .setTitle(R.string.hc_detail_review_title);
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailConditions))
                .setTitle(R.string.hc_detail_conditions);
        ((SectionHeaderView) requireView().findViewById(R.id.headerDetailCancelPolicy))
                .setTitle(R.string.hc_detail_cancel_policy);
    }

    private void setupLists() {
        galleryAdapter = new EventGalleryAdapter();
        galleryAdapter.setOnGalleryClickListener((item, position) -> Snackbar.make(
                requireView(), item.label, Snackbar.LENGTH_SHORT).show());
        galleryList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        galleryList.setAdapter(galleryAdapter);

        ticketAdapter = new EventTicketAdapter();
        ticketAdapter.setOnTicketSelectedListener(position -> {
            viewModel.selectTicket(position);
            ticketList.postDelayed(() -> {
                if (isAdded()) {
                    ticketAdapter.notifyDataSetChanged();
                }
            }, 10L);
        });
        ticketList.setLayoutManager(new LinearLayoutManager(requireContext()));
        ticketList.setNestedScrollingEnabled(false);
        ticketList.setAdapter(ticketAdapter);

        reviewAdapter = new ReviewAdapter(ReviewAdapter.Mode.COMPACT);
        reviewAdapter.setShowEventName(false);
        reviewList.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewList.setNestedScrollingEnabled(false);
        reviewList.setAdapter(reviewAdapter);
    }

    private void setupActions() {
        primaryButton.setOnClickListener(v -> {
            EventDetailViewModel.DetailState state = viewModel.getState().getValue();
            if (state == null) {
                return;
            }
            if (state.disabledReason != null) {
                Snackbar.make(requireView(), state.disabledReason, Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (state.offline) {
                Snackbar.make(requireView(), R.string.hc_offline, Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (state.primaryAction == null) {
                return;
            }
            switch (state.primaryAction) {
                case BUY_TICKET:
                    navigateTo(R.id.checkoutFragment, eventArgs(state));
                    break;
                case REGISTER_FREE:
                    navigateTo(R.id.free_registerFragment, eventArgs(state));
                    break;
                case VIEW_MY_TICKET:
                    navigateTo(R.id.my_ticketsFragment, null);
                    break;
                case WRITE_REVIEW:
                    navigateTo(R.id.write_reviewFragment, reviewArgs(state.event, false));
                    break;
                case NONE:
                default:
                    break;
            }
        });

        writeReviewButton.setOnClickListener(v -> {
            EventDetailViewModel.DetailState state = viewModel.getState().getValue();
            if (state != null && state.event != null) {
                navigateTo(R.id.write_reviewFragment, reviewArgs(state.event, false));
            }
        });

        requireView().findViewById(R.id.btnDetailSeeReviews).setOnClickListener(v -> {
            EventDetailViewModel.DetailState state = viewModel.getState().getValue();
            if (state != null && state.event != null) {
                navigateTo(R.id.event_reviewsFragment, reviewArgs(state.event, false));
            }
        });

        requireView().findViewById(R.id.btnDetailTicketRetry).setOnClickListener(v ->
                viewModel.retryTickets());
        requireView().findViewById(R.id.btnDetailReviewRetry).setOnClickListener(v ->
                viewModel.retryReviews());
        partialError.setOnClickListener(v -> {
            viewModel.retryTickets();
            viewModel.retryReviews();
        });
        onlineJoin.setOnClickListener(v -> Snackbar.make(requireView(),
                R.string.hc_detail_online_link_locked, Snackbar.LENGTH_SHORT).show());
        errorState.setOnRetryClickListener(v -> viewModel.retry());
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefresh.setRefreshing(false);
            skeleton.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            content.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean hasError = Boolean.TRUE.equals(error);
            errorState.setVisibility(hasError ? View.VISIBLE : View.GONE);
            if (hasError) {
                errorState.setTitle(R.string.hc_error_title);
                errorState.setMessage(R.string.hc_error_default);
            }
        });
        viewModel.getUnavailable().observe(getViewLifecycleOwner(), unavailable -> {
            boolean isUnavailable = Boolean.TRUE.equals(unavailable);
            unavailableState.setVisibility(isUnavailable ? View.VISIBLE : View.GONE);
            if (isUnavailable) {
                unavailableState.setTitle(R.string.hc_detail_unavailable_title);
                unavailableState.setMessage(R.string.hc_detail_unavailable_message);
                unavailableState.setActionText(R.string.hc_detail_back_home);
                unavailableState.setOnActionClickListener(v ->
                        Navigation.findNavController(requireView()).popBackStack());
                skeleton.setVisibility(View.GONE);
                content.setVisibility(View.INVISIBLE);
            }
        });
        viewModel.getState().observe(getViewLifecycleOwner(), this::bindState);
    }

    private void bindState(EventDetailViewModel.DetailState state) {
        if (state == null || state.event == null) {
            return;
        }
        EventUiModel event = state.event;
        swipeRefresh.setRefreshing(false);
        skeleton.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        unavailableState.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        offlineBanner.setVisibility(state.offline ? View.VISIBLE : View.GONE);
        if (state.offline) {
            offlineBanner.setCachedAt(asOfFormat(state.asOf));
            offlineBanner.setOnRetryClickListener(v -> viewModel.refresh());
        }
        cancelledBanner.setVisibility(state.cancelled ? View.VISIBLE : View.GONE);
        completedBanner.setVisibility(state.completed ? View.VISIBLE : View.GONE);
        partialError.setVisibility(state.ticketsError || state.reviewsError
                ? View.VISIBLE : View.GONE);

        bindHero(event, state);
        bindHeader(event, state);
        bindOverview(event, state);
        bindTickets(state);
        bindReviews(state);
        bindPolicy(event, state);
        bindBottomBar(state);
        tabLayout.selectTab(tabLayout.getTabAt(selectedTab), false);
        showTab(selectedTab);
    }

    private void bindHero(EventUiModel event, EventDetailViewModel.DetailState state) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{EventCategoryUi.gradientStart(requireContext(), event.category),
                        EventCategoryUi.gradientEnd(requireContext(), event.category)});
        gradient.setCornerRadius(dp(20));
        cover.setBackground(gradient);
        cover.setImageResource(EventCategoryUi.iconRes(event.category));
        cover.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.ds_white)));
        heroCategory.setText(EventCategoryUi.label(requireContext(), event.category));
        galleryCount.setText(getString(R.string.hc_detail_gallery_count, state.gallery.size()));
        bindHeroStatus(state);

        requireView().findViewById(R.id.btnDetailFavorite).setBackgroundResource(
                android.R.color.transparent);
        ImageButton favorite = requireView().findViewById(R.id.btnDetailFavorite);
        favorite.setImageResource(event.favorite
                ? R.drawable.ic_ds_heart_filled : R.drawable.ic_ds_heart);
        favorite.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                event.favorite ? R.color.ds_cta : R.color.ds_text)));
        favorite.setContentDescription(getString(event.favorite
                ? R.string.hc_detail_remove_save : R.string.hc_detail_save));
    }

    private void bindHeroStatus(EventDetailViewModel.DetailState state) {
        if (state.cancelled) {
            heroStatus.setStatus(getString(R.string.hc_event_cancelled),
                    StatusChip.Tone.DANGER);
            heroStatus.setVisibility(View.VISIBLE);
        } else if (state.event.availability == EventUiModel.Availability.SOLD_OUT) {
            heroStatus.setStatus(getString(R.string.hc_detail_ticket_sold_out),
                    StatusChip.Tone.MUTED);
            heroStatus.setVisibility(View.VISIBLE);
        } else if (state.event.availability == EventUiModel.Availability.FEW_LEFT) {
            heroStatus.setStatus(getString(R.string.hc_event_few_left),
                    StatusChip.Tone.WARNING);
            heroStatus.setVisibility(View.VISIBLE);
        } else {
            heroStatus.setVisibility(View.GONE);
        }
    }

    private void bindHeader(EventUiModel event, EventDetailViewModel.DetailState state) {
        title.setText(event.title);
        headerStars.setStarSize(18);
        headerStars.setRating(state.reviewAverage > 0f ? state.reviewAverage : event.rating);
        rating.setText(getString(R.string.hc_detail_review_summary,
                formatRating(state.reviewAverage > 0f ? state.reviewAverage : event.rating),
                state.reviews.size() > 0 ? state.reviews.size() : event.reviewCount));
        time.setText(getString(R.string.hc_detail_time_range,
                formatDateTime(event.startAt), formatDateTime(event.endAt)));
        location.setText(event.online ? getString(R.string.hc_detail_online)
                : (TextUtils.isEmpty(event.venue) ? event.city
                : event.venue + ", " + event.city));
        bindTags(event.tags);
        asOf.setText(getString(R.string.hc_detail_updated_at, asOfFormat(state.asOf)));

        boolean showAi = event.aiScore != null;
        aiBadge.setVisibility(showAi ? View.VISIBLE : View.GONE);
        if (showAi) {
            aiBadge.setScore(event.aiScore);
            aiBadge.setReason(event.aiReason);
            aiBadge.showReason(true);
        }
    }

    private void bindTags(List<String> values) {
        tags.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String value : values) {
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_tag_chip, tags, false);
            chip.setText(value);
            chip.setCloseIconVisible(false);
            chip.setClickable(false);
            chip.setCheckable(false);
            tags.addView(chip);
        }
        tags.setVisibility(values.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void bindOverview(EventUiModel event, EventDetailViewModel.DetailState state) {
        description.setText(TextUtils.isEmpty(event.description)
                ? getString(R.string.hc_empty_default) : event.description);
        galleryAdapter.setCategory(event.category);
        galleryAdapter.submitList(state.gallery);
        organizerName.setText(event.organizer);
        organizerMeta.setText(getString(R.string.hc_detail_organizer_events,
                24 + (int) (event.id % 12)));
        organizerVerified.setStatus(getString(R.string.hc_detail_organizer_verified),
                StatusChip.Tone.SUCCESS);
        organizerVerified.setVisibility(View.VISIBLE);
    }

    private void bindTickets(EventDetailViewModel.DetailState state) {
        ticketError.setVisibility(state.ticketsError ? View.VISIBLE : View.GONE);
        ticketList.setVisibility(state.ticketsError ? View.GONE : View.VISIBLE);
        ticketAdapter.submitList(state.tickets, state.selectedTicketIndex);
    }

    private void bindReviews(EventDetailViewModel.DetailState state) {
        reviewError.setVisibility(state.reviewsError ? View.VISIBLE : View.GONE);
        reviewList.setVisibility(state.reviewsError ? View.GONE : View.VISIBLE);
        List<ReviewUiModel> visible = new ArrayList<>();
        for (ReviewUiModel review : state.reviews) {
            if (review.status == ReviewUiModel.Status.VISIBLE) {
                visible.add(review);
            }
        }
        List<ReviewUiModel> preview = new ArrayList<>(visible);
        if (preview.size() > 3) {
            preview = new ArrayList<>(preview.subList(0, 3));
        }
        reviewAdapter.submitList(preview);
        reviewEmpty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        reviewList.setVisibility(visible.isEmpty() ? View.GONE : reviewList.getVisibility());

        ratingBig.setText(formatRating(state.reviewAverage));
        summaryStars.setStarSize(16);
        summaryStars.setRating(state.reviewAverage);
        reviewCount.setText(getString(R.string.hc_detail_review_count, visible.size()));
        distribution.setDistribution(state.reviewDistribution);

        boolean canWrite = state.canReview;
        writeReviewButton.setVisibility(canWrite ? View.VISIBLE : View.GONE);
    }

    private void bindPolicy(EventUiModel event, EventDetailViewModel.DetailState state) {
        eventsPolicy(state);
        onlineBox.setVisibility(event.online ? View.VISIBLE : View.GONE);
        onlineJoin.setVisibility(state.onlineLinkAvailable ? View.VISIBLE : View.GONE);
    }

    private void eventsPolicy(EventDetailViewModel.DetailState state) {
        conditions.setText(TextUtils.isEmpty(state.conditions)
                ? getString(R.string.hc_detail_conditions_empty) : state.conditions);
        cancelPolicy.setText(TextUtils.isEmpty(state.cancelPolicy)
                ? getString(R.string.hc_detail_policy_empty) : state.cancelPolicy);
    }

    private void bindBottomBar(EventDetailViewModel.DetailState state) {
        if (state.cancelled) {
            bottomBar.setVisibility(View.GONE);
            return;
        }
        bottomBar.setVisibility(View.VISIBLE);
        EventDetailViewModel.TicketOption selected = selectedTicket(state);
        if (selected == null || state.event.free) {
            bottomPrice.setText(R.string.hc_detail_ticket_free);
        } else {
            bottomPrice.setText(PriceTextView.formatVnd(selected.price));
        }

        String statusText;
        if (state.disabledReason != null) {
            statusText = state.disabledReason;
        } else {
            switch (state.primaryAction) {
                case BUY_TICKET:
                    statusText = getString(R.string.hc_detail_ticket_status_on_sale);
                    break;
                case REGISTER_FREE:
                    statusText = getString(R.string.hc_detail_register_free);
                    break;
                case WRITE_REVIEW:
                    statusText = getString(R.string.hc_detail_review_window);
                    break;
                case VIEW_MY_TICKET:
                    statusText = getString(R.string.hc_detail_action_registered);
                    break;
                case NONE:
                default:
                    statusText = getString(R.string.hc_detail_action_completed);
                    break;
            }
        }
        bottomStatus.setText(statusText);

        int text;
        switch (state.primaryAction) {
            case REGISTER_FREE:
                text = R.string.hc_detail_register_free;
                break;
            case WRITE_REVIEW:
                text = R.string.hc_detail_write_review;
                break;
            case VIEW_MY_TICKET:
                text = R.string.hc_detail_view_ticket;
                break;
            case BUY_TICKET:
                text = R.string.hc_detail_buy_ticket;
                break;
            case NONE:
            default:
                text = R.string.hc_detail_action_completed;
                break;
        }
        ((TextView) primaryButton).setText(text);
        boolean enabled = state.disabledReason == null
                && state.primaryAction != EventDetailViewModel.PrimaryAction.NONE
                && !state.offline;
        primaryButton.setEnabled(enabled);
        primaryButton.setAlpha(enabled ? 1f : 0.6f);
    }

    private EventDetailViewModel.TicketOption selectedTicket(
            EventDetailViewModel.DetailState state) {
        if (state.tickets.isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(state.selectedTicketIndex, state.tickets.size() - 1));
        return state.tickets.get(index);
    }

    private void showTab(int index) {
        overviewSection.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        ticketsSection.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        reviewsSection.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        policySection.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
    }

    private void shareEvent() {
        EventDetailViewModel.DetailState state = viewModel.getState().getValue();
        if (state == null || state.event == null) {
            return;
        }
        String text = getString(R.string.hc_detail_share_text, state.event.title);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, state.event.title);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.hc_detail_share_chooser)));
    }

    private Bundle eventArgs(EventDetailViewModel.DetailState state) {
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, state.event.id);
        args.putString(ARG_EVENT_TITLE, state.event.title);
        return args;
    }

    private Bundle reviewArgs(EventUiModel event, boolean editMode) {
        Bundle args = new Bundle();
        args.putLong(WriteReviewFragment.ARG_EVENT_ID, event.id);
        args.putString(WriteReviewFragment.ARG_EVENT_TITLE, event.title);
        args.putBoolean(WriteReviewFragment.ARG_EDIT_MODE, editMode);
        args.putLong(WriteReviewFragment.ARG_EVENT_END_AT, event.endAt);
        args.putBoolean(WriteReviewFragment.ARG_CAN_REVIEW, true);
        return args;
    }

    private void navigateTo(int destination, @Nullable Bundle args) {
        if (!isAdded()) {
            return;
        }
        try {
            Navigation.findNavController(requireView()).navigate(destination, args);
        } catch (IllegalArgumentException exception) {
            Snackbar.make(requireView(), R.string.hc_coming_soon, Snackbar.LENGTH_SHORT).show();
        }
    }

    private String formatDateTime(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy • HH:mm", new Locale("vi", "VN"))
                .format(new Date(millis));
    }

    private String formatRating(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String asOfFormat(long millis) {
        return new SimpleDateFormat("HH:mm", new Locale("vi", "VN"))
                .format(new Date(millis));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
