package com.aliucord.plugins.mediafavorites;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.GsonUtils;
import com.aliucord.widgets.BottomSheet;
import com.discord.app.AppFragment;
import com.discord.databinding.WidgetChatListAdapterItemAttachmentBinding;
import com.discord.models.gifpicker.dto.ModelGif;
import com.discord.widgets.chat.input.WidgetChatInputAttachments;
import com.discord.widgets.chat.input.gifpicker.GifSearchViewModel;
import com.discord.widgets.chat.input.gifpicker.WidgetGifPickerSearch;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment;
import com.discord.widgets.chat.list.entries.AttachmentEntry;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.segmentedcontrol.CardSegment;
import com.discord.views.segmentedcontrol.SegmentedControlContainer;
import com.lytefast.flexinput.model.Attachment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import kotlin.jvm.functions.Function1;

@AliucordPlugin
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public class MediaFavorites extends Plugin {
    private static final Logger logger = new Logger("MediaFavorites");
    private static final String FAVORITES_KEY = "mediaFavoritesData";
    private static final String FAV_BTN_TAG = "MediaFavoritesStarBtn";
    private static final String FAV_GIF_STAR_TAG = "MediaFavoritesGifStar";
    private static final String FAV_SEGMENT_TAG = "MediaFavoritesSegment";
    private static final String FAV_TYPE_TABS_TAG = "MediaFavoritesTypeTabs";
    private static final String FAV_CONTENT_TAG = "MediaFavoritesContent";
    private static final String CACHE_DIR = "mediafavorites";

    private static String activeFavType = "images";
    private static String mediaSearchQuery = "";
    private static WidgetChatInputAttachments currentChatAttachments;
    /** Callback captured from GifViewHolder.Gif.configure; equivalent to viewModel::selectGif */
    private static Function1 currentGifSelectCallback;

    private interface TypeChangeListener {
        void onTypeChanged(String key);
    }

    public static class FavoriteAttachment {
        public String rawJson;
        public String url;
        public String proxy_url;
        public String filename;
        public int width;
        public int height;
        public long added_timestamp;
        public List<String> tags = new ArrayList<>();

        public String getKey() { return url != null ? url : proxy_url; }

        public MessageAttachment toMessageAttachment() {
            if (rawJson == null) return null;
            return GsonUtils.fromJson(rawJson, MessageAttachment.class);
        }
    }

    public static class FavoritesData {
        public List<FavoriteAttachment> images = new ArrayList<>();
        public List<FavoriteAttachment> gifs   = new ArrayList<>();
        public List<FavoriteAttachment> videos = new ArrayList<>();
        public List<FavoriteAttachment> audio  = new ArrayList<>();
    }

    @Override
    public void start(Context context) throws Throwable {
        instance = this;
        settingsTab = new SettingsTab(MediaFavoritesSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patchAttachmentStar();
        patchChatAttachments();
        patchExpressionSegmentedControl();
        patchExpressionSearchBarClick();
        patchGifPickerStar();
        logger.info("MediaFavorites started");
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
        instance = null;
        currentChatAttachments = null;
        currentGifSelectCallback = null;
        activeFavType = "images";
        logger.info("MediaFavorites stopped");
    }

    private void patchChatAttachments() throws NoSuchMethodException {
        patcher.patch(
            WidgetChatInputAttachments.class.getDeclaredMethod("configureFlexInputFragment", AppFragment.class),
            new Hook(callFrame -> currentChatAttachments = (WidgetChatInputAttachments) callFrame.thisObject)
        );
    }

    private void patchExpressionSearchBarClick() throws NoSuchMethodException {
        patcher.patch(
            View.class.getDeclaredMethod("performClick"),
            new InsteadHook(callFrame -> {
                View view = (View) callFrame.thisObject;
                if (isExpressionSearchBarView(view) && isMediaFavoritesVisible(view)) {
                    showMediaSearchSheet(findExpressionTrayRoot(view));
                    return true;
                }
                if (isBuiltInExpressionSegment(view) && isMediaFavoritesVisible(view)) {
                    SegmentedControlContainer segments = (SegmentedControlContainer) view.getParent();
                    int index = segments.indexOfChild(view);
                    Object result = invokeOriginalClick(callFrame);
                    restoreBuiltInExpressionTab(segments, index);
                    segments.post(() -> restoreBuiltInExpressionTab(segments, index));
                    return result;
                }
                return invokeOriginalClick(callFrame);
            })
        );
    }

    private Object invokeOriginalClick(de.robv.android.xposed.XC_MethodHook.MethodHookParam callFrame) {
        try {
            return XposedBridge.invokeOriginalMethod(callFrame.method, callFrame.thisObject, callFrame.args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private boolean isBuiltInExpressionSegment(View view) {
        if (!(view instanceof CardSegment)) return false;
        if (!(view.getParent() instanceof SegmentedControlContainer)) return false;
        if (FAV_SEGMENT_TAG.equals(view.getTag())) return false;

        SegmentedControlContainer segments = (SegmentedControlContainer) view.getParent();
        return isExpressionTraySegmentedControl(segments);
    }

    private void restoreBuiltInExpressionTab(SegmentedControlContainer segments, int index) {
        if (index < 0 || index > 2) return;

        try {
            segments.setSelectedIndex(index);
        } catch (Throwable t) {
            logger.error("Error restoring expression tab selection", t);
        }

        View root = findExpressionTrayRoot(segments);
        hideFavContent(root);

        View emojiContent   = findViewByResourceEntryName(root, "expression_tray_emoji_picker_content");
        View gifContent     = findViewByResourceEntryName(root, "expression_tray_gif_picker_content");
        View stickerContent = findViewByResourceEntryName(root, "expression_tray_sticker_picker_content");
        if (emojiContent   != null) emojiContent.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (gifContent     != null) gifContent.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (stickerContent != null) stickerContent.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        View searchButton = findViewByResourceEntryName(root, "expression_tray_search_button");
        if (searchButton instanceof TextView) {
            TextView text = (TextView) searchButton;
            text.setText("");
            text.setHint(index == 0 ? "Search Emoji" : index == 1 ? "Search GIFs" : "Search Stickers");
        }
        View searchBar = findViewByResourceEntryName(root, "expression_tray_search_bar");
        if (searchBar != null) {
            searchBar.setContentDescription(index == 0 ? "Search Emoji" : index == 1 ? "Search GIFs" : "Search Stickers");
        }
    }

    /* ── Star button on chat-message attachments ────────────────────────── */

    private void patchAttachmentStar() throws NoSuchMethodException {
        patcher.patch(
            WidgetChatListAdapterItemAttachment.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class),
            new Hook(callFrame -> {
                try {
                    AttachmentEntry entry = (AttachmentEntry) callFrame.args[1];
                    MessageAttachment attachment = entry.getAttachment();
                    if (attachment == null) return;

                    WidgetChatListAdapterItemAttachment item =
                            (WidgetChatListAdapterItemAttachment) callFrame.thisObject;
                    WidgetChatListAdapterItemAttachmentBinding binding =
                            WidgetChatListAdapterItemAttachment.access$getBinding$p(item);
                    ConstraintLayout root = binding.a;

                    String rawJson = GsonUtils.toJson(attachment);
                    FavoriteAttachment fav = GsonUtils.fromJson(rawJson, FavoriteAttachment.class);
                    if (fav == null || fav.getKey() == null) {
                        removeStarButton(root);
                        return;
                    }
                    fav.rawJson = rawJson;

                    String type = getAttachmentType(fav);
                    if (type == null) {
                        removeStarButton(root);
                        return;
                    }

                    TextView starBtn = root.findViewWithTag(FAV_BTN_TAG);
                    if (starBtn == null) {
                        starBtn = createStarButton(root.getContext());
                    }
                    attachStarButton(binding, starBtn);

                    final TextView fb = starBtn;
                    boolean isFav = isFavorited(fav.getKey());
                    updateStarState(fb, isFav);
                    fb.setOnClickListener(v -> {
                        boolean nowFav = toggleFavorite(fav, type);
                        updateStarState(fb, nowFav);
                    });
                    fb.setOnLongClickListener(v -> {
                        FavoriteAttachment saved = ensureFavorite(fav, type);
                        updateStarState(fb, true);
                        showTagEditor(root.getContext(), saved, type, null);
                        return true;
                    });
                } catch (Throwable t) {
                    logger.error("Error in attachment star hook", t);
                }
            })
        );
    }

    private static String getAttachmentType(FavoriteAttachment fav) {
        if (fav == null || fav.filename == null) return null;
        String fn = fav.filename.toLowerCase();
        if (fn.endsWith(".mp3")||fn.endsWith(".ogg")||fn.endsWith(".wav")||fn.endsWith(".m4a")||
            fn.endsWith(".aac")||fn.endsWith(".flac")||fn.endsWith(".opus")||fn.endsWith(".wma")||fn.endsWith(".oga"))
            return "audio";
        if (fn.endsWith(".mp4")||fn.endsWith(".webm")||fn.endsWith(".mov")||fn.endsWith(".avi")||
            fn.endsWith(".mkv")||fn.endsWith(".m4v")||fn.endsWith(".mpg")||fn.endsWith(".mpeg")||fn.endsWith(".wmv"))
            return "video";
        if (fn.endsWith(".png")||fn.endsWith(".jpg")||fn.endsWith(".jpeg")||fn.endsWith(".gif")||
            fn.endsWith(".webp")||fn.endsWith(".bmp")||fn.endsWith(".tiff")||fn.endsWith(".tif"))
            return "image";
        return null;
    }

    private TextView createStarButton(Context ctx) {
        TextView star = new TextView(ctx);
        star.setTag(FAV_BTN_TAG);
        star.setId(View.generateViewId());
        star.setText("\u2605");
        star.setTextSize(15f);
        star.setGravity(Gravity.CENTER);
        star.setTypeface(Typeface.DEFAULT_BOLD);
        star.setIncludeFontPadding(false);
        star.setBackground(makeStarBackground(Color.argb(190, 20, 21, 25)));
        star.setElevation(DimenUtils.dpToPx(3));
        star.setClickable(true);
        star.setFocusable(true);
        return star;
    }

    private static GradientDrawable makeStarBackground(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        bg.setStroke(DimenUtils.dpToPx(1), Color.argb(90, 255, 255, 255));
        return bg;
    }

    private void attachStarButton(WidgetChatListAdapterItemAttachmentBinding binding, TextView starBtn) {
        ViewGroup target = binding.h.getVisibility() == View.VISIBLE ? binding.h : binding.d;

        if (starBtn.getParent() != target) {
            if (starBtn.getParent() instanceof ViewGroup) {
                ((ViewGroup) starBtn.getParent()).removeView(starBtn);
            }
            int size = DimenUtils.dpToPx(28);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.gravity = Gravity.TOP | Gravity.RIGHT;
            lp.topMargin = DimenUtils.dpToPx(6);
            lp.rightMargin = DimenUtils.dpToPx(6);
            target.addView(starBtn, lp);
        }
    }

    private void removeStarButton(View root) {
        View star = root.findViewWithTag(FAV_BTN_TAG);
        if (star != null && star.getParent() instanceof ViewGroup) {
            ((ViewGroup) star.getParent()).removeView(star);
        }
    }

    private static void updateStarState(TextView star, boolean favorited) {
        star.setTextColor(favorited ? Color.parseColor("#FFD700") : Color.WHITE);
    }

    /* ── Star button inside the native GIF picker ───────────────────────── */

    private void patchGifPickerStar() {
        try {
            Class<?> gifAdapterClass = Class.forName(
                "com.discord.widgets.chat.input.gifpicker.GifAdapter");

            // Patch the bridge method: onBindViewHolder(RecyclerView.ViewHolder, int)
            // This is the method RecyclerView actually dispatches to.
            Method onBind = gifAdapterClass.getDeclaredMethod(
                "onBindViewHolder", RecyclerView.ViewHolder.class, int.class);
            onBind.setAccessible(true);

            patcher.patch(onBind, new Hook(callFrame -> {
                try {
                    Object adapter = callFrame.thisObject;
                    RecyclerView.ViewHolder holder = (RecyclerView.ViewHolder) callFrame.args[0];
                    int position = (int) callFrame.args[1];

                    // Fetch the items list via the field
                    java.lang.reflect.Field itemsField = gifAdapterClass.getDeclaredField("items");
                    itemsField.setAccessible(true);
                    java.util.List<?> items = (java.util.List<?>) itemsField.get(adapter);
                    if (items == null || position < 0 || position >= items.size()) return;

                    Object gifAdapterItem = items.get(position);
                    if (gifAdapterItem == null) return;

                    // Only decorate GifItem rows (not SuggestedTerms rows)
                    if (!"GifItem".equals(gifAdapterItem.getClass().getSimpleName())) return;

                    // Get ModelGif from the item
                    Method getGif = gifAdapterItem.getClass().getDeclaredMethod("getGif");
                    getGif.setAccessible(true);
                    ModelGif modelGif = (ModelGif) getGif.invoke(gifAdapterItem);
                    if (modelGif == null) return;

                    String gifImageUrl = modelGif.getGifImageUrl();
                    String tenorGifUrl = modelGif.getTenorGifUrl();
                    if (gifImageUrl == null && tenorGifUrl == null) return;

                    // Capture the select callback from the adapter field
                    java.lang.reflect.Field onSelectGifField = gifAdapterClass.getDeclaredField("onSelectGif");
                    onSelectGifField.setAccessible(true);
                    Function1 cb = (Function1) onSelectGifField.get(adapter);
                    if (cb != null) currentGifSelectCallback = cb;

                    // Build a FavoriteAttachment for this GIF
                    FavoriteAttachment fav = new FavoriteAttachment();
                    fav.url       = tenorGifUrl;
                    fav.proxy_url = gifImageUrl;
                    fav.filename  = "tenor.gif";  // .gif → isImage() returns true in grid
                    fav.width     = modelGif.getWidth();
                    fav.height    = modelGif.getHeight();

                    // holder.itemView is the CardView (FrameLayout) for GIF cells
                    ViewGroup cardView = (ViewGroup) holder.itemView;

                    // Find-or-create the star overlay button
                    TextView starBtn = cardView.findViewWithTag(FAV_GIF_STAR_TAG);
                    if (starBtn == null) {
                        starBtn = createGifStarButton(cardView.getContext());
                        int size = DimenUtils.dpToPx(26);
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
                        lp.gravity     = Gravity.TOP | Gravity.END;
                        lp.topMargin   = DimenUtils.dpToPx(4);
                        lp.rightMargin = DimenUtils.dpToPx(4);
                        cardView.addView(starBtn, lp);
                    }
                    // Ensure star is drawn on top of the SimpleDraweeView
                    starBtn.bringToFront();

                    // Sync filled / unfilled colour
                    updateStarState(starBtn, isFavorited(fav.getKey()));

                    final TextView finalStar = starBtn;
                    final FavoriteAttachment finalFav = fav;
                    finalStar.setOnClickListener(v -> {
                        boolean nowFav = toggleFavorite(finalFav, "gifs");
                        updateStarState(finalStar, nowFav);
                    });
                    finalStar.setOnLongClickListener(v -> {
                        FavoriteAttachment saved = ensureFavorite(finalFav, "gifs");
                        updateStarState(finalStar, true);
                        showTagEditor(cardView.getContext(), saved, "gifs", null);
                        return true;
                    });
                } catch (Throwable t) {
                    logger.error("Error in GIF adapter bind hook", t);
                }
            }));

        } catch (Throwable t) {
            logger.error("Could not patch GIF picker star button", t);
        }
    }

    private TextView createGifStarButton(Context ctx) {
        TextView star = new TextView(ctx);
        star.setTag(FAV_GIF_STAR_TAG);
        star.setId(View.generateViewId());
        star.setText("\u2605");
        star.setTextSize(13f);
        star.setGravity(Gravity.CENTER);
        star.setTypeface(Typeface.DEFAULT_BOLD);
        star.setIncludeFontPadding(false);
        star.setBackground(makeStarBackground(Color.argb(170, 20, 21, 25)));
        star.setElevation(DimenUtils.dpToPx(4));
        star.setClickable(true);
        star.setFocusable(true);
        return star;
    }

    /* ── Expression tray favorites tab ──────────────────────────────────── */

    private void patchExpressionSegmentedControl() throws NoSuchMethodException {
        patcher.patch(
            SegmentedControlContainer.class.getDeclaredMethod("a", int.class),
            new Hook(callFrame -> {
                try {
                    addFavoriteSegment((SegmentedControlContainer) callFrame.thisObject);
                } catch (Throwable t) {
                    logger.error("Error adding media tab to expression segmented control", t);
                }
            })
        );

        patcher.patch(
            SegmentedControlContainer.class.getDeclaredMethod("setSelectedIndex", int.class),
            new Hook(callFrame -> {
                try {
                    SegmentedControlContainer segments = (SegmentedControlContainer) callFrame.thisObject;
                    View favSegment = segments.findViewWithTag(FAV_SEGMENT_TAG);
                    if (favSegment == null) return;

                    int index = (int) callFrame.args[0];
                    if (index != segments.indexOfChild(favSegment)) {
                        hideFavContent(segments.getRootView());
                    }
                } catch (Throwable t) {
                    logger.error("Error hiding media favorites tab", t);
                }
            })
        );
    }

    private void addFavoriteSegment(SegmentedControlContainer segCtrl) {
        if (!isExpressionTraySegmentedControl(segCtrl)) return;
        if (segCtrl.findViewWithTag(FAV_SEGMENT_TAG) != null) return;

        Context ctx = segCtrl.getContext();
        View root = findExpressionTrayRoot(segCtrl);
        CardSegment mediaSegment = new CardSegment(ctx, null);
        mediaSegment.setTag(FAV_SEGMENT_TAG);
        mediaSegment.setText("Media");
        mediaSegment.setContentDescription("Open Media Favorites");
        mediaSegment.a(false);
        mediaSegment.setOnClickListener(v -> {
            activeFavType = activeFavType == null ? "images" : activeFavType;
            selectMediaSegment(segCtrl, mediaSegment);
            View trayRoot = findExpressionTrayRoot(segCtrl);
            ensureFavContent(ctx, trayRoot);
            showFavContent(trayRoot, activeFavType);
        });

        View referenceSegment = segCtrl.getChildCount() > 0 ? segCtrl.getChildAt(segCtrl.getChildCount() - 1) : null;
        LinearLayout.LayoutParams lp;
        if (referenceSegment != null && referenceSegment.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            lp = new LinearLayout.LayoutParams((LinearLayout.LayoutParams) referenceSegment.getLayoutParams());
        } else {
            lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        }
        segCtrl.addView(mediaSegment, lp);
        ensureFavContent(ctx, root);
        logger.info("Inserted Media tab into expression segmented control; children=" + segCtrl.getChildCount());
    }

    private View findExpressionTrayRoot(View view) {
        View current = view;
        while (current != null) {
            if (hasResourceEntryName(current, "expression_tray_container")) return current;
            if (!(current.getParent() instanceof View)) break;
            current = (View) current.getParent();
        }

        View root = view.getRootView();
        View trayRoot = findViewByResourceEntryName(root, "expression_tray_container");
        return trayRoot != null ? trayRoot : root;
    }

    private boolean hasResourceEntryName(View view, String name) {
        if (view == null || view.getId() == View.NO_ID) return false;
        try {
            return name.equals(view.getResources().getResourceEntryName(view.getId()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private View findViewByResourceEntryName(View root, String name) {
        if (hasResourceEntryName(root, name)) return root;
        if (!(root instanceof ViewGroup)) return null;

        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findViewByResourceEntryName(group.getChildAt(i), name);
            if (found != null) return found;
        }
        return null;
    }

    private boolean isExpressionTraySegmentedControl(SegmentedControlContainer segCtrl) {
        if (segCtrl == null || segCtrl.getId() == View.NO_ID) return false;
        try {
            return "expression_tray_segmented_control".equals(
                    segCtrl.getResources().getResourceEntryName(segCtrl.getId()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void selectMediaSegment(SegmentedControlContainer segCtrl, CardSegment mediaSegment) {
        for (int i = 0; i < segCtrl.getChildCount(); i++) {
            View child = segCtrl.getChildAt(i);
            if (child instanceof CardSegment) {
                ((CardSegment) child).a(child == mediaSegment);
            }
        }
    }

    /** 4 tabs: Images | GIFs | Videos | Audio */
    private LinearLayout createFavTypeTabs(Context ctx, TypeChangeListener listener) {
        LinearLayout favTabs = new LinearLayout(ctx);
        favTabs.setTag(FAV_TYPE_TABS_TAG);
        favTabs.setOrientation(LinearLayout.HORIZONTAL);
        favTabs.setGravity(Gravity.CENTER);
        favTabs.setPadding(
            DimenUtils.dpToPx(12), DimenUtils.dpToPx(8),
            DimenUtils.dpToPx(12), DimenUtils.dpToPx(6));

        String[] labels = {"Images", "GIFs", "Videos", "Audio"};
        String[] keys   = {"images", "gifs", "videos", "audio"};

        for (int i = 0; i < labels.length; i++) {
            final String key = keys[i];
            TextView tab = new TextView(ctx);
            tab.setText(labels[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(12f);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tab.setPadding(
                DimenUtils.dpToPx(8), DimenUtils.dpToPx(7),
                DimenUtils.dpToPx(8), DimenUtils.dpToPx(7));
            tab.setOnClickListener(v -> {
                activeFavType = key;
                updateFavTypeTabStyles(favTabs);
                listener.onTypeChanged(key);
            });
            favTabs.addView(tab, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        updateFavTypeTabStyles(favTabs);
        return favTabs;
    }

    private void ensureFavContent(Context ctx, View root) {
        ViewGroup contentContainer = (ViewGroup) findViewByResourceEntryName(
            root, "expression_tray_content_container");
        if (contentContainer != null) {
            FrameLayout favContent = contentContainer.findViewWithTag(FAV_CONTENT_TAG);
            if (favContent == null) {
                favContent = new FrameLayout(ctx);
                favContent.setTag(FAV_CONTENT_TAG);
                favContent.setVisibility(View.GONE);
                favContent.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                contentContainer.addView(favContent);
            }
        }
    }

    private void updateFavTypeTabStyles(LinearLayout tabs) {
        int idx = getActiveFavTypeIndex();
        for (int i = 0; i < tabs.getChildCount(); i++) {
            View child = tabs.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(i == idx
                    ? ColorCompat.getThemedColor(tabs.getContext(), com.lytefast.flexinput.R.b.colorInteractiveActive)
                    : ColorCompat.getThemedColor(tabs.getContext(), com.lytefast.flexinput.R.b.colorInteractiveMuted));
            }
        }
    }

    private void hideFavContent(View root) {
        root = findExpressionTrayRoot(root);
        ViewGroup contentContainer = (ViewGroup) findViewByResourceEntryName(
            root, "expression_tray_content_container");
        if (contentContainer != null) {
            FrameLayout favContent = contentContainer.findViewWithTag(FAV_CONTENT_TAG);
            if (favContent != null) favContent.setVisibility(View.GONE);
        }
    }

    private int getActiveFavTypeIndex() {
        if ("images".equals(activeFavType)) return 0;
        if ("gifs".equals(activeFavType))   return 1;
        if ("videos".equals(activeFavType)) return 2;
        if ("audio".equals(activeFavType))  return 3;
        return -1;
    }

    private void showFavContent(View root, String type) {
        root = findExpressionTrayRoot(root);
        final View trayRoot = root;
        activeFavType = type;
        FavoritesData data = getFavorites(settings);
        List<FavoriteAttachment> allItems = getListForType(data, type);
        List<FavoriteAttachment> list = filterFavorites(allItems, mediaSearchQuery);

        Context ctx = root.getContext();
        configureDefaultSearchBar(root);

        ViewGroup contentContainer = (ViewGroup) findViewByResourceEntryName(
            root, "expression_tray_content_container");
        if (contentContainer == null) {
            logger.warn("Media tab content container not found");
            return;
        }
        contentContainer.setVisibility(View.VISIBLE);

        // Hide Discord's own content views
        View emojiContent   = findViewByResourceEntryName(root, "expression_tray_emoji_picker_content");
        View gifContent     = findViewByResourceEntryName(root, "expression_tray_gif_picker_content");
        View stickerContent = findViewByResourceEntryName(root, "expression_tray_sticker_picker_content");
        if (emojiContent   != null) emojiContent.setVisibility(View.GONE);
        if (gifContent     != null) gifContent.setVisibility(View.GONE);
        if (stickerContent != null) stickerContent.setVisibility(View.GONE);

        FrameLayout favContent = contentContainer.findViewWithTag(FAV_CONTENT_TAG);
        if (favContent == null) return;

        favContent.removeAllViews();
        favContent.setVisibility(View.VISIBLE);
        favContent.bringToFront();

        LinearLayout page = new LinearLayout(ctx);
        page.setOrientation(LinearLayout.VERTICAL);
        favContent.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        page.addView(createFavTypeTabs(ctx, key -> showFavContent(trayRoot, key)),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addMediaGrid(ctx, page, allItems, list,
                () -> showFavContent(trayRoot, activeFavType),
                null, true, trayRoot);
    }

    private void addMediaGrid(
            Context ctx,
            LinearLayout page,
            List<FavoriteAttachment> allItems,
            List<FavoriteAttachment> list,
            Runnable refreshAction,
            Runnable afterClick,
            boolean fillRemainingHeight,
            View trayRoot
    ) {
        if (allItems.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText(getEmptyMessage(activeFavType));
            empty.setTextColor(ColorCompat.getThemedColor(ctx,
                com.lytefast.flexinput.R.b.colorInteractiveMuted));
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            page.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    fillRemainingHeight ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT,
                    fillRemainingHeight ? 1f : 0f));
            return;
        }

        RecyclerView recycler = new RecyclerView(ctx);
        int colCount = Math.max(2,
            page.getResources().getDisplayMetrics().widthPixels / DimenUtils.dpToPx(164));
        recycler.setLayoutManager(new StaggeredGridLayoutManager(colCount, 1));

        final FavoriteGridAdapter adapter = new FavoriteGridAdapter(
            list,
            fav -> {
                if ("gifs".equals(activeFavType)) {
                    sendGifFavorite(fav, trayRoot);
                } else {
                    addFavoriteToDraft(ctx, fav);
                }
                if (afterClick != null) afterClick.run();
            },
            fav -> showTagEditor(ctx, fav, activeFavType, refreshAction)
        );

        recycler.setAdapter(adapter);
        page.addView(recycler, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                fillRemainingHeight ? 0 : DimenUtils.dpToPx(420),
                fillRemainingHeight ? 1f : 0f));
    }

    private static String getEmptyMessage(String type) {
        if ("gifs".equals(type)) {
            return "No GIF favorites yet\n\nTap \u2605 on GIFs in the GIF picker\nto add them here.";
        }
        return "No favorites yet\n\nTap \u2605 on attachments in chat\nto add them here.";
    }

    /** Send a favorited GIF via the captured GIF select callback (same path as tapping a GIF normally). */
    private void sendGifFavorite(FavoriteAttachment fav, View trayRoot) {
        Function1 callback = currentGifSelectCallback;
        if (callback == null) {
            Utils.showToast("Open the GIF picker and browse GIFs first, then try again");
            return;
        }

        try {
            String gifImageUrl = fav.proxy_url != null ? fav.proxy_url : fav.url;
            String tenorGifUrl = fav.url     != null ? fav.url       : fav.proxy_url;
            int width  = fav.width  > 0 ? fav.width  : 100;
            int height = fav.height > 0 ? fav.height : 100;

            ModelGif modelGif = new ModelGif(gifImageUrl, tenorGifUrl, width, height);

            // Construct GifAdapterItem.GifItem reflectively (inner class may not be public)
            Class<?> gifItemClass = Class.forName(
                "com.discord.widgets.chat.input.gifpicker.GifAdapterItem$GifItem");
            Constructor<?> ctor = gifItemClass.getDeclaredConstructor(ModelGif.class, String.class);
            ctor.setAccessible(true);
            Object gifItem = ctor.newInstance(modelGif, "");

            // Fire the callback — equivalent to viewModel.selectGif(gifItem)
            callback.invoke(gifItem);

            // Restore the expression tray to show the native GIF picker tab
            if (trayRoot != null) {
                View resolvedRoot = findExpressionTrayRoot(trayRoot);
                hideFavContent(resolvedRoot);
                View segCtrlView = findViewByResourceEntryName(
                    resolvedRoot, "expression_tray_segmented_control");
                if (segCtrlView instanceof SegmentedControlContainer) {
                    restoreBuiltInExpressionTab((SegmentedControlContainer) segCtrlView, 1);
                }
            }
        } catch (Throwable t) {
            logger.error("Error sending GIF favorite", t);
            Utils.showToast("Could not send GIF");
        }
    }

    private void configureDefaultSearchBar(View root) {
        View searchBar    = findViewByResourceEntryName(root, "expression_tray_search_bar");
        View searchButton = findViewByResourceEntryName(root, "expression_tray_search_button");
        if (searchBar != null) {
            searchBar.setContentDescription("Search Media");
        }
        if (searchButton instanceof TextView) {
            TextView text = (TextView) searchButton;
            text.setHint("Search Media");
            text.setText(mediaSearchQuery == null ? "" : mediaSearchQuery);
        }
    }

    private boolean isExpressionSearchBarView(View view) {
        return hasResourceEntryName(view, "expression_tray_search_bar")
            || hasResourceEntryName(view, "expression_tray_search_button")
            || hasResourceEntryName(view, "expression_tray_search_icon");
    }

    private boolean isMediaFavoritesVisible(View view) {
        View root = findExpressionTrayRoot(view);
        ViewGroup contentContainer = (ViewGroup) findViewByResourceEntryName(
            root, "expression_tray_content_container");
        if (contentContainer == null) return false;

        FrameLayout favContent = contentContainer.findViewWithTag(FAV_CONTENT_TAG);
        return favContent != null && favContent.getVisibility() == View.VISIBLE;
    }

    private void showMediaSearchSheet(View root) {
        new MediaSearchBottomSheet(this, root)
            .show(Utils.getAppActivity().getSupportFragmentManager(), "MediaFavoritesSearch");
    }

    public static class MediaSearchBottomSheet extends BottomSheet {
        private final MediaFavorites plugin;
        private final View trayRoot;
        private LinearLayout mediaContainer;

        public MediaSearchBottomSheet(MediaFavorites plugin, View trayRoot) {
            this.plugin = plugin;
            this.trayRoot = trayRoot;
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            Context ctx = requireContext();
            setPadding(DimenUtils.dpToPx(12));

            FrameLayout searchWrap = new FrameLayout(ctx);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundTertiary));
            bg.setCornerRadius(DimenUtils.dpToPx(4));
            searchWrap.setBackground(bg);
            searchWrap.setMinimumHeight(DimenUtils.dpToPx(40));
            searchWrap.setContentDescription("Search Media");
            LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wrapLp.setMargins(0, 0, 0, DimenUtils.dpToPx(8));

            EditText input = new EditText(ctx);
            input.setSingleLine(true);
            input.setHint("Search Media");
            input.setText(mediaSearchQuery == null ? "" : mediaSearchQuery);
            input.setTextSize(14f);
            input.setGravity(Gravity.CENTER_VERTICAL);
            input.setBackgroundColor(Color.TRANSPARENT);
            input.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive));
            input.setHintTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveMuted));
            input.setPadding(
                DimenUtils.dpToPx(12), DimenUtils.dpToPx(8),
                DimenUtils.dpToPx(44), DimenUtils.dpToPx(8));
            input.setSelectAllOnFocus(false);
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mediaSearchQuery = s == null ? "" : s.toString();
                    renderMediaContent(ctx);
                    plugin.showFavContent(trayRoot, activeFavType);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            searchWrap.addView(input, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL));

            ImageView icon = new ImageView(ctx);
            icon.setImageResource(com.lytefast.flexinput.R.e.ic_search_white_24dp);
            icon.setColorFilter(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveMuted));
            icon.setContentDescription("Search Media");
            icon.setPadding(
                DimenUtils.dpToPx(8), DimenUtils.dpToPx(8),
                DimenUtils.dpToPx(8), DimenUtils.dpToPx(8));
            searchWrap.addView(icon, new FrameLayout.LayoutParams(
                    DimenUtils.dpToPx(40), ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.RIGHT | Gravity.CENTER_VERTICAL));

            searchWrap.setLayoutParams(wrapLp);
            addView(searchWrap);

            mediaContainer = new LinearLayout(ctx);
            mediaContainer.setOrientation(LinearLayout.VERTICAL);
            mediaContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(mediaContainer);
            renderMediaContent(ctx);

            input.requestFocus();
        }

        private void renderMediaContent(Context ctx) {
            if (mediaContainer == null) return;
            mediaContainer.removeAllViews();

            plugin.configureDefaultSearchBar(trayRoot);
            FavoritesData data = getFavorites(plugin.settings);
            List<FavoriteAttachment> allItems = getListForType(data, activeFavType);
            List<FavoriteAttachment> list = plugin.filterFavorites(allItems, mediaSearchQuery);

            mediaContainer.addView(plugin.createFavTypeTabs(ctx, key -> renderMediaContent(ctx)),
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            plugin.addMediaGrid(ctx, mediaContainer, allItems, list, () -> {
                renderMediaContent(ctx);
                plugin.showFavContent(trayRoot, activeFavType);
            }, this::dismiss, false, trayRoot);
        }
    }

    private void addFavoriteToDraft(Context ctx, FavoriteAttachment fav) {
        WidgetChatInputAttachments chatAttachments = currentChatAttachments;
        if (chatAttachments == null) {
            Utils.showToast("Chat input is not ready");
            return;
        }

        String mediaUrl = fav.url != null ? fav.url : fav.proxy_url;
        if (mediaUrl == null) return;

        Utils.showToast("Adding attachment...");
        Utils.threadPool.execute(() -> {
            try {
                File file = downloadFavoriteToCache(ctx, fav, mediaUrl);
                Attachment<Object> attachment = new Attachment<>(
                    System.currentTimeMillis(),
                    Uri.fromFile(file),
                    file.getName(),
                    null,
                    false
                );
                Utils.getAppActivity().runOnUiThread(() -> {
                    try {
                        chatAttachments.addExternalAttachment(attachment);
                    } catch (Throwable t) {
                        logger.error("Error adding favorite to draft", t);
                        Utils.showToast("Could not add attachment");
                    }
                });
            } catch (Throwable t) {
                logger.error("Error downloading favorite attachment", t);
                Utils.getAppActivity().runOnUiThread(() ->
                    Utils.showToast("Could not download attachment"));
            }
        });
    }

    private File downloadFavoriteToCache(Context ctx, FavoriteAttachment fav, String mediaUrl) throws Exception {
        File dir = new File(ctx.getCacheDir(), CACHE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create media favorites cache");
        }

        String filename = sanitizeFilename(fav.filename);
        File file = new File(dir, Math.abs(mediaUrl.hashCode()) + "_" + filename);

        HttpURLConnection connection = (HttpURLConnection) new URL(mediaUrl).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "Aliucord MediaFavorites");
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }

        return file;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) return "favorite_media";
        String sanitized = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isEmpty()) return "favorite_media";
        return sanitized;
    }

    private List<FavoriteAttachment> filterFavorites(List<FavoriteAttachment> items, String query) {
        if (query == null || query.trim().isEmpty()) return items;

        String q = query.trim().toLowerCase();
        List<FavoriteAttachment> filtered = new ArrayList<>();
        for (FavoriteAttachment fav : items) {
            if (fav == null) continue;
            if (fav.filename != null && fav.filename.toLowerCase().contains(q)) {
                filtered.add(fav);
                continue;
            }
            for (String tag : getTags(fav)) {
                if (tag.toLowerCase().contains(q)) {
                    filtered.add(fav);
                    break;
                }
            }
        }
        return filtered;
    }

    private void showTagEditor(Context ctx, FavoriteAttachment fav, String type, Runnable afterSave) {
        InputDialog dialog = new InputDialog()
            .setTitle(fav.filename == null ? "Media tags" : fav.filename)
            .setDescription("Separate tags with commas.")
            .setPlaceholderText("tag1, tag2, tag3");

        dialog.setOnDialogShownListener(view -> {
            EditText editText = dialog.getInputLayout().getEditText();
            if (editText != null) {
                editText.setSingleLine(false);
                editText.setMinLines(2);
                editText.setText(joinTags(getTags(fav)));
                editText.setSelection(editText.getText().length());
            }
        });
        dialog.setOnOkListener(v -> {
            saveFavoriteTags(fav.getKey(), type, parseTags(dialog.getInput()));
            dialog.dismiss();
            if (afterSave != null) afterSave.run();
        });
        dialog.show(Utils.getAppActivity().getSupportFragmentManager(), "MediaFavoritesTags");
    }

    private static List<String> getTags(FavoriteAttachment fav) {
        if (fav == null) return Collections.emptyList();
        if (fav.tags == null) fav.tags = new ArrayList<>();
        return fav.tags;
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) continue;
            if (out.length() > 0) out.append(", ");
            out.append(tag.trim());
        }
        return out.toString();
    }

    private static List<String> parseTags(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();

        List<String> tags = new ArrayList<>();
        for (String part : raw.split(",")) {
            String tag = part.trim();
            if (tag.isEmpty()) continue;

            boolean duplicate = false;
            for (String existing : tags) {
                if (existing.equalsIgnoreCase(tag)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) tags.add(tag);
        }
        return tags;
    }

    /* ── Favorites CRUD ─────────────────────────────────────────────────── */

    public static FavoritesData getFavorites(SettingsAPI settings) {
        try {
            String json = settings.getString(FAVORITES_KEY, null);
            if (json == null) return new FavoritesData();
            FavoritesData data = GsonUtils.fromJson(json, FavoritesData.class);
            return data != null ? data : new FavoritesData();
        } catch (Throwable t) {
            logger.error("Error loading favorites", t);
            return new FavoritesData();
        }
    }

    public static void saveFavorites(SettingsAPI settings, FavoritesData data) {
        try { settings.setString(FAVORITES_KEY, GsonUtils.toJson(data)); }
        catch (Throwable t) { logger.error("Error saving favorites", t); }
    }

    public static boolean toggleFavorite(FavoriteAttachment fav, String type) {
        SettingsAPI s = instance != null ? instance.settings : null;
        if (s == null) return false;
        FavoritesData data = getFavorites(s);
        List<FavoriteAttachment> list = getListForType(data, type);
        String key = fav.getKey();

        FavoriteAttachment existing = null;
        for (FavoriteAttachment f : list) {
            if (key.equals(f.getKey())) { existing = f; break; }
        }

        if (existing != null) {
            list.remove(existing);
            saveFavorites(s, data);
            return false;
        } else {
            fav.added_timestamp = System.currentTimeMillis();
            list.add(fav);
            saveFavorites(s, data);
            return true;
        }
    }

    public static FavoriteAttachment ensureFavorite(FavoriteAttachment fav, String type) {
        SettingsAPI s = instance != null ? instance.settings : null;
        if (s == null || fav == null || fav.getKey() == null) return fav;

        FavoritesData data = getFavorites(s);
        List<FavoriteAttachment> list = getListForType(data, type);
        String key = fav.getKey();
        for (FavoriteAttachment existing : list) {
            if (key.equals(existing.getKey())) return existing;
        }

        fav.added_timestamp = System.currentTimeMillis();
        list.add(fav);
        saveFavorites(s, data);
        return fav;
    }

    public static boolean isFavorited(String url) {
        SettingsAPI s = instance != null ? instance.settings : null;
        if (s == null || url == null) return false;
        FavoritesData data = getFavorites(s);
        List[] allLists = {
            data.images != null ? data.images : Collections.emptyList(),
            data.gifs   != null ? data.gifs   : Collections.emptyList(),
            data.videos != null ? data.videos : Collections.emptyList(),
            data.audio  != null ? data.audio  : Collections.emptyList()
        };
        for (List<FavoriteAttachment> list : allLists) {
            for (FavoriteAttachment f : list) {
                if (url.equals(f.getKey())) return true;
            }
        }
        return false;
    }

    public static void removeFavorite(String url, String type) {
        SettingsAPI s = instance != null ? instance.settings : null;
        if (s == null || url == null) return;
        FavoritesData data = getFavorites(s);
        getListForType(data, type).removeIf(f -> url.equals(f.getKey()));
        saveFavorites(s, data);
    }

    public static void saveFavoriteTags(String url, String type, List<String> tags) {
        SettingsAPI s = instance != null ? instance.settings : null;
        if (s == null || url == null) return;

        FavoritesData data = getFavorites(s);
        List<FavoriteAttachment> list = getListForType(data, type);
        for (FavoriteAttachment fav : list) {
            if (url.equals(fav.getKey())) {
                fav.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
                break;
            }
        }
        saveFavorites(s, data);
    }

    public static List<FavoriteAttachment> getListForType(FavoritesData data, String type) {
        switch (type) {
            case "images": case "image":
                if (data.images == null) data.images = new ArrayList<>();
                return data.images;
            case "gifs": case "gif":
                if (data.gifs == null) data.gifs = new ArrayList<>();
                return data.gifs;
            case "videos": case "video":
                if (data.videos == null) data.videos = new ArrayList<>();
                return data.videos;
            case "audio":
                if (data.audio == null) data.audio = new ArrayList<>();
                return data.audio;
            default:
                if (data.images == null) data.images = new ArrayList<>();
                return data.images;
        }
    }

    private static MediaFavorites instance;
}
