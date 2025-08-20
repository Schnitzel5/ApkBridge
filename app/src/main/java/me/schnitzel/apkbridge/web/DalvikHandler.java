package me.schnitzel.apkbridge.web;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import dalvik.system.DexClassLoader;
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource;
import eu.kanade.tachiyomi.animesource.AnimeSource;
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory;
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList;
import eu.kanade.tachiyomi.source.CatalogueSource;
import eu.kanade.tachiyomi.source.MangaSource;
import eu.kanade.tachiyomi.source.SourceFactory;
import eu.kanade.tachiyomi.source.model.FilterList;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import me.schnitzel.apkbridge.MainActivityKt;

public class DalvikHandler extends RouterNanoHTTPD.GeneralHandler {
    private final String ANIME_PACKAGE = "tachiyomi.animeextension";
    private final String MANGA_PACKAGE = "tachiyomi.extension";
    private final String XX_METADATA_SOURCE_CLASS = ".class";

    private final PackageManager pm;

    public DalvikHandler() {
        pm = MainActivityKt.pm;
    }

    @Override
    public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            try {
                final Map<String, String> body = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                session.parseBody(body);
                body.forEach((k, v) -> System.out.println(k + " - " + v));
                String postData = body.get("postData");
                DataBody data = mapper.readValue(postData, DataBody.class);
                File file = File.createTempFile("ext", ".apk");
                file.setWritable(true);
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    outputStream.write(Base64.getDecoder().decode(data.data));
                }
                file.setReadOnly();
                DexClassLoader loader = load(file);
                try (NanoHTTPD.Response response = resolve(loader, file, data, mapper)) {
                    return response;
                } finally {
                    file.delete();
                }
            } catch (IOException | NanoHTTPD.ResponseException | InterruptedException e) {
                System.err.println(e.getMessage());
                return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "");
            }
        }
        return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "");
    }

    protected NanoHTTPD.Response resolve(DexClassLoader classLoader, File file, DataBody data, ObjectMapper mapper) throws InterruptedException {
        switch (data.method) {
            case "supportLatestManga":
                return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getSupportsLatest()), mapper);
            case "getPopularManga":
                return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getPopularManga(data.page, continuation)), mapper);
            case "getLatestManga":
                return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getLatestUpdates(data.page, continuation)), mapper);
            case "getSearchManga":
                return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getSearchManga(data.page, data.search, new FilterList(), continuation)), mapper);
            case "getDetailsManga":
                if (data.mangaData != null) {
                    return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getMangaDetails(data.mangaData, continuation)), mapper);
                }
                break;
            case "getChapterList":
                if (data.mangaData != null) {
                    return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getChapterList(data.mangaData, continuation)), mapper);
                }
                break;
            case "getPageList":
                if (data.chapterData != null) {
                    return buildResponse(invokeMangaSource(classLoader, file, (catalogueSource, continuation) -> catalogueSource.getPageList(data.chapterData, continuation)), mapper);
                }
                break;
            case "supportLatestAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getSupportsLatest()), mapper);
            case "getPopularAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getPopularAnime(data.page, continuation)), mapper);
            case "getLatestAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getLatestUpdates(data.page, continuation)), mapper);
            case "getSearchAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getSearchAnime(data.page, data.search, new AnimeFilterList(), continuation)), mapper);
            case "getDetailsAnime":
                if (data.animeData != null) {
                    return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getAnimeDetails(data.animeData, continuation)), mapper);
                }
                break;
            case "getEpisodeList":
                if (data.animeData != null) {
                    return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getEpisodeList(data.animeData, continuation)), mapper);
                }
                break;
            case "getVideoList":
                if (data.episodeData != null) {
                    return buildResponse(invokeAnimeSource(classLoader, file, (animeCatalogueSource, continuation) -> animeCatalogueSource.getVideoList(data.episodeData, continuation)), mapper);
                }
                break;
        }
        return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "");
    }

    protected NanoHTTPD.Response buildResponse(Optional<? extends Object> result, ObjectMapper mapper) {
        return result.map(obj -> {
            try {
                return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", mapper.writeValueAsString(obj));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).orElse(newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT, ""));
    }

    protected <T> Optional<T> invokeMangaSource(DexClassLoader classLoader, File file, BiFunction<CatalogueSource, Continuation<? super T>, ?> callback) {
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_CONFIGURATIONS | PackageManager.GET_META_DATA);
        if (info != null && info.applicationInfo != null) {
            System.out.println(info.applicationInfo.metaData.toString());
            return Arrays.stream(info.applicationInfo.metaData.getString(MANGA_PACKAGE + XX_METADATA_SOURCE_CLASS).split(";")).map(s -> {
                String sourceClass = s.trim();
                if (sourceClass.startsWith(".")) {
                    return info.packageName + sourceClass;
                }
                return sourceClass;
            }).findFirst().map(sourceClass -> {
                try {
                    Object obj = Class.forName(sourceClass, false, classLoader).getDeclaredConstructor().newInstance();
                    if (obj instanceof MangaSource) {
                        return List.of((MangaSource) obj);
                    } else if (obj instanceof SourceFactory) {
                        return ((SourceFactory) obj).createSources();
                    } else {
                        throw new Exception("Unknown source class type! " + obj.getClass());
                    }
                } catch (Exception e) {
                    System.err.println("Error loading " + sourceClass + ": " + e.getMessage());
                }
                return null;
            }).filter(sources -> !sources.isEmpty()).map(sources -> (CatalogueSource) sources.get(0)).map(src -> {
                T result;
                try {
                    result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> callback.apply(src, continuation));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return result;
            });
        }
        return Optional.empty();
    }

    protected <T> Optional<T> invokeAnimeSource(DexClassLoader classLoader, File file, BiFunction<AnimeCatalogueSource, Continuation<? super T>, ?> callback) {
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_CONFIGURATIONS | PackageManager.GET_META_DATA);
        if (info != null && info.applicationInfo != null) {
            System.out.println(info.applicationInfo.metaData.toString());
            return Arrays.stream(info.applicationInfo.metaData.getString(ANIME_PACKAGE + XX_METADATA_SOURCE_CLASS).split(";")).map(s -> {
                String sourceClass = s.trim();
                if (sourceClass.startsWith(".")) {
                    return info.packageName + sourceClass;
                }
                return sourceClass;
            }).findFirst().map(sourceClass -> {
                try {
                    Object obj = Class.forName(sourceClass, false, classLoader).getDeclaredConstructor().newInstance();
                    if (obj instanceof AnimeSource) {
                        return List.of((AnimeSource) obj);
                    } else if (obj instanceof AnimeSourceFactory) {
                        return ((AnimeSourceFactory) obj).createSources();
                    } else {
                        throw new Exception("Unknown source class type! " + obj.getClass());
                    }
                } catch (Exception e) {
                    System.err.println("Error loading " + sourceClass + ": " + e.getMessage());
                }
                return null;
            }).filter(sources -> !sources.isEmpty()).map(sources -> (AnimeCatalogueSource) sources.get(0)).map(src -> {
                T result;
                try {
                    result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> callback.apply(src, continuation));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return result;
            });
        }
        return Optional.empty();
    }

    protected DexClassLoader load(File file) throws IOException {
        return new DexClassLoader(file.getAbsolutePath(), null, null, this.getClass().getClassLoader());
    }
}
