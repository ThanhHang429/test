package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminFinanceBinding;
import com.google.android.material.snackbar.Snackbar;

/** Tab Tài chính: escrow, cấu hình phí, hoàn tiền và tra cứu giao dịch. */
public class AdminFinanceFragment extends Fragment {

    private FragmentAdminFinanceBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminFinanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.etFinanceLookup.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Snackbar.make(binding.getRoot(), R.string.admin_fin_invoice_opened,
                        Snackbar.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
