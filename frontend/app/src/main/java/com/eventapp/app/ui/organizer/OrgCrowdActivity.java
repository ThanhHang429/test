package com.eventapp.app.ui.organizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.notification.NotificationResponse;
import com.eventapp.app.data.remote.dto.organizer.CrowdDtos;
import com.eventapp.app.data.remote.dto.organizerai.OrganizerAiDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.databinding.ActivityOrgCrowdBinding;
import com.eventapp.app.databinding.DialogOrgRoomBinding;
import com.eventapp.app.databinding.ItemOrgNotificationBinding;
import com.eventapp.app.databinding.ItemOrgOverflowRoomBinding;
import com.eventapp.app.ui.organizer.model.OrgTone;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
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
    private static final String SOURCE_AI = "AI";
    private static final String SOURCE_MANUAL = "MANUAL";

    private ActivityOrgCrowdBinding binding;
    private OrganizerRepository repository;
    private long eventId;
    private String eventTitle = "";
    private CrowdDtos.CrowdStatus status;
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

        repository = ServiceLocator.get(OrganizerRepository.class);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvEventName.setText(eventTitle == null ? "" : eventTitle);
        binding.tvRecipients.setText(R.string.org_crowd_audience);
        binding.btnAddRoom.setOnClickListener(v -> openRoomDialog());
        binding.btnComposeAi.setOnClickListener(v -> composeWithAi());
        binding.btnSend.setOnClickListener(v -> send());

        if (eventId <= 0) {
            snack(getString(R.string.org_event_not_found));
            return;
        }
        loadStatus();
        loadRooms();
        loadHistory();
    }

    // ------------------------------------------------------------------ đọc dữ liệu

    private void loadStatus() {
        binding.progress.setVisibility(View.VISIBLE);
        repository.crowdStatus(eventId, new ApiCallback<CrowdDtos.CrowdStatus>() {
            @Override
            public void onSuccess(@Nullable CrowdDtos.CrowdStatus data,
                                  @Nullable com.google.gson.JsonElement meta) {
                binding.progress.setVisibility(View.GONE);
                if (binding == null || data == null) {
                    return;
                }
                status = data;
                if (eventTitle == null || eventTitle.isEmpty()) {
                    eventTitle = data.eventTitle;
                    binding.tvEventName.setText(eventTitle);
                }
                bindStatus(data);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                binding.progress.setVisibility(View.GONE);
                snack(error.getUserMessage());
            }
        });
    }

    private void loadRooms() {
        repository.overflowRooms(eventId, new ApiCallback<List<CrowdDtos.OverflowRoom>>() {
            @Override
            public void onSuccess(@Nullable List<CrowdDtos.OverflowRoom> data,
                                  @Nullable com.google.gson.JsonElement meta) {
                if (binding == null) {
                    return;
                }
                bindRooms(data == null ? new ArrayList<>() : data);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (binding != null) {
                    snack(error.getUserMessage());
                }
            }
        });
    }

    private void loadHistory() {
        repository.notifications(eventId, new ApiCallback<List<NotificationResponse>>() {
            @Override
            public void onSuccess(@Nullable List<NotificationResponse> data,
                                  @Nullable com.google.gson.JsonElement meta) {
                if (binding == null) {
                    return;
                }
                bindHistory(data == null ? new ArrayList<>() : data);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (binding != null) {
                    snack(error.getUserMessage());
                }
            }
        });
    }

    // ------------------------------------------------------------------ hiển thị

    private void bindStatus(CrowdDtos.CrowdStatus data) {
        int percent = (int) Math.round(data.occupancyRate * 100);
        binding.tvPercent.setText(percent + "%");
        binding.progressOccupancy.setProgressCompat(Math.min(percent, 100), true);

        binding.tvLevel.setText(levelLabel(data.level));
        OrgUi.applyPill(binding.tvLevel, levelTone(data.level));

        binding.tvCapacity.setText(getString(R.string.org_crowd_capacity_fmt,
                OrgFormat.count(data.capacityLimit), OrgFormat.count(data.checkedIn)));
        binding.tvRegistered.setText(OrgFormat.count(data.registered));
        binding.tvCheckedIn.setText(OrgFormat.count(data.checkedIn));
        binding.tvNotCheckedIn.setText(OrgFormat.count(data.notCheckedIn));

        boolean warn = data.warning != null && !data.warning.trim().isEmpty();
        binding.cardWarning.setVisibility(warn ? View.VISIBLE : View.GONE);
        if (warn) {
            binding.tvWarning.setText(data.warning);
            String suggestion = data.suggestion;
            if (suggestion == null || suggestion.trim().isEmpty()) {
                suggestion = data.suggestedRoom == null ? "" : data.suggestedRoom.roomName;
            }
            binding.tvSuggestion.setText(suggestion);
            binding.tvSuggestion.setVisibility(suggestion.isEmpty() ? View.GONE : View.VISIBLE);
        }

        boolean hasRecipients = data.notCheckedIn > 0;
        binding.btnSend.setEnabled(hasRecipients);
        binding.btnSend.setAlpha(hasRecipients ? 1f : 0.5f);
        binding.tvRecipients.setText(hasRecipients
                ? getString(R.string.org_crowd_recipients, data.notCheckedIn)
                : getString(R.string.org_crowd_recipients_empty));
    }

    private void bindRooms(List<CrowdDtos.OverflowRoom> rooms) {
        binding.roomList.removeAllViews();
        binding.tvRoomsEmpty.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < rooms.size(); i++) {
            CrowdDtos.OverflowRoom room = rooms.get(i);
            ItemOrgOverflowRoomBinding row = ItemOrgOverflowRoomBinding.inflate(
                    inflater, binding.roomList, false);
            row.tvRoomName.setText(room.roomName);
            row.tvRoomMeta.setText(getString(R.string.org_crowd_room_capacity,
                    OrgFormat.count(room.capacity)));

            boolean note = room.note != null && !room.note.trim().isEmpty();
            row.tvRoomNote.setText(room.note);
            row.tvRoomNote.setVisibility(note ? View.VISIBLE : View.GONE);

            boolean active = "ACTIVE".equalsIgnoreCase(room.status);
            row.tvRoomStatus.setText(active
                    ? R.string.org_crowd_room_active : R.string.org_crowd_room_closed);
            OrgUi.applyPill(row.tvRoomStatus, active ? OrgTone.SUCCESS : OrgTone.NEUTRAL);
            row.btnCloseRoom.setVisibility(active ? View.VISIBLE : View.GONE);
            row.btnCloseRoom.setOnClickListener(v -> confirmCloseRoom(room));
            row.divider.setVisibility(i == rooms.size() - 1 ? View.GONE : View.VISIBLE);
            binding.roomList.addView(row.getRoot());
        }
    }

    private void bindHistory(List<NotificationResponse> items) {
        binding.historyList.removeAllViews();
        if (items.isEmpty()) {
            binding.historyList.addView(sectionNote(getString(R.string.org_crowd_history_empty)));
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        for (NotificationResponse item : items) {
            ItemOrgNotificationBinding row = ItemOrgNotificationBinding.inflate(
                    inflater, binding.historyList, false);
            row.tvNotifTitle.setText(item.title);
            row.tvNotifBody.setText(item.body);
            row.tvNotifTime.setText(OrgFormat.dateTime(DateTimeUtils.toMillis(item.sentAt)));
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
        binding.progress.setVisibility(View.VISIBLE);
        CrowdDtos.CreateRoomRequest request = new CrowdDtos.CreateRoomRequest(
                name, capacity, note.isEmpty() ? null : note);
        repository.addOverflowRoom(eventId, request,
                new ApiCallback<CrowdDtos.OverflowRoom>() {
                    @Override
                    public void onSuccess(@Nullable CrowdDtos.OverflowRoom data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        binding.progress.setVisibility(View.GONE);
                        if (binding == null) {
                            return;
                        }
                        snack(getString(R.string.org_crowd_room_added));
                        loadRooms();
                        loadStatus();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        binding.progress.setVisibility(View.GONE);
                        snack(error.getUserMessage());
                    }
                });
    }

    private void confirmCloseRoom(CrowdDtos.OverflowRoom room) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.org_crowd_room_close)
                .setMessage(getString(R.string.org_crowd_room_close_confirm, room.roomName))
                .setNegativeButton(R.string.org_close, null)
                .setPositiveButton(R.string.org_crowd_room_close, (d, which) -> closeRoom(room.id))
                .show();
    }

    private void closeRoom(long roomId) {
        repository.closeOverflowRoom(eventId, roomId, new ApiCallback<CrowdDtos.OverflowRoom>() {
            @Override
            public void onSuccess(@Nullable CrowdDtos.OverflowRoom data,
                                  @Nullable com.google.gson.JsonElement meta) {
                if (binding == null) {
                    return;
                }
                snack(getString(R.string.org_crowd_room_closed_toast));
                loadRooms();
                loadStatus();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (binding != null) {
                    snack(error.getUserMessage());
                }
            }
        });
    }

    // ------------------------------------------------------------------ thông báo

    private void composeWithAi() {
        binding.progress.setVisibility(View.VISIBLE);
        OrganizerAiDtos.AnnouncementRequest request = new OrganizerAiDtos.AnnouncementRequest();
        request.eventId = eventId;
        request.eventTitle = eventTitle;
        request.topic = getString(R.string.org_crowd_title);
        request.audience = getString(R.string.org_crowd_audience);
        request.tone = "friendly";
        request.length = "short";
        request.variants = 1;
        request.keyPoints.add(status == null || status.warning == null
                ? getString(R.string.org_crowd_occupancy) : status.warning);
        CrowdDtos.OverflowRoom room = status == null ? null : status.suggestedRoom;
        if (room != null) {
            request.keyPoints.add(room.roomName + " · "
                    + getString(R.string.org_crowd_room_capacity, OrgFormat.count(room.capacity)));
        }

        repository.announce(request, new ApiCallback<OrganizerAiDtos.AnnouncementResponse>() {
            @Override
            public void onSuccess(@Nullable OrganizerAiDtos.AnnouncementResponse data,
                                  @Nullable com.google.gson.JsonElement meta) {
                binding.progress.setVisibility(View.GONE);
                if (binding == null || data == null || data.variants.isEmpty()) {
                    snack(getString(R.string.org_crowd_ai_failed));
                    return;
                }
                String text = data.variants.get(0).text;
                if (text == null || text.trim().isEmpty()) {
                    snack(getString(R.string.org_crowd_ai_failed));
                    return;
                }
                binding.etTitle.setText(getString(R.string.org_crowd_title_fmt,
                        room == null ? eventTitle : room.roomName));
                binding.etBody.setText(text.trim());
                composedByAi = true;
                snack(getString(R.string.org_crowd_ai_filled));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                binding.progress.setVisibility(View.GONE);
                snack(getString(R.string.org_crowd_ai_failed));
            }
        });
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
        binding.progress.setVisibility(View.VISIBLE);
        binding.btnSend.setEnabled(false);

        CrowdDtos.SendNotificationRequest request = new CrowdDtos.SendNotificationRequest(
                title, body, AUDIENCE_NOT_CHECKED_IN, composedByAi ? SOURCE_AI : SOURCE_MANUAL);
        repository.sendNotification(eventId, request,
                new ApiCallback<CrowdDtos.SendNotificationResponse>() {
                    @Override
                    public void onSuccess(@Nullable CrowdDtos.SendNotificationResponse data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        binding.progress.setVisibility(View.GONE);
                        if (binding == null) {
                            return;
                        }
                        int count = data == null ? 0 : data.recipientCount;
                        snack(getString(R.string.org_crowd_sent, count));
                        binding.etTitle.setText("");
                        binding.etBody.setText("");
                        composedByAi = false;
                        loadHistory();
                        loadStatus();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        binding.progress.setVisibility(View.GONE);
                        binding.btnSend.setEnabled(true);
                        snack(error.getUserMessage());
                    }
                });
    }

    // ------------------------------------------------------------------ tiện ích

    private String levelLabel(String level) {
        if (level == null) {
            return getString(R.string.org_crowd_level_normal);
        }
        switch (level) {
            case "FULL":
                return getString(R.string.org_crowd_level_full);
            case "CROWDED":
                return getString(R.string.org_crowd_level_crowded);
            case "WATCH":
                return getString(R.string.org_crowd_level_watch);
            default:
                return getString(R.string.org_crowd_level_normal);
        }
    }

    private int levelTone(String level) {
        if (level == null) {
            return OrgTone.SUCCESS;
        }
        switch (level) {
            case "FULL":
                return OrgTone.DANGER;
            case "CROWDED":
                return OrgTone.WARNING;
            case "WATCH":
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
