package com.eventapp.app.ui.auth;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;

import java.util.ArrayList;
import java.util.List;

/** Danh sách tài khoản seed để chọn khi đăng nhập (chỉ có ở backend profile dev). */
public class DevAccountAdapter extends RecyclerView.Adapter<DevAccountAdapter.AccountHolder> {

    public interface Listener {
        void onAccountSelected(@NonNull DevAccountDto account);
    }

    private final List<DevAccountDto> items = new ArrayList<>();
    private final Listener listener;

    public DevAccountAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@Nullable List<DevAccountDto> accounts) {
        items.clear();
        if (accounts != null) {
            items.addAll(accounts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dev_account, parent, false);
        return new AccountHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountHolder holder, int position) {
        DevAccountDto account = items.get(position);
        String email = account.email == null ? ("#" + account.userId) : account.email;
        holder.email.setText(email);
        holder.roles.setText(describe(holder.itemView, account));
        holder.itemView.setContentDescription(
                holder.itemView.getContext().getString(R.string.auth_cd_account_item, email));
        holder.itemView.setOnClickListener(v -> listener.onAccountSelected(account));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String describe(@NonNull View view, @NonNull DevAccountDto account) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(account.fullName)) {
            builder.append(account.fullName);
        }
        if (account.roles != null) {
            for (String role : account.roles) {
                if (builder.length() > 0) {
                    builder.append(" · ");
                }
                builder.append(labelOf(view, role));
            }
        }
        return builder.toString();
    }

    private static String labelOf(@NonNull View view, @Nullable String role) {
        if (role == null) {
            return "";
        }
        switch (role) {
            case "ADMIN":
                return view.getContext().getString(R.string.auth_role_admin);
            case "ORGANIZER":
                return view.getContext().getString(R.string.auth_role_organizer);
            default:
                return view.getContext().getString(R.string.auth_role_user);
        }
    }

    static final class AccountHolder extends RecyclerView.ViewHolder {

        final TextView email;
        final TextView roles;

        AccountHolder(@NonNull View itemView) {
            super(itemView);
            email = itemView.findViewById(R.id.tvAccountEmail);
            roles = itemView.findViewById(R.id.tvAccountRoles);
        }
    }
}
