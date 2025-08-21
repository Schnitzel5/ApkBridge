package me.schnitzel.apkbridge.web.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.kanade.tachiyomi.source.model.SChapter;

public class Chapter implements SChapter {
    private String url;
    private String name;
    private long dateUpload;
    private float chapterNumber;
    private String scanlator;

    public Chapter() {}

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
    public float getChapter_number() {
        return chapterNumber;
    }

    @Override
    public void setChapter_number(float v) {
        chapterNumber = v;
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
    public void copyFrom(@NonNull SChapter other) {
        url = other.getUrl();
        name = other.getName();
        dateUpload = other.getDate_upload();
        chapterNumber = other.getChapter_number();
        scanlator = other.getScanlator();
    }
}
