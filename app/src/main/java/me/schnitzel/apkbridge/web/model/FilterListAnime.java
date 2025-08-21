package me.schnitzel.apkbridge.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList;

public class FilterListAnime extends AnimeFilterList {
    @JsonCreator
    public FilterListAnime(List<FilterAnime<?>> list) {
        super.setList(list);
    }
}

