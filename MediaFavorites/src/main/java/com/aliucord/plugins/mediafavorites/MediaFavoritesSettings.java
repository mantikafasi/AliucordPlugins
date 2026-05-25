package com.aliucord.plugins.mediafavorites;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;
import com.lytefast.flexinput.R;

import java.util.List;

public class MediaFavoritesSettings extends BottomSheet {
    private final SettingsAPI settings;

    private LinearLayout tabBar;
    private LinearLayout contentContainer;

    private TextView imagesTab;
    private TextView videosTab;
    private TextView audioTab;

    private String activeTab = "images";

    public MediaFavoritesSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        Context ctx = requireContext();
        setPadding(DimenUtils.dpToPx(16));

        TextView title = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header);
        title.setText("Media Favorites");
        addView(title);

        buildTabBar(ctx);

        contentContainer = new LinearLayout(ctx);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        addView(contentContainer);

        refreshContent();
    }

    private void buildTabBar(Context ctx) {
        tabBar = new LinearLayout(ctx);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(0, DimenUtils.dpToPx(8), 0, DimenUtils.dpToPx(8));

        imagesTab = createTab(ctx, "Images", "images");
        videosTab = createTab(ctx, "Videos", "videos");
        audioTab = createTab(ctx, "Audio", "audio");

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        tabBar.addView(imagesTab, tabLp);
        tabBar.addView(videosTab, tabLp);
        tabBar.addView(audioTab, tabLp);

        addView(tabBar);

        View sep = new View(ctx);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, DimenUtils.dpToPx(1)));
        sep.setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundTertiary));
        addView(sep);
    }

    private TextView createTab(Context ctx, String label, String key) {
        TextView tab = new TextView(ctx);
        tab.setText(label);
        tab.setGravity(Gravity.CENTER);
        tab.setTextSize(14f);
        tab.setPadding(0, DimenUtils.dpToPx(8), 0, DimenUtils.dpToPx(8));
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setOnClickListener(v -> {
            activeTab = key;
            updateTabStyles();
            refreshContent();
        });
        return tab;
    }

    private void updateTabStyles() {
        int activeColor = ColorCompat.getThemedColor(requireContext(), R.b.colorInteractiveActive);
        int normalColor = ColorCompat.getThemedColor(requireContext(), R.b.colorInteractiveNormal);

        imagesTab.setTextColor("images".equals(activeTab) ? activeColor : normalColor);
        videosTab.setTextColor("videos".equals(activeTab) ? activeColor : normalColor);
        audioTab.setTextColor("audio".equals(activeTab) ? activeColor : normalColor);
    }

    private void refreshContent() {
        contentContainer.removeAllViews();
        Context ctx = requireContext();

        MediaFavorites.FavoritesData data = MediaFavorites.getFavorites(settings);
        List<MediaFavorites.FavoriteAttachment> list = getActiveList(data);

        updateTabStyles();

        if (list.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("No favorites yet\n\nTap the \u2605 on attachments in chat to add them here.");
            empty.setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, DimenUtils.dpToPx(40), 0, DimenUtils.dpToPx(40));
            contentContainer.addView(empty);
            return;
        }

        String countLabel = list.size() + " item" + (list.size() != 1 ? "s" : "");
        TextView countView = new TextView(ctx);
        countView.setText(countLabel);
        countView.setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveMuted));
        countView.setTextSize(12f);
        countView.setPadding(0, DimenUtils.dpToPx(4), 0, DimenUtils.dpToPx(8));
        contentContainer.addView(countView);

        for (MediaFavorites.FavoriteAttachment fav : list) {
            contentContainer.addView(buildFavItem(ctx, fav));
        }
    }

    private LinearLayout buildFavItem(Context ctx, MediaFavorites.FavoriteAttachment fav) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, DimenUtils.dpToPx(4), 0, DimenUtils.dpToPx(4));

        LinearLayout info = new LinearLayout(ctx);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoLp);

        TextView nameView = new TextView(ctx);
        nameView.setText(fav.filename != null ? fav.filename : "Unknown");
        nameView.setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal));
        nameView.setTextSize(14f);
        nameView.setMaxLines(2);
        info.addView(nameView);

        if (fav.filename != null) {
            String ext = "";
            int dot = fav.filename.lastIndexOf('.');
            if (dot >= 0) ext = fav.filename.substring(dot + 1).toUpperCase();
            TextView typeView = new TextView(ctx);
            typeView.setText(ext.isEmpty() ? "Media" : ext);
            typeView.setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveMuted));
            typeView.setTextSize(11f);
            info.addView(typeView);
        }

        row.addView(info);

        TextView removeBtn = new TextView(ctx);
        removeBtn.setText("\u2715");
        removeBtn.setTextSize(18f);
        removeBtn.setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveMuted));
        removeBtn.setGravity(Gravity.CENTER);
        removeBtn.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(4), DimenUtils.dpToPx(4), DimenUtils.dpToPx(4));
        removeBtn.setOnClickListener(v -> {
            MediaFavorites.removeFavorite(fav.getKey(), activeTab);
            refreshContent();
        });
        row.addView(removeBtn);

        View sep = new View(ctx);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        sep.setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundTertiary));

        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        wrapper.addView(sep);

        return wrapper;
    }

    private List<MediaFavorites.FavoriteAttachment> getActiveList(MediaFavorites.FavoritesData data) {
        switch (activeTab) {
            case "videos": return data.videos;
            case "audio": return data.audio;
            default: return data.images;
        }
    }
}
