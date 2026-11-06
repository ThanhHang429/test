package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.api.RecommendationApi;
import com.eventapp.app.data.remote.dto.recommendation.RecommendationDtos;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gợi ý AI (FR-U07).
 *
 * <p>Trang chủ và màn "Dành cho bạn" cùng gọi `/recommendations`; repository giữ kết quả 60 giây
 * theo `limit` và gộp các lời gọi trùng, nên mở lại tab không phải chờ mô hình một lần nữa. Nút làm
 * mới và thao tác bỏ gợi ý luôn xoá cache.</p>
 */
public class AiRepository {

    private static final long TTL_MS = 60_000L;

    private final RecommendationApi api;
    private final Map<Integer, Entry> cache = new HashMap<>();
    private final Map<Integer, List<ApiCallback<RecommendationDtos.ListResult>>> waiting =
            new HashMap<>();

    public AiRepository(@NonNull RecommendationApi api) {
        this.api = api;
    }

    public void recommendations(int limit, boolean refresh,
                                @NonNull ApiCallback<RecommendationDtos.ListResult> callback) {
        callback.onStart();
        if (refresh) {
            invalidate();
        } else {
            RecommendationDtos.ListResult cached = cachedFor(limit);
            if (cached != null) {
                callback.onSuccess(cached, null);
                callback.onFinish();
                return;
            }
        }

        List<ApiCallback<RecommendationDtos.ListResult>> pending = waiting.get(limit);
        if (pending != null) {
            pending.add(callback);
            return;
        }
        pending = new ArrayList<>();
        pending.add(callback);
        waiting.put(limit, pending);

        ApiCallback.enqueue(api.forMe(limit, refresh),
                new ApiCallback<RecommendationDtos.ListResult>() {
                    @Override
                    public void onSuccess(RecommendationDtos.ListResult data, JsonElement meta) {
                        cache.put(limit, new Entry(data, System.currentTimeMillis()));
                        flush(limit, data, null);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        flush(limit, null, exception);
                    }
                });
    }

    public void preview(@NonNull RecommendationDtos.PreviewRequest request,
                        @NonNull ApiCallback<RecommendationDtos.ListResult> callback) {
        ApiCallback.enqueue(api.preview(request), callback);
    }

    public void dismiss(long eventId,
                        @NonNull ApiCallback<RecommendationDtos.DismissResult> callback) {
        invalidate();
        ApiCallback.enqueue(api.dismiss(eventId), callback);
    }

    /** Xoá cache, dùng khi sở thích thay đổi hoặc người dùng yêu cầu gợi ý mới. */
    public void invalidate() {
        cache.clear();
    }

    /**
     * Trả kết quả còn hạn có ít nhất [limit] phần tử (ưu tiên đúng limit), đã cắt về đúng số lượng
     * xin — nhờ vậy màn hình xin 6 gợi ý vẫn dùng lại được kết quả 8 gợi ý của màn khác.
     */
    @Nullable
    private RecommendationDtos.ListResult cachedFor(int limit) {
        long now = System.currentTimeMillis();
        RecommendationDtos.ListResult best = null;
        int bestSize = 0;
        for (Map.Entry<Integer, Entry> entry : new ArrayList<>(cache.entrySet())) {
            if (now - entry.getValue().at > TTL_MS) {
                cache.remove(entry.getKey());
                continue;
            }
            int size = entry.getValue().value.items == null ? 0 : entry.getValue().value.items.size();
            if (size >= limit && (best == null || Math.abs(size - limit) < Math.abs(bestSize - limit))) {
                best = entry.getValue().value;
                bestSize = size;
            }
        }
        if (best == null) {
            return null;
        }
        if (bestSize == limit) {
            return best;
        }
        RecommendationDtos.ListResult sliced = new RecommendationDtos.ListResult();
        sliced.items = new ArrayList<>(best.items.subList(0, limit));
        sliced.meta = best.meta;
        return sliced;
    }

    private void flush(int limit,
                       @Nullable RecommendationDtos.ListResult data,
                       @Nullable ApiException error) {
        List<ApiCallback<RecommendationDtos.ListResult>> pending = waiting.remove(limit);
        if (pending == null) {
            return;
        }
        for (ApiCallback<RecommendationDtos.ListResult> callback : pending) {
            if (error == null) {
                callback.onSuccess(data, null);
            } else {
                callback.onError(error);
            }
            callback.onFinish();
        }
    }

    private static final class Entry {
        final RecommendationDtos.ListResult value;
        final long at;

        Entry(RecommendationDtos.ListResult value, long at) {
            this.value = value;
            this.at = at;
        }
    }
}
