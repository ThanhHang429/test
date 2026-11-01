package com.eventapp.app.ui.user.order;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.fake.DemoOrder;
import com.eventapp.app.ui.fake.SampleOrderFactory;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

/**
 * FR-U09: đăng ký vé miễn phí.
 *
 * <p>Vé miễn phí được phát hành ngay trong dữ liệu demo, không đi qua bước thanh toán và không
 * gọi API.</p>
 */
public class FreeRegisterFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";
    public static final String ARG_EVENT_TITLE = "arg_event_title";

    private static final int STEP_INDEX = 0;
    private static final long FAKE_PROCESSING_MS = 450L;

    private EventUiModel event;
    private SampleOrderFactory.TicketOption freeTicket;
    private int quantity = 1;
    private boolean submitting;

    private NestedScrollView scrollView;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextView quantityView;
    private TextView quantityHint;
    private TextView summaryLabel;
    private TextView termsError;
    private MaterialCheckBox termsCheck;
    private ImageButton minusButton;
    private ImageButton plusButton;
    private MaterialButton confirmButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_order_free_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        long eventId = args == null ? -1L : args.getLong(ARG_EVENT_ID, -1L);
        String fallbackTitle = args == null ? "" : args.getString(ARG_EVENT_TITLE, "");
        event = SampleOrderFactory.findEvent(eventId);

        scrollView = view.findViewById(R.id.scrollFreeRegister);
        view.findViewById(R.id.btnFreeRegisterBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        if (event == null) {
            showMissingEvent(view, fallbackTitle);
            return;
        }

        OrderUiUtils.bindSteps(view, STEP_INDEX);
        bindEventCard(view);
        bindHeaders(view);
        bindForm(view);
        bindStepper(view);
        bindFreeTicket(view);

        NavBarSpacing.padBottomForNavBar(view, scrollView, 96);
        NavBarSpacing.marginBottomForNavBar(view,
                view.findViewById(R.id.layoutFreeRegisterBottomBar), 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        submitting = false;
        setConfirmLoading(false);
    }

    private void bindEventCard(View view) {
        ((TextView) view.findViewById(R.id.tvFreeRegisterDay))
                .setText(OrderUiUtils.dayOfMonth(event.startAt));
        ((TextView) view.findViewById(R.id.tvFreeRegisterMonth))
                .setText(OrderUiUtils.monthLabel(event.startAt));
        ((TextView) view.findViewById(R.id.tvFreeRegisterEventTitle)).setText(event.title);
        ((TextView) view.findViewById(R.id.tvFreeRegisterEventVenue))
                .setText(OrderUiUtils.venueLine(event));
        ((TextView) view.findViewById(R.id.tvFreeRegisterEventDate))
                .setText(OrderUiUtils.eventDate(event.startAt));
    }

    private void bindHeaders(View view) {
        ((SectionHeaderView) view.findViewById(R.id.headerFreeRegisterAttendee))
                .setSubtitle(R.string.hc_freereg_attendee_hint);
        ((SectionHeaderView) view.findViewById(R.id.headerFreeRegisterSummary))
                .setTitle(R.string.hc_freereg_summary);
    }

    private void bindForm(View view) {
        nameLayout = view.findViewById(R.id.layoutFreeRegisterName);
        emailLayout = view.findViewById(R.id.layoutFreeRegisterEmail);
        phoneLayout = view.findViewById(R.id.layoutFreeRegisterPhone);
        nameInput = view.findViewById(R.id.inputFreeRegisterName);
        emailInput = view.findViewById(R.id.inputFreeRegisterEmail);
        phoneInput = view.findViewById(R.id.inputFreeRegisterPhone);
        termsCheck = view.findViewById(R.id.checkFreeRegisterTerms);
        termsError = view.findViewById(R.id.tvFreeRegisterTermsError);
        confirmButton = view.findViewById(R.id.btnFreeRegisterConfirm);

        OrderUiUtils.prefillAccount(requireContext(), nameInput, emailInput, phoneInput);
        OrderFormUtils.clearErrorOnType(nameInput, nameLayout);
        OrderFormUtils.clearErrorOnType(emailInput, emailLayout);
        OrderFormUtils.clearErrorOnType(phoneInput, phoneLayout);
        termsCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                termsError.setVisibility(View.GONE);
            }
        });
        confirmButton.setOnClickListener(v -> submit());
    }

    private void bindStepper(View view) {
        quantityView = view.findViewById(R.id.tvFreeRegisterQuantity);
        quantityHint = view.findViewById(R.id.tvFreeRegisterQuantityHint);
        summaryLabel = view.findViewById(R.id.tvFreeRegisterSummaryLabel);
        minusButton = view.findViewById(R.id.btnFreeRegisterMinus);
        plusButton = view.findViewById(R.id.btnFreeRegisterPlus);

        minusButton.setOnClickListener(v -> changeQuantity(-1));
        plusButton.setOnClickListener(v -> changeQuantity(1));
    }

    private void bindFreeTicket(View view) {
        List<SampleOrderFactory.TicketOption> options = SampleOrderFactory.ticketOptions(event);
        if (options.isEmpty()) {
            showEmptyTickets(view);
            return;
        }
        freeTicket = options.get(0);
        StatusChip chip = view.findViewById(R.id.chipFreeRegisterTicket);
        chip.setStatus(getString(R.string.hc_freereg_free), StatusChip.Tone.SUCCESS);
        updateQuantity();
    }

    private void changeQuantity(int delta) {
        int max = maxQuantity();
        int next = Math.max(1, Math.min(max, quantity + delta));
        if (next == quantity) {
            if (delta > 0 && isAdded()) {
                Snackbar.make(requireView(),
                                getString(R.string.hc_freereg_quantity_limit, max),
                                Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.layoutFreeRegisterBottomBar)
                        .show();
            }
            return;
        }
        quantity = next;
        updateQuantity();
    }

    private int maxQuantity() {
        if (freeTicket == null) {
            return 1;
        }
        int available = freeTicket.remaining > 0 ? freeTicket.remaining : 1;
        return Math.max(1, Math.min(freeTicket.maxPerOrder, available));
    }

    private void updateQuantity() {
        int max = maxQuantity();
        quantityView.setText(String.valueOf(quantity));
        quantityHint.setText(getString(R.string.hc_freereg_quantity_limit, max));
        summaryLabel.setText(getString(R.string.hc_freereg_summary_ticket, quantity));
        minusButton.setEnabled(quantity > 1);
        minusButton.setAlpha(quantity > 1 ? 1f : 0.35f);
        plusButton.setEnabled(quantity < max);
        plusButton.setAlpha(quantity < max ? 1f : 0.35f);
    }

    private void submit() {
        if (submitting || freeTicket == null || event == null) {
            return;
        }
        if (!validateForm()) {
            return;
        }
        hideKeyboard();
        DemoOrder order = SampleOrderFactory.createFreeOrder(event, quantity,
                OrderFormUtils.textOf(nameInput),
                OrderFormUtils.textOf(emailInput),
                OrderFormUtils.textOf(phoneInput));

        Bundle args = new Bundle();
        args.putSerializable(OrderStatusFragment.ARG_ORDER, order);
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.free_registerFragment, true)
                .build();
        submitting = true;
        setConfirmLoading(true);
        requireView().postDelayed(() -> {
            if (!isAdded() || !submitting) {
                return;
            }
            Navigation.findNavController(requireView())
                    .navigate(R.id.order_statusFragment, args, options);
        }, FAKE_PROCESSING_MS);
    }

    private boolean validateForm() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        clearErrorEnabled(nameLayout);
        clearErrorEnabled(emailLayout);
        clearErrorEnabled(phoneLayout);
        termsError.setVisibility(View.GONE);

        String name = OrderFormUtils.textOf(nameInput);
        String email = OrderFormUtils.textOf(emailInput);
        String phone = OrderFormUtils.textOf(phoneInput);
        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.hc_checkout_name_required));
            valid = false;
        }
        if (!OrderFormUtils.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.hc_checkout_email_invalid));
            valid = false;
        }
        if (!OrderFormUtils.isValidPhone(phone)) {
            phoneLayout.setError(getString(R.string.hc_checkout_phone_invalid));
            valid = false;
        }
        if (!termsCheck.isChecked()) {
            termsError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) {
            View target;
            if (name.isEmpty()) {
                target = nameInput;
            } else if (!OrderFormUtils.isValidEmail(email)) {
                target = emailInput;
            } else if (!OrderFormUtils.isValidPhone(phone)) {
                target = phoneInput;
            } else {
                target = termsCheck;
            }
            target.requestFocus();
            Snackbar.make(requireView(), R.string.hc_checkout_form_invalid,
                    Snackbar.LENGTH_SHORT).show();
        }
        return valid;
    }

    private void clearErrorEnabled(TextInputLayout layout) {
        if (layout != null && layout.getError() == null) {
            layout.setErrorEnabled(false);
        }
    }

    private void setConfirmLoading(boolean loading) {
        if (confirmButton == null) {
            return;
        }
        confirmButton.setEnabled(!loading);
        confirmButton.setText(loading
                ? R.string.hc_order_processing : R.string.hc_freereg_action);
    }

    private void hideKeyboard() {
        View focused = requireActivity().getCurrentFocus();
        if (focused == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void showMissingEvent(View view, String fallbackTitle) {
        view.findViewById(R.id.scrollFreeRegister).setVisibility(View.GONE);
        view.findViewById(R.id.layoutFreeRegisterBottomBar).setVisibility(View.GONE);
        EmptyStateView empty = view.findViewById(R.id.emptyFreeRegister);
        empty.setTitle(R.string.hc_error_title);
        empty.setMessage(fallbackTitle == null || fallbackTitle.isEmpty()
                ? getString(R.string.hc_order_missing)
                : getString(R.string.hc_order_missing_event, fallbackTitle));
        empty.setIcon(R.drawable.ic_ds_error);
        empty.setActionText(R.string.hc_status_home_action);
        empty.setOnActionClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.homeFragment));
        empty.setVisibility(View.VISIBLE);
    }

    private void showEmptyTickets(View view) {
        showMissingEvent(view, "");
        EmptyStateView empty = view.findViewById(R.id.emptyFreeRegister);
        empty.setTitle(R.string.hc_error_title);
        empty.setMessage(R.string.hc_checkout_no_tickets);
    }
}
