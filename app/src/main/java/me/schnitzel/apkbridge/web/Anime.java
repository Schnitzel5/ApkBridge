package me.schnitzel.apkbridge.web;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import eu.kanade.tachiyomi.animesource.model.SAnime;
import eu.kanade.tachiyomi.source.model.UpdateStrategy;

public class Anime implements SAnime {
    private String url;
    private String title;
    private String artist;
    private String author;
    private String description;
    private String genre;
    private int status;
    private String thumbnailUrl;
    private UpdateStrategy updateStrategy;
    private boolean initialized;

    public Anime() {
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
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(@NonNull String s) {
        title = s;
    }

    @Nullable
    @Override
    public String getArtist() {
        return artist;
    }

    @Override
    public void setArtist(@Nullable String s) {
        artist = s;
    }

    @Nullable
    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public void setAuthor(@Nullable String s) {
        author = s;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(@Nullable String s) {
        description = s;
    }

    @Nullable
    @Override
    public String getGenre() {
        return genre;
    }

    @Override
    public void setGenre(@Nullable String s) {
        genre = s;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int i) {
        status = i;
    }

    @Nullable
    @Override
    public String getThumbnail_url() {
        return thumbnailUrl;
    }

    @Override
    public void setThumbnail_url(@Nullable String s) {
        thumbnailUrl = s;
    }

    @NonNull
    @Override
    public UpdateStrategy getUpdate_strategy() {
        return updateStrategy;
    }

    @Override
    public void setUpdate_strategy(@NonNull UpdateStrategy updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    @Override
    public boolean getInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean b) {
        initialized = b;
    }

    @Nullable
    @Override
    public List<String> getGenres() {
        return List.of();
    }

    @Override
    public void copyFrom(@NonNull SAnime other) {
        url = other.getUrl();
        title = other.getTitle();
        artist = other.getArtist();
        author = other.getAuthor();
        description = other.getDescription();
        genre = other.getGenre();
        status = other.getStatus();
        thumbnailUrl = other.getThumbnail_url();
        updateStrategy = other.getUpdate_strategy();
        initialized = other.getInitialized();
    }

    @NonNull
    @Override
    public SAnime copy() {
        Anime anime = new Anime();
        anime.copyFrom(this);
        return anime;
    }
}
