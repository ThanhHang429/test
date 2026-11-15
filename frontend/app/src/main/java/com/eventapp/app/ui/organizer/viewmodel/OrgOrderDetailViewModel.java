package com.eventapp.app.ui.organizer.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.repository.OrderRepository;
import com.google.gson.JsonElement;

/** Chi tiết đơn hàng nhà tổ chức (FR-O17). */
public class OrgOrderDetailViewModel extends ViewModel {

    private final OrderRepository repository;
    private final MutableLiveData<OrderDtos.Order> orderLive = new MutableLiveData<>();
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    public OrgOrderDetailViewModel() {
        this.repository = ServiceLocator.get(OrderRepository.class);
    }

    public LiveData<OrderDtos.Order> getOrder() {
        return orderLive;
    }

    public LiveData<String> getError() {
        return errorLive;
    }

    public void consumeError() {
        errorLive.setValue(null);
    }

    public void load(@NonNull String orderCode) {
        repository.getOrganizerOrderDetail(orderCode, new ApiCallback<OrderDtos.Order>() {
            @Override
            public void onSuccess(OrderDtos.Order data, JsonElement meta) {
                orderLive.setValue(data);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                errorLive.setValue(error.getUserMessage());
            }
        });
    }
}
