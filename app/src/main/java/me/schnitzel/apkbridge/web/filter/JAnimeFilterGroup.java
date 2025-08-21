package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;

import java.util.List;

import eu.kanade.tachiyomi.animesource.model.AnimeFilter;

public class JAnimeFilterGroup<T> extends AnimeFilter.Group<T> {
    public JAnimeFilterGroup(@NonNull String name, @NonNull List<? extends T> state) {
        super(name, state);
    }

    public static class JFilterCheckbox extends AnimeFilter.CheckBox {
        public JFilterCheckbox(@NonNull String name, boolean state) {
            super(name, state);
        }
    }

    public static class JFilterTriState extends AnimeFilter.TriState {
        public JFilterTriState(@NonNull String name, int state) {
            super(name, state);
        }
    }
}
