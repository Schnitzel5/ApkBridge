package me.schnitzel.apkbridge.web.filter;

import androidx.annotation.NonNull;

import eu.kanade.tachiyomi.animesource.model.AnimeFilter;

public class JAnimeFilterText extends AnimeFilter.Text {
    public JAnimeFilterText(@NonNull String name, @NonNull String state) {
        super(name, state);
    }
}
