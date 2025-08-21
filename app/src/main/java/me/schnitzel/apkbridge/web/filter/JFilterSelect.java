package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;

import eu.kanade.tachiyomi.source.model.Filter;

public class JFilterSelect<T> extends Filter.Select<T> {
    public JFilterSelect(@NonNull String name, @NonNull T[] values, int state) {
        super(name, values, state);
    }
}
