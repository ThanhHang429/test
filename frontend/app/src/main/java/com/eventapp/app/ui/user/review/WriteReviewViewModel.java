package com.eventapp.app.ui.user.review;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/** Submission state for FR-U20 write/edit review. */
public class WriteReviewViewModel extends ViewModel {

    public static class Result {
        public final boolean success;
        public final boolean held;

        Result(boolean success, boolean held) {
            this.success = success;
            this.held = held;
        }
    }

    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<Result> getResult() {
        return result;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void submit(float rating, String comment, boolean editMode) {
        if (Boolean.TRUE.equals(submitting.getValue())) {
            return;
        }
        error.setValue(null);
        result.setValue(null);
        submitting.setValue(true);
        handler.postDelayed(() -> {
            submitting.setValue(false);
            if (rating < 1f) {
                error.setValue("Vui lòng chọn số sao");
                return;
            }
            String normalized = comment == null ? "" : comment.trim();
            if (!normalized.isEmpty() && normalized.length() < 10) {
                error.setValue("Nhận xét phải có ít nhất 10 ký tự");
                return;
            }
            if (normalized.length() > 1000) {
                error.setValue("Nhận xét tối đa 1000 ký tự");
                return;
            }
            boolean held = containsLinkOrPhone(normalized);
            result.setValue(new Result(true, held));
        }, 650L);
    }

    /** Mirrors the backend moderation filter for the dummy flow. */
    private boolean containsLinkOrPhone(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        if (lower.contains("http://") || lower.contains("https://")
                || lower.contains("www.") || lower.contains("zalo")) {
            return true;
        }
        return text.matches(".*(0\\d{9}|\\+84\\d{9}).*");
    }

    public void resetResult() {
        result.setValue(null);
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
