package com.eventapp.app.data.local.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-U04-R10: the last 10 search keywords stay on the device and are never sent to the server.
 * SharedPreferences is used for the UI slice; it can be swapped for the SQLite cache later
 * without changing the screen contract.
 */
public class SearchHistoryStore {

    private static final String PREFS = "eventapp_search_history";
    private static final String KEY = "queries";
    private static final int MAX_ITEMS = 10;

    private final SharedPreferences preferences;

    public SearchHistoryStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<String> getQueries() {
        List<String> result = new ArrayList<>();
        String raw = preferences.getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length() && result.size() < MAX_ITEMS; i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        } catch (JSONException ignored) {
            // Corrupt local history must never block search.
        }
        return result;
    }

    public void add(String query) {
        String value = query == null ? "" : query.trim();
        if (value.length() < 2) {
            return;
        }
        List<String> items = getQueries();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).equalsIgnoreCase(value)) {
                items.remove(i);
            }
        }
        items.add(0, value);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        save(items);
    }

    public void clear() {
        preferences.edit().remove(KEY).apply();
    }

    private void save(List<String> items) {
        JSONArray array = new JSONArray();
        for (String item : items) {
            array.put(item);
        }
        preferences.edit().putString(KEY, array.toString()).apply();
    }
}
