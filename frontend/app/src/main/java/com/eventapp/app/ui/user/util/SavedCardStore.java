package com.eventapp.app.ui.user.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class SavedCardStore {

    private static final String PREF_NAME = "user_saved_cards_v2";
    private static final String KEY_CARDS_JSON = "cards_json";
    private static final Gson GSON = new Gson();

    public static class SavedCard {
        public String brand;
        public String last4;
        public String expiry;
        public String displayLabel;

        public SavedCard() {}

        public SavedCard(String brand, String last4, String expiry) {
            this.brand = brand;
            this.last4 = last4;
            this.expiry = expiry;
            this.displayLabel = "💳 " + brand + "  •••• •••• •••• " + last4 + "  (" + expiry + ")";
        }
    }

    private SavedCardStore() {}

    public static List<SavedCard> getCards(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CARDS_JSON, null);
        List<SavedCard> list = null;
        if (json != null && !json.isEmpty()) {
            try {
                Type type = new TypeToken<List<SavedCard>>() {}.getType();
                list = GSON.fromJson(json, type);
            } catch (Exception ignored) {
            }
        }
        if (list == null || list.isEmpty()) {
            list = new ArrayList<>();
            list.add(new SavedCard("Visa Debit", "4242", "12/28"));
            list.add(new SavedCard("Mastercard", "8888", "10/27"));
        }
        return list;
    }

    public static SavedCard addCard(Context context, String rawCardNumber, String expiry) {
        String digits = rawCardNumber.replaceAll("\\s+", "");
        String last4 = digits.substring(digits.length() - 4);
        String brand;
        if (digits.startsWith("4")) {
            brand = "Visa";
        } else if (digits.startsWith("5") || digits.startsWith("2")) {
            brand = "Mastercard";
        } else if (digits.startsWith("35")) {
            brand = "JCB";
        } else {
            brand = "Thẻ Quốc Tế";
        }

        SavedCard newCard = new SavedCard(brand, last4, expiry);
        List<SavedCard> current = getCards(context);
        for (int i = 0; i < current.size(); i++) {
            SavedCard c = current.get(i);
            if (last4.equals(c.last4) && expiry.equals(c.expiry)) {
                current.remove(i);
                break;
            }
        }
        current.add(0, newCard);
        if (current.size() > 5) {
            current = current.subList(0, 5);
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CARDS_JSON, GSON.toJson(current)).apply();
        return newCard;
    }

    public static String validateCard(String rawCardNumber, String expiry, String cvv) {
        String digits = rawCardNumber == null ? "" : rawCardNumber.replaceAll("\\s+", "");
        if (!digits.matches("\\d{15,19}")) {
            return "Số thẻ không hợp lệ (cần từ 15 - 19 chữ số)!";
        }
        if (!passesLuhn(digits)) {
            return "Số thẻ không đúng định dạng kiểm tra (Luhn)!";
        }
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            return "Hạn dùng không hợp lệ! Nhập dạng MM/YY (ví dụ: 08/28).";
        }
        String[] parts = expiry.split("/");
        int expMonth = Integer.parseInt(parts[0]);
        int expYear = 2000 + Integer.parseInt(parts[1]);
        Calendar now = Calendar.getInstance();
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH) + 1;
        if (expYear < curYear || (expYear == curYear && expMonth < curMonth)) {
            return "Thẻ đã hết hạn sử dụng!";
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            return "Mã CVV không hợp lệ (cần 3 hoặc 4 chữ số)!";
        }
        return null;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}