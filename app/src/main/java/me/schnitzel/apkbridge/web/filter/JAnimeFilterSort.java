package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.kanade.tachiyomi.animesource.model.AnimeFilter;

public class JAnimeFilterSort extends AnimeFilter.Sort {
    public JAnimeFilterSort(@NonNull String name, @NonNull String[] values, @Nullable Selection state) {
        super(name, values, state);
    }
}
