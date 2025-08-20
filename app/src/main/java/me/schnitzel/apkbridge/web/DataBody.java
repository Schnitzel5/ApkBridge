package me.schnitzel.apkbridge.web;

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList;
import eu.kanade.tachiyomi.source.model.FilterList;

public class DataBody {
    public String data;
    public String method;
    public int page;
    public String search;
    public Manga mangaData;
    public Chapter chapterData;
    public Anime animeData;
    public Episode episodeData;
    public FilterList filterList;
    public AnimeFilterList animeFilterList;

    public DataBody() {
    }
}
