package com.eventapp.app.ui.organizer.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.organizerai.OrganizerAiDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FR-O06 — AI viết bản nháp mô tả qua {@code POST /organizer/ai/event-description}.
 *
 * <p>AI chỉ sinh văn bản, không lưu vào sự kiện (O06-R01). Kết quả có thể kèm {@code warnings} do
 * server hậu kiểm (số tiền/ngày/SĐT/link không có trong dữ liệu nhập).</p>
 */
public class AiDescriptionViewModel extends ViewModel {

    public static final int MAX_PER_HOUR = 10;

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<AiDescriptionUiModel.Result> result = new MutableLiveData<>();
    private final MutableLiveData<AiDescriptionUiModel.Status> status =
            new MutableLiveData<>(AiDescriptionUiModel.Status.IDLE);
    private final MutableLiveData<Integer> selectedVariant = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> generation = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> remainingMinutes = new MutableLiveData<>(0);

    private AiDescriptionUiModel.Tone tone = AiDescriptionUiModel.Tone.PROFESSIONAL;
    private AiDescriptionUiModel.Length length = AiDescriptionUiModel.Length.MEDIUM;
    private int variantCount = 1;

    private long categoryId;
    private String title = "";
    private String format = "";
    private String audience = "";
    private List<String> keyPoints = new ArrayList<>();

    private boolean initialized;
    private final List<AiDescriptionUiModel.Result> undoStack = new ArrayList<>();

    public void init(long categoryId, String title, String format, String audience,
                     List<String> keyPoints) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.categoryId = categoryId;
        this.title = title == null ? "" : title;
        this.format = format == null ? "" : format;
        this.audience = audience == null ? "" : audience;
        this.keyPoints = keyPoints == null ? new ArrayList<>() : new ArrayList<>(keyPoints);
        generate();
    }

    public LiveData<AiDescriptionUiModel.Result> getResult() {
        return result;
    }

    public LiveData<AiDescriptionUiModel.Status> getStatus() {
        return status;
    }

    public LiveData<Integer> getSelectedVariant() {
        return selectedVariant;
    }

    /** Bumped on every response so the screen re-renders tabs even for identical payloads. */
    public LiveData<Integer> getGeneration() {
        return generation;
    }

    public void setTone(AiDescriptionUiModel.Tone value) {
        if (value == null || value == tone) {
            return;
        }
        tone = value;
    }

    public void setLength(AiDescriptionUiModel.Length value) {
        if (value == null || value == length) {
            return;
        }
        length = value;
    }

    public void setVariantCount(int count) {
        variantCount = Math.max(1, Math.min(3, count));
        AiDescriptionUiModel.Result current = result.getValue();
        if (current != null && selectedVariant.getValue() != null
                && selectedVariant.getValue() >= variantCount) {
            selectedVariant.setValue(variantCount - 1);
        }
    }

    public void selectVariant(int index) {
        AiDescriptionUiModel.Result current = result.getValue();
        if (current == null || current.variants.isEmpty()) {
            return;
        }
        int safe = Math.max(0, Math.min(index, current.variants.size() - 1));
        if (selectedVariant.getValue() != null && selectedVariant.getValue() == safe) {
            return;
        }
        selectedVariant.setValue(safe);
    }

    public boolean isRequesting() {
        return status.getValue() == AiDescriptionUiModel.Status.REQUESTING;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Gửi yêu cầu sinh mô tả; dữ liệu thiếu được chặn ở client trước khi gọi API. */
    public void generate() {
        if (isRequesting()) {
            return;
        }
        if (title.trim().length() < 5) {
            status.setValue(AiDescriptionUiModel.Status.BLOCKED);
            return;
        }
        if (categoryId <= 0L || keyPoints.size() < 3) {
            status.setValue(AiDescriptionUiModel.Status.BLOCKED);
            return;
        }

        OrganizerAiDtos.DescriptionRequest request = new OrganizerAiDtos.DescriptionRequest();
        request.title = title.trim();
        request.categoryId = categoryId;
        request.format = format.isEmpty() ? "OFFLINE" : format;
        request.audience = audience.trim();
        request.keyPoints = new ArrayList<>(keyPoints);
        request.tone = tone.name();
        request.length = length.name();
        request.variants = variantCount;

        status.setValue(AiDescriptionUiModel.Status.REQUESTING);
        repository.describe(request,
                new ApiCallback<OrganizerAiDtos.DescriptionResponse>() {
                    @Override
                    public void onSuccess(@Nullable OrganizerAiDtos.DescriptionResponse data,
                                          @Nullable JsonElement meta) {
                        if (data == null || data.descriptions == null
                                || data.descriptions.isEmpty()) {
                            status.setValue(AiDescriptionUiModel.Status.ERROR);
                            return;
                        }
                        AiDescriptionUiModel.Result previous = result.getValue();
                        if (previous != null) {
                            undoStack.add(previous);
                            if (undoStack.size() > 3) {
                                undoStack.remove(0);
                            }
                        }
                        result.setValue(map(data));
                        selectedVariant.setValue(0);
                        generation.setValue(
                                (generation.getValue() == null ? 0 : generation.getValue()) + 1);
                        status.setValue(AiDescriptionUiModel.Status.SUCCESS);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        if (exception.getHttpCode() == 429) {
                            remainingMinutes.setValue(60);
                            status.setValue(AiDescriptionUiModel.Status.QUOTA);
                        } else {
                            status.setValue(AiDescriptionUiModel.Status.ERROR);
                        }
                    }
                });
    }

    @NonNull
    private AiDescriptionUiModel.Result map(@NonNull OrganizerAiDtos.DescriptionResponse dto) {
        List<AiDescriptionUiModel.Variant> variants = new ArrayList<>();
        for (OrganizerAiDtos.DescriptionVariant item : dto.descriptions) {
            if (item == null) {
                continue;
            }
            variants.add(new AiDescriptionUiModel.Variant(item.text, item.shortSummary));
        }
        List<AiDescriptionUiModel.Warning> warnings = new ArrayList<>();
        if (dto.warnings != null) {
            for (String warning : dto.warnings) {
                if (warning == null || warning.trim().isEmpty()) {
                    continue;
                }
                warnings.add(new AiDescriptionUiModel.Warning(warningKind(warning), warning));
            }
        }
        return new AiDescriptionUiModel.Result(
                dto.aiRequestId == null ? "" : String.valueOf(dto.aiRequestId),
                System.currentTimeMillis(),
                variants,
                dto.missingInfo,
                warnings);
    }

    @NonNull
    private static AiDescriptionUiModel.WarningKind warningKind(@NonNull String warning) {
        String lower = warning.toLowerCase(Locale.ROOT);
        if (lower.contains("http") || lower.contains("link") || lower.contains("www")) {
            return AiDescriptionUiModel.WarningKind.LINK;
        }
        if (lower.contains("điện thoại") || lower.contains("sđt")) {
            return AiDescriptionUiModel.WarningKind.PHONE;
        }
        if (lower.contains("ngày") || lower.contains("tháng")) {
            return AiDescriptionUiModel.WarningKind.DATE;
        }
        return AiDescriptionUiModel.WarningKind.NUMBER;
    }

    /** Huỷ yêu cầu đang chạy và giữ nội dung đang hiển thị. */
    public void cancel() {
        if (!isRequesting()) {
            return;
        }
        status.setValue(result.getValue() == null
                ? AiDescriptionUiModel.Status.IDLE
                : AiDescriptionUiModel.Status.SUCCESS);
    }

    public void retry() {
        if (status.getValue() == AiDescriptionUiModel.Status.QUOTA) {
            return;
        }
        generate();
    }

    /** Restores the previous generation; returns false when there is nothing to undo. */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        AiDescriptionUiModel.Result restored = undoStack.remove(undoStack.size() - 1);
        result.setValue(restored);
        selectedVariant.setValue(0);
        generation.setValue((generation.getValue() == null ? 0 : generation.getValue()) + 1);
        status.setValue(AiDescriptionUiModel.Status.SUCCESS);
        return true;
    }

    public int getQuotaWaitMinutes() {
        return remainingMinutes.getValue() == null ? 60 : remainingMinutes.getValue();
    }

    public int getQuotaUsed() {
        return 0;
    }
}
