package com.eventapp.app.ui.user.order;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.DemoQrView;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.fake.DemoOrder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.Locale;

/**
 * FR-U11: thanh toán bằng VietQR demo.
 *
 * <p>Mã QR, thời gian giữ chỗ và trạng thái giao dịch đều là mô phỏng giao diện. Người dùng có thể
 * xác nhận đã chuyển khoản hoặc hủy đơn để xem trạng thái tương ứng.</p>
 */
public class PaymentQrFragment extends Fragment {

    public static final String ARG_ORDER = "arg_order";

    private static final long HOLD_DURATION_MS = 10L * 60L * 1000L;
    private static final long FAKE_PROCESSING_MS = 500L;

    private DemoOrder order;
    private CountDownTimer countdown;
    private boolean expired;
    private boolean navigating;

    private NestedScrollView scrollView;
    private LinearLayout holdCard;
    private View holdIconFrame;
    private ImageView holdIcon;
    private TextView holdTitle;
    private TextView holdMessage;
    private TextView countdownView;
    private TextView orderSubtitle;
    private TextView ticketLine;
    private TextView contentView;
    private LinearLayout listeningView;
    private DemoQrView qrView;
    private StatusChip statusChip;
    private MaterialButton confirmButton;
    private MaterialButton retryButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_order_payment_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollView = view.findViewById(R.id.scrollPayment);
        view.findViewById(R.id.btnPaymentBack).setOnClickListener(v -> showCancelDialog());

        order = readOrder(getArguments());
        if (order == null) {
            showMissingOrder(view);
            return;
        }

        OrderUiUtils.bindSteps(view, 1);
        bindContent(view);
        bindCopyActions(view);
        NavBarSpacing.padBottomForNavBar(view, scrollView, 140);
        NavBarSpacing.marginBottomForNavBar(view,
                view.findViewById(R.id.layoutPaymentBottomBar), 0);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showCancelDialog();
                    }
                });

        startCountdown();
    }

    private void bindContent(View view) {
        holdCard = view.findViewById(R.id.cardPaymentHold);
        holdIconFrame = view.findViewById(R.id.framePaymentHoldIcon);
        holdIcon = view.findViewById(R.id.ivPaymentHoldIcon);
        holdTitle = view.findViewById(R.id.tvPaymentHoldTitle);
        holdMessage = view.findViewById(R.id.tvPaymentHoldMessage);
        countdownView = view.findViewById(R.id.tvPaymentCountdown);
        orderSubtitle = view.findViewById(R.id.tvPaymentOrderSubtitle);
        ticketLine = view.findViewById(R.id.tvPaymentTicketLine);
        contentView = view.findViewById(R.id.tvPaymentContent);
        listeningView = view.findViewById(R.id.layoutPaymentListening);
        qrView = view.findViewById(R.id.qrPayment);
        statusChip = view.findViewById(R.id.chipPaymentStatus);
        confirmButton = view.findViewById(R.id.btnPaymentConfirm);
        retryButton = view.findViewById(R.id.btnPaymentRetry);

        orderSubtitle.setText(getString(R.string.hc_pay_subtitle, order.code));
        ticketLine.setText(getString(R.string.hc_status_ticket_count,
                order.ticketName, order.quantity));
        statusChip.setStatus(getString(R.string.hc_pay_status_pending),
                StatusChip.Tone.WARNING);
        contentView.setText(getString(R.string.hc_pay_content_value, order.code));

        qrView.setPayload("ETICKET|" + order.code + "|" + order.total);
        qrView.setContentDescription(getString(R.string.hc_pay_qr_desc, order.code));

        PriceTextView amount = view.findViewById(R.id.tvPaymentAmount);
        amount.setPrice(order.total);

        confirmButton.setOnClickListener(v -> confirmPaid());
        retryButton.setOnClickListener(v -> startCountdown());
        view.findViewById(R.id.btnPaymentCancel).setOnClickListener(v -> showCancelDialog());
    }

    private void bindCopyActions(View view) {
        view.findViewById(R.id.btnCopyPaymentAccount).setOnClickListener(v ->
                copyAndNotify(getString(R.string.hc_pay_account_value)));
        view.findViewById(R.id.btnCopyPaymentAmount).setOnClickListener(v ->
                copyAndNotify(PriceTextView.formatVnd(order.total)));
        view.findViewById(R.id.btnCopyPaymentContent).setOnClickListener(v ->
                copyAndNotify(contentView.getText().toString()));
    }

    private void copyAndNotify(String value) {
        OrderUiUtils.copyText(requireContext(), value);
        Snackbar.make(requireView(), R.string.hc_pay_copied_short, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.layoutPaymentBottomBar)
                .show();
    }

    private void startCountdown() {
        stopCountdown();
        expired = false;
        applyHoldState(false);
        countdownView.setText(formatRemaining(HOLD_DURATION_MS));
        countdown = new CountDownTimer(HOLD_DURATION_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (countdownView == null) {
                    return;
                }
                countdownView.setText(formatRemaining(millisUntilFinished));
                countdownView.setTextColor(ContextCompat.getColor(requireContext(),
                        millisUntilFinished <= 60_000L
                                ? R.color.ds_danger : R.color.ds_warning_dark));
            }

            @Override
            public void onFinish() {
                markExpired();
            }
        }.start();
    }

    private void stopCountdown() {
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
    }

    private void markExpired() {
        expired = true;
        stopCountdown();
        countdownView.setText(formatRemaining(0L));
        countdownView.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_danger));
        holdCard.setBackgroundResource(R.drawable.bg_ds_error_soft);
        holdIconFrame.setBackgroundResource(R.drawable.bg_ds_circle_danger);
        holdIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.ds_danger_dark));
        holdTitle.setText(R.string.hc_pay_hold_expired_title);
        holdMessage.setText(R.string.hc_pay_hold_expired_message);
        retryButton.setVisibility(View.VISIBLE);
        listeningView.setVisibility(View.GONE);
        statusChip.setStatus(getString(R.string.hc_pay_status_expired),
                StatusChip.Tone.DANGER);
        confirmButton.setEnabled(false);
        confirmButton.setText(R.string.hc_pay_expired_action);
    }

    private void applyHoldState(boolean isExpired) {
        holdCard.setBackgroundResource(isExpired
                ? R.drawable.bg_ds_error_soft : R.drawable.bg_ds_warning_soft);
        holdIconFrame.setBackgroundResource(isExpired
                ? R.drawable.bg_ds_circle_danger : R.drawable.bg_ds_circle_warning);
        holdIcon.setColorFilter(ContextCompat.getColor(requireContext(),
                isExpired ? R.color.ds_danger_dark : R.color.ds_warning_dark));
        holdTitle.setText(isExpired
                ? R.string.hc_pay_hold_expired_title : R.string.hc_pay_hold_title);
        holdMessage.setText(isExpired
                ? R.string.hc_pay_hold_expired_message : R.string.hc_pay_hold_message);
        retryButton.setVisibility(isExpired ? View.VISIBLE : View.GONE);
        listeningView.setVisibility(isExpired ? View.GONE : View.VISIBLE);
        statusChip.setStatus(getString(isExpired
                        ? R.string.hc_pay_status_expired : R.string.hc_pay_status_pending),
                isExpired ? StatusChip.Tone.DANGER : StatusChip.Tone.WARNING);
        confirmButton.setEnabled(!isExpired);
        confirmButton.setText(isExpired
                ? R.string.hc_pay_expired_action : R.string.hc_pay_confirm);
        countdownView.setTextColor(ContextCompat.getColor(requireContext(),
                isExpired ? R.color.ds_danger : R.color.ds_warning_dark));
    }

    private String formatRemaining(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.US, "%02d:%02d",
                totalSeconds / 60L, totalSeconds % 60L);
    }

    private void confirmPaid() {
        if (navigating) {
            return;
        }
        if (expired) {
            Snackbar.make(requireView(), R.string.hc_pay_hold_expired_message,
                    Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.layoutPaymentBottomBar)
                    .show();
            return;
        }
        confirmButton.setEnabled(false);
        confirmButton.setText(R.string.hc_order_processing);
        requireView().postDelayed(() -> {
            if (isAdded() && !navigating) {
                navigateToStatus(order.withState(DemoOrder.State.PAID));
            }
        }, FAKE_PROCESSING_MS);
    }

    private void showCancelDialog() {
        if (navigating || !isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hc_pay_cancel_title)
                .setMessage(R.string.hc_pay_cancel_message)
                .setNegativeButton(R.string.hc_pay_cancel_keep, null)
                .setPositiveButton(R.string.hc_pay_cancel_confirm,
                        (dialog, which) -> cancelOrder())
                .show();
    }

    private void cancelOrder() {
        if (navigating) {
            return;
        }
        navigateToStatus(order.withState(DemoOrder.State.CANCELLED));
    }

    private void navigateToStatus(DemoOrder nextOrder) {
        if (navigating) {
            return;
        }
        navigating = true;
        stopCountdown();
        Bundle args = new Bundle();
        args.putSerializable(OrderStatusFragment.ARG_ORDER, nextOrder);
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.payment_qrFragment, true)
                .build();
        Navigation.findNavController(requireView())
                .navigate(R.id.order_statusFragment, args, options);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private DemoOrder readOrder(@Nullable Bundle args) {
        if (args == null) {
            return null;
        }
        Serializable value = args.getSerializable(ARG_ORDER);
        return value instanceof DemoOrder ? (DemoOrder) value : null;
    }

    private void showMissingOrder(View view) {
        view.findViewById(R.id.scrollPayment).setVisibility(View.GONE);
        view.findViewById(R.id.layoutPaymentBottomBar).setVisibility(View.GONE);
        EmptyStateView empty = view.findViewById(R.id.emptyPayment);
        empty.setTitle(R.string.hc_error_title);
        empty.setMessage(R.string.hc_order_missing);
        empty.setIcon(R.drawable.ic_ds_error);
        empty.setActionText(R.string.hc_status_home_action);
        empty.setOnActionClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.homeFragment));
        empty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        stopCountdown();
        super.onDestroyView();
    }
}
