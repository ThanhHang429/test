package com.eventapp.app.ui.organizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityOrgCrowdBinding;
import com.eventapp.app.databinding.DialogOrgRoomBinding;
import com.eventapp.app.databinding.ItemOrgNotificationBinding;
import com.eventapp.app.databinding.ItemOrgOverflowRoomBinding;
import com.eventapp.app.ui.model.CrowdStatusUiModel;
import com.eventapp.app.ui.model.NotificationUiModel;
import com.eventapp.app.ui.model.OverflowRoomUiModel;
import com.eventapp.app.ui.organizer.model.OrgTone;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.eventapp.app.ui.organizer.viewmodel.OrgCrowdViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Điều phối phòng (FR-O28).
 *
 * <p>Màn hình chỉ làm một việc: đọc mức lấp đầy phòng chính, cảnh báo organizer khi đông, và dựa
 * trên phòng dự phòng đã thêm để gửi thông báo cho người đã đăng ký nhưng chưa check-in.</p>
 */
public class OrgCrowdActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";
    private static final String EXTRA_EVENT_TITLE = "event_title";

    private static final String AUDIENCE_NOT_CHECKED_IN = "REGISTERED_NOT_CHECKED_IN";

    private ActivityOrgCrowdBinding binding;
    private OrgCrowdViewModel viewModel;
    private long eventId;
    private boolean composedByAi;

    public static void start(@NonNull Context context, long eventId, @Nullable String eventTitle) {
        Intent intent = new Intent(context, OrgCrowdActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_EVENT_TITLE, eventTitle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgCrowdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);

        viewModel = new ViewModelProvider(this).get(OrgCrowdViewModel.class);
        viewModel.init(eventId, getIntent().getStringExtra(EXTRA_EVENT_TITLE));
        observe();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvRecipients.setText(R.string.org_crowd_audience);
        binding.btnAddRoom.setOnClickListener(v -> openRoomDialog());
        binding.btnComposeAi.setOnClickListener(v -> composeWithAi());
        binding.btnSend.setOnClickListener(v -> send());

        if (eventId <= 0) {
            snack(getString(R.string.org_event_not_found));
            return;
        }
        viewModel.load();
    }

    // ------------------------------------------------------------------ đọc dữ liệu

    private void observe() {
        viewModel.getEventTitle().observe(this, title -> {
            if (binding != null) {
                binding.tvEventName.setText(title == null ? "" : title);
            }
        });
        viewModel.getStatus().observe(this, data -> {
            if (data != null && binding != null) {
                bindStatus(data);
            }
        });
        viewModel.getRooms().observe(this, data -> {
            if (binding != null) {
                bindRooms(data == null ? new ArrayList<>() : data);
            }
        });
        viewModel.getHistory().observe(this, data -> {
            if (binding != null) {
                bindHistory(data == null ? new ArrayList<>() : data);
            }
        });
        viewModel.getLoading().observe(this, loading -> updateProgress());
        viewModel.getSending().observe(this, sending -> {
            updateProgress();
            if (binding == null) {
                return;
            }
            if (Boolean.TRUE.equals(sending)) {
                binding.btnSend.setEnabled(false);
                binding.btnSend.setAlpha(0.5f);
            } else {
                applySendState(viewModel.getStatus().getValue());
            }
        });
        viewModel.getError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            snack(error.getUserMessage());
        });
        viewModel.getRoomAdded().observe(this, added -> {
            if (!Boolean.TRUE.equals(added)) {
                return;
            }
            viewModel.consumeRoomAdded();
            snack(getString(R.string.org_crowd_room_added));
        });
        viewModel.getRoomClosed().observe(this, closed -> {
            if (!Boolean.TRUE.equals(closed)) {
                return;
            }
            viewModel.consumeRoomClosed();
            snack(getString(R.string.org_crowd_room_closed_toast));
        });
        viewModel.getAiDraft().observe(this, draft -> {
            if (draft == null || binding == null) {
                return;
            }
            viewModel.consumeAiDraft();
            String target = draft.roomName == null
                    ? viewModel.getEventTitle().getValue() : draft.roomName;
            binding.etTitle.setText(getString(R.string.org_crowd_title_fmt,
                    target == null ? "" : target));
            binding.etBody.setText(draft.text);
            composedByAi = true;
            snack(getString(R.string.org_crowd_ai_filled));
        });
        viewModel.getAiFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) {
                return;
            }
            viewModel.consumeAiFailed();
            snack(getString(R.string.org_crowd_ai_failed));
        });
        viewModel.getSentCount().observe(this, count -> {
            if (count == null) {
                return;
            }
            viewModel.consumeSentCount();
            snack(getString(R.string.org_crowd_sent, count));
            if (binding != null) {
                binding.etTitle.setText("");
                binding.etBody.setText("");
            }
            composedByAi = false;
        });
    }

    private void updateProgress() {
        if (binding == null) {
            return;
        }
        boolean busy = Boolean.TRUE.equals(viewModel.getLoading().getValue())
                || Boolean.TRUE.equals(viewModel.getSending().getValue());
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------ hiển thị

    private void bindStatus(CrowdStatusUiModel data) {
        int percent = data.occupancyPercent;
        binding.tvPercent.setText(percent + "%");
        binding.progressOccupancy.setProgressCompat(Math.min(percent, 100), true);

        binding.tvLevel.setText(levelLabel(data.level));
        OrgUi.applyPill(binding.tvLevel, levelTone(data.level));

        binding.tvCapacity.setText(getString(R.string.org_crowd_capacity_fmt,
                OrgFormat.count(data.capacityLimit), OrgFormat.count(data.checkedIn)));
        binding.tvRegistered.setText(OrgFormat.count(data.registered));
        binding.tvCheckedIn.setText(OrgFormat.count(data.checkedIn));
        binding.tvNotCheckedIn.setText(OrgFormat.count(data.notCheckedIn));

        boolean warn = !data.warning.trim().isEmpty();
        binding.cardWarning.setVisibility(warn ? View.VISIBLE : View.GONE);
        if (warn) {
            binding.tvWarning.setText(data.warning);
            String suggestion = data.suggestion;
            if (suggestion.trim().isEmpty()) {
                suggestion = data.suggestedRoom == null ? "" : data.suggestedRoom.roomName;
            }
            binding.tvSuggestion.setText(suggestion);
            binding.tvSuggestion.setVisibility(suggestion.isEmpty() ? View.GONE : View.VISIBLE);
        }

        applySendState(data);
        binding.tvRecipients.setText(data.notCheckedIn > 0
                ? getString(R.string.org_crowd_recipients, data.notCheckedIn)
                : getString(R.string.org_crowd_recipients_empty));
    }

    private void applySendState(@Nullable CrowdStatusUiModel data) {
        if (binding == null) {
            return;
        }
        boolean hasRecipients = data != null && data.notCheckedIn > 0;
        binding.btnSend.setEnabled(hasRecipients);
        binding.btnSend.setAlpha(hasRecipients ? 1f : 0.5f);
    }

    private void bindRooms(List<OverflowRoomUiModel> rooms) {
        binding.roomList.removeAllViews();
        binding.tvRoomsEmpty.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < rooms.size(); i++) {
            OverflowRoomUiModel room = rooms.get(i);
            ItemOrgOverflowRoomBinding row = ItemOrgOverflowRoomBinding.inflate(
                    inflater, binding.roomList, false);
            row.tvRoomName.setText(room.roomName);
            row.tvRoomMeta.setText(getString(R.string.org_crowd_room_capacity,
                    OrgFormat.count(room.capacity)));

            boolean note = !room.note.trim().isEmpty();
            row.tvRoomNote.setText(room.note);
            row.tvRoomNote.setVisibility(note ? View.VISIBLE : View.GONE);

            boolean active = room.active;
            row.tvRoomStatus.setText(active
                    ? R.string.org_crowd_room_active : R.string.org_crowd_room_closed);
            OrgUi.applyPill(row.tvRoomStatus, active ? OrgTone.SUCCESS : OrgTone.NEUTRAL);
            row.btnCloseRoom.setVisibility(active ? View.VISIBLE : View.GONE);
            row.btnCloseRoom.setOnClickListener(v -> confirmCloseRoom(room));
            row.divider.setVisibility(i == rooms.size() - 1 ? View.GONE : View.VISIBLE);
            binding.roomList.addView(row.getRoot());
        }
    }

    private void bindHistory(List<NotificationUiModel> items) {
        binding.historyList.removeAllViews();
        if (items.isEmpty()) {
            binding.historyList.addView(sectionNote(getString(R.string.org_crowd_history_empty)));
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        for (NotificationUiModel item : items) {
            ItemOrgNotificationBinding row = ItemOrgNotificationBinding.inflate(
                    inflater, binding.historyList, false);
            row.tvNotifTitle.setText(item.title);
            row.tvNotifBody.setText(item.body);
            row.tvNotifTime.setText(OrgFormat.dateTime(item.sentAtMillis));
            row.tvNotifMeta.setText(getString(R.string.org_crowd_history_meta,
                    audienceLabel(item.audience), item.recipientCount));
            binding.historyList.addView(row.getRoot());
        }
    }

    private View sectionNote(String text) {
        android.widget.TextView view = new android.widget.TextView(this);
        view.setText(text);
        view.setTextColor(getColor(R.color.org_text_dim));
        view.setTextSize(12f);
        int pad = Math.round(4 * getResources().getDisplayMetrics().density);
        view.setPadding(pad, pad, pad, pad);
        return view;
    }

    // ------------------------------------------------------------------ phòng dự phòng

    private void openRoomDialog() {
        DialogOrgRoomBinding dialog = DialogOrgRoomBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.org_crowd_add_room)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.org_close, null)
                .setPositiveButton(R.string.org_crowd_room_save, (d, which) -> {
                    String name = text(dialog.etRoomName.getText());
                    if (name.isEmpty()) {
                        snack(getString(R.string.org_crowd_room_name));
                        return;
                    }
                    int capacity = parseCapacity(text(dialog.etCapacity.getText()));
                    if (capacity <= 0) {
                        snack(getString(R.string.org_crowd_room_capacity_label));
                        return;
                    }
                    addRoom(name, capacity, text(dialog.etNote.getText()));
                })
                .show();
    }

    private void addRoom(String name, int capacity, String note) {
        viewModel.addRoom(name, capacity, note);
    }

    private void confirmCloseRoom(OverflowRoomUiModel room) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.org_crowd_room_close)
                .setMessage(getString(R.string.org_crowd_room_close_confirm, room.roomName))
                .setNegativeButton(R.string.org_close, null)
                .setPositiveButton(R.string.org_crowd_room_close, (d, which) -> closeRoom(room.id))
                .show();
    }

    private void closeRoom(long roomId) {
        viewModel.closeRoom(roomId);
    }

    // ------------------------------------------------------------------ thông báo

    private void composeWithAi() {
        CrowdStatusUiModel status = viewModel.getStatus().getValue();
        String capacityLabel = status == null || status.suggestedRoom == null ? null
                : getString(R.string.org_crowd_room_capacity,
                OrgFormat.count(status.suggestedRoom.capacity));
        viewModel.composeWithAi(
                getString(R.string.org_crowd_title),
                getString(R.string.org_crowd_audience),
                getString(R.string.org_crowd_occupancy),
                capacityLabel);
    }

    private void send() {
        String title = text(binding.etTitle.getText());
        String body = text(binding.etBody.getText());
        if (title.isEmpty()) {
            snack(getString(R.string.org_crowd_title_required));
            return;
        }
        if (body.isEmpty()) {
            snack(getString(R.string.org_crowd_body_required));
            return;
        }
        viewModel.send(title, body, composedByAi);
    }

    // ------------------------------------------------------------------ tiện ích

    private String levelLabel(CrowdStatusUiModel.Level level) {
        switch (level) {
            case FULL:
                return getString(R.string.org_crowd_level_full);
            case CROWDED:
                return getString(R.string.org_crowd_level_crowded);
            case WATCH:
                return getString(R.string.org_crowd_level_watch);
            default:
                return getString(R.string.org_crowd_level_normal);
        }
    }

    private int levelTone(CrowdStatusUiModel.Level level) {
        switch (level) {
            case FULL:
                return OrgTone.DANGER;
            case CROWDED:
                return OrgTone.WARNING;
            case WATCH:
                return OrgTone.INFO;
            default:
                return OrgTone.SUCCESS;
        }
    }

    private String audienceLabel(String audience) {
        return AUDIENCE_NOT_CHECKED_IN.equals(audience)
                ? getString(R.string.org_crowd_audience) : audience;
    }

    private int parseCapacity(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 && value <= 100_000 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
