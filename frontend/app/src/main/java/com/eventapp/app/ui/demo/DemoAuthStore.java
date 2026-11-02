package com.eventapp.app.ui.demo;

import android.content.Context;
import android.content.SharedPreferences;

import com.eventapp.app.core.enums.Role;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kho tài khoản demo cục bộ.
 *
 * <p>Đây là lớp thay thế tạm thời cho session backend trong giai đoạn UI-only. Không có token, không
 * gọi mạng. Mật khẩu chỉ được lưu dưới dạng hash để tránh hiển thị plaintext trong bộ nhớ app.</p>
 */
public final class DemoAuthStore {

    public static final String DEMO_PASSWORD = "123456";
    public static final String DEMO_OTP = "123456";

    private static final String PREFS_NAME = "eticket_demo_auth";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String PASSWORD_SALT = "eticket-demo-v1";

    private static volatile DemoAuthStore instance;

    private final SharedPreferences prefs;
    /** Phiên demo chỉ nằm trong RAM — không ghi xuống đĩa để mỗi lần mở app phải chọn lại. */
    private volatile String sessionEmail;

    private DemoAuthStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Dọn khoá remember của phiên bản cũ.
        prefs.edit().remove("session_email").apply();
        seedIfNeeded();
    }

    public static DemoAuthStore get(Context context) {
        if (instance == null) {
            synchronized (DemoAuthStore.class) {
                if (instance == null) {
                    instance = new DemoAuthStore(context);
                }
            }
        }
        return instance;
    }

    public enum LoginStatus {
        SUCCESS,
        NOT_FOUND,
        WRONG_PASSWORD,
        UNVERIFIED
    }

    public static final class LoginResult {
        public final LoginStatus status;
        public final DemoAccount account;

        private LoginResult(LoginStatus status, DemoAccount account) {
            this.status = status;
            this.account = account;
        }
    }

    public synchronized void seedIfNeeded() {
        if (readAccounts().isEmpty()) {
            writeAccounts(createSeedAccounts());
        }
    }

    public synchronized LoginResult login(String rawEmail, String rawPassword) {
        String email = normalizeEmail(rawEmail);
        List<DemoAccount> accounts = readAccounts();
        DemoAccount account = findAccount(accounts, email);
        if (account == null) {
            return new LoginResult(LoginStatus.NOT_FOUND, null);
        }
        if (!account.getPasswordHash().equals(hash(rawPassword))) {
            return new LoginResult(LoginStatus.WRONG_PASSWORD, account);
        }
        if (!account.isEmailVerified()) {
            return new LoginResult(LoginStatus.UNVERIFIED, account);
        }
        setSession(account.getEmail());
        return new LoginResult(LoginStatus.SUCCESS, account);
    }

    public synchronized DemoAccount loginAs(Role role) {
        seedIfNeeded();
        DemoAccount account = findAccount(readAccounts(), seedEmailFor(role));
        if (account != null) {
            setSession(account.getEmail());
        }
        return account;
    }

    /**
     * Tạo tài khoản User mới ở trạng thái chưa xác thực email.
     *
     * @return tài khoản vừa tạo, hoặc {@code null} nếu email đã tồn tại.
     */
    public synchronized DemoAccount register(String displayName,
                                             String rawEmail,
                                             String phone,
                                             String password) {
        String email = normalizeEmail(rawEmail);
        List<DemoAccount> accounts = readAccounts();
        if (findAccount(accounts, email) != null) {
            return null;
        }
        DemoAccount account = new DemoAccount(
                email,
                hash(password),
                displayName == null ? "" : displayName.trim(),
                Role.USER,
                phone == null ? "" : phone.trim(),
                "",
                false);
        accounts.add(account);
        writeAccounts(accounts);
        return account;
    }

    /**
     * Xác thực OTP demo. OTP đúng sẽ đánh dấu email đã xác thực và tạo session.
     */
    public synchronized DemoAccount verifyEmail(String rawEmail, String otp) {
        if (!DEMO_OTP.equals(otp)) {
            return null;
        }
        String email = normalizeEmail(rawEmail);
        List<DemoAccount> accounts = readAccounts();
        DemoAccount account = findAccount(accounts, email);
        if (account == null) {
            return null;
        }
        DemoAccount verified = account.withEmailVerified(true);
        replaceAccount(accounts, verified);
        writeAccounts(accounts);
        setSession(verified.getEmail());
        return verified;
    }

    public synchronized boolean resetPassword(String rawEmail, String newPassword) {
        String email = normalizeEmail(rawEmail);
        List<DemoAccount> accounts = readAccounts();
        DemoAccount account = findAccount(accounts, email);
        if (account == null) {
            return false;
        }
        replaceAccount(accounts, account.withPasswordHash(hash(newPassword)));
        writeAccounts(accounts);
        return true;
    }

    public synchronized boolean accountExists(String rawEmail) {
        return findAccount(readAccounts(), normalizeEmail(rawEmail)) != null;
    }

    public synchronized DemoAccount currentAccount() {
        String email = sessionEmail;
        if (email == null || email.isEmpty()) {
            return null;
        }
        DemoAccount account = findAccount(readAccounts(), email);
        if (account == null) {
            clearSession();
        }
        return account;
    }

    public synchronized void logout() {
        clearSession();
    }

    public synchronized DemoAccount switchRole(Role role) {
        return loginAs(role);
    }

    public synchronized void resetDemoData() {
        prefs.edit().clear().apply();
        seedIfNeeded();
    }

    public synchronized List<DemoAccount> getAccounts() {
        return new ArrayList<>(readAccounts());
    }

    public static String seedEmailFor(Role role) {
        if (role == Role.ORGANIZER) {
            return "organizer@eticket.demo";
        }
        if (role == Role.ADMIN) {
            return "admin@eticket.demo";
        }
        return "user@eticket.demo";
    }

    private void setSession(String email) {
        sessionEmail = normalizeEmail(email);
    }

    private void clearSession() {
        sessionEmail = null;
    }

    private List<DemoAccount> createSeedAccounts() {
        List<DemoAccount> accounts = new ArrayList<>();
        accounts.add(new DemoAccount(
                "user@eticket.demo",
                hash(DEMO_PASSWORD),
                "Nguyễn Minh Anh",
                Role.USER,
                "0901 234 567",
                "",
                true));
        accounts.add(new DemoAccount(
                "organizer@eticket.demo",
                hash(DEMO_PASSWORD),
                "Saigon Stage",
                Role.ORGANIZER,
                "0902 345 678",
                "Saigon Stage",
                true));
        accounts.add(new DemoAccount(
                "admin@eticket.demo",
                hash(DEMO_PASSWORD),
                "ETicket Admin",
                Role.ADMIN,
                "0903 456 789",
                "",
                true));
        return accounts;
    }

    private List<DemoAccount> readAccounts() {
        List<DemoAccount> accounts = new ArrayList<>();
        String raw = prefs.getString(KEY_ACCOUNTS, "");
        if (raw == null || raw.isEmpty()) {
            return accounts;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Role role;
                try {
                    role = Role.valueOf(object.optString("role", Role.USER.name()));
                } catch (IllegalArgumentException exception) {
                    role = Role.USER;
                }
                accounts.add(new DemoAccount(
                        object.optString("email", ""),
                        object.optString("passwordHash", ""),
                        object.optString("displayName", ""),
                        role,
                        object.optString("phone", ""),
                        object.optString("organizationName", ""),
                        object.optBoolean("emailVerified", true)));
            }
        } catch (JSONException ignored) {
            // Dữ liệu demo hỏng: coi như chưa có tài khoản và seed lại.
        }
        return accounts;
    }

    private void writeAccounts(List<DemoAccount> accounts) {
        JSONArray array = new JSONArray();
        for (DemoAccount account : accounts) {
            JSONObject object = new JSONObject();
            try {
                object.put("email", account.getEmail());
                object.put("passwordHash", account.getPasswordHash());
                object.put("displayName", account.getDisplayName());
                object.put("role", account.getRole().name());
                object.put("phone", account.getPhone());
                object.put("organizationName", account.getOrganizationName());
                object.put("emailVerified", account.isEmailVerified());
                array.put(object);
            } catch (JSONException ignored) {
                // Các trường đều là primitive nên hiếm khi xảy ra.
            }
        }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply();
    }

    private DemoAccount findAccount(List<DemoAccount> accounts, String normalizedEmail) {
        for (DemoAccount account : accounts) {
            if (account.getEmail().equalsIgnoreCase(normalizedEmail)) {
                return account;
            }
        }
        return null;
    }

    private void replaceAccount(List<DemoAccount> accounts, DemoAccount updated) {
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getEmail().equalsIgnoreCase(updated.getEmail())) {
                accounts.set(i, updated);
                return;
            }
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String raw) {
        String value = raw == null ? "" : raw;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((PASSWORD_SALT + value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return value;
        }
    }
}
