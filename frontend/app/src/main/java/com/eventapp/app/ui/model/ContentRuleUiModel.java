package com.eventapp.app.ui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Banned keyword or suspicious domain for FR-A09 / FR-A07. */
public class ContentRuleUiModel {

    public enum Kind {
        KEYWORD,
        DOMAIN
    }

    public enum MatchType {
        CONTAINS,
        WORD
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public final long id;
    public final String term;
    public final Kind kind;
    public final MatchType matchType;
    public final List<String> scopes;
    public final Severity severity;
    public final boolean active;

    public ContentRuleUiModel(long id, String term, Kind kind, MatchType matchType,
                              List<String> scopes, Severity severity, boolean active) {
        this.id = id;
        this.term = term;
        this.kind = kind;
        this.matchType = matchType;
        this.scopes = scopes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(scopes));
        this.severity = severity;
        this.active = active;
    }

    public ContentRuleUiModel withActive(boolean nextActive) {
        return new ContentRuleUiModel(id, term, kind, matchType, scopes, severity, nextActive);
    }

    public String scopesLabel() {
        StringBuilder builder = new StringBuilder();
        for (String scope : scopes) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(scope);
        }
        return builder.toString();
    }
}
