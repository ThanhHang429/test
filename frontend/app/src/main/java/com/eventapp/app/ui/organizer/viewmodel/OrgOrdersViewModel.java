package com.eventapp.app.ui.organizer.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.organizer.data.OrgDemo;
import com.eventapp.app.ui.organizer.model.OrgOrder;

import java.util.List;

/** Đơn hàng nhà tổ chức (FR-O04). */
public class OrgOrdersViewModel extends ViewModel {
    public List<OrgOrder> orders() {
        return OrgDemo.orders();
    }
}
