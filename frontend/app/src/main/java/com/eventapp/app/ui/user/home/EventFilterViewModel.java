package com.eventapp.app.ui.user.home;

import androidx.lifecycle.ViewModel;

/** Draft filter state shared across EventFilterFragment configuration changes. */
public class EventFilterViewModel extends ViewModel {

    private EventFilterCriteria criteria;

    public EventFilterCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(EventFilterCriteria criteria) {
        this.criteria = criteria == null ? null : criteria.copy();
    }

    public void reset() {
        if (criteria != null) {
            criteria.reset();
        }
    }

    public boolean hasCriteria() {
        return criteria != null;
    }
}
