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
import android.os.CountDownTimer;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.SsePaymentClient;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.remote.dto.payment.PaymentDtos;
import com.eventapp.app.data.remote.dto.promotion.PromotionDtos;
import com.eventapp.app.data.remote.dto.wallet.WalletResponse;
import com.eventapp.app.data.remote.dto.wallet.WalletTransactionDto;
import com.eventapp.app.data.repository.OrderRepository;
import com.eventapp.app.data.repository.PromotionRepository;
import com.eventapp.app.data.repository.TicketRepository;
import com.eventapp.app.data.repository.WalletRepository;
import com.eventapp.app.databinding.ActivityUserCheckoutBinding;
import com.eventapp.app.databinding.DialogCheckoutCardBinding;
import com.eventapp.app.databinding.DialogCheckoutVietqrBinding;
import com.eventapp.app.databinding.DialogWalletPinBinding;
import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.util.SavedCardStore;
import com.eventapp.app.ui.user.util.UserFmt;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonElement;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Màn hình Xác nhận đơn hàng & Thanh toán (Chuẩn nghiệp vụ FR-U10, FR-U11, FR-U13, FR-O18):
 * - Luồng chuẩn: Tạo đơn hàng (PENDING_PAYMENT) trên Backend -> Khởi tạo giao dịch -> Xác nhận thanh toán -> Chuyển PAID & phát hành vé.
 * - Hỗ trợ thanh toán tiếp các đơn hàng đang chờ (PENDING_PAYMENT) từ Lịch sử đơn hàng.
 * - VietQR hiển thị đồng hồ đếm ngược giữ chỗ 15 phút, KHÔNG tự động chốt đơn sau 6 giây; hỗ trợ Thanh toán sau (Lưu đơn chờ).
 * - Thẻ quốc tế dùng chung SavedCardStore, kiểm tra Luhn & hạn dùng, xác nhận 3D-Secure & gọi confirmPayment.
 * - Ví ETicket kiểm tra số dư thật, tự chuyển sang nạp đúng số tiền thiếu, hỗ trợ thiết lập PIN nếu đăng nhập máy mới.
 */
public class CheckoutActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_VENUE = "venue";
    private static final String EXTRA_CITY = "city";
    private static final String EXTRA_START = "start_at";
    private static final String EXTRA_END = "end_at";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_TICKET_TYPE_IDS = "ticket_type_ids";
    private static final String EXTRA_TYPE_NAMES = "type_names";
    private static final String EXTRA_TYPE_PRICES = "type_prices";
    private static final String EXTRA_QUANTITIES = "quantities";

    private static final String EXTRA_RESUME_ORDER_ID = "resume_order_id";
    private static final String EXTRA_RESUME_ORDER_CODE = "resume_order_code";
    private static final String EXTRA_RESUME_TOTAL = "resume_total";
    private static final String EXTRA_RESUME_DUE_AT = "resume_due_at";

    private static final int METHOD_WALLET = 0;
    private static final int METHOD_VIETQR = 1;
    private static final int METHOD_CARD = 2;
    private static final long DEFAULT_WINDOW_MS = 15 * 60 * 1000L;

    private ActivityUserCheckoutBinding binding;
    private WalletRepository walletRepository;
    private OrderRepository orderRepository;

    private long eventId;
    private String eventTitle;
    private String venue;
    private String city;
    private long startAt;
    private long endAt;
    private EventCategory category = EventCategory.OTHER;
    private long[] ticketTypeIds = new long[0];
    private String[] typeNames = new String[0];
    private long[] typePrices = new long[0];
    private int[] quantities = new int[0];

    private long total;
    private long originalTotal;
    private long discountAmount = 0L;
    private Long appliedCouponId = null;
    private String appliedCouponCode = null;
    private int quantity;
    private int method = METHOD_VIETQR;

    private String checkoutIdempotencyKey = UUID.randomUUID().toString();
    private OrderDtos.Order activeOrder;
    private boolean isResumingPendingOrder = false;

    private BottomSheetDialog activeDialog;
    private CountDownTimer expiryTimer;

    private SsePaymentClient.Subscription paymentSseSubscription;
    private final Handler fallbackPollHandler = new Handler(Looper.getMainLooper());
    private Runnable fallbackPollRunnable;
    private long activeVietQrPaymentId = -1L;
    private boolean paymentCompleted = false;
    @Nullable private DialogCheckoutVietqrBinding activeQrBinding;
    @Nullable private OrderDtos.Order activeQrOrder;
    @Nullable private String activeQrTransferCode;

    private interface OnOrderReady {
        void onReady(OrderDtos.Order order);
    }

    private interface OnOrderError {
        void onError(@NonNull ApiException error);
    }

    public static void start(Context context, long eventId, String title, String venue, String city,
                             long startAt, long endAt, int categoryOrdinal,
                             long[] ticketTypeIds, String[] typeNames, long[] typePrices, int[] quantities) {
        Intent intent = new Intent(context, CheckoutActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_VENUE, venue);
        intent.putExtra(EXTRA_CITY, city);
        intent.putExtra(EXTRA_START, startAt);
        intent.putExtra(EXTRA_END, endAt);
        intent.putExtra(EXTRA_CATEGORY, categoryOrdinal);
        intent.putExtra(EXTRA_TICKET_TYPE_IDS, ticketTypeIds);
        intent.putExtra(EXTRA_TYPE_NAMES, typeNames);
        intent.putExtra(EXTRA_TYPE_PRICES, typePrices);
        intent.putExtra(EXTRA_QUANTITIES, quantities);
        context.startActivity(intent);
    }

    public static void start(Context context, long eventId, String title, String venue, String city,
                             long startAt, long endAt, int categoryOrdinal,
                             String[] typeNames, long[] typePrices, int[] quantities) {
        start(context, eventId, title, venue, city, startAt, endAt, categoryOrdinal, null, typeNames, typePrices, quantities);
    }

    /** Mở màn hình Checkout để thanh toán tiếp đơn hàng đang ở trạng thái PENDING_PAYMENT. */
    public static void startForPendingOrder(Context context, OrderDtos.Order order) {
        if (order == null) return;
        Intent intent = new Intent(context, CheckoutActivity.class);
        intent.putExtra(EXTRA_RESUME_ORDER_ID, order.orderId);
        intent.putExtra(EXTRA_RESUME_ORDER_CODE, order.orderCode);
        intent.putExtra(EXTRA_RESUME_TOTAL, order.totalAmount != null ? order.totalAmount.longValue() : 0L);
        intent.putExtra(EXTRA_RESUME_DUE_AT, order.paymentDueAt);
        intent.putExtra(EXTRA_EVENT_ID, order.eventId > 0 ? order.eventId : 1L);
        intent.putExtra(EXTRA_TITLE, order.eventTitle != null ? order.eventTitle : "Đơn vé " + order.orderCode);
        intent.putExtra(EXTRA_VENUE, order.venueName != null ? order.venueName : "");
        intent.putExtra(EXTRA_CITY, order.city != null ? order.city : "");
        intent.putExtra(EXTRA_START, parseIsoMillis(order.eventStartAt, System.currentTimeMillis()));
        intent.putExtra(EXTRA_END, parseIsoMillis(order.eventEndAt, System.currentTimeMillis()));

        if (order.items != null && !order.items.isEmpty()) {
            int n = order.items.size();
            long[] ids = new long[n];
            String[] names = new String[n];
            long[] prices = new long[n];
            int[] qtys = new int[n];
            for (int i = 0; i < n; i++) {
                OrderDtos.Item item = order.items.get(i);
                ids[i] = item.ticketTypeId;
                names[i] = item.ticketName != null ? item.ticketName : "Vé sự kiện";
                prices[i] = item.unitPrice != null ? item.unitPrice.longValue() : 0L;
                qtys[i] = Math.max(1, item.quantity);
            }
            intent.putExtra(EXTRA_TICKET_TYPE_IDS, ids);
            intent.putExtra(EXTRA_TYPE_NAMES, names);
            intent.putExtra(EXTRA_TYPE_PRICES, prices);
            intent.putExtra(EXTRA_QUANTITIES, qtys);
        } else {
            long amt = order.totalAmount != null ? order.totalAmount.longValue() : 0L;
            int qty = Math.max(1, order.ticketsIssued);
            intent.putExtra(EXTRA_TICKET_TYPE_IDS, new long[]{1L});
            intent.putExtra(EXTRA_TYPE_NAMES, new String[]{"Vé đã đặt (" + order.orderCode + ")"});
            intent.putExtra(EXTRA_TYPE_PRICES, new long[]{amt / qty});
            intent.putExtra(EXTRA_QUANTITIES, new int[]{qty});
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        walletRepository = ServiceLocator.get(WalletRepository.class);
        orderRepository = ServiceLocator.get(OrderRepository.class);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        if (!readExtras()) {
            Toast.makeText(this, "Thông tin đơn hàng không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        computeTotals();
        bindEventInfo();
        buildTicketRows();
        initMethodSelection();
        loadWalletBalance();
        initListeners();

        if (isResumingPendingOrder) {
            binding.cardCoupon.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWalletBalance();
        if (activeDialog != null && activeDialog.isShowing()
                && activeVietQrPaymentId > 0 && !paymentCompleted) {
            checkOrderPaymentStatus(activeVietQrPaymentId, true);
        }
    }

    private void loadWalletBalance() {
        walletRepository.getMyWallet(new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse wallet, JsonElement meta) {
                if (wallet != null) {
                    long balance = Math.round(wallet.balance);
                    binding.tvWalletSubtitle.setText("Số dư khả dụng: " + UserFmt.amount(balance));
                } else {
                    binding.tvWalletSubtitle.setText("Chưa kích hoạt ví (0 đ)");
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                binding.tvWalletSubtitle.setText("Chưa kích hoạt ví (0 đ)");
            }
        });
    }

    private boolean readExtras() {
        Intent i = getIntent();
        if (i == null) return false;

        long resumeOrderId = i.getLongExtra(EXTRA_RESUME_ORDER_ID, -1L);
        if (resumeOrderId > 0) {
            isResumingPendingOrder = true;
            activeOrder = new OrderDtos.Order();
            activeOrder.orderId = resumeOrderId;
            activeOrder.orderCode = i.getStringExtra(EXTRA_RESUME_ORDER_CODE);
            activeOrder.paymentDueAt = i.getStringExtra(EXTRA_RESUME_DUE_AT);
            long resumeTotal = i.getLongExtra(EXTRA_RESUME_TOTAL, 0L);
            activeOrder.totalAmount = BigDecimal.valueOf(resumeTotal);
            activeOrder.status = "PENDING_PAYMENT";
        }

        eventId = i.getLongExtra(EXTRA_EVENT_ID, -1L);
        if (eventId <= 0) return false;
        eventTitle = i.getStringExtra(EXTRA_TITLE);
        venue = i.getStringExtra(EXTRA_VENUE);
        city = i.getStringExtra(EXTRA_CITY);
        startAt = i.getLongExtra(EXTRA_START, 0L);
        endAt = i.getLongExtra(EXTRA_END, 0L);
        int catOrd = i.getIntExtra(EXTRA_CATEGORY, EventCategory.OTHER.ordinal());
        EventCategory[] categories = EventCategory.values();
        category = (catOrd >= 0 && catOrd < categories.length) ? categories[catOrd] : EventCategory.OTHER;
        typeNames = i.getStringArrayExtra(EXTRA_TYPE_NAMES);
        typePrices = i.getLongArrayExtra(EXTRA_TYPE_PRICES);
        quantities = i.getIntArrayExtra(EXTRA_QUANTITIES);
        ticketTypeIds = i.getLongArrayExtra(EXTRA_TICKET_TYPE_IDS);
        if (ticketTypeIds == null && quantities != null) {
            ticketTypeIds = new long[quantities.length];
            for (int k = 0; k < quantities.length; k++) {
                ticketTypeIds[k] = k + 1;
            }
        }
        return typeNames != null && typePrices != null && quantities != null
                && typeNames.length == typePrices.length && typePrices.length == quantities.length;
    }

    private void computeTotals() {
        if (isResumingPendingOrder && activeOrder != null && activeOrder.totalAmount != null) {
            total = activeOrder.totalAmount.longValue();
            originalTotal = total;
            quantity = 0;
            for (int q : quantities) quantity += q;
            return;
        }
        total = 0;
        quantity = 0;
        for (int i = 0; i < quantities.length; i++) {
            quantity += quantities[i];
            total += quantities[i] * typePrices[i];
        }
        originalTotal = total;
        if (discountAmount > 0) {
            total = Math.max(0, originalTotal - discountAmount);
        }
    }

    private void bindEventInfo() {
        binding.tvEventTitle.setText(eventTitle != null ? eventTitle : "");
        binding.tvEventVenue.setText(venue != null && !venue.isEmpty() ? venue : city);
        binding.tvEventDate.setText(UserFmt.dateTime(startAt));
    }

    private void buildTicketRows() {
        binding.orderLines.removeAllViews();
        for (int i = 0; i < typeNames.length; i++) {
            if (quantities[i] <= 0) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView left = new TextView(this);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            left.setText(quantities[i] + "x " + typeNames[i]);
            left.setTextColor(ContextCompat.getColor(this, R.color.user_text));
            left.setTextSize(14f);

            TextView right = new TextView(this);
            right.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            long lineTotal = quantities[i] * typePrices[i];
            right.setText(UserFmt.amount(lineTotal));
            right.setTextColor(ContextCompat.getColor(this, R.color.user_text));
            right.setTextSize(14f);

            row.addView(left);
            row.addView(right);
            binding.orderLines.addView(row);
        }

        binding.tvSubtotal.setText(UserFmt.amount(originalTotal));
        binding.tvFee.setText("Miễn phí");
        binding.tvTotal.setText(UserFmt.amount(total));
        binding.tvBottomTotal.setText(UserFmt.amount(total));
    }

    private void initMethodSelection() {
        binding.optVietqr.setOnClickListener(v -> selectMethod(METHOD_VIETQR));
        binding.optWallet.setOnClickListener(v -> selectMethod(METHOD_WALLET));
        binding.optCard.setOnClickListener(v -> selectMethod(METHOD_CARD));
        selectMethod(METHOD_VIETQR);
    }

    private void selectMethod(int m) {
        this.method = m;
        binding.optVietqr.setSelected(m == METHOD_VIETQR);
        binding.optWallet.setSelected(m == METHOD_WALLET);
        binding.optCard.setSelected(m == METHOD_CARD);
        binding.radioVietqr.setChecked(m == METHOD_VIETQR);
        binding.radioWallet.setChecked(m == METHOD_WALLET);
        binding.radioCard.setChecked(m == METHOD_CARD);
        binding.qrPanel.setVisibility(View.GONE);
    }

    private void updateCta() {
        binding.tvTotal.setText(UserFmt.amount(total));
        binding.tvBottomTotal.setText(UserFmt.amount(total));
        if (total <= 0) {
            binding.btnPay.setText("Xác nhận nhận vé (0 đ)");
        } else {
            binding.btnPay.setText(getString(R.string.user_pay_now, UserFmt.amount(total)));
        }
    }

    private void onPayClicked() {
        if (total <= 0) {
            ensureOrderCreated(order -> {
                String code = order != null && order.orderCode != null ? order.orderCode : fallbackOrderCode();
                paySuccess(code);
            });
            return;
        }

        switch (method) {
            case METHOD_VIETQR:
                handleVietQrPayment();
                break;
            case METHOD_WALLET:
                handleWalletPayment();
                break;
            case METHOD_CARD:
                showCardDialog();
                break;
        }
    }

    /**
     * Đảm bảo đơn hàng đã được tạo trên Backend trước khi mở cổng thanh toán.
     * Sử dụng checkoutIdempotencyKey để không tạo đơn trùng khi người dùng bấm lại hoặc đổi phương thức.
     */
    private void ensureOrderCreated(OnOrderReady callback) {
        ensureOrderCreated(callback, null);
    }

    /**
     * Đảm bảo đơn hàng đã được tạo trên Backend trước khi mở cổng thanh toán.
     * Đồng bộ lại discountAmount, platformFeeAmount và totalAmount chuẩn từ Backend.
     */
    private void ensureOrderCreated(OnOrderReady callback, @Nullable OnOrderError errorCallback) {
        if (activeOrder != null && activeOrder.orderId > 0) {
            callback.onReady(activeOrder);
            return;
        }

        setPayButtonBusy(true, "Đang giữ chỗ vé...");

        OrderDtos.CreateRequest orderReq = new OrderDtos.CreateRequest();
        orderReq.eventId = eventId;
        orderReq.idempotencyKey = checkoutIdempotencyKey;
        orderReq.couponId = appliedCouponId;
        orderReq.couponCode = appliedCouponCode;
        orderReq.customerNote = "Thanh toán " + methodLabel();
        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i] > 0) {
                long tId = (ticketTypeIds != null && ticketTypeIds.length > i && ticketTypeIds[i] > 0)
                        ? ticketTypeIds[i] : (i + 1);
                orderReq.items.add(new OrderDtos.CreateItem(tId, quantities[i]));
            }
        }

        orderRepository.createOrder(orderReq, new ApiCallback<OrderDtos.Order>() {
            @Override
            public void onSuccess(OrderDtos.Order createdOrder, JsonElement meta) {
                setPayButtonBusy(false, null);
                if (createdOrder != null && createdOrder.orderId > 0) {
                    activeOrder = createdOrder;
                    syncOrderAmounts(createdOrder);
                    callback.onReady(createdOrder);
                } else {
                    ApiException fallbackErr = ApiException.of(500, "ORDER_INVALID",
                            "Không thể tạo đơn hàng hợp lệ từ máy chủ.");
                    if (errorCallback != null) {
                        errorCallback.onError(fallbackErr);
                    }
                    Toast.makeText(CheckoutActivity.this,
                            "❌ " + fallbackErr.getUserMessage() + " Vui lòng thử lại!",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                setPayButtonBusy(false, null);
                if (errorCallback != null) {
                    errorCallback.onError(error);
                }
                Toast.makeText(CheckoutActivity.this,
                        "❌ " + error.getUserMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void syncOrderAmounts(@NonNull OrderDtos.Order order) {
        if (order.discountAmount != null) {
            discountAmount = order.discountAmount.longValue();
        }
        long fee = order.platformFeeAmount != null ? order.platformFeeAmount.longValue() : 0L;
        if (order.totalAmount != null) {
            total = order.totalAmount.longValue();
        }
        binding.tvFee.setText(fee > 0 ? UserFmt.amount(fee) : "Miễn phí");
        if (discountAmount > 0) {
            binding.layoutDiscountRow.setVisibility(View.VISIBLE);
            binding.tvDiscountAmount.setText("- " + UserFmt.amount(discountAmount));
        }
        updateCta();
    }

    private String methodLabel() {
        if (method == METHOD_WALLET) return "Ví ETicket";
        if (method == METHOD_CARD) return "Thẻ Quốc Tế";
        return "VietQR";
    }

    private void setPayButtonBusy(boolean busy, @Nullable String label) {
        binding.btnPay.setEnabled(!busy);
        if (busy && label != null) {
            binding.btnPay.setText(label);
        } else {
            updateCta();
        }
    }

    // =========================================================================
    // 1. THANH TOÁN BẰNG VÍ ETICKET (KIỂM TRA SỐ DƯ + PIN + TRỪ TIỀN & CHỐT ĐƠN)
    // =========================================================================

    private void handleWalletPayment() {
        walletRepository.getMyWallet(new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse wallet, JsonElement meta) {
                if (isFinishing() || isDestroyed()) return;
                if (wallet == null) {
                    Toast.makeText(CheckoutActivity.this,
                            "Bạn chưa kích hoạt Ví ETicket, hãy tạo ví ngay!", Toast.LENGTH_LONG).show();
                    CreateWalletActivity.start(CheckoutActivity.this);
                    return;
                }

                long currentBalance = Math.round(wallet.balance);
                if (currentBalance < total) {
                    showInsufficientWalletDialog(currentBalance, total);
                    return;
                }

                if (!wallet.hasPin) {
                    showSetupDevicePinDialog(currentBalance);
                } else {
                    showWalletPinDialog(currentBalance);
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(CheckoutActivity.this,
                        "Không thể kiểm tra số dư ví: " + error.getUserMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSetupDevicePinDialog(long currentBalance) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        container.setPadding(pad, dp(8), pad, 0);

        EditText etPin = new EditText(this);
        etPin.setHint("Nhập mã PIN 6 số cho Ví ETicket");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        container.addView(etPin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thiết lập mã PIN Ví ETicket")
                .setMessage("Ví của bạn có số dư " + UserFmt.amount(currentBalance)
                        + " nhưng chưa đặt mã PIN. Hãy tạo mã PIN 6 số để bảo mật giao dịch thanh toán.")
                .setView(container)
                .setPositiveButton("Lưu & Tiếp tục", null)
                .setNegativeButton("Huỷ", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPin = etPin.getText().toString().trim();
            if (!newPin.matches("\\d{6}")) {
                Toast.makeText(this, "Mã PIN phải gồm đúng 6 chữ số!", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            walletRepository.setPin(null, newPin, new ApiCallback<WalletResponse>() {
                @Override
                public void onSuccess(WalletResponse data, JsonElement meta) {
                    if (isFinishing() || isDestroyed()) return;
                    dialog.dismiss();
                    showWalletPinDialog(currentBalance);
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    if (isFinishing() || isDestroyed()) return;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(CheckoutActivity.this,
                            "❌ " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private void showInsufficientWalletDialog(long currentBalance, long orderTotal) {
        dismissActiveDialog();

        long missing = orderTotal - currentBalance;
        activeDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wallet_insufficient, null);

        TextView tvOrderTotal = dialogView.findViewById(R.id.tvOrderTotal);
        TextView tvCurrentBalance = dialogView.findViewById(R.id.tvCurrentBalance);
        TextView tvMissingAmount = dialogView.findViewById(R.id.tvMissingAmount);
        View btnTopupWallet = dialogView.findViewById(R.id.btnTopupWallet);
        View btnSwitchMethod = dialogView.findViewById(R.id.btnSwitchMethod);

        if (tvOrderTotal != null) tvOrderTotal.setText(UserFmt.amount(orderTotal));
        if (tvCurrentBalance != null) tvCurrentBalance.setText(UserFmt.amount(currentBalance));
        if (tvMissingAmount != null) tvMissingAmount.setText(UserFmt.amount(missing));

        if (btnTopupWallet instanceof TextView) {
            ((TextView) btnTopupWallet).setText("Nạp thêm " + UserFmt.amount(missing) + " vào ví");
        }
        if (btnTopupWallet != null) {
            btnTopupWallet.setOnClickListener(v -> {
                dismissActiveDialog();
                // Mở WalletActivity và điền sẵn số tiền thiếu
                WalletActivity.start(this, Math.max(10_000L, missing));
            });
        }

        if (btnSwitchMethod != null) {
            btnSwitchMethod.setOnClickListener(v -> {
                dismissActiveDialog();
                selectMethod(METHOD_VIETQR);
                Toast.makeText(this, "Đã chuyển sang phương thức Chuyển khoản VietQR", Toast.LENGTH_SHORT).show();
            });
        }

        activeDialog.setContentView(dialogView);
        activeDialog.show();
    }

    private void showWalletPinDialog(long currentBalance) {
        dismissActiveDialog();

        activeDialog = new BottomSheetDialog(this);
        DialogWalletPinBinding dialogBinding = DialogWalletPinBinding.inflate(LayoutInflater.from(this));

        dialogBinding.tvAmount.setText(UserFmt.amount(total));
        dialogBinding.tvBalance.setText("Số dư ví hiện tại: " + UserFmt.amount(currentBalance));

        dialogBinding.btnConfirmPay.setOnClickListener(v -> {
            String enteredPin = dialogBinding.etPin.getText().toString().trim();
            if (!enteredPin.matches("\\d{6}")) {
                Toast.makeText(this, "Vui lòng nhập đúng 6 chữ số mã PIN!", Toast.LENGTH_SHORT).show();
                return;
            }

            dialogBinding.progressBar.setVisibility(View.VISIBLE);
            dialogBinding.btnConfirmPay.setEnabled(false);

            ensureOrderCreated(order -> {
                long authoritativeTotal = order.totalAmount != null ? order.totalAmount.longValue() : total;
                dialogBinding.tvAmount.setText(UserFmt.amount(authoritativeTotal));
                String note = "Thanh toán vé sự kiện " + (eventTitle != null ? eventTitle : "");
                walletRepository.pay(order.orderId, (double) authoritativeTotal, note, enteredPin,
                        new ApiCallback<WalletTransactionDto>() {
                            @Override
                            public void onSuccess(WalletTransactionDto data, JsonElement meta) {
                                if (isFinishing() || isDestroyed()) return;
                                dismissActiveDialog();
                                long remaining = Math.max(0, currentBalance - authoritativeTotal);
                                Toast.makeText(CheckoutActivity.this,
                                        "✅ Đã trừ " + UserFmt.amount(authoritativeTotal)
                                                + " từ Ví ETicket! Số dư còn lại: " + UserFmt.amount(remaining),
                                        Toast.LENGTH_LONG).show();
                                loadWalletBalance();
                                String code = order.orderCode != null ? order.orderCode : fallbackOrderCode();
                                paySuccess(code);
                            }

                            @Override
                            public void onError(@NonNull ApiException error) {
                                if (isFinishing() || isDestroyed()) return;
                                dialogBinding.progressBar.setVisibility(View.GONE);
                                dialogBinding.btnConfirmPay.setEnabled(true);
                                dialogBinding.etPin.setText("");
                                Toast.makeText(CheckoutActivity.this,
                                        "❌ " + error.getUserMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }, error -> {
                dialogBinding.progressBar.setVisibility(View.GONE);
                dialogBinding.btnConfirmPay.setEnabled(true);
            });
        });

        activeDialog.setContentView(dialogBinding.getRoot());
        activeDialog.show();
    }

    // =========================================================================
    // 2. THANH TOÁN VIETQR (GỌI INITIATE + CONFIRM BACKEND, ĐẾM NGƯỢC 15 PHÚT)
    // =========================================================================

    private void handleVietQrPayment() {
        ensureOrderCreated(order -> {
            setPayButtonBusy(true, "Đang tạo mã VietQR...");
            orderRepository.initiatePayment(order.orderId,
                    new PaymentDtos.CreateRequest("BANK_TRANSFER_QR"),
                    new ApiCallback<PaymentDtos.Payment>() {
                        @Override
                        public void onSuccess(PaymentDtos.Payment payment, JsonElement meta) {
                            setPayButtonBusy(false, null);
                            showVietQrDialog(order, payment);
                        }

                        @Override
                        public void onError(@NonNull ApiException error) {
                            setPayButtonBusy(false, null);
                            Toast.makeText(CheckoutActivity.this,
                                    "Không thể khởi tạo thanh toán QR: " + error.getUserMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void showVietQrDialog(OrderDtos.Order order, @Nullable PaymentDtos.Payment payment) {
        dismissActiveDialog();
        paymentCompleted = false;

        String transferCode = (payment != null && payment.transferContent != null && !payment.transferContent.isEmpty())
                ? payment.transferContent
                : (order.orderCode != null ? order.orderCode : fallbackOrderCode());
        long payAmount = (payment != null && payment.amount != null) ? payment.amount.longValue() : total;
        String bankCode = (payment != null && payment.bankCode != null && !payment.bankCode.isEmpty())
                ? payment.bankCode : "ACB";
        String bankAcc = (payment != null && payment.bankAccount != null && !payment.bankAccount.isEmpty())
                ? payment.bankAccount : "190368688888";
        String accName = (payment != null && payment.accountName != null && !payment.accountName.trim().isEmpty())
                ? payment.accountName.trim() : "CONG TY ETICKET";

        activeDialog = new BottomSheetDialog(this);
        DialogCheckoutVietqrBinding dialogBinding = DialogCheckoutVietqrBinding.inflate(LayoutInflater.from(this));
        activeVietQrPaymentId = payment != null ? payment.paymentId : -1L;
        activeQrBinding = dialogBinding;
        activeQrOrder = order;
        activeQrTransferCode = transferCode;

        dialogBinding.tvBankName.setText(bankCode);
        dialogBinding.tvAccountName.setText(accName);
        dialogBinding.tvAccountNumber.setText(bankAcc);
        dialogBinding.tvAmount.setText(UserFmt.amount(payAmount));
        dialogBinding.tvTransferCode.setText(transferCode);
        dialogBinding.btnPayLater.setVisibility(View.VISIBLE);
        dialogBinding.progressConfirming.setVisibility(View.VISIBLE);
        dialogBinding.tvLiveBadge.setVisibility(View.VISIBLE);
        dialogBinding.tvLiveBadge.setText("● Đang kết nối Realtime (SSE)...");
        dialogBinding.tvQrStatus.setText("Mở app ngân hàng quét mã. Màn hình sẽ tự chuyển ngay khi Webhook xác nhận tiền vào.");
        dialogBinding.btnManualConfirm.setText("Kiểm tra trạng thái ngay");

        String qrUrl = "https://img.vietqr.io/image/" + Uri.encode(bankCode) + "-" + Uri.encode(bankAcc) + "-compact2.png"
                + "?amount=" + payAmount
                + "&addInfo=" + Uri.encode(transferCode)
                + "&accountName=" + Uri.encode(accName);
        Glide.with(this).load(qrUrl).into(dialogBinding.ivQrCode);

        dialogBinding.btnSaveQr.setOnClickListener(v -> saveQrImageToGallery(dialogBinding.ivQrCode, transferCode));
        dialogBinding.btnCopyAccount.setOnClickListener(v -> copyText("Số tài khoản", bankAcc));
        dialogBinding.btnCopyAmount.setOnClickListener(v -> copyText("Số tiền", String.valueOf(payAmount)));
        dialogBinding.btnCopyCode.setOnClickListener(v -> copyText("Nội dung chuyển khoản", transferCode));

        // Đếm ngược giữ chỗ 15 phút theo paymentDueAt từ Backend
        long dueMillis = parseIsoMillis(
                payment != null && payment.expiresAt != null ? payment.expiresAt : order.paymentDueAt,
                System.currentTimeMillis() + DEFAULT_WINDOW_MS);
        long remainingMs = Math.max(0L, dueMillis - System.currentTimeMillis());
        startExpiryCountdown(remainingMs > 0 ? remainingMs : DEFAULT_WINDOW_MS, dialogBinding);

        // Nút Kiểm tra ngay: CHỈ gọi GET hỏi server trạng thái thật, không tự chốt đơn từ Client
        dialogBinding.btnManualConfirm.setOnClickListener(v -> {
            if (activeVietQrPaymentId > 0) {
                dialogBinding.btnManualConfirm.setEnabled(false);
                dialogBinding.tvQrStatus.setText("Đang kiểm tra giao dịch từ ngân hàng...");
                checkOrderPaymentStatus(activeVietQrPaymentId, false);
            }
        });

        // Chế độ Dev/Test Local: gửi payload giả lập Webhook SePay/PayOS qua đường dẫn webhook thật
        if (BuildConfig.DEBUG) {
            dialogBinding.btnSimulateWebhook.setVisibility(View.VISIBLE);
            dialogBinding.btnSimulateWebhook.setOnClickListener(v -> {
                dialogBinding.btnSimulateWebhook.setEnabled(false);
                dialogBinding.btnSimulateWebhook.setText("Đang gửi Webhook SePay/PayOS (Dev)...");
                orderRepository.simulateOrderWebhook(transferCode, payAmount, new ApiCallback<PaymentDtos.Payment>() {
                    @Override
                    public void onSuccess(PaymentDtos.Payment simulated, JsonElement meta) {
                        if (!paymentCompleted) {
                            // Nếu SSE đẩy về chậm hơn hoặc đang chạy offline DemoFallback, kiểm tra kết quả ngay
                            if (simulated != null && "SUCCESS".equalsIgnoreCase(simulated.status)) {
                                onOrderPaymentConfirmed(order, transferCode, simulated.providerTransactionId);
                            } else if (activeVietQrPaymentId > 0) {
                                checkOrderPaymentStatus(activeVietQrPaymentId, true);
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (activeQrBinding != null) {
                            activeQrBinding.btnSimulateWebhook.setEnabled(true);
                            activeQrBinding.btnSimulateWebhook.setText("⚡ Giả lập Webhook ngân hàng (Dev)");
                        }
                        Toast.makeText(CheckoutActivity.this,
                                "Lỗi giả lập Webhook: " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        // Lưu đơn chờ để thanh toán sau trong Lịch sử đơn hàng
        dialogBinding.btnPayLater.setOnClickListener(v -> {
            dismissActiveDialog();
            Toast.makeText(CheckoutActivity.this,
                    "Đã lưu đơn " + transferCode + " vào mục Chờ thanh toán (giữ chỗ 15 phút).",
                    Toast.LENGTH_LONG).show();
            UserOrdersActivity.start(CheckoutActivity.this, "PENDING_PAYMENT");
            finish();
        });

        activeDialog.setContentView(dialogBinding.getRoot());
        activeDialog.setOnDismissListener(d -> {
            stopExpiryCountdown();
            stopPaymentRealtimeListeners();
        });
        activeDialog.show();

        if (activeVietQrPaymentId > 0) {
            startOrderPaymentSse(activeVietQrPaymentId, order, transferCode, dialogBinding);
            scheduleOrderFallbackPoll(activeVietQrPaymentId);
        }
    }

    private void startOrderPaymentSse(long paymentId,
                                      OrderDtos.Order order,
                                      String transferCode,
                                      DialogCheckoutVietqrBinding dialogBinding) {
        stopPaymentSseOnly();
        paymentSseSubscription = SsePaymentClient.subscribe(
                "payments/" + paymentId + "/events",
                PaymentDtos.Payment.class,
                new SsePaymentClient.Listener<PaymentDtos.Payment>() {
                    @Override
                    public void onConnected() {
                        if (!paymentCompleted && activeQrBinding == dialogBinding) {
                            dialogBinding.tvLiveBadge.setText("● Realtime SSE đang lắng nghe");
                        }
                    }

                    @Override
                    public void onEvent(@NonNull PaymentDtos.Payment updated) {
                        if (paymentCompleted) return;
                        if ("SUCCESS".equalsIgnoreCase(updated.status)) {
                            onOrderPaymentConfirmed(order, transferCode, updated.providerTransactionId);
                        } else if ("FAILED".equalsIgnoreCase(updated.status)
                                || "REVERSED".equalsIgnoreCase(updated.status)) {
                            showQrFailure(dialogBinding, updated.failureMessage);
                        }
                    }

                    @Override
                    public void onDisconnected(@Nullable Throwable error) {
                        if (!paymentCompleted && activeQrBinding == dialogBinding) {
                            dialogBinding.tvLiveBadge.setText("○ Đang tự kiểm tra dự phòng (15s)");
                        }
                    }
                });
    }

    private void scheduleOrderFallbackPoll(long paymentId) {
        stopFallbackPollingOnly();
        fallbackPollRunnable = new Runnable() {
            @Override
            public void run() {
                if (paymentCompleted || activeDialog == null || !activeDialog.isShowing() || isFinishing()) {
                    return;
                }
                checkOrderPaymentStatus(paymentId, true);
                fallbackPollHandler.postDelayed(this, 15_000L);
            }
        };
        fallbackPollHandler.postDelayed(fallbackPollRunnable, 15_000L);
    }

    private void checkOrderPaymentStatus(long paymentId, boolean silent) {
        orderRepository.getPayment(paymentId, new ApiCallback<PaymentDtos.Payment>() {
            @Override
            public void onSuccess(PaymentDtos.Payment current, JsonElement meta) {
                if (paymentCompleted || isFinishing()) return;
                if (current != null && "SUCCESS".equalsIgnoreCase(current.status)) {
                    OrderDtos.Order targetOrder = activeQrOrder != null ? activeQrOrder : activeOrder;
                    String code = activeQrTransferCode != null ? activeQrTransferCode : fallbackOrderCode();
                    onOrderPaymentConfirmed(targetOrder, code, current.providerTransactionId);
                    return;
                }
                if (current != null && ("FAILED".equalsIgnoreCase(current.status)
                        || "REVERSED".equalsIgnoreCase(current.status))) {
                    if (activeQrBinding != null) {
                        showQrFailure(activeQrBinding, current.failureMessage);
                    }
                    return;
                }
                if (activeQrBinding != null) {
                    activeQrBinding.btnManualConfirm.setEnabled(true);
                    if (!silent) {
                        activeQrBinding.tvQrStatus.setText("Ngân hàng chưa ghi nhận tiền vào. Hệ thống vẫn đang lắng nghe tự động...");
                        Toast.makeText(CheckoutActivity.this,
                                "Chưa nhận được giao dịch chuyển khoản. Vui lòng đợi vài giây sau khi chuyển.",
                                Toast.LENGTH_SHORT).show();
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

    private void showQrFailure(@NonNull DialogCheckoutVietqrBinding dialogBinding,
                               @Nullable String failureMessage) {
        stopPaymentRealtimeListeners();
        dialogBinding.progressConfirming.setVisibility(View.GONE);
        dialogBinding.tvLiveBadge.setText("✕ Giao dịch không hợp lệ");
        dialogBinding.btnManualConfirm.setEnabled(false);
        String msg = (failureMessage != null && !failureMessage.trim().isEmpty())
                ? failureMessage.trim()
                : "Giao dịch không thành công hoặc số tiền chưa khớp. Vui lòng liên hệ hỗ trợ.";
        dialogBinding.tvQrStatus.setText(msg);
        Toast.makeText(this, "❌ " + msg, Toast.LENGTH_LONG).show();
    }

    private void onOrderPaymentConfirmed(@Nullable OrderDtos.Order order,
                                         @NonNull String transferCode,
                                         @Nullable String providerTxId) {
        if (paymentCompleted || isFinishing() || isDestroyed()) {
            return;
        }
        paymentCompleted = true;
        dismissActiveDialog();
        String txSuffix = (providerTxId != null && !providerTxId.trim().isEmpty())
                ? " (Mã GD: " + providerTxId.trim() + ")"
                : "";
        Toast.makeText(CheckoutActivity.this,
                "🎉 Đã nhận thanh toán qua Webhook" + txSuffix + "! Đang đồng bộ vé điện tử...",
                Toast.LENGTH_SHORT).show();
        String finalOrderCode = (order != null && order.orderCode != null && !order.orderCode.isEmpty())
                ? order.orderCode : transferCode;
        paySuccess(finalOrderCode);
    }

    private void startExpiryCountdown(long durationMs, DialogCheckoutVietqrBinding dialogBinding) {
        stopExpiryCountdown();
        expiryTimer = new CountDownTimer(durationMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSec = millisUntilFinished / 1000L;
                long min = totalSec / 60L;
                long sec = totalSec % 60L;
                dialogBinding.tvExpiryCountdown.setText(
                        String.format(Locale.US, "Giữ chỗ vé còn: %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                dialogBinding.tvExpiryCountdown.setText("Đã hết thời gian giữ chỗ!");
                dialogBinding.btnManualConfirm.setEnabled(false);
                dialogBinding.btnSimulateWebhook.setEnabled(false);
                dialogBinding.progressConfirming.setVisibility(View.GONE);
                dialogBinding.tvQrStatus.setText("Đơn hàng đã hết hạn thanh toán. Vui lòng tạo đơn mới.");
                stopPaymentRealtimeListeners();
            }
        }.start();
    }

    private void stopExpiryCountdown() {
        if (expiryTimer != null) {
            expiryTimer.cancel();
            expiryTimer = null;
        }
    }

    private void stopPaymentSseOnly() {
        if (paymentSseSubscription != null) {
            paymentSseSubscription.close();
            paymentSseSubscription = null;
        }
    }

    private void stopFallbackPollingOnly() {
        if (fallbackPollRunnable != null) {
            fallbackPollHandler.removeCallbacks(fallbackPollRunnable);
            fallbackPollRunnable = null;
        }
    }

    private void stopPaymentRealtimeListeners() {
        stopPaymentSseOnly();
        stopFallbackPollingOnly();
        activeVietQrPaymentId = -1L;
        activeQrBinding = null;
        activeQrOrder = null;
        activeQrTransferCode = null;
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
            String fileName = "VietQR_" + code + ".jpg";

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
                    Toast.makeText(this, "✅ Đã lưu mã QR vào Thư viện ảnh!", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 3. THANH TOÁN THẺ QUỐC TẾ (SavedCardStore + LUHN CHECK + INITIATE/CONFIRM)
    // =========================================================================

    private void showCardDialog() {
        dismissActiveDialog();

        activeDialog = new BottomSheetDialog(this);
        DialogCheckoutCardBinding dialogBinding = DialogCheckoutCardBinding.inflate(LayoutInflater.from(this));

        dialogBinding.btnSubmitCard.setText("Thanh toán " + UserFmt.amount(total));

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
                dialogBinding.btnSubmitCard.setText("Xác thực thẻ mới & Thanh toán");
            } else {
                dialogBinding.layoutNewCardFields.setVisibility(View.GONE);
                dialogBinding.btnSubmitCard.setText("Thanh toán " + UserFmt.amount(total));
            }
        });

        dialogBinding.btnSubmitCard.setOnClickListener(v -> {
            int selectedId = dialogBinding.rgCardSelection.getCheckedRadioButtonId();
            String cardDesc;

            if (selectedId == R.id.rbCardMastercard && savedCards.size() > 1) {
                cardDesc = savedCards.get(1).displayLabel;
            } else if (selectedId == R.id.rbNewCard) {
                String cardNum = dialogBinding.etCardNumber.getText().toString().trim();
                String expiry = dialogBinding.etExpiry.getText().toString().trim();
                String cvv = dialogBinding.etCvv.getText().toString().trim();

                String validationError = SavedCardStore.validateCard(cardNum, expiry, cvv);
                if (validationError != null) {
                    Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
                    return;
                }
                SavedCardStore.SavedCard saved = SavedCardStore.addCard(this, cardNum, expiry);
                cardDesc = saved.displayLabel;
            } else {
                cardDesc = !savedCards.isEmpty() ? savedCards.get(0).displayLabel : "Thẻ Visa •••• 4242";
            }

            dialogBinding.btnSubmitCard.setEnabled(false);
            dialogBinding.btnSubmitCard.setText("Đang khởi tạo & xác thực 3D-Secure...");
            dialogBinding.progressBar.setVisibility(View.VISIBLE);

            final String finalDesc = cardDesc;
            ensureOrderCreated(order -> orderRepository.initiatePayment(order.orderId,
                    new PaymentDtos.CreateRequest("CARD"),
                    new ApiCallback<PaymentDtos.Payment>() {
                        @Override
                        public void onSuccess(PaymentDtos.Payment payment, JsonElement meta) {
                            if (isFinishing()) return;
                            if (payment != null && payment.paymentId > 0) {
                                orderRepository.confirmPayment(payment.paymentId, new ApiCallback<PaymentDtos.Payment>() {
                                    @Override
                                    public void onSuccess(PaymentDtos.Payment confirmed, JsonElement meta2) {
                                        dismissActiveDialog();
                                        Toast.makeText(CheckoutActivity.this,
                                                "✅ Thanh toán thành công qua " + finalDesc,
                                                Toast.LENGTH_SHORT).show();
                                        paySuccess(order.orderCode != null ? order.orderCode : fallbackOrderCode());
                                    }

                                    @Override
                                    public void onError(@NonNull ApiException error) {
                                        dialogBinding.btnSubmitCard.setEnabled(true);
                                        dialogBinding.btnSubmitCard.setText("Thanh toán " + UserFmt.amount(total));
                                        dialogBinding.progressBar.setVisibility(View.GONE);
                                        Toast.makeText(CheckoutActivity.this,
                                                "❌ " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                dialogBinding.btnSubmitCard.setEnabled(true);
                                dialogBinding.btnSubmitCard.setText("Thanh toán " + UserFmt.amount(total));
                                dialogBinding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(CheckoutActivity.this,
                                        "❌ Không thể khởi tạo giao dịch thẻ. Vui lòng thử lại!",
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(@NonNull ApiException error) {
                            dialogBinding.btnSubmitCard.setEnabled(true);
                            dialogBinding.btnSubmitCard.setText("Thanh toán " + UserFmt.amount(total));
                            dialogBinding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(CheckoutActivity.this,
                                    "❌ " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                        }
                    }));
        });

        activeDialog.setContentView(dialogBinding.getRoot());
        activeDialog.show();
    }

    private void dismissActiveDialog() {
        stopExpiryCountdown();
        stopPaymentRealtimeListeners();
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    private void initListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPay.setOnClickListener(v -> onPayClicked());
        binding.btnApplyCoupon.setOnClickListener(v -> {
            if (appliedCouponCode != null) {
                removeCoupon();
            } else {
                applyCoupon();
            }
        });
        updateCta();
    }

    private void applyCoupon() {
        String code = binding.etCouponCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã ưu đãi!", Toast.LENGTH_SHORT).show();
            return;
        }
        PromotionRepository repo = ServiceLocator.get(PromotionRepository.class);
        repo.applyCoupon(code, eventId, BigDecimal.valueOf(originalTotal),
                new ApiCallback<PromotionDtos.CouponResponse>() {
                    @Override
                    public void onSuccess(PromotionDtos.CouponResponse coupon, JsonElement meta) {
                        appliedCouponId = coupon.couponId;
                        appliedCouponCode = coupon.code;
                        discountAmount = coupon.discountAmount != null ? coupon.discountAmount.longValue() : 0L;
                        total = Math.max(0, originalTotal - discountAmount);

                        // Thay đổi mã giảm giá -> cần tạo đơn với tổng tiền mới
                        activeOrder = null;
                        checkoutIdempotencyKey = UUID.randomUUID().toString();

                        binding.layoutDiscountRow.setVisibility(View.VISIBLE);
                        binding.tvDiscountAmount.setText("- " + UserFmt.amount(discountAmount));
                        binding.tvCouponMessage.setVisibility(View.VISIBLE);
                        binding.tvCouponMessage.setTextColor(ContextCompat.getColor(CheckoutActivity.this, R.color.user_success_text));
                        binding.tvCouponMessage.setText("🎉 " + coupon.description);

                        binding.etCouponCode.setEnabled(false);
                        binding.btnApplyCoupon.setText("Gỡ mã");
                        updateCta();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        binding.tvCouponMessage.setVisibility(View.VISIBLE);
                        binding.tvCouponMessage.setTextColor(ContextCompat.getColor(CheckoutActivity.this, R.color.user_danger));
                        binding.tvCouponMessage.setText("❌ " + error.getUserMessage());
                    }
                });
    }

    private void removeCoupon() {
        appliedCouponId = null;
        appliedCouponCode = null;
        discountAmount = 0L;
        total = originalTotal;
        activeOrder = null;
        checkoutIdempotencyKey = UUID.randomUUID().toString();

        binding.etCouponCode.setEnabled(true);
        binding.etCouponCode.setText("");
        binding.btnApplyCoupon.setText("Áp dụng");
        binding.layoutDiscountRow.setVisibility(View.GONE);
        binding.tvCouponMessage.setVisibility(View.GONE);
        updateCta();
    }

    private void paySuccess(String code) {
        TicketRepository ticketRepository = ServiceLocator.get(TicketRepository.class);
        ticketRepository.getMyTickets("ALL", 0, 50, new ApiCallback<List<TicketUiModel>>() {
            @Override
            public void onSuccess(@Nullable List<TicketUiModel> data, @Nullable JsonElement meta) {
                if (isFinishing() || isDestroyed()) return;
                TicketUiModel matched = null;
                if (data != null && !data.isEmpty()) {
                    for (TicketUiModel item : data) {
                        if (item.eventId == eventId) {
                            matched = item;
                            break;
                        }
                    }
                    if (matched == null) {
                        matched = data.get(0);
                    }
                }
                showSuccess(matched, code);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (isFinishing() || isDestroyed()) return;
                showSuccess(null, code);
            }
        });
    }

    private String fallbackOrderCode() {
        long now = System.currentTimeMillis() % 1_000_000L;
        return String.format(Locale.US, "ETK%06d", now);
    }

    private void showSuccess(@Nullable TicketUiModel ticket, String displayOrderCode) {
        binding.scroll.setVisibility(View.GONE);
        binding.bottomBar.setVisibility(View.GONE);
        binding.successPanel.setVisibility(View.VISIBLE);

        binding.tvOrderCode.setText(displayOrderCode != null ? displayOrderCode : fallbackOrderCode());

        binding.btnViewTicket.setOnClickListener(v -> {
            if (ticket != null) {
                TicketDetailActivity.start(this, ticket.id, ticket.code);
            } else {
                UserOrdersActivity.start(this, "PAID");
            }
            finish();
        });

        binding.btnHome.setOnClickListener(v -> finish());
    }

    private static long parseIsoMillis(@Nullable String iso, long fallback) {
        if (iso == null || iso.trim().isEmpty()) return fallback;
        try {
            return Instant.parse(iso.trim()).toEpochMilli();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissActiveDialog();
    }
}
