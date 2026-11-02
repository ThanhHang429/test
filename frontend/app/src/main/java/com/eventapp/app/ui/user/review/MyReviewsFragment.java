package com.eventapp.app.ui.user.review;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.adapter.ReviewAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * FR-U20 "Đánh giá của tôi": mọi đánh giá người dùng đã viết, kèm trạng thái kiểm duyệt và lối
 * vào chỉnh sửa. Đánh giá HELD/HIDDEN chỉ tác giả thấy, đúng U20-R03.
 */
public class MyReviewsFragment extends Fragment {

    private MyReviewsViewModel viewModel;
    private ReviewAdapter adapter;
    private RecyclerView list;
    private ChipGroup filterGroup;
    private Chip chipAll;
    private Chip chipVisible;
    private Chip chipHeld;
    private Chip chipHidden;
    private TextView countView;
    private TextView noteView;
    private EmptyStateView emptyState;
    private SkeletonLayout skeleton;
    private ErrorStateView errorState;

    private boolean firstLoad = true;
    private boolean bindingFilter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_review_my_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyReviewsViewModel.class);
        bindViews(view);
        setupToolbar();
        setupList();
        setupFilter();
        observe();
        NavBarSpacing.padBottomForNavBar(requireView(), list, 16);
    }

    private void bindViews(View view) {
        list = view.findViewById(R.id.rvMyReviews);
        filterGroup = view.findViewById(R.id.chipGroupMyReviews);
        chipAll = view.findViewById(R.id.chipMyReviewsAll);
        chipVisible = view.findViewById(R.id.chipMyReviewsVisible);
        chipHeld = view.findViewById(R.id.chipMyReviewsHeld);
        chipHidden = view.findViewById(R.id.chipMyReviewsHidden);
        countView = view.findViewById(R.id.tvMyReviewsCount);
        noteView = view.findViewById(R.id.tvMyReviewsNote);
        emptyState = view.findViewById(R.id.emptyMyReviews);
        skeleton = view.findViewById(R.id.skeletonMyReviews);
        errorState = view.findViewById(R.id.errorMyReviews);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnMyReviewsBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        emptyState.setIcon(R.drawable.ic_ds_star);
        emptyState.setActionText(R.string.hc_myreviews_empty_action);
        emptyState.setOnActionClickListener(v -> Navigation.findNavController(requireView())
                .navigate(R.id.homeFragment));

        errorState.setOnRetryClickListener(v -> viewModel.retry());
    }

    private void setupList() {
        adapter = new ReviewAdapter(ReviewAdapter.Mode.FULL);
        adapter.setShowEventName(true);
        adapter.setOnReviewActionListener(new ReviewAdapter.OnReviewActionListener() {
            @Override
            public void onEditReview(ReviewUiModel review) {
                openEditor(review);
            }

            @Override
            public void onReportReview(ReviewUiModel review) {
                // Đánh giá của chính mình không có thao tác báo cáo.
            }

            @Override
            public void onModerateReview(ReviewUiModel review) {
                // Màn người dùng không có thao tác kiểm duyệt.
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setItemAnimator(null);
        list.setAdapter(adapter);
    }

    private void setupFilter() {
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingFilter || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipMyReviewsVisible) {
                viewModel.setFilter(MyReviewsViewModel.Filter.VISIBLE);
            } else if (id == R.id.chipMyReviewsHeld) {
                viewModel.setFilter(MyReviewsViewModel.Filter.HELD);
            } else if (id == R.id.chipMyReviewsHidden) {
                viewModel.setFilter(MyReviewsViewModel.Filter.HIDDEN);
            } else {
                viewModel.setFilter(MyReviewsViewModel.Filter.ALL);
            }
        });
    }

    private void observe() {
        viewModel.getCounts().observe(getViewLifecycleOwner(), this::renderCounts);
        viewModel.getFilter().observe(getViewLifecycleOwner(), this::renderFilter);
        viewModel.getReviews().observe(getViewLifecycleOwner(), this::renderReviews);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = loading != null && loading;
            skeleton.setVisibility(busy && firstLoad ? View.VISIBLE : View.GONE);
            if (!busy) {
                firstLoad = false;
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean failed = error != null && error;
            errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        });
    }

    private void renderCounts(MyReviewsViewModel.Counts counts) {
        if (counts == null) {
            return;
        }
        chipAll.setText(getString(R.string.hc_myreviews_filter_all_count, counts.all));
        chipVisible.setText(getString(R.string.hc_myreviews_filter_visible_count,
                counts.visible));
        chipHeld.setText(getString(R.string.hc_myreviews_filter_held_count, counts.held));
        chipHidden.setText(getString(R.string.hc_myreviews_filter_hidden_count, counts.hidden));
        countView.setText(getString(R.string.hc_myreviews_summary, counts.all, counts.visible));
    }

    private void renderFilter(MyReviewsViewModel.Filter filter) {
        bindingFilter = true;
        int chipId = R.id.chipMyReviewsAll;
        if (filter == MyReviewsViewModel.Filter.VISIBLE) {
            chipId = R.id.chipMyReviewsVisible;
        } else if (filter == MyReviewsViewModel.Filter.HELD) {
            chipId = R.id.chipMyReviewsHeld;
        } else if (filter == MyReviewsViewModel.Filter.HIDDEN) {
            chipId = R.id.chipMyReviewsHidden;
        }
        filterGroup.check(chipId);
        bindingFilter = false;

        boolean hidden = filter == MyReviewsViewModel.Filter.HIDDEN;
        noteView.setVisibility(hidden ? View.VISIBLE : View.GONE);
        noteView.setText(R.string.hc_myreviews_hidden_note);
    }

    private void renderReviews(List<ReviewUiModel> reviews) {
        adapter.submitList(reviews);
        boolean empty = reviews == null || reviews.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            emptyState.setTitle(R.string.hc_myreviews_empty_title);
            emptyState.setMessage(R.string.hc_myreviews_empty_message);
        }
    }

    private void openEditor(ReviewUiModel review) {
        Bundle args = new Bundle();
        args.putLong(WriteReviewFragment.ARG_EVENT_ID, review.eventId);
        args.putString(WriteReviewFragment.ARG_EVENT_TITLE, review.eventTitle);
        args.putBoolean(WriteReviewFragment.ARG_EDIT_MODE, true);
        args.putBoolean(WriteReviewFragment.ARG_CAN_REVIEW, true);
        args.putString(WriteReviewFragment.ARG_COMMENT, review.comment);
        args.putFloat(WriteReviewFragment.ARG_RATING, review.rating);
        args.putInt(WriteReviewFragment.ARG_EDIT_COUNT, review.editCount);
        args.putLong(WriteReviewFragment.ARG_EDIT_VERSION, review.editedAt);
        Navigation.findNavController(requireView())
                .navigate(R.id.write_reviewFragment, args);
    }
}
