package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.wallet.TopupIntentResponse;
import com.eventapp.app.data.remote.dto.wallet.WalletResponse;
import com.eventapp.app.data.remote.dto.wallet.WalletTransactionDto;
import com.eventapp.app.data.repository.WalletRepository;
import com.google.gson.JsonElement;

import java.util.List;

/** ViewModel quản lý dữ liệu Ví ETicket, TopupIntent + Webhook/SSE và lịch sử giao dịch. */
public class WalletViewModel extends ViewModel {

    private final WalletRepository repository;

    private final MutableLiveData<WalletResponse> walletLive = new MutableLiveData<>();
    private final MutableLiveData<List<WalletTransactionDto>> transactionsLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLive = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    public WalletViewModel() {
        this.repository = ServiceLocator.get(WalletRepository.class);
    }

    public LiveData<WalletResponse> getWallet() {
        return walletLive;
    }

    public LiveData<List<WalletTransactionDto>> getTransactions() {
        return transactionsLive;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLive;
    }

    public LiveData<String> getError() {
        return errorLive;
    }

    public LiveData<String> getMessage() {
        return messageLive;
    }

    /** Tải thông tin ví của user */
    public void loadWallet() {
        loadingLive.setValue(true);
        repository.getMyWallet(new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse data, JsonElement meta) {
                loadingLive.setValue(false);
                walletLive.setValue(data);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingLive.setValue(false);
                errorLive.setValue("Không thể tải thông tin ví: " + error.getUserMessage());
            }
        });
    }

    /** Tải danh sách lịch sử giao dịch ví */
    public void loadTransactions() {
        repository.getTransactions(new ApiCallback<List<WalletTransactionDto>>() {
            @Override
            public void onSuccess(List<WalletTransactionDto> data, JsonElement meta) {
                transactionsLive.setValue(data);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                // Không chặn giao diện nếu lịch sử chưa có
            }
        });
    }

    /** Kích hoạt tạo ví mới */
    public void createWallet(Runnable onComplete) {
        loadingLive.setValue(true);
        repository.createWallet(new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse data, JsonElement meta) {
                loadingLive.setValue(false);
                walletLive.setValue(data);
                messageLive.setValue("🎉 Kích hoạt Ví ETicket thành công! Số dư: 0 đ");
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingLive.setValue(false);
                errorLive.setValue(error.getUserMessage());
            }
        });
    }

    /** Thiết lập hoặc đổi mã PIN ví 6 số trên Backend (FR-U13). */
    public void setPin(@androidx.annotation.Nullable String oldPin,
                       @NonNull String newPin,
                       @NonNull ApiCallback<WalletResponse> callback) {
        loadingLive.setValue(true);
        repository.setPin(oldPin, newPin, new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse data, JsonElement meta) {
                loadingLive.setValue(false);
                if (data != null) {
                    walletLive.setValue(data);
                }
                callback.onSuccess(data, meta);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingLive.setValue(false);
                callback.onError(error);
            }
        });
    }

    /** Tạo yêu cầu nạp ví VietQR (PENDING) để chờ Webhook + SSE đối soát. */
    public void createTopupIntent(double amount, String note,
                                  @NonNull ApiCallback<TopupIntentResponse> callback) {
        loadingLive.setValue(true);
        Runnable proceed = () -> repository.createTopupIntent(amount, "BANK_TRANSFER_QR", note,
                new ApiCallback<TopupIntentResponse>() {
                    @Override
                    public void onSuccess(TopupIntentResponse data, JsonElement meta) {
                        loadingLive.setValue(false);
                        callback.onSuccess(data, meta);
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        loadingLive.setValue(false);
                        callback.onError(error);
                    }
                });

        if (walletLive.getValue() == null) {
            repository.createWallet(new ApiCallback<WalletResponse>() {
                @Override
                public void onSuccess(WalletResponse createdWallet, JsonElement meta) {
                    walletLive.setValue(createdWallet);
                    proceed.run();
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    proceed.run();
                }
            });
        } else {
            proceed.run();
        }
    }

    public void checkTopupStatus(long topupId, @NonNull ApiCallback<TopupIntentResponse> callback) {
        repository.getTopupIntent(topupId, callback);
    }

    public void simulateTopupWebhook(@NonNull String transferContent, long amount,
                                     @NonNull ApiCallback<TopupIntentResponse> callback) {
        repository.simulateTopupWebhook(transferContent, amount, callback);
    }

    /** Nạp tiền vào ví qua Thẻ đã xác thực 3D-Secure */
    public void topup(double amount, String method, String note, Runnable onComplete) {
        loadingLive.setValue(true);
        if (walletLive.getValue() == null) {
            repository.createWallet(new ApiCallback<WalletResponse>() {
                @Override
                public void onSuccess(WalletResponse createdWallet, JsonElement meta) {
                    walletLive.setValue(createdWallet);
                    executeBackendTopup(amount, method, note, onComplete);
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    executeBackendTopup(amount, method, note, onComplete);
                }
            });
        } else {
            executeBackendTopup(amount, method, note, onComplete);
        }
    }

    private void executeBackendTopup(double amount, String method, String note, Runnable onComplete) {
        repository.topup(amount, method, note, new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse data, JsonElement meta) {
                loadingLive.setValue(false);
                walletLive.setValue(data);
                messageLive.setValue("✅ Nạp tiền vào ví thành công!");
                loadTransactions();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingLive.setValue(false);
                errorLive.setValue(error.getUserMessage());
            }
        });
    }
}
