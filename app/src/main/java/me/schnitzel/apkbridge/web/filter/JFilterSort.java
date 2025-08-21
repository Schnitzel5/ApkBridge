package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.kanade.tachiyomi.source.model.Filter;

public class JFilterSort extends Filter.Sort {
    public JFilterSort(@NonNull String name, @NonNull String[] values, @Nullable Selection state) {
        super(name, values, state);
    }
}
