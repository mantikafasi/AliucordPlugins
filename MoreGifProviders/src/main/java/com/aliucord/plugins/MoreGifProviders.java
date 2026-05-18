package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliucord.Http;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.GsonUtils;
import com.discord.databinding.WidgetGifPickerSearchBinding;
import com.discord.models.gifpicker.dto.GifDto;
import com.discord.models.gifpicker.dto.ModelGif;
import com.discord.stores.StoreGifPicker;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.SearchInputView;
import com.discord.widgets.chat.input.gifpicker.GifSearchViewModel;
import com.discord.widgets.chat.input.gifpicker.WidgetGifPickerSearch;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AliucordPlugin
@SuppressWarnings("unused")
public class MoreGifProviders extends Plugin {
    public static final String PROVIDER_KEY = "provider";
    public static final String[] PROVIDERS = {"tenor", "klipy", "giphy"};
    private static final String LOCALE = "en-US";
    private static final int GIF_LIMIT = 50;
    private final int providerRowId = View.generateViewId();
    private volatile String currentSearchQuery;
    private WidgetGifPickerSearch currentGifPickerSearch;

    @Override
    public void start(Context context) throws Throwable {
        settingsTab = new SettingsTab(MoreGifProvidersSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        Method fetchTrending = StoreGifPicker.class.getDeclaredMethod("fetchTrendingCategoryGifs");
        Method fetchSearch = StoreGifPicker.class.getDeclaredMethod("fetchGifsForSearchQuery", String.class);
        Method bindGifPicker = WidgetGifPickerSearch.class.getDeclaredMethod("onViewBound", View.class);
        Method setSearchText = GifSearchViewModel.class.getDeclaredMethod("setSearchText", String.class);

        patcher.patch(bindGifPicker, new Hook(callFrame -> {
            WidgetGifPickerSearch picker = (WidgetGifPickerSearch) callFrame.thisObject;
            currentGifPickerSearch = picker;
            View root = (View) callFrame.args[0];
            addProviderPicker(root);
            updateSearchLabel(picker);
        }));

        patcher.patch(setSearchText, new Hook(callFrame -> {
            String query = (String) callFrame.args[0];
            currentSearchQuery = query == null || query.isEmpty() ? null : query;
        }));

        patcher.patch(fetchTrending, new InsteadHook(callFrame -> {
            StoreGifPicker store = (StoreGifPicker) callFrame.thisObject;
            fetchGifsAsync(store, null);
            return null;
        }));

        patcher.patch(fetchSearch, new InsteadHook(callFrame -> {
            StoreGifPicker store = (StoreGifPicker) callFrame.thisObject;
            String query = (String) callFrame.args[0];
            fetchGifsAsync(store, query);
            return null;
        }));
    }

    private void addProviderPicker(View root) {
        if (!(root instanceof ViewGroup) || root.findViewById(providerRowId) != null) return;

        Context context = root.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setId(providerRowId);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(4), DimenUtils.dpToPx(12), DimenUtils.dpToPx(6));

        for (String provider : PROVIDERS) {
            TextView item = createProviderButton(context, provider);
            row.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }

        ViewGroup parent = findAppBar(root);
        if (parent != null) {
            parent.addView(row, new AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            ((ViewGroup) root).addView(row, 0);
        }
        updateProviderPicker(row);
    }

    private TextView createProviderButton(Context context, String provider) {
        TextView view = new TextView(context);
        view.setText(provider.substring(0, 1).toUpperCase() + provider.substring(1));
        view.setGravity(Gravity.CENTER);
        view.setPadding(DimenUtils.dpToPx(4), DimenUtils.dpToPx(8), DimenUtils.dpToPx(4), DimenUtils.dpToPx(8));
        view.setTag(provider);
        view.setOnClickListener(v -> {
            settings.setString(PROVIDER_KEY, provider);
            updateProviderPicker((LinearLayout) v.getParent());
            if (currentGifPickerSearch != null) updateSearchLabel(currentGifPickerSearch);
            fetchGifsAsync(StoreStream.Companion.getGifPicker(), currentSearchQuery);
        });
        return view;
    }

    private void updateProviderPicker(LinearLayout row) {
        String selectedProvider = getProvider();
        int selectedColor = ColorCompat.getThemedColor(row.getContext(), com.lytefast.flexinput.R.b.colorInteractiveActive);
        int normalColor = ColorCompat.getThemedColor(row.getContext(), com.lytefast.flexinput.R.b.colorInteractiveNormal);

        for (int i = 0; i < row.getChildCount(); i++) {
            TextView child = (TextView) row.getChildAt(i);
            boolean selected = selectedProvider.equals(child.getTag());
            child.setTextColor(selected ? selectedColor : normalColor);
            child.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private ViewGroup findAppBar(View view) {
        if (view instanceof AppBarLayout) return (ViewGroup) view;
        if (!(view instanceof ViewGroup)) return null;

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup appBar = findAppBar(group.getChildAt(i));
            if (appBar != null) return appBar;
        }
        return null;
    }

    private void updateSearchLabel(WidgetGifPickerSearch picker) {
        try {
            Method getBinding = WidgetGifPickerSearch.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);
            WidgetGifPickerSearchBinding binding = (WidgetGifPickerSearchBinding) getBinding.invoke(picker);
            if (binding != null) forceSearchInputLabel(binding.e);
        } catch (Throwable throwable) {
            logger.error("Failed to update GIF search hint", throwable);
        }
    }

    private void forceSearchInputLabel(SearchInputView searchInputView) {
        String hint = getSearchProviderText();
        searchInputView.setHint(hint);

        View editText = searchInputView.getEditText();
        if (editText instanceof TextView) {
            ((TextView) editText).setHint(hint);
        }

        ViewParent parent = editText.getParent();
        while (parent instanceof View) {
            if (parent instanceof TextInputLayout) {
                ((TextInputLayout) parent).setHint(hint);
                break;
            }
            parent = parent.getParent();
        }
    }

    private String getSearchProviderText() {
        return "Search " + getProviderDisplayName();
    }

    private String getProviderDisplayName() {
        String provider = getProvider();
        return provider.substring(0, 1).toUpperCase() + provider.substring(1);
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }

    private void fetchGifsAsync(StoreGifPicker store, String query) {
        new Thread(() -> {
            try {
                List<ModelGif> gifs = fetchGifs(query);
                runOnStoreThread(() -> handleFetchSuccess(store, query, gifs));
            } catch (Throwable throwable) {
                logger.error("Failed to fetch GIFs from extra providers", throwable);
                runOnStoreThread(() -> handleFetchError(store, query));
            }
        }, "MoreGifProviders").start();
    }

    private void handleFetchSuccess(StoreGifPicker store, String query, List<ModelGif> gifs) {
        try {
            if (query == null) {
                StoreGifPicker.access$handleFetchTrendingGifsOnNext(store, gifs);
            } else {
                StoreGifPicker.access$handleGifSearchResults(store, query, gifs);
            }
        } catch (Throwable throwable) {
            logger.error("Failed to update GIF picker state", throwable);
        }
    }

    private void handleFetchError(StoreGifPicker store, String query) {
        try {
            if (query == null) {
                StoreGifPicker.access$handleFetchTrendingGifsError(store);
            } else {
                StoreGifPicker.access$handleGifSearchResults(store, query, Collections.emptyList());
            }
        } catch (Throwable throwable) {
            logger.error("Failed to update GIF picker error state", throwable);
        }
    }

    private List<ModelGif> fetchGifs(String query) throws IOException {
        String provider = getProvider();
        List<ModelGif> gifs = fetchProviderGifs(provider, query);

        if (gifs.isEmpty()) {
            throw new IOException("No GIFs returned from " + provider);
        }
        return gifs;
    }

    private String getProvider() {
        String provider = settings.getString(PROVIDER_KEY, PROVIDERS[0]);
        for (String knownProvider : PROVIDERS) {
            if (knownProvider.equals(provider)) return provider;
        }
        return PROVIDERS[0];
    }

    private List<ModelGif> fetchProviderGifs(String provider, String query) throws IOException {
        String route = query == null
                ? "/gifs/trending-gifs?provider=" + provider + "&locale=" + LOCALE + "&media_format=webp"
                : "/gifs/search?q=" + encode(query) + "&media_format=webp&provider=" + provider + "&locale=" + LOCALE + "&limit=" + GIF_LIMIT;

        Http.Response response = Http.Request.newDiscordRNRequest(route).execute();

        if (response.statusCode < 200 || response.statusCode >= 300) {
            throw new IOException("Discord GIF request failed: " + response.statusCode + " " + response.text());
        }

        return parseGifResponse(response.text());
    }

    private List<ModelGif> parseGifResponse(String body) {
        GifDto[] gifsJson = GsonUtils.fromJson(body, GifDto[].class);

        ArrayList<ModelGif> gifs = new ArrayList<>();
        for (GifDto gifDTO : gifsJson) {
            gifs.add(ModelGif.Companion.createFromGifDto(gifDTO));
        }
        return gifs;
    }

    private void runOnStoreThread(Runnable runnable) {
        StoreStream.access$getDispatcher$p(StoreStream.getNotices().getStream()).schedule(() -> {
            runnable.run();
            return null;
        });
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Throwable ignored) {
            return value;
        }
    }
}
