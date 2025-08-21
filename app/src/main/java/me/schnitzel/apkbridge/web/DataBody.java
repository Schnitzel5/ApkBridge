package me.schnitzel.apkbridge.web;

import java.util.List;

import me.schnitzel.apkbridge.web.model.Anime;
import me.schnitzel.apkbridge.web.model.Chapter;
import me.schnitzel.apkbridge.web.model.Episode;
import me.schnitzel.apkbridge.web.filter.JFilterList;
import me.schnitzel.apkbridge.web.model.Manga;
import me.schnitzel.apkbridge.web.preference.JPreference;

public class DataBody {
    public String data;
    public String method;
    public int page;
    public String search;
    public Manga mangaData;
    public Chapter chapterData;
    public Anime animeData;
    public Episode episodeData;
    public List<JFilterList> filterList;
    public List<JPreference> preferences;

    public DataBody() {
    }
}
