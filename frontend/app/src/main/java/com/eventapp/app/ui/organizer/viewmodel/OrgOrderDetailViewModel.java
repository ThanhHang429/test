package com.eventapp.app.ui.organizer.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.organizer.data.OrgDemo;
import com.eventapp.app.ui.organizer.model.OrgOrder;

/** Chi tiết đơn hàng (FR-O04). */
public class OrgOrderDetailViewModel extends ViewModel {
    public OrgOrder findOrder(String code) {
        return OrgDemo.findOrder(code);
    }
}
