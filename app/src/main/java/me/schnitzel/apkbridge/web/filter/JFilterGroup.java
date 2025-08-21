package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;

import java.util.List;

import eu.kanade.tachiyomi.source.model.Filter;

public class JFilterGroup<T> extends Filter.Group<T> {
    public JFilterGroup(@NonNull String name, @NonNull List<? extends T> state) {
        super(name, state);
    }

    public static class JFilterCheckbox extends Filter.CheckBox {
        public JFilterCheckbox(@NonNull String name, boolean state) {
            super(name, state);
        }
    }

    public static class JFilterTriState extends Filter.TriState {
        public JFilterTriState(@NonNull String name, int state) {
            super(name, state);
        }
    }
}
