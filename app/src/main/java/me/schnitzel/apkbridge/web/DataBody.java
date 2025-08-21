package me.schnitzel.apkbridge.web;

import me.schnitzel.apkbridge.web.model.Anime;
import me.schnitzel.apkbridge.web.model.Chapter;
import me.schnitzel.apkbridge.web.model.Episode;
import me.schnitzel.apkbridge.web.model.FilterListAnime;
import me.schnitzel.apkbridge.web.model.FilterListManga;
import me.schnitzel.apkbridge.web.model.Manga;

public class DataBody {
    public String data;
    public String method;
    public int page;
    public String search;
    public Manga mangaData;
    public Chapter chapterData;
    public Anime animeData;
    public Episode episodeData;
    public FilterListManga filterListManga;
    public FilterListAnime filterListAnime;

    public DataBody() {
    }
}
