package com.eventapp.app.ui.user;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.SsePaymentClient;
import com.eventapp.app.data.remote.dto.wallet.TopupIntentResponse;
import com.eventapp.app.data.remote.dto.wallet.WalletResponse;
import com.eventapp.app.databinding.ActivityUserWalletBinding;
import com.eventapp.app.databinding.DialogCheckoutCardBinding;
import com.eventapp.app.databinding.DialogCheckoutVietqrBinding;
import com.eventapp.app.databinding.DialogWalletTopupBinding;
import com.eventapp.app.ui.user.adapter.WalletTransactionAdapter;
import com.eventapp.app.ui.user.util.SavedCardStore;
import com.eventapp.app.ui.user.util.UserFmt;
import com.eventapp.app.ui.user.viewmodel.WalletViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonElement;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình Quản lý Ví ETicket (Chuẩn nghiệp vụ):
 * - Hiển thị số dư thực tế từ cơ sở dữ liệu qua API backend.
 * - Nạp tiền bằng VietQR: Hiện mã VietQR thật, copy STK, lưu ảnh QR, xác nhận chủ động khi chuyển xong.
 * - Nạp tiền bằng Thẻ Quốc Tế: Dùng chung danh sách thẻ SavedCardStore & kiểm tra Luhn/hạn dùng.
 * - Hỗ trợ truyền sẵn số tiền gợi ý cần nạp thêm từ màn hình Checkout.
 * - Quản lý & Đổi mã PIN 6 số cho Ví ETicket.
 */
public class WalletActivity extends AppCompatActivity {

    private static final String EXTRA_SUGGESTED_AMOUNT = "suggested_amount";

    private ActivityUserWalletBinding binding;
    private WalletViewModel viewModel;
    private WalletTransactionAdapter adapter;
    private BottomSheetDialog activeDialog;
    private long pendingSuggestedTopup = 0L;
    private boolean autoOpenedTopup = false;

    private SsePaymentClient.Subscription topupSseSubscription;
    private final Handler topupFallbackHandler = new Handler(Looper.getMainLooper());
    private Runnable topupFallbackRunnable;
    private long activeTopupId = -1L;
    private boolean topupCompleted = false;
    @Nullable private DialogCheckoutVietqrBinding activeQrBinding;

    public static void start(Context context) {
        Intent intent = new Intent(context, WalletActivity.class);
        context.startActivity(intent);
    }

    public static void start(Context context, long suggestedTopupAmount) {
        Intent intent = new Intent(context, WalletActivity.class);
        intent.putExtra(EXTRA_SUGGESTED_AMOUNT, suggestedTopupAmount);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        if (getIntent() != null) {
            pendingSuggestedTopup = getIntent().getLongExtra(EXTRA_SUGGESTED_AMOUNT, 0L);
        }

        setupRecyclerView();
        initListeners();
        observeViewModel();

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activeDialog != null && activeDialog.isShowing()
                && activeTopupId > 0 && !topupCompleted) {
            checkTopupStatus(activeTopupId, true);
        }
    }

    private void setupRecyclerView() {
        adapter = new WalletTransactionAdapter();
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(adapter);
    }

    private void initListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> loadData());
        binding.btnTopup.setOnClickListener(v -> showTopupSelectionDialog(0L));
        binding.btnChangePin.setOnClickListener(v -> showChangePinDialog());
    }

    private void loadData() {
        viewModel.loadWallet();
        viewModel.loadTransactions();
    }

    private void observeViewModel() {
        viewModel.getWallet().observe(this, wallet -> {
            if (wallet == null) {
                CreateWalletActivity.start(this);
                finish();
                return;
            }
            long balance = Math.round(wallet.balance);
            binding.tvWalletBalance.setText(UserFmt.amount(balance));
            binding.tvWalletStatus.setText(wallet.status != null ? wallet.status : "Hoạt động");
            binding.btnChangePin.setText(wallet.hasPin ? "Đổi mã PIN" : "Đặt mã PIN");

            if (pendingSuggestedTopup > 0 && !autoOpenedTopup) {
                autoOpenedTopup = true;
                long amountToFill = Math.max(10_000L, pendingSuggestedTopup);
                showTopupSelectionDialog(amountToFill);
            }
        });

        viewModel.getTransactions().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.rvTransactions.setVisibility(View.GONE);
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.rvTransactions.setVisibility(View.VISIBLE);
                adapter.submitList(list);
            }
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }

    // =========================================================================
    // 1. ĐỔI / THIẾT LẬP MÃ PIN VÍ 6 SỐ (LƯU TRÊN BACKEND)
    // =========================================================================

    private void showChangePinDialog() {
        WalletResponse currentWallet = viewModel.getWallet().getValue();
        boolean hasPin = currentWallet != null && currentWallet.hasPin;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText etOldPin = null;
        if (hasPin) {
            etOldPin = createPinInput("Nhập mã PIN hiện tại (6 số)");
            container.addView(etOldPin);
        }

        EditText etNewPin = createPinInput("Nhập mã PIN mới (6 số)");
        EditText etConfirmPin = createPinInput("Xác nhận mã PIN mới");
        container.addView(etNewPin);
        container.addView(etConfirmPin);

        final EditText finalOldPin = etOldPin;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(hasPin ? "Đổi mã PIN Ví ETicket" : "Thiết lập mã PIN Ví ETicket")
                .setMessage(hasPin
                        ? "Nhập mã PIN cũ và mã PIN 6 số mới để bảo vệ giao dịch."
                        : "Tài khoản chưa đặt mã PIN ví. Hãy tạo mã PIN 6 số để bảo vệ giao dịch thanh toán.")
                .setView(container)
                .setPositiveButton("Lưu mã PIN", null)
                .setNegativeButton("Huỷ", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldVal = finalOldPin != null ? finalOldPin.getText().toString().trim() : null;
            if (hasPin && (oldVal == null || !oldVal.matches("\\d{6}"))) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số mã PIN hiện tại!", Toast.LENGTH_SHORT).show();
                return;
            }
            String newVal = etNewPin.getText().toString().trim();
            String confirmVal = etConfirmPin.getText().toString().trim();
            if (!newVal.matches("\\d{6}")) {
                Toast.makeText(this, "Mã PIN mới phải gồm đúng 6 chữ số!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newVal.equals(confirmVal)) {
                Toast.makeText(this, "Mã PIN xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            viewModel.setPin(oldVal, newVal, new ApiCallback<WalletResponse>() {
                @Override
                public void onSuccess(WalletResponse data, JsonElement meta) {
                    if (isFinishing() || isDestroyed()) return;
                    dialog.dismiss();
                    Toast.makeText(WalletActivity.this, "✅ Đã cập nhật mã PIN Ví ETicket!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    if (isFinishing() || isDestroyed()) return;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(WalletActivity.this, "❌ " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private EditText createPinInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        return et;
    }

    // =========================================================================
    // 2. CHỌN MỨC TIỀN VÀ HÌNH THỨC NẠP (VIETQR / THẺ)
    // =========================================================================

    private void showTopupSelectionDialog(long prefillAmount) {
        dismissActiveDialog();

        activeDialog = new BottomSheetDialog(this);
        DialogWalletTopupBinding dialogBinding = DialogWalletTopupBinding.inflate(LayoutInflater.from(this));

        if (prefillAmount > 0) {
            dialogBinding.etAmount.setText(String.valueOf(prefillAmount));
        }

        dialogBinding.chip100k.setOnClickListener(v -> dialogBinding.etAmount.setText("100000"));
        dialogBinding.chip200k.setOnClickListener(v -> dialogBinding.etAmount.setText("200000"));
        dialogBinding.chip500k.setOnClickListener(v -> dialogBinding.etAmount.setText("500000"));

        dialogBinding.btnSubmitTopup.setOnClickListener(v -> {
            String amountStr = dialogBinding.etAmount.getText().toString().trim().replace(".", "").replace(",", "");
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền muốn nạp!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount < 10000) {
                Toast.makeText(this, "Số tiền nạp tối thiểu là 10.000 đ!", Toast.LENGTH_SHORT).show();
                return;
            }

            dismissActiveDialog();

            if (dialogBinding.rbVietQr.isChecked()) {
                initiateVietQrTopup(amount);
            } else {
                showTopupCardDialog(amount);
            }
        });

        activeDialog.setContentView(dialogBinding.getRoot());
        activeDialog.show();
    }

    // =========================================================================
    // 3. NẠP TIỀN BẰNG VIETQR QUA TOPUP INTENT + WEBHOOK + SSE REALTIME
    // =========================================================================

    private void initiateVietQrTopup(double amount) {
        String note = "Nạp tiền vào Ví ETicket qua VietQR";
        viewModel.createTopupIntent(amount, note, new ApiCallback<TopupIntentResponse>() {
            @Override
            public void onSuccess(TopupIntentResponse intent, JsonElement meta) {
                if (isFinishing() || isDestroyed()) return;
                if (intent != null && intent.topupId > 0) {
                    showTopupVietQrDialog(intent);
                } else {
                    Toast.makeText(WalletActivity.this,
                            "Không tạo được mã nạp tiền VietQR", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                Toast.makeText(WalletActivity.this,
                        "Lỗi khởi tạo nạp VietQR: " + error.getUserMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showTopupVietQrDialog(TopupIntentResponse intent) {
        dismissActiveDialog();
        topupCompleted = false;

        long amountLong = Math.round(intent.amount);
        String topupCode = (intent.transferContent != null && !intent.transferContent.isEmpty())
                ? intent.transferContent
                : (intent.topupCode != null ? intent.topupCode
                   : String.format(Locale.US, "ETNAP%06d", Math.max(1L, intent.topupId)));
        String bankCode = (intent.bankCode != null && !intent.bankCode.isEmpty()) ? intent.bankCode : "ACB";
        String bankAcc = (intent.bankAccount != null && !intent.bankAccount.isEmpty())
                ? intent.bankAccount : "190368688888";
        String accName = (intent.accountName != null && !intent.accountName.trim().isEmpty())
                ? intent.accountName.trim() : "CONG TY ETICKET";

        activeDialog = new BottomSheetDialog(this);
        DialogCheckoutVietqrBinding dialogBinding = DialogCheckoutVietqrBinding.inflate(LayoutInflater.from(this));
        activeTopupId = intent.topupId;
        activeQrBinding = dialogBinding;

        dialogBinding.layoutExpiryBanner.setVisibility(View.VISIBLE);
        dialogBinding.tvExpiryCountdown.setText("Mã nạp tiền định danh: " + topupCode);
        dialogBinding.btnPayLater.setVisibility(View.GONE);
        dialogBinding.progressConfirming.setVisibility(View.VISIBLE);
        dialogBinding.tvLiveBadge.setVisibility(View.VISIBLE);
        dialogBinding.tvLiveBadge.setText("● Đang kết nối Realtime (SSE)...");
        dialogBinding.tvQrStatus.setText("Mở app ngân hàng chuyển đúng số tiền & nội dung. Tiền tự động vào ví ngay khi Webhook xác nhận.");
        dialogBinding.tvBankName.setText(bankCode);
        dialogBinding.tvAccountName.setText(accName);
        dialogBinding.tvAccountNumber.setText(bankAcc);
        dialogBinding.tvAmount.setText(UserFmt.amount(amountLong));
        dialogBinding.tvTransferCode.setText(topupCode);
        dialogBinding.btnManualConfirm.setText("Kiểm tra giao dịch ngay");

        String qrUrl = "https://img.vietqr.io/image/" + Uri.encode(bankCode) + "-" + Uri.encode(bankAcc) + "-compact2.png"
                + "?amount=" + amountLong
                + "&addInfo=" + Uri.encode(topupCode)
                + "&accountName=" + Uri.encode(accName);
        Glide.with(this).load(qrUrl).into(dialogBinding.ivQrCode);

        dialogBinding.btnSaveQr.setOnClickListener(v -> saveQrImageToGallery(dialogBinding.ivQrCode, topupCode));
        dialogBinding.btnCopyAccount.setOnClickListener(v -> copyText("Số tài khoản", bankAcc));
        dialogBinding.btnCopyAmount.setOnClickListener(v -> copyText("Số tiền", String.valueOf(amountLong)));
        dialogBinding.btnCopyCode.setOnClickListener(v -> copyText("Nội dung chuyển khoản", topupCode));

        dialogBinding.btnManualConfirm.setOnClickListener(v -> {
            dialogBinding.btnManualConfirm.setEnabled(false);
            dialogBinding.tvQrStatus.setText("Đang kiểm tra trạng thái nạp tiền...");
            checkTopupStatus(intent.topupId, false);
        });

        if (BuildConfig.DEBUG) {
            dialogBinding.btnSimulateWebhook.setVisibility(View.VISIBLE);
            dialogBinding.btnSimulateWebhook.setOnClickListener(v -> {
                dialogBinding.btnSimulateWebhook.setEnabled(false);
                dialogBinding.btnSimulateWebhook.setText("Đang gửi Webhook nạp ví (Dev)...");
                viewModel.simulateTopupWebhook(topupCode, amountLong, new ApiCallback<TopupIntentResponse>() {
                    @Override
                    public void onSuccess(TopupIntentResponse simulated, JsonElement meta) {
                        if (!topupCompleted) {
                            if (simulated != null && "SUCCESS".equalsIgnoreCase(simulated.status)) {
                                onTopupWebhookSucceeded(
                                        Math.round(simulated.amount > 0 ? simulated.amount : amountLong),
                                        simulated.providerTransactionId);
                            } else {
                                checkTopupStatus(intent.topupId, true);
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (activeQrBinding != null) {
                            activeQrBinding.btnSimulateWebhook.setEnabled(true);
                            activeQrBinding.btnSimulateWebhook.setText("⚡ Giả lập Webhook ngân hàng (Dev)");
                        }
                        Toast.makeText(WalletActivity.this,
                                "Lỗi giả lập Webhook: " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        activeDialog.setContentView(dialogBinding.getRoot());
        activeDialog.setOnDismissListener(d -> stopTopupRealtimeListeners());
        activeDialog.show();

        startTopupSse(intent.topupId, amountLong, dialogBinding);
        scheduleTopupFallbackPoll(intent.topupId);
    }

    private void startTopupSse(long topupId, long amountLong, DialogCheckoutVietqrBinding dialogBinding) {
        stopTopupSseOnly();
        topupSseSubscription = SsePaymentClient.subscribe(
                "wallets/topups/" + topupId + "/events",
                TopupIntentResponse.class,
                new SsePaymentClient.Listener<TopupIntentResponse>() {
                    @Override
                    public void onConnected() {
                        if (!topupCompleted && activeQrBinding == dialogBinding) {
                            dialogBinding.tvLiveBadge.setText("● Realtime SSE đang lắng nghe");
                        }
                    }

                    @Override
                    public void onEvent(@NonNull TopupIntentResponse updated) {
                        if (topupCompleted) return;
                        if ("SUCCESS".equalsIgnoreCase(updated.status)) {
                            onTopupWebhookSucceeded(
                                    Math.round(updated.amount > 0 ? updated.amount : amountLong),
                                    updated.providerTransactionId);
                        } else if ("FAILED".equalsIgnoreCase(updated.status)
                                || "EXPIRED".equalsIgnoreCase(updated.status)) {
                            showTopupFailure(dialogBinding, updated.failureMessage);
                        }
                    }

                    @Override
                    public void onDisconnected(@Nullable Throwable error) {
                        if (!topupCompleted && activeQrBinding == dialogBinding) {
                            dialogBinding.tvLiveBadge.setText("○ Đang tự kiểm tra dự phòng (15s)");
                        }
                    }
                });
    }

    private void scheduleTopupFallbackPoll(long topupId) {
        stopTopupFallbackOnly();
        topupFallbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (topupCompleted || activeDialog == null || !activeDialog.isShowing() || isFinishing()) {
                    return;
                }
                checkTopupStatus(topupId, true);
                topupFallbackHandler.postDelayed(this, 15_000L);
            }
        };
        topupFallbackHandler.postDelayed(topupFallbackRunnable, 15_000L);
    }

    private void checkTopupStatus(long topupId, boolean silent) {
        viewModel.checkTopupStatus(topupId, new ApiCallback<TopupIntentResponse>() {
            @Override
            public void onSuccess(TopupIntentResponse current, JsonElement meta) {
                if (topupCompleted || isFinishing()) return;
                if (current != null && "SUCCESS".equalsIgnoreCase(current.status)) {
                    onTopupWebhookSucceeded(Math.round(current.amount), current.providerTransactionId);
                    return;
                }
                if (current != null && ("FAILED".equalsIgnoreCase(current.status)
                        || "EXPIRED".equalsIgnoreCase(current.status))) {
                    if (activeQrBinding != null) {
                        showTopupFailure(activeQrBinding, current.failureMessage);
                    }
                    return;
                }
                if (activeQrBinding != null) {
                    activeQrBinding.btnManualConfirm.setEnabled(true);
                    if (!silent) {
                        activeQrBinding.tvQrStatus.setText("Ngân hàng chưa báo nhận tiền. Hệ thống vẫn đang lắng nghe tự động...");
                        Toast.makeText(WalletActivity.this,
                                "Chưa ghi nhận chuyển khoản. Vui lòng chờ thêm vài giây.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (activeQrBinding != null) {
                    activeQrBinding.btnManualConfirm.setEnabled(true);
                    if (!silent) {
                        activeQrBinding.tvQrStatus.setText("Chưa thể kiểm tra lúc này: " + error.getUserMessage());
                    }
                }
            }
        });
    }

    private void showTopupFailure(@NonNull DialogCheckoutVietqrBinding dialogBinding,
                                  @Nullable String failureMessage) {
        stopTopupRealtimeListeners();
        dialogBinding.progressConfirming.setVisibility(View.GONE);
        dialogBinding.tvLiveBadge.setText("✕ Yêu cầu nạp không hợp lệ");
        dialogBinding.btnManualConfirm.setEnabled(false);
        String msg = (failureMessage != null && !failureMessage.trim().isEmpty())
                ? failureMessage.trim()
                : "Yêu cầu nạp tiền đã hết hạn hoặc số tiền chưa khớp.";
        dialogBinding.tvQrStatus.setText(msg);
        Toast.makeText(this, "❌ " + msg, Toast.LENGTH_LONG).show();
    }

    private void onTopupWebhookSucceeded(long creditedAmount, @Nullable String providerTxId) {
        if (topupCompleted || isFinishing() || isDestroyed()) {
            return;
        }
        topupCompleted = true;
        dismissActiveDialog();
        String txSuffix = (providerTxId != null && !providerTxId.trim().isEmpty())
                ? " (Mã GD: " + providerTxId.trim() + ")"
                : "";
        Toast.makeText(this,
                "🎉 Đã nhận " + UserFmt.amount(creditedAmount) + " qua Webhook" + txSuffix + "! Số dư ví đã được cập nhật.",
                Toast.LENGTH_LONG).show();
        loadData();
    }

    private void stopTopupSseOnly() {
        if (topupSseSubscription != null) {
            topupSseSubscription.close();
            topupSseSubscription = null;
        }
    }

    private void stopTopupFallbackOnly() {
        if (topupFallbackRunnable != null) {
            topupFallbackHandler.removeCallbacks(topupFallbackRunnable);
            topupFallbackRunnable = null;
        }
    }

    private void stopTopupRealtimeListeners() {
        stopTopupSseOnly();
        stopTopupFallbackOnly();
        activeTopupId = -1L;
        activeQrBinding = null;
    }

    // =========================================================================
    // 4. NẠP TIỀN BẰNG THẺ (DÙNG CHUNG SavedCardStore)
    // =========================================================================

    private void showTopupCardDialog(double amount) {
        dismissActiveDialog();

        long amountLong = Math.round(amount);
        activeDialog = new BottomSheetDialog(this);
        DialogCheckoutCardBinding dialogBinding = DialogCheckoutCardBinding.inflate(LayoutInflater.from(this));

        dialogBinding.tvCardDialogTitle.setText("Nạp tiền bằng Thẻ Quốc Tế");
        dialogBinding.btnSubmitCard.setText("Nạp " + UserFmt.amount(amountLong) + " vào ví");

        List<SavedCardStore.SavedCard> savedCards = SavedCardStore.getCards(this);
        if (!savedCards.isEmpty()) {
            dialogBinding.rbCardVisa.setText(savedCards.get(0).displayLabel);
        }
        if (savedCards.size() > 1) {
            dialogBinding.rbCardMastercard.setText(savedCards.get(1).displayLabel);
            dialogBinding.rbCardMastercard.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.rbCardMastercard.setVisibility(View.GONE);
        }

        dialogBinding.rgCardSelection.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbNewCard) {
                dialogBinding.layoutNewCardFields.setVisibility(View.VISIBLE);
            } else {
                dialogBinding.layoutNewCardFields.setVisibility(View.GONE);
            }
        });

        dialogBinding.btnSubmitCard.setOnClickListener(v -> {
            String cardDesc;
            int selectedId = dialogBinding.rgCardSelection.getCheckedRadioButtonId();
            if (selectedId == R.id.rbCardMastercard && savedCards.size() > 1) {
                cardDesc = savedCards.get(1).displayLabel;
            } else if (selectedId == R.id.rbNewCard) {
                String cardNum = dialogBinding.etCardNumber.getText().toString().trim();
                String expiry = dialogBinding.etExpiry.getText().toString().trim();
                String cvv = dialogBinding.etCvv.getText().toString().trim();
                String err = SavedCardStore.validateCard(cardNum, expiry, cvv);
                if (err != null) {
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                    return;
                }
                SavedCardStore.SavedCard saved = SavedCardStore.addCard(this, cardNum, expiry);
                cardDesc = saved.displayLabel;
            } else {
                cardDesc = !savedCards.isEmpty() ? savedCards.get(0).displayLabel : "Thẻ Visa •••• 4242";
            }

            final String note = "Nạp tiền vào ví bằng " + cardDesc;
            dialogBinding.btnSubmitCard.setEnabled(false);
            dialogBinding.btnSubmitCard.setText("Đang xác thực 3D-Secure...");
            dialogBinding.progressBar.setVisibility(View.VISIBLE);

            if (!isFinishing()) {
                executeTopup(amount, "CARD", note);
            }
        });

        activeDialog.setContentView(dialogBinding.getRoot());
        activeDialog.show();
    }

    private void executeTopup(double amount, String method, String note) {
        viewModel.topup(amount, method, note, () -> {
            dismissActiveDialog();
            Toast.makeText(this, "🎉 Đã nạp thành công " + UserFmt.amount(Math.round(amount)) + " vào ví!", Toast.LENGTH_LONG).show();
            loadData();
        });
    }

    private void copyText(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            Toast.makeText(this, "Đã sao chép: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQrImageToGallery(ImageView ivQr, String code) {
        if (ivQr.getDrawable() == null || !(ivQr.getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Đang tải mã QR, vui lòng thử lại sau!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = ((BitmapDrawable) ivQr.getDrawable()).getBitmap();
            String fileName = "VietQR_Topup_" + code + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ETicket");
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.close();
                    Toast.makeText(this, "✅ Đã lưu mã QR nạp tiền vào máy!", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissActiveDialog() {
        stopTopupRealtimeListeners();
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissActiveDialog();
    }
}
