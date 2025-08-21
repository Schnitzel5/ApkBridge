package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;

import eu.kanade.tachiyomi.source.model.Filter;

public class JFilterText extends Filter.Text {
    public JFilterText(@NonNull String name, @NonNull String state) {
        super(name, state);
    }
}
