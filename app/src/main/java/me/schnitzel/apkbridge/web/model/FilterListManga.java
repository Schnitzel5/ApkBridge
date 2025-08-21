package me.schnitzel.apkbridge.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

import eu.kanade.tachiyomi.source.model.FilterList;

public class FilterListManga extends FilterList {
    @JsonCreator
    public FilterListManga(List<FilterManga<?>> list) {
        super.setList(list);
    }
}
