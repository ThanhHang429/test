package com.eventapp.app.ui.model;

/** Guidance entry (FAQ) for FR-A09; used as the policy source of the future AI assistant. */
public class FaqUiModel {

    public final long id;
    public final String title;
    public final String content;
    public final boolean visible;
    public final int sortOrder;

    public FaqUiModel(long id, String title, String content, boolean visible, int sortOrder) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.visible = visible;
        this.sortOrder = sortOrder;
    }

    public FaqUiModel withContent(String nextTitle, String nextContent, boolean nextVisible) {
        return new FaqUiModel(id, nextTitle, nextContent, nextVisible, sortOrder);
    }
}
