package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.ui.model.ReviewSummaryUiModel;
import com.eventapp.app.ui.model.ReviewUiModel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Chuyển DTO đánh giá backend sang ReviewUiModel. */
public final class ReviewMapper {

    private ReviewMapper() {
    }

    @NonNull
    public static ReviewSummaryUiModel fromSummary(
            @NonNull DiscoveryDtos.ReviewSummary summary) {
        Map<String, Long> distribution = summary.distribution == null
                ? new LinkedHashMap<>() : summary.distribution;
        return new ReviewSummaryUiModel(
                summary.averageRating, summary.totalReviews, distribution);
    }

    @NonNull
    public static ReviewUiModel fromPublic(@Nullable ReviewDtos.Review dto) {
        if (dto == null) {
            return new ReviewUiModel.Builder().build();
        }
        return new ReviewUiModel.Builder()
                .id(dto.id)
                .authorName(dto.authorName)
                .rating(dto.rating)
                .comment(dto.text)
                .createdAt(DateTimeUtils.toMillis(dto.createdAt))
                .editedAt(DateTimeUtils.toMillis(dto.updatedAt))
                .status(status(dto.status))
                .mine(dto.mine)
                .source(ReviewUiModel.Source.USER)
                .build();
    }

    @NonNull
    public static ReviewUiModel fromMine(@Nullable ReviewDtos.MyReview dto) {
        if (dto == null) {
            return new ReviewUiModel.Builder().build();
        }
        return new ReviewUiModel.Builder()
                .id(dto.id)
                .eventId(dto.eventId)
                .eventTitle(dto.eventTitle)
                .authorName("Bạn")
                .rating(dto.rating)
                .comment(dto.text)
                .createdAt(DateTimeUtils.toMillis(dto.createdAt))
                .editedAt(DateTimeUtils.toMillis(dto.updatedAt))
                .editCount(dto.editCount)
                .canEdit(dto.canEdit)
                .editDaysLeft(dto.editDaysLeft)
                .status(status(dto.status))
                .mine(true)
                .flagReason(dto.moderationReason)
                .source(ReviewUiModel.Source.USER)
                .build();
    }

    @NonNull
    public static ReviewUiModel fromAdmin(@Nullable ReviewDtos.AdminReview dto) {
        if (dto == null) {
            return new ReviewUiModel.Builder().build();
        }
        return new ReviewUiModel.Builder()
                .id(dto.id)
                .eventId(dto.eventId)
                .eventTitle(dto.eventTitle)
                .authorName(dto.authorName)
                .authorCode(dto.authorEmail)
                .rating(dto.rating)
                .comment(dto.text)
                .createdAt(DateTimeUtils.toMillis(dto.createdAt))
                .editedAt(DateTimeUtils.toMillis(dto.updatedAt))
                .status(status(dto.status))
                .source(ReviewUiModel.Source.FLAGGED)
                .flagReason(dto.moderationReason)
                .evidence(dto.flaggedCodes == null || dto.flaggedCodes.isEmpty()
                        ? null : String.join(", ", dto.flaggedCodes))
                .build();
    }

    @NonNull
    public static ReviewUiModel fromFlag(@Nullable ReviewDtos.ModerationFlag dto) {
        if (dto == null) {
            return new ReviewUiModel.Builder().build();
        }
        return new ReviewUiModel.Builder()
                .id(dto.targetId)
                .eventId(dto.eventId == null ? 0L : dto.eventId)
                .eventTitle(dto.eventTitle)
                .authorName(dto.eventTitle == null ? "Nội dung bị gắn cờ" : dto.eventTitle)
                .comment(dto.evidence)
                .createdAt(DateTimeUtils.toMillis(dto.createdAt))
                .status(ReviewUiModel.Status.HELD)
                .source("REPORTED".equalsIgnoreCase(dto.reasonCode)
                        ? ReviewUiModel.Source.REPORTED : ReviewUiModel.Source.FLAGGED)
                .flagReason(dto.reasonLabel)
                .evidence(dto.evidence)
                .severity(dto.severity)
                .build();
    }

    @NonNull
    private static ReviewUiModel.Status status(@Nullable String raw) {
        if (raw == null) {
            return ReviewUiModel.Status.VISIBLE;
        }
        switch (raw.toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING":
            case "HELD":
                return ReviewUiModel.Status.HELD;
            case "HIDDEN":
            case "REMOVED":
                return ReviewUiModel.Status.HIDDEN;
            default:
                return ReviewUiModel.Status.VISIBLE;
        }
    }
}
