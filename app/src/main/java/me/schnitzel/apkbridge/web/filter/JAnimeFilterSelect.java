package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;

import eu.kanade.tachiyomi.animesource.model.AnimeFilter;

public class JAnimeFilterSelect<T> extends AnimeFilter.Select<T> {
    public JAnimeFilterSelect(@NonNull String name, @NonNull T[] values, int state) {
        super(name, values, state);
    }
}
