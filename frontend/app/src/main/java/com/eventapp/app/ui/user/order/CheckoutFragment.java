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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.fake.DemoOrder;
import com.eventapp.app.ui.fake.SampleOrderFactory;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

/**
 * FR-U10: chọn loại vé, số lượng và thông tin người nhận vé.
 *
 * <p>Dữ liệu sự kiện và vé lấy từ {@link SampleOrderFactory}; đơn hàng chỉ được tạo trong bộ nhớ
 * để chuyển sang màn thanh toán, chưa gọi API.</p>
 */
public class CheckoutFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";
    public static final String ARG_EVENT_TITLE = "arg_event_title";

    private static final int STEP_INDEX = 0;
    private static final long FAKE_PROCESSING_MS = 450L;

    private EventUiModel event;
    private List<SampleOrderFactory.TicketOption> ticketOptions = Collections.emptyList();
    private SampleOrderFactory.TicketOption selectedTicket;
    private int quantity = 1;
    private boolean submitting;

    private TicketOptionAdapter ticketAdapter;
    private NestedScrollView scrollView;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextView quantityView;
    private TextView quantityHint;
    private TextView subtotalLabel;
    private PriceTextView subtotalView;
    private PriceTextView totalView;
    private PriceTextView bottomTotalView;
    private ImageButton minusButton;
    private ImageButton plusButton;
    private MaterialButton continueButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_order_checkout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        long eventId = args == null ? -1L : args.getLong(ARG_EVENT_ID, -1L);
        String fallbackTitle = args == null ? "" : args.getString(ARG_EVENT_TITLE, "");
        event = SampleOrderFactory.findEvent(eventId);

        scrollView = view.findViewById(R.id.scrollCheckout);
        bindToolbar(view);
        if (event == null) {
            showMissingEvent(view, fallbackTitle);
            return;
        }

        bindStepBar(view);
        bindEventCard(view);
        bindHeaderCopy(view);
        bindForm(view);
        bindSummary(view);
        bindStepper(view);
        bindTickets(view);

        NavBarSpacing.padBottomForNavBar(view, scrollView, 96);
        NavBarSpacing.marginBottomForNavBar(view,
                view.findViewById(R.id.layoutCheckoutBottomBar), 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        submitting = false;
        setContinueLoading(false);
    }

    private void bindToolbar(View view) {
        view.findViewById(R.id.btnCheckoutBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
    }

    private void bindStepBar(View view) {
        OrderUiUtils.bindSteps(view, STEP_INDEX);
    }

    private void bindEventCard(View view) {
        ((TextView) view.findViewById(R.id.tvCheckoutDay))
                .setText(OrderUiUtils.dayOfMonth(event.startAt));
        ((TextView) view.findViewById(R.id.tvCheckoutMonth))
                .setText(OrderUiUtils.monthLabel(event.startAt));
        ((TextView) view.findViewById(R.id.tvCheckoutEventTitle)).setText(event.title);
        ((TextView) view.findViewById(R.id.tvCheckoutEventVenue))
                .setText(OrderUiUtils.venueLine(event));
        ((TextView) view.findViewById(R.id.tvCheckoutEventDate))
                .setText(OrderUiUtils.eventDate(event.startAt));
    }

    private void bindHeaderCopy(View view) {
        SectionHeaderView tickets = view.findViewById(R.id.headerCheckoutTickets);
        tickets.setTitle(R.string.hc_checkout_choose_ticket);
        tickets.setSubtitle(R.string.hc_checkout_choose_ticket_hint);

        SectionHeaderView buyer = view.findViewById(R.id.headerCheckoutBuyer);
        buyer.setTitle(R.string.hc_checkout_buyer_title);
        buyer.setSubtitle(R.string.hc_checkout_buyer_hint);

        ((SectionHeaderView) view.findViewById(R.id.headerCheckoutPayment))
                .setTitle(R.string.hc_checkout_payment_method);
        ((SectionHeaderView) view.findViewById(R.id.headerCheckoutSummary))
                .setTitle(R.string.hc_checkout_summary);
    }

    private void bindForm(View view) {
        nameLayout = view.findViewById(R.id.layoutCheckoutName);
        emailLayout = view.findViewById(R.id.layoutCheckoutEmail);
        phoneLayout = view.findViewById(R.id.layoutCheckoutPhone);
        nameInput = view.findViewById(R.id.inputCheckoutName);
        emailInput = view.findViewById(R.id.inputCheckoutEmail);
        phoneInput = view.findViewById(R.id.inputCheckoutPhone);

        OrderUiUtils.prefillAccount(requireContext(), nameInput, emailInput, phoneInput);
        OrderFormUtils.clearErrorOnType(nameInput, nameLayout);
        OrderFormUtils.clearErrorOnType(emailInput, emailLayout);
        OrderFormUtils.clearErrorOnType(phoneInput, phoneLayout);

        continueButton = view.findViewById(R.id.btnCheckoutContinue);
        continueButton.setOnClickListener(v -> submit());
    }

    private void bindSummary(View view) {
        subtotalLabel = view.findViewById(R.id.tvCheckoutSubtotalLabel);
        subtotalView = view.findViewById(R.id.tvCheckoutSubtotal);
        totalView = view.findViewById(R.id.tvCheckoutTotal);
        bottomTotalView = view.findViewById(R.id.tvCheckoutBottomTotal);
    }

    private void bindStepper(View view) {
        quantityView = view.findViewById(R.id.tvCheckoutQuantity);
        quantityHint = view.findViewById(R.id.tvCheckoutQuantityHint);
        minusButton = view.findViewById(R.id.btnCheckoutMinus);
        plusButton = view.findViewById(R.id.btnCheckoutPlus);

        minusButton.setOnClickListener(v -> changeQuantity(-1));
        plusButton.setOnClickListener(v -> changeQuantity(1));
    }

    private void bindTickets(View view) {
        ticketOptions = SampleOrderFactory.ticketOptions(event);
        if (ticketOptions.isEmpty()) {
            showEmptyTickets(view);
            return;
        }

        ticketAdapter = new TicketOptionAdapter();
        ticketAdapter.setOnTicketSelectedListener(position -> selectTicket(position, true));
        RecyclerView recyclerView = view.findViewById(R.id.rvCheckoutTickets);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(ticketAdapter);
        ticketAdapter.submit(ticketOptions, 0);
        selectTicket(0, false);
    }

    private void selectTicket(int position, boolean fromUser) {
        if (position < 0 || position >= ticketOptions.size()) {
            return;
        }
        selectedTicket = ticketOptions.get(position);
        quantity = 1;
        if (ticketAdapter != null) {
            ticketAdapter.setSelectedIndex(position);
        }
        if (quantityView != null) {
            updateQuantityControls();
            updateSummary();
        }
        if (fromUser && isAdded()) {
            Snackbar.make(requireView(), selectedTicket.name, Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.layoutCheckoutBottomBar)
                    .show();
        }
    }

    private void changeQuantity(int delta) {
        int max = maxQuantity();
        int next = Math.max(1, Math.min(max, quantity + delta));
        if (next == quantity) {
            if (delta > 0 && isAdded()) {
                Snackbar.make(requireView(),
                                getString(R.string.hc_checkout_quantity_limit, max),
                                Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.layoutCheckoutBottomBar)
                        .show();
            }
            return;
        }
        quantity = next;
        updateQuantityControls();
        updateSummary();
    }

    private int maxQuantity() {
        if (selectedTicket == null) {
            return 1;
        }
        int available = selectedTicket.remaining > 0 ? selectedTicket.remaining : 1;
        return Math.max(1, Math.min(selectedTicket.maxPerOrder, available));
    }

    private void updateQuantityControls() {
        int max = maxQuantity();
        quantityView.setText(String.valueOf(quantity));
        quantityHint.setText(getString(R.string.hc_checkout_quantity_limit, max));
        minusButton.setEnabled(quantity > 1);
        minusButton.setAlpha(quantity > 1 ? 1f : 0.35f);
        plusButton.setEnabled(quantity < max);
        plusButton.setAlpha(quantity < max ? 1f : 0.35f);
    }

    private void updateSummary() {
        if (selectedTicket == null) {
            return;
        }
        long subtotal = selectedTicket.price * quantity;
        subtotalLabel.setText(getString(R.string.hc_checkout_subtotal, quantity));
        subtotalView.setPrice(subtotal);
        totalView.setPrice(subtotal);
        bottomTotalView.setPrice(subtotal);
    }

    private void submit() {
        if (submitting || selectedTicket == null || event == null) {
            return;
        }
        if (!validateForm()) {
            return;
        }
        hideKeyboard();
        String name = OrderFormUtils.textOf(nameInput);
        String email = OrderFormUtils.textOf(emailInput);
        String phone = OrderFormUtils.textOf(phoneInput);
        DemoOrder order = SampleOrderFactory.createPendingOrder(event, selectedTicket, quantity,
                name, email, phone);

        Bundle args = new Bundle();
        args.putSerializable(PaymentQrFragment.ARG_ORDER, order);
        submitting = true;
        setContinueLoading(true);
        requireView().postDelayed(() -> {
            if (!isAdded() || !submitting) {
                return;
            }
            Navigation.findNavController(requireView())
                    .navigate(R.id.payment_qrFragment, args);
        }, FAKE_PROCESSING_MS);
    }

    private boolean validateForm() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        clearErrorEnabled(nameLayout);
        clearErrorEnabled(emailLayout);
        clearErrorEnabled(phoneLayout);

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
        if (!valid) {
            View target = !name.isEmpty() ? (!OrderFormUtils.isValidEmail(email)
                    ? emailInput : phoneInput) : nameInput;
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

    private void setContinueLoading(boolean loading) {
        if (continueButton == null) {
            return;
        }
        continueButton.setEnabled(!loading);
        continueButton.setText(loading
                ? R.string.hc_order_processing : R.string.hc_checkout_continue);
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
        view.findViewById(R.id.scrollCheckout).setVisibility(View.GONE);
        view.findViewById(R.id.layoutCheckoutBottomBar).setVisibility(View.GONE);
        EmptyStateView empty = view.findViewById(R.id.emptyCheckout);
        empty.setTitle(R.string.hc_error_title);
        empty.setMessage(fallbackTitle == null || fallbackTitle.isEmpty()
                ? getString(R.string.hc_order_missing)
                : getString(R.string.hc_order_missing_event, fallbackTitle));
        empty.setIcon(R.drawable.ic_ds_error);
        empty.setActionText(R.string.hc_status_home_action);
        empty.setOnActionClickListener(v -> goHome(v));
        empty.setVisibility(View.VISIBLE);
    }

    private void showEmptyTickets(View view) {
        showMissingEvent(view, "");
        EmptyStateView empty = view.findViewById(R.id.emptyCheckout);
        empty.setTitle(R.string.hc_error_title);
        empty.setMessage(R.string.hc_checkout_no_tickets);
    }

    private void goHome(View view) {
        Navigation.findNavController(view).navigate(R.id.homeFragment);
    }
}
