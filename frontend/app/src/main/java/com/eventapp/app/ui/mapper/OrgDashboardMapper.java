package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;

import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.ui.model.OrgDashboardUiModel;

import java.util.ArrayList;
import java.util.List;

/** Chuyển dashboard organizer sang OrgDashboardUiModel. */
public final class OrgDashboardMapper {

    private OrgDashboardMapper() {
    }

    @NonNull
    public static OrgDashboardUiModel from(@NonNull DashboardDtos.Dashboard data) {
        long onSale = countOf(data.events, "ON_SALE");
        long pending = countOf(data.events, "PENDING_REVIEW");
        long sold = data.registrations == null ? 0 : data.registrations.ticketsIssued;
        long confirmed = data.registrations == null ? 0 : data.registrations.confirmed;
        long checkedIn = data.checkin == null ? 0 : data.checkin.checkedIn;
        int rate = data.checkin == null
                ? 0 : Math.round((float) data.checkin.checkinRate * 100f);
        long revenue = data.finance == null ? 0L : Math.round(data.finance.verifiedAmount);

        List<OrgDashboardUiModel.Task> tasks = new ArrayList<>();
        if (data.tasks != null) {
            for (DashboardDtos.Task task : data.tasks) {
                if (task != null) {
                    tasks.add(new OrgDashboardUiModel.Task(task.title, task.detail));
                }
            }
        }
        return new OrgDashboardUiModel(onSale, pending, sold, confirmed, checkedIn, rate,
                revenue, tasks);
    }

    private static long countOf(DashboardDtos.EventStatusBlock block, String key) {
        if (block == null || block.counts == null) {
            return 0;
        }
        Long value = block.counts.get(key);
        return value == null ? 0 : value;
    }
}
