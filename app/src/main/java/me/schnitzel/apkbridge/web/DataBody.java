package me.schnitzel.apkbridge.web;

import java.util.Optional;

import eu.kanade.tachiyomi.animesource.model.SAnime;
import eu.kanade.tachiyomi.animesource.model.SEpisode;
import eu.kanade.tachiyomi.source.model.SChapter;
import eu.kanade.tachiyomi.source.model.SManga;

public class DataBody {
    public String data;
    public String method;
    public int page;
    public String search;
    public Optional<SManga> mangaData;
    public Optional<SChapter> chapterData;
    public Optional<SAnime> animeData;
    public Optional<SEpisode> episodeData;

    public DataBody() {
    }
}
