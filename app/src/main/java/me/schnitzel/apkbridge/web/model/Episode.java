package me.schnitzel.apkbridge.web.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.kanade.tachiyomi.animesource.model.SEpisode;

public class Episode implements SEpisode {
    private String url;
    private String name;
    private long dateUpload;
    private float episodeNumber;
    private String scanlator;

    public Episode() {
    }

    @NonNull
    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(@NonNull String s) {
        url = s;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NonNull String s) {
        name = s;
    }

    @Override
    public long getDate_upload() {
        return dateUpload;
    }

    @Override
    public void setDate_upload(long l) {
        dateUpload = l;
    }

    @Override
    public float getEpisode_number() {
        return episodeNumber;
    }

    @Override
    public void setEpisode_number(float v) {
        episodeNumber = v;
    }

    @Nullable
    @Override
    public String getScanlator() {
        return scanlator;
    }

    @Override
    public void setScanlator(@Nullable String s) {
        scanlator = s;
    }

    @Override
    public void copyFrom(@NonNull SEpisode other) {
        url = other.getUrl();
        name = other.getName();
        dateUpload = other.getDate_upload();
        episodeNumber = other.getEpisode_number();
        scanlator = other.getScanlator();
    }
}
