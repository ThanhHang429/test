package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.ui.host.SessionRouter;

import java.util.List;

/**
 * FR-U02: đăng nhập dev bằng tài khoản seed.
 *
 * <p>Không có email/mật khẩu. Danh sách lấy từ {@code GET /dev/accounts}; chọn một dòng sẽ đổi
 * {@code userId} thành JWT thật qua {@code POST /dev/token}. Nhờ vậy các màn còn lại dùng đúng
 * quyền của backend.</p>
 */
public class LoginFragment extends Fragment {

    private LoginViewModel viewModel;
    private DevAccountAdapter adapter;
    private ProgressBar progress;
    private TextView errorView;
    private View retryButton;
    private View quickGroup;
    private boolean navigated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        progress = view.findViewById(R.id.progressAccounts);
        errorView = view.findViewById(R.id.textAccountsError);
        retryButton = view.findViewById(R.id.btnRetryAccounts);
        quickGroup = view.findViewById(R.id.groupQuickRoles);

        RecyclerView list = view.findViewById(R.id.listDevAccounts);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DevAccountAdapter(account -> {
            if (account != null) {
                viewModel.login(account);
            }
        });
        list.setAdapter(adapter);

        retryButton.setOnClickListener(v -> viewModel.loadAccounts(true));
        bindQuickRole(view.findViewById(R.id.rowDemoUser), "USER");
        bindQuickRole(view.findViewById(R.id.rowDemoOrganizer), "ORGANIZER");
        bindQuickRole(view.findViewById(R.id.rowDemoAdmin), "ADMIN");

        viewModel.getAccounts().observe(getViewLifecycleOwner(), accounts -> {
            adapter.submit(accounts);
            boolean empty = accounts == null || accounts.isEmpty();
            if (empty && !Boolean.TRUE.equals(viewModel.getLoading().getValue())) {
                showError(getString(R.string.auth_account_empty));
            }
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            retryButton.setVisibility(!busy && errorView.getVisibility() == View.VISIBLE
                    ? View.VISIBLE : View.GONE);
            if (busy) {
                errorView.setVisibility(View.GONE);
            }
        });
        viewModel.getSubmitting().observe(getViewLifecycleOwner(), submitting -> {
            boolean busy = Boolean.TRUE.equals(submitting);
            quickGroup.setAlpha(busy ? 0.55f : 1f);
            quickGroup.setEnabled(!busy);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), exception -> {
            if (exception == null) {
                return;
            }
            String message = exception.getUserMessage();
            if (viewModel.getAccounts().getValue() == null
                    || viewModel.getAccounts().getValue().isEmpty()) {
                showError(message);
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
            viewModel.consumeError();
        });
        viewModel.getLoggedIn().observe(getViewLifecycleOwner(), this::openHost);

        viewModel.loadAccounts(false);
    }

    private void bindQuickRole(@Nullable View view, @NonNull String role) {
        if (view == null) {
            return;
        }
        view.setOnClickListener(v -> viewModel.loginAsRole(role));
    }

    private void showError(@NonNull String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void openHost(@Nullable AuthSession session) {
        if (session == null || navigated) {
            return;
        }
        navigated = true;
        SessionRouter.openHost(requireContext(), session);
        requireActivity().finish();
    }
}
