package me.schnitzel.apkbridge.web.model;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.kanade.tachiyomi.animesource.model.AnimeFilter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterAnime<T> extends AnimeFilter<T> {
    @JsonCreator
    public FilterAnime(@NonNull @JsonProperty("name") String name, @JsonProperty("state") T state) {
        super(name, state);
    }
}
