package me.schnitzel.apkbridge.web;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.kanade.tachiyomi.source.model.Filter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterManga<T> extends Filter<T> {
    @JsonCreator
    public FilterManga(@NonNull @JsonProperty("name") String name, @JsonProperty("state") T state) {
        super(name, state);
    }
}
