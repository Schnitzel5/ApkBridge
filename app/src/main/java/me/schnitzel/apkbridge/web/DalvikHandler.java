package me.schnitzel.apkbridge.web;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import dalvik.system.DexClassLoader;
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource;
import eu.kanade.tachiyomi.animesource.AnimeSource;
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory;
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource;
import eu.kanade.tachiyomi.animesource.model.AnimeFilter;
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList;
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource;
import eu.kanade.tachiyomi.source.CatalogueSource;
import eu.kanade.tachiyomi.source.ConfigurableSource;
import eu.kanade.tachiyomi.source.MangaSource;
import eu.kanade.tachiyomi.source.SourceFactory;
import eu.kanade.tachiyomi.source.model.Filter;
import eu.kanade.tachiyomi.source.model.FilterList;
import eu.kanade.tachiyomi.source.online.HttpSource;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import me.schnitzel.apkbridge.MainActivity;
import me.schnitzel.apkbridge.MainActivityKt;
import keiyoushi.utils.PreferencesKt;
import me.schnitzel.apkbridge.web.filter.JFilterList;
import me.schnitzel.apkbridge.web.preference.JCheckBoxPreference;
import me.schnitzel.apkbridge.web.preference.JEditTextPreference;
import me.schnitzel.apkbridge.web.preference.JListPreference;
import me.schnitzel.apkbridge.web.preference.JMultiSelectListPreference;
import me.schnitzel.apkbridge.web.preference.JPreference;
import me.schnitzel.apkbridge.web.preference.JSwitchPreference;

public class DalvikHandler extends RouterNanoHTTPD.GeneralHandler {
    private static final String ANIME_PACKAGE = "tachiyomi.animeextension";
    private static final String MANGA_PACKAGE = "tachiyomi.extension";
    private static final String XX_METADATA_SOURCE_CLASS = ".class";

    private final PackageManager pm;

    public DalvikHandler() {
        pm = MainActivityKt.pm;
    }

    @Override
    public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            File file = null;
            try {
                final Map<String, String> body = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                session.parseBody(body);
                body.forEach((k, v) -> System.out.println(k + " - " + v));
                String postData = body.get("postData");
                DataBody data = mapper.readValue(postData, DataBody.class);
                file = File.createTempFile("ext", ".apk");
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
            } finally {
                if (file != null && file.exists()) {
                    file.delete();
                }
            }
        }
        return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "");
    }

    protected NanoHTTPD.Response resolve(DexClassLoader classLoader, File file, DataBody data, ObjectMapper mapper) throws InterruptedException {
        switch (data.method) {
            case "headersManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> {
                    if (catalogueSource instanceof HttpSource) {
                        return ((HttpSource) catalogueSource).getHeaders().getNamesAndValues$okhttp();
                    }
                    return List.of();
                }), mapper);
            case "filtersManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getFilterList()), mapper);
            case "supportLatestManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getSupportsLatest()), mapper);
            case "getPopularManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getPopularManga(data.page, continuation)), mapper);
            case "getLatestManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getLatestUpdates(data.page, continuation)), mapper);
            case "getSearchManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> {
                    FilterList filterList = data.filterList != null ? convertFilterListManga(catalogueSource.getFilterList(), data.filterList) : catalogueSource.getFilterList();
                    return catalogueSource.getSearchManga(data.page, data.search, filterList, continuation);
                }), mapper);
            case "getDetailsManga":
                if (data.mangaData != null) {
                    return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getMangaDetails(data.mangaData, continuation)), mapper);
                }
                break;
            case "getChapterList":
                if (data.mangaData != null) {
                    return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getChapterList(data.mangaData, continuation)), mapper);
                }
                break;
            case "getPageList":
                if (data.chapterData != null) {
                    return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> catalogueSource.getPageList(data.chapterData, continuation)), mapper);
                }
                break;
            case "preferencesManga":
                return buildResponse(invokeMangaSource(classLoader, file, data, (catalogueSource, continuation) -> {
                    PreferenceManager preferenceManager = MainActivityKt.preferenceManager;
                    MainActivity instance = MainActivityKt.instance;
                    if (preferenceManager != null && instance != null && catalogueSource instanceof ConfigurableSource) {
                        PreferenceScreen screen = preferenceManager.createPreferenceScreen(instance.getApplicationContext());
                        ((ConfigurableSource) catalogueSource).setupPreferenceScreen(screen);
                        List<JPreference> preferences = new ArrayList<>();
                        processPreferences(screen, preferences);
                        return preferences;
                    }
                    return Map.of();
                }), mapper);
            case "headersAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> {
                    if (animeCatalogueSource instanceof AnimeHttpSource) {
                        return ((AnimeHttpSource) animeCatalogueSource).getHeaders().getNamesAndValues$okhttp();
                    }
                    return List.of();
                }), mapper);
            case "filtersAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getFilterList()), mapper);
            case "supportLatestAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getSupportsLatest()), mapper);
            case "getPopularAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getPopularAnime(data.page, continuation)), mapper);
            case "getLatestAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getLatestUpdates(data.page, continuation)), mapper);
            case "getSearchAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> {
                    AnimeFilterList animeFilterList = data.filterList != null ? convertFilterListAnime(animeCatalogueSource.getFilterList(), data.filterList) : animeCatalogueSource.getFilterList();
                    return animeCatalogueSource.getSearchAnime(data.page, data.search, animeFilterList, continuation);
                }), mapper);
            case "getDetailsAnime":
                if (data.animeData != null) {
                    return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getAnimeDetails(data.animeData, continuation)), mapper);
                }
                break;
            case "getEpisodeList":
                if (data.animeData != null) {
                    return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getEpisodeList(data.animeData, continuation)), mapper);
                }
                break;
            case "getVideoList":
                if (data.episodeData != null) {
                    return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> animeCatalogueSource.getVideoList(data.episodeData, continuation)), mapper);
                }
                break;
            case "preferencesAnime":
                return buildResponse(invokeAnimeSource(classLoader, file, data, (animeCatalogueSource, continuation) -> {
                    PreferenceManager preferenceManager = MainActivityKt.preferenceManager;
                    MainActivity instance = MainActivityKt.instance;
                    if (preferenceManager != null && instance != null && animeCatalogueSource instanceof ConfigurableAnimeSource) {
                        PreferenceScreen screen = preferenceManager.createPreferenceScreen(instance.getApplicationContext());
                        ((ConfigurableAnimeSource) animeCatalogueSource).setupPreferenceScreen(screen);
                        List<JPreference> preferences = new ArrayList<>();
                        processPreferences(screen, preferences);
                        return preferences;
                    }
                    return Map.of();
                }), mapper);
        }
        return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "");
    }

    protected NanoHTTPD.Response buildResponse(Optional<? extends Object> result, ObjectMapper mapper) {
        return result.map(obj -> {
            try {
                return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", mapper.writeValueAsString(obj));
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "");
            }
        }).orElse(newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT, ""));
    }

    protected <T> Optional<T> invokeMangaSource(DexClassLoader classLoader, File file, DataBody data, BiFunction<CatalogueSource, Continuation<? super T>, ?> callback) {
        if (pm == null) {
            return Optional.empty();
        }
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_CONFIGURATIONS | PackageManager.GET_META_DATA);
        if (info != null && info.applicationInfo != null) {
            System.out.println(info.applicationInfo.metaData.toString());
            String metaSourceClass = info.applicationInfo.metaData.getString(MANGA_PACKAGE + XX_METADATA_SOURCE_CLASS);
            if (metaSourceClass == null) {
                return Optional.empty();
            }
            return Arrays.stream(metaSourceClass.split(";")).map(s -> {
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
                    applyPreferences(data, src.getId());
                    result = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> callback.apply(src, continuation));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return result;
            });
        }
        return Optional.empty();
    }

    protected <T> Optional<T> invokeAnimeSource(DexClassLoader classLoader, File file, DataBody data, BiFunction<AnimeCatalogueSource, Continuation<? super T>, ?> callback) {
        if (pm == null) {
            return Optional.empty();
        }
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_CONFIGURATIONS | PackageManager.GET_META_DATA);
        if (info != null && info.applicationInfo != null) {
            System.out.println(info.applicationInfo.metaData.toString());
            String metaSourceClass = info.applicationInfo.metaData.getString(ANIME_PACKAGE + XX_METADATA_SOURCE_CLASS);
            if (metaSourceClass == null) {
                return Optional.empty();
            }
            return Arrays.stream(metaSourceClass.split(";")).map(s -> {
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
                    applyPreferences(data, src.getId());
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

    protected FilterList convertFilterListManga(FilterList defaultFilters, List<JFilterList> data) {
        for (Filter<?> filter : defaultFilters.getList()) {
            JFilterList dataFilter = data.stream().filter(e -> filter.getName().equalsIgnoreCase(e.name)).findFirst().orElse(null);
            if (dataFilter == null) {
                continue;
            }
            if (filter instanceof Filter.Text) {
                Filter.Text temp = (Filter.Text) filter;
                temp.setState(dataFilter.stateString);
            }
            if (filter instanceof Filter.Group<?>) {
                Filter.Group<?> temp = (Filter.Group<?>) filter;
                if (temp.getState() != null) {
                    for (Object state : temp.getState()) {
                        if (!(state instanceof Filter<?>)) {
                            continue;
                        }
                        JFilterList.JGroupFilter dataGroup = dataFilter.stateList.stream().filter(e -> ((Filter<?>) state).getName().equalsIgnoreCase(e.name)).findFirst().orElse(null);
                        if (dataGroup == null) {
                            continue;
                        }
                        if (state instanceof Filter.CheckBox) {
                            Filter.CheckBox checkBox = (Filter.CheckBox) state;
                            checkBox.setState(dataGroup.stateBoolean);
                        }
                        if (state instanceof Filter.TriState) {
                            Filter.TriState checkBox = (Filter.TriState) state;
                            checkBox.setState(dataGroup.stateInt);
                        }
                    }
                }
            }
            if (filter instanceof Filter.Select<?>) {
                Filter.Select<?> temp = (Filter.Select<?>) filter;
                temp.setState(dataFilter.stateInt);
            }
            if (filter instanceof Filter.Sort) {
                Filter.Sort temp = (Filter.Sort) filter;
                JFilterList.JSortFilter dataSort = dataFilter.stateSort;
                if (dataSort == null) {
                    continue;
                }
                temp.setState(temp.getState().copy(dataSort.index, dataSort.ascending));
            }
        }
        return defaultFilters;
    }

    protected AnimeFilterList convertFilterListAnime(AnimeFilterList defaultFilters, List<JFilterList> data) {
        for (AnimeFilter<?> filter : defaultFilters.getList()) {
            JFilterList dataFilter = data.stream().filter(e -> filter.getName().equalsIgnoreCase(e.name)).findFirst().orElse(null);
            if (dataFilter == null) {
                continue;
            }
            if (filter instanceof AnimeFilter.Text) {
                AnimeFilter.Text temp = (AnimeFilter.Text) filter;
                temp.setState(dataFilter.stateString);
            }
            if (filter instanceof AnimeFilter.Group<?>) {
                AnimeFilter.Group<?> temp = (AnimeFilter.Group<?>) filter;
                if (temp.getState() != null) {
                    for (Object state : temp.getState()) {
                        if (!(state instanceof AnimeFilter<?>)) {
                            continue;
                        }
                        JFilterList.JGroupFilter dataGroup = dataFilter.stateList.stream().filter(e -> ((AnimeFilter<?>) state).getName().equalsIgnoreCase(e.name)).findFirst().orElse(null);
                        if (dataGroup == null) {
                            continue;
                        }
                        if (state instanceof AnimeFilter.CheckBox) {
                            AnimeFilter.CheckBox checkBox = (AnimeFilter.CheckBox) state;
                            checkBox.setState(dataGroup.stateBoolean);
                        }
                        if (state instanceof AnimeFilter.TriState) {
                            AnimeFilter.TriState checkBox = (AnimeFilter.TriState) state;
                            checkBox.setState(dataGroup.stateInt);
                        }
                    }
                }
            }
            if (filter instanceof AnimeFilter.Select<?>) {
                AnimeFilter.Select<?> temp = (AnimeFilter.Select<?>) filter;
                temp.setState(dataFilter.stateInt);
            }
            if (filter instanceof AnimeFilter.Sort) {
                AnimeFilter.Sort temp = (AnimeFilter.Sort) filter;
                JFilterList.JSortFilter dataSort = dataFilter.stateSort;
                if (dataSort == null) {
                    continue;
                }
                temp.setState(temp.getState().copy(dataSort.index, dataSort.ascending));
            }
        }
        return defaultFilters;
    }

    private void applyPreferences(DataBody data, long sourceId) {
        if (data.preferences != null && !data.preferences.isEmpty()) {
            SharedPreferences prefs = PreferencesKt.getPreferences(sourceId);
            SharedPreferences.Editor editor = prefs.edit();
            for (JPreference pref : data.preferences) {
                if (pref.checkBoxPreference != null) {
                    editor.putBoolean(pref.key, pref.checkBoxPreference.value);
                }
                if (pref.editTextPreference != null) {
                    editor.putString(pref.key, pref.editTextPreference.value);
                }
                if (pref.listPreference != null) {
                    editor.putString(pref.key, pref.listPreference.entryValues.get(pref.listPreference.valueIndex));
                }
                if (pref.multiSelectListPreference != null) {
                    editor.putStringSet(pref.key, new HashSet<>(pref.multiSelectListPreference.values));
                }
                if (pref.switchPreferenceCompat != null) {
                    editor.putBoolean(pref.key, pref.switchPreferenceCompat.value);
                }
            }
            if (!editor.commit()) {
                Log.e("DalvikHandler", "Unable to apply prefs for id: " + sourceId);
            }
        }
    }

    private void processPreferences(PreferenceScreen screen, List<JPreference> preferences) {
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference pref = screen.getPreference(i);
            JPreference temp = new JPreference();
            temp.key = pref.getKey();
            if (pref instanceof CheckBoxPreference) {
                CheckBoxPreference lp = (CheckBoxPreference) pref;
                JCheckBoxPreference checkBox = new JCheckBoxPreference();
                checkBox.title = Objects.requireNonNull(lp.getTitle()).toString();
                checkBox.summary = lp.getSummary() != null ? lp.getSummary().toString() : "";
                checkBox.value = lp.isChecked();
                temp.checkBoxPreference = checkBox;
            }
            if (pref instanceof EditTextPreference) {
                EditTextPreference lp = (EditTextPreference) pref;
                JEditTextPreference editText = new JEditTextPreference();
                editText.title = Objects.requireNonNull(lp.getTitle()).toString();
                editText.summary = lp.getSummary() != null ? lp.getSummary().toString() : "";
                editText.dialogTitle = lp.getDialogTitle() != null ? lp.getDialogTitle().toString() : "";
                editText.dialogMessage = lp.getDialogMessage() != null ? lp.getDialogMessage().toString() : "";
                editText.text = lp.getText();
                editText.value = lp.getText();
                temp.editTextPreference = editText;
            }
            if (pref instanceof ListPreference) {
                ListPreference lp = (ListPreference) pref;
                JListPreference list = new JListPreference();
                list.title = Objects.requireNonNull(lp.getTitle()).toString();
                list.summary = lp.getSummary() != null ? lp.getSummary().toString() : "";
                list.valueIndex = Arrays.asList(lp.getEntryValues()).indexOf(lp.getValue());
                list.entries = Arrays.stream(lp.getEntries()).map(CharSequence::toString).collect(Collectors.toList());
                list.entryValues = Arrays.stream(lp.getEntryValues()).map(CharSequence::toString).collect(Collectors.toList());
                temp.listPreference = list;
            }
            if (pref instanceof MultiSelectListPreference) {
                MultiSelectListPreference lp = (MultiSelectListPreference) pref;
                JMultiSelectListPreference multiList = new JMultiSelectListPreference();
                multiList.title = Objects.requireNonNull(lp.getTitle()).toString();
                multiList.summary = lp.getSummary() != null ? lp.getSummary().toString() : "";
                multiList.entries = Arrays.stream(lp.getEntries()).map(CharSequence::toString).collect(Collectors.toList());
                multiList.entryValues = Arrays.stream(lp.getEntryValues()).map(CharSequence::toString).collect(Collectors.toList());
                multiList.values = new ArrayList<>(lp.getValues());
                temp.multiSelectListPreference = multiList;
            }
            if (pref instanceof SwitchPreference) {
                SwitchPreference lp = (SwitchPreference) pref;
                JSwitchPreference jswitch = new JSwitchPreference();
                jswitch.title = Objects.requireNonNull(lp.getTitle()).toString();
                jswitch.summary = lp.getSummary() != null ? lp.getSummary().toString() : "";
                jswitch.value = lp.isChecked();
                temp.switchPreferenceCompat = jswitch;
            }
            preferences.add(temp);
        }
    }
}
