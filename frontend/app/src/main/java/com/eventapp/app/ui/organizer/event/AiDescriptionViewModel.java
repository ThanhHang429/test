package com.eventapp.app.ui.organizer.event;


import com.eventapp.app.ui.fake.SampleAiDescriptionData;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-O06 state holder: tone, length, variant count, request lifecycle and the undo stack.
 *
 * <p>The assistant only produces text — this view model never mutates event data (O06-R01) and
 * never keeps the input or output beyond the screen (O06-R07). Quota mirrors the documented
 * limit of 10 requests per organizer per hour; exceeding it surfaces a wait time instead of
 * another call.</p>
 */
public class AiDescriptionViewModel extends ViewModel {

    private static final long GENERATE_DELAY_MS = 1600L;
    private static final long TIMEOUT_MS = 25_000L;
    private static final long HOUR_MS = 60L * 60L * 1000L;
    public static final int MAX_PER_HOUR = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
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
    private int requestsThisHour;
    private long hourWindowStart;
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
        this.keyPoints = keyPoints == null ? new ArrayList<>(new ArrayList<>()) : keyPoints;
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

    /** Generates (or regenerates) variants; refused input and quota are handled client-side. */
    public void generate() {
        if (isRequesting()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (hourWindowStart == 0L || now - hourWindowStart > HOUR_MS) {
            hourWindowStart = now;
            requestsThisHour = 0;
        }
        if (requestsThisHour >= MAX_PER_HOUR) {
            remainingMinutes.setValue(minutesLeft(now));
            status.setValue(AiDescriptionUiModel.Status.QUOTA);
            return;
        }
        if (SampleAiDescriptionData.isBlocked(title, keyPoints)) {
            status.setValue(AiDescriptionUiModel.Status.BLOCKED);
            return;
        }
        requestsThisHour++;

        status.setValue(AiDescriptionUiModel.Status.REQUESTING);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            AiDescriptionUiModel.Result previous = result.getValue();
            if (previous != null) {
                undoStack.add(previous);
                if (undoStack.size() > 3) {
                    undoStack.remove(0);
                }
            }
            result.setValue(SampleAiDescriptionData.create(
                    tone, length, variantCount, categoryId, title, format, audience, keyPoints));
            selectedVariant.setValue(0);
            generation.setValue((generation.getValue() == null ? 0 : generation.getValue()) + 1);
            status.setValue(AiDescriptionUiModel.Status.SUCCESS);
        }, GENERATE_DELAY_MS);

        // 25s guard: a hung request must never leave the screen spinning.
        handler.postDelayed(() -> {
            if (status.getValue() == AiDescriptionUiModel.Status.REQUESTING) {
                status.setValue(AiDescriptionUiModel.Status.ERROR);
            }
        }, TIMEOUT_MS);
    }

    /** Aborts an in-flight request and keeps whatever the screen already showed. */
    public void cancel() {
        if (!isRequesting()) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
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

    /** Minutes left in the hourly quota window (for the 429-style message). */
    public int getQuotaWaitMinutes() {
        return remainingMinutes.getValue() == null ? 60 : remainingMinutes.getValue();
    }

    public int getQuotaUsed() {
        return requestsThisHour;
    }

    private int minutesLeft(long now) {
        long left = HOUR_MS - (now - hourWindowStart);
        return (int) Math.max(1L, (left + 59_999L) / 60_000L);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}