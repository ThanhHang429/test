package com.eventapp.app.ui.user.review;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.adapter.ReviewAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.RatingDistributionView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.common.view.StarRatingView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;

/** FR-U20 public reviews with average, distribution, sort and pinned own review. */
public class EventReviewsFragment extends Fragment {

    public static final String ARG_EVENT_ID = WriteReviewFragment.ARG_EVENT_ID;
    public static final String ARG_EVENT_TITLE = WriteReviewFragment.ARG_EVENT_TITLE;
    public static final String ARG_CAN_REVIEW = WriteReviewFragment.ARG_CAN_REVIEW;
    public static final String ARG_EVENT_END_AT = WriteReviewFragment.ARG_EVENT_END_AT;
    public static final String ARG_HAS_MY_REVIEW = "arg_reviews_has_my_review";

    private EventReviewsViewModel viewModel;
    private ReviewAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;
    private TextView title;
    private TextView subtitle;
    private TextView average;
    private TextView count;
    private StarRatingView summaryStars;
    private RatingDistributionView distribution;
    private TextView mineNote;
    private ChipGroup sortGroup;
    private View writeTop;
    private View writeBottom;
    private View loadMore;

    private long eventId;
    private String eventTitle = "";
    private boolean canReview = true;
    private boolean hasMine = true;
    private long eventEndAt;
    private boolean bindingSort;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_event_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readArguments();
        viewModel = new ViewModelProvider(this).get(EventReviewsViewModel.class);
        bindViews(view);
        setupToolbar();
        setupList();
        setupSort();
        observe();
        viewModel.load(eventId, eventTitle, canReview, hasMine);
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        eventId = args.getLong(ARG_EVENT_ID, -1L);
        eventTitle = args.getString(ARG_EVENT_TITLE, "");
        canReview = args.getBoolean(ARG_CAN_REVIEW, true);
        hasMine = args.getBoolean(ARG_HAS_MY_REVIEW, true);
        eventEndAt = args.getLong(ARG_EVENT_END_AT, 0L);
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.rvReviews);
        swipeRefresh = view.findViewById(R.id.swipeRefreshReviews);
        skeleton = view.findViewById(R.id.skeletonReviews);
        emptyState = view.findViewById(R.id.emptyReviews);
        errorState = view.findViewById(R.id.errorReviews);
        title = view.findViewById(R.id.tvReviewsTitle);
        subtitle = view.findViewById(R.id.tvReviewsSubtitle);
        average = view.findViewById(R.id.tvReviewsAverage);
        count = view.findViewById(R.id.tvReviewsCount);
        summaryStars = view.findViewById(R.id.ratingReviewsSummary);
        distribution = view.findViewById(R.id.ratingReviewsDistribution);
        mineNote = view.findViewById(R.id.tvReviewsMineNote);
        sortGroup = view.findViewById(R.id.chipGroupReviewsSort);
        writeTop = view.findViewById(R.id.btnReviewsWriteTop);
        writeBottom = view.findViewById(R.id.btnReviewsWrite);
        loadMore = view.findViewById(R.id.btnReviewsLoadMore);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnReviewsBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        writeTop.setOnClickListener(v -> openWriteReview(false));
        writeBottom.setOnClickListener(v -> openWriteReview(false));
        title.setText(R.string.hc_reviews_title);
        subtitle.setText(TextUtils.isEmpty(eventTitle) ? getString(R.string.hc_reviews_write_note)
                : eventTitle);
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        loadMore.setOnClickListener(v -> viewModel.loadMore());
        errorState.setOnRetryClickListener(v -> viewModel.retry());
        emptyState.setTitle(R.string.hc_reviews_empty_title);
        emptyState.setMessage(R.string.hc_reviews_empty_message);
        emptyState.setActionText(R.string.hc_reviews_write);
        emptyState.setOnActionClickListener(v -> openWriteReview(false));
    }

    private void setupList() {
        adapter = new ReviewAdapter(ReviewAdapter.Mode.FULL);
        adapter.setShowEventName(false);
        adapter.setOnReviewActionListener(new ReviewAdapter.OnReviewActionListener() {
            @Override
            public void onEditReview(ReviewUiModel review) {
                openWriteReview(true, review);
            }

            @Override
            public void onReportReview(ReviewUiModel review) {
                showReportDialog(review);
            }

            @Override
            public void onModerateReview(ReviewUiModel review) {
                // User-facing list has no moderation action.
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
    }

    private void setupSort() {
        sortGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingSort || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipReviewsHigh) {
                viewModel.setSort(EventReviewsViewModel.Sort.RATING_HIGH);
            } else if (id == R.id.chipReviewsLow) {
                viewModel.setSort(EventReviewsViewModel.Sort.RATING_LOW);
            } else {
                viewModel.setSort(EventReviewsViewModel.Sort.NEWEST);
            }
        });
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefresh.setRefreshing(false);
            skeleton.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean hasError = Boolean.TRUE.equals(error);
            errorState.setVisibility(hasError ? View.VISIBLE : View.GONE);
            if (hasError) {
                errorState.setTitle(R.string.hc_error_title);
                errorState.setMessage(R.string.hc_error_default);
            }
        });

        viewModel.getSummary().observe(getViewLifecycleOwner(), this::bindSummary);
        viewModel.getReviews().observe(getViewLifecycleOwner(), this::bindReviews);
        viewModel.getHasMore().observe(getViewLifecycleOwner(), more ->
                loadMore.setVisibility(Boolean.TRUE.equals(more) ? View.VISIBLE : View.GONE));
    }

    private void bindSummary(EventReviewsViewModel.Summary summary) {
        if (summary == null) {
            return;
        }
        average.setText(String.format(Locale.US, "%.1f", summary.average));
        summaryStars.setStarSize(16);
        summaryStars.setRating(summary.average);
        count.setText(getString(R.string.hc_reviews_total, summary.total));
        distribution.setDistribution(summary.distribution);
        mineNote.setVisibility(summary.hasMine ? View.VISIBLE : View.GONE);
        boolean showWrite = summary.canWrite && !summary.hasMine;
        writeTop.setVisibility(showWrite ? View.VISIBLE : View.GONE);
        writeBottom.setVisibility(showWrite ? View.VISIBLE : View.GONE);
        subtitle.setText(TextUtils.isEmpty(eventTitle)
                ? getString(R.string.hc_reviews_write_note) : eventTitle);
    }

    private void bindReviews(List<ReviewUiModel> reviews) {
        adapter.submitList(reviews);
        boolean isEmpty = reviews == null || reviews.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            skeleton.setVisibility(View.GONE);
        }
    }

    private void showReportDialog(ReviewUiModel review) {
        String[] reasons = getResources().getStringArray(R.array.hc_report_reasons);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.hc_review_report_title)
                .setItems(reasons, (dialog, which) -> Snackbar.make(requireView(),
                        R.string.hc_review_report_sent, Snackbar.LENGTH_SHORT).show())
                .setNegativeButton(R.string.hc_action_cancel, null)
                .show();
    }

    private void openWriteReview(boolean edit) {
        openWriteReview(edit, edit && adapter.getItemCount() > 0 ? adapter.getItem(0) : null);
    }

    private void openWriteReview(boolean edit, @Nullable ReviewUiModel review) {
        Bundle args = new Bundle();
        args.putLong(WriteReviewFragment.ARG_EVENT_ID, eventId);
        args.putString(WriteReviewFragment.ARG_EVENT_TITLE, eventTitle);
        args.putBoolean(WriteReviewFragment.ARG_EDIT_MODE, edit);
        args.putBoolean(WriteReviewFragment.ARG_CAN_REVIEW, canReview);
        args.putLong(WriteReviewFragment.ARG_EVENT_END_AT, eventEndAt);
        if (review != null) {
            args.putString(WriteReviewFragment.ARG_COMMENT, review.comment);
            args.putFloat(WriteReviewFragment.ARG_RATING, review.rating);
            args.putInt(WriteReviewFragment.ARG_EDIT_COUNT, review.editCount);
        }
        Navigation.findNavController(requireView())
                .navigate(R.id.write_reviewFragment, args);
    }
}
