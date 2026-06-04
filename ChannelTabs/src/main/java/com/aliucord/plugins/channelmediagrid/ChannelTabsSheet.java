package com.aliucord.plugins.channelmediagrid;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.PagerAdapter;

import com.facebook.drawee.view.SimpleDraweeView;
import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.GsonUtils;
import com.discord.app.AppFragment;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.images.MGImages;
import com.discord.widgets.media.WidgetMedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChannelTabsSheet extends AppFragment {
    static final String ARG_GUILD_ID = "guild_id";
    static final String ARG_CHANNEL_ID = "channel_id";
    private static final int PAGE_SIZE = 25;
    private static final int VIEW_TYPE_FOOTER = 100;
    private static final int VIEW_TYPE_MEDIA_GRID = 101;
    private long guildId;
    private long channelId;

    public enum Tab {
        MEDIA,
        FILES,
        LINKS
    }

    private Tab currentTab = Tab.MEDIA;
    private TextView mediaTabButton;
    private TextView filesTabButton;
    private TextView linksTabButton;
    private ViewPager viewPager;
    private TextView gridToggleBtn;
    private boolean isMediaGrid = false;

    private static class TabState {
        final Tab tab;
        final ArrayList<MediaItem> items = new ArrayList<>();
        final Set<String> seenUrls = new HashSet<>();
        RecyclerView recycler;
        MediaAdapter adapter;
        TextView status;
        ProgressBar spinner;
        int offset = 0;
        boolean loading = false;
        boolean canLoadMore = true;

        TabState(Tab tab) {
            this.tab = tab;
        }

        String getEmptyMessage() {
            if (tab == Tab.MEDIA) return "No media found";
            if (tab == Tab.FILES) return "No files found";
            return "No links found";
        }
    }

    private final TabState[] tabStates = new TabState[] {
        new TabState(Tab.MEDIA),
        new TabState(Tab.FILES),
        new TabState(Tab.LINKS)
    };

    public ChannelTabsSheet() {
    }

    static Bundle createArgs(long guildId, long channelId) {
        Bundle args = new Bundle();
        args.putLong(ARG_GUILD_ID, guildId);
        args.putLong(ARG_CHANNEL_ID, channelId);
        return args;
    }

    public static ChannelTabsSheet newInstance(long guildId, long channelId) {
        ChannelTabsSheet fragment = new ChannelTabsSheet();
        Bundle args = createArgs(guildId, channelId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(inflater.getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            guildId = args.getLong(ARG_GUILD_ID);
            channelId = args.getLong(ARG_CHANNEL_ID);
        }
        isMediaGrid = ChannelTabs.isMediaGridMode();
        setActionBarTitle(channelId == 0 ? "Server Content" : "Channel Content");
        setActionBarDisplayHomeAsUpEnabled(true);
        setOnBackPressed(() -> {
            closePage();
            return true;
        });

        android.content.Context ctx = view.getContext();
        LinearLayout root = (LinearLayout) view;
        root.setBackgroundColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundPrimary));
        root.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(36), DimenUtils.dpToPx(12), 0);

        LinearLayout headerLayout = new LinearLayout(ctx);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageButton backButton = new ImageButton(ctx);
        backButton.setContentDescription("Back");
        backButton.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(12), DimenUtils.dpToPx(12), DimenUtils.dpToPx(12));
        backButton.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        backButton.setBackgroundResource(resolveSelectableBorderless(ctx));
        backButton.setOnClickListener(v -> closePage());

        android.graphics.drawable.Drawable backIcon = resolveBackIcon(ctx);
        if (backIcon != null) {
            backIcon = backIcon.mutate();
            backIcon.setTint(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal));
            backButton.setImageDrawable(backIcon);
        }
        headerLayout.addView(backButton, new LinearLayout.LayoutParams(
                DimenUtils.dpToPx(48),
                DimenUtils.dpToPx(48)
        ));

        TextView title = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText(channelId == 0 ? "Server Content" : "Channel Content");
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, DimenUtils.dpToPx(16), 0, DimenUtils.dpToPx(12));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        headerLayout.addView(title);

        gridToggleBtn = new TextView(ctx);
        gridToggleBtn.setGravity(Gravity.CENTER);
        gridToggleBtn.setTextSize(13f);
        gridToggleBtn.setTypeface(Typeface.DEFAULT_BOLD);
        gridToggleBtn.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(8), DimenUtils.dpToPx(12), DimenUtils.dpToPx(8));
        gridToggleBtn.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal));
        gridToggleBtn.setOnClickListener(v -> {
            isMediaGrid = !isMediaGrid;
            ChannelTabs.setMediaGridMode(isMediaGrid);
            updateGridToggleStyle();
            refreshMediaLayout();
        });
        headerLayout.addView(gridToggleBtn, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        root.addView(headerLayout);

        LinearLayout tabsLayout = new LinearLayout(ctx);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tabsLayout.setPadding(0, DimenUtils.dpToPx(4), 0, DimenUtils.dpToPx(12));

        mediaTabButton = createTabButton(ctx, "Media", Tab.MEDIA);
        filesTabButton = createTabButton(ctx, "Files", Tab.FILES);
        linksTabButton = createTabButton(ctx, "Links", Tab.LINKS);

        tabsLayout.addView(mediaTabButton);
        tabsLayout.addView(filesTabButton);
        tabsLayout.addView(linksTabButton);
        root.addView(tabsLayout);

        viewPager = new ViewPager(ctx);
        viewPager.setId(View.generateViewId());
        viewPager.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        viewPager.setAdapter(new ChannelTabsPagerAdapter(ctx));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                currentTab = Tab.values()[position];
                updateTabStyles();
                if (gridToggleBtn != null) {
                    gridToggleBtn.setVisibility(currentTab == Tab.MEDIA ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
        root.addView(viewPager);

        updateTabStyles();
        updateGridToggleStyle();
    }

    private TextView createTabButton(android.content.Context ctx, String text, Tab tab) {
        TextView button = new TextView(ctx);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                DimenUtils.dpToPx(36),
                1f
        );
        params.setMargins(DimenUtils.dpToPx(4), 0, DimenUtils.dpToPx(4), 0);
        button.setLayoutParams(params);
        button.setPadding(DimenUtils.dpToPx(8), 0, DimenUtils.dpToPx(8), 0);
        
        button.setOnClickListener(v -> selectTab(tab));
        return button;
    }

    private void selectTab(Tab tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        updateTabStyles();
        viewPager.setCurrentItem(tab.ordinal(), true);
    }

    private void updateTabStyles() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        int activeColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundSecondary);
        int inactiveColor = 0; // Transparent
        int activeTextColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive);
        int inactiveTextColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal);

        setTabButtonStyle(mediaTabButton, currentTab == Tab.MEDIA, activeColor, inactiveColor, activeTextColor, inactiveTextColor);
        setTabButtonStyle(filesTabButton, currentTab == Tab.FILES, activeColor, inactiveColor, activeTextColor, inactiveTextColor);
        setTabButtonStyle(linksTabButton, currentTab == Tab.LINKS, activeColor, inactiveColor, activeTextColor, inactiveTextColor);
        updateGridToggleStyle();
    }

    private void setTabButtonStyle(TextView button, boolean selected, int activeBg, int inactiveBg, int activeText, int inactiveText) {
        if (button == null) return;
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setCornerRadius(DimenUtils.dpToPx(18));
        gd.setColor(selected ? activeBg : inactiveBg);
        
        button.setBackground(gd);
        button.setTextColor(selected ? activeText : inactiveText);
    }

    private void updateGridToggleStyle() {
        if (gridToggleBtn == null) return;
        android.content.Context ctx = gridToggleBtn.getContext();
        gridToggleBtn.setText(isMediaGrid ? "List" : "Grid");
        gridToggleBtn.setVisibility(currentTab == Tab.MEDIA ? View.VISIBLE : View.GONE);

        int bgColor = ColorCompat.getThemedColor(ctx, isMediaGrid
                ? com.lytefast.flexinput.R.b.colorBackgroundSecondary
                : com.lytefast.flexinput.R.b.colorBackgroundTertiary);
        int textColor = ColorCompat.getThemedColor(ctx, isMediaGrid
                ? com.lytefast.flexinput.R.b.colorInteractiveActive
                : com.lytefast.flexinput.R.b.colorInteractiveNormal);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(DimenUtils.dpToPx(16));
        bg.setColor(bgColor);
        gridToggleBtn.setBackground(bg);
        gridToggleBtn.setTextColor(textColor);
    }

    private int resolveSelectableBorderless(android.content.Context ctx) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)) {
            return typedValue.resourceId;
        }
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
            return typedValue.resourceId;
        }
        return 0;
    }

    private android.graphics.drawable.Drawable resolveBackIcon(android.content.Context ctx) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        int backAttr = Utils.getResId("ic_action_bar_back", "attr");
        if (backAttr != 0 && ctx.getTheme().resolveAttribute(backAttr, typedValue, true) && typedValue.resourceId != 0) {
            android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(ctx, typedValue.resourceId);
            if (icon != null) return icon;
        }

        int backDrawable = Utils.getResId("ic_arrow_back_white_24dp", "drawable");
        if (backDrawable == 0) backDrawable = Utils.getResId("ic_arrow_back_dark_grey_24dp", "drawable");
        return backDrawable == 0 ? null : androidx.core.content.ContextCompat.getDrawable(ctx, backDrawable);
    }

    private void closePage() {
        try {
            getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        } catch (Throwable ignored) {
        }
    }

    private void refreshMediaLayout() {
        TabState state = tabStates[Tab.MEDIA.ordinal()];
        if (state.recycler == null) return;
        
        android.content.Context ctx = state.recycler.getContext();
        if (isMediaGrid) {
            androidx.recyclerview.widget.GridLayoutManager glm = new androidx.recyclerview.widget.GridLayoutManager(ctx, 3);
            glm.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (state.adapter != null && state.adapter.getItemViewType(position) == VIEW_TYPE_FOOTER) {
                        return 3;
                    }
                    return 1;
                }
            });
            state.recycler.setLayoutManager(glm);
        } else {
            state.recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(ctx));
        }
        if (state.adapter != null) {
            state.recycler.getRecycledViewPool().clear();
            state.adapter.notifyDataSetChanged();
        }
    }

    private void loadNextPage(TabState state) {
        if (state.loading || !state.canLoadMore) return;
        state.loading = true;
        updateLoadingState(state);

        Utils.threadPool.execute(() -> {
            try {
                SearchTab responseTab = fetchPage(state);
                Utils.mainThread.post(() -> applyResponse(state, responseTab));
            } catch (Throwable throwable) {
                Utils.mainThread.post(() -> {
                    if (!isAdded()) return;
                    state.loading = false;
                    updateLoadingState(state);
                    if (state.status != null) {
                        state.status.setText("Failed to load items");
                        state.status.setVisibility(View.VISIBLE);
                    }
                    Utils.showToast("Failed to load items");
                });
            }
        });
    }

    private SearchTab fetchPage(TabState state) throws Exception {
        Map<String, Object> tabConfig = new HashMap<>();
        tabConfig.put("limit", PAGE_SIZE);
        tabConfig.put("sort_by", "timestamp");
        tabConfig.put("sort_order", "desc");
        if (state.offset > 0) tabConfig.put("offset", state.offset);

        String tabKey;
        if (state.tab == Tab.MEDIA) {
            tabKey = "media";
            tabConfig.put("has", new String[]{"image", "video"});
        } else if (state.tab == Tab.FILES) {
            tabKey = "files";
            tabConfig.put("has", new String[]{"file"});
        } else {
            tabKey = "links";
            tabConfig.put("has", new String[]{"link"});
        }

        Map<String, Object> tabs = new HashMap<>();
        tabs.put(tabKey, tabConfig);

        Map<String, Object> body = new HashMap<>();
        body.put("tabs", tabs);
        body.put("include_nsfw", true);
        body.put("track_exact_total_hits", false);

        String url;
        if (guildId == 0) {
            url = "/channels/" + channelId + "/messages/search/tabs";
        } else {
            url = "/guilds/" + guildId + "/messages/search/tabs";
            if (channelId != 0) {
                body.put("channel_ids", new String[]{String.valueOf(channelId)});
            }
        }

        Http.Response response = Http.Request
                .newDiscordRNRequest(url, "POST")
                .executeWithJson(body);
        SearchResponse searchResponse = GsonUtils.fromJson(response.text(), SearchResponse.class);
        if (searchResponse == null || searchResponse.tabs == null) return null;

        if (state.tab == Tab.MEDIA) return searchResponse.tabs.media;
        if (state.tab == Tab.FILES) return searchResponse.tabs.files;
        return searchResponse.tabs.links;
    }

    private void applyResponse(TabState state, SearchTab tab) {
        if (!isAdded()) return;
        state.loading = false;

        if (tab == null) {
            state.canLoadMore = false;
            updateLoadingState(state);
            return;
        }

        int oldSize = state.items.size();
        int messageCount = 0;
        
        Map<Long, String> channelNames = new HashMap<>();
        if (tab.channels != null) {
            for (Channel chan : tab.channels) {
                if (chan != null) {
                    channelNames.put(chan.id, chan.name);
                }
            }
        }
        
        if (tab.messages != null) {
            for (Message[] group : tab.messages) {
                if (group == null) continue;
                messageCount += group.length;
                for (Message message : group) {
                    if (message == null) continue;
                    if (message.hit != null && !message.hit) continue;
                    
                    String authorName = message.author != null ? message.author.username : "Unknown";
                    String authorAvatarUrl = message.author != null ? message.author.getAvatarUrl() : "https://cdn.discordapp.com/embed/avatars/0.png";
                    long channelId = message.channel_id;
                    long messageId = message.id;
                    String channelName = channelNames.get(channelId);
                    if (channelName == null) {
                        channelName = "channel";
                    }

                    if (state.tab == Tab.MEDIA) {
                        if (message.attachments != null) {
                            for (Attachment attachment : message.attachments) {
                                if (attachment == null || attachment.url == null) continue;
                                boolean video = isVideo(attachment.content_type, attachment.filename);
                                String preview = video
                                        ? videoPreviewUrl(attachment.proxy_url != null ? attachment.proxy_url : attachment.url)
                                        : (attachment.proxy_url != null ? attachment.proxy_url : attachment.url);
                                addMedia(state, preview, attachment.url, attachment.content_type, attachment.filename, attachment.width, attachment.height, authorName, authorAvatarUrl, channelName, channelId, messageId);
                            }
                        }
                        if (message.embeds != null) {
                            for (Embed embed : message.embeds) {
                                if (embed == null) continue;
                                if (embed.video != null && embed.video.url != null) {
                                    String preview = embed.thumbnail != null && embed.thumbnail.url != null
                                            ? (embed.thumbnail.proxy_url != null ? embed.thumbnail.proxy_url : embed.thumbnail.url)
                                            : videoPreviewUrl(embed.video.url);
                                    addMedia(state, preview, embed.video.url, "video", null, embed.video.width, embed.video.height, authorName, authorAvatarUrl, channelName, channelId, messageId);
                                    continue;
                                }
                                if (embed.image != null && embed.image.url != null) {
                                    addMedia(state, embed.image.proxy_url != null ? embed.image.proxy_url : embed.image.url, embed.image.url, "image", null, embed.image.width, embed.image.height, authorName, authorAvatarUrl, channelName, channelId, messageId);
                                } else if (embed.thumbnail != null && embed.thumbnail.url != null) {
                                    addMedia(state, embed.thumbnail.proxy_url != null ? embed.thumbnail.proxy_url : embed.thumbnail.url, embed.thumbnail.url, "image", null, embed.thumbnail.width, embed.thumbnail.height, authorName, authorAvatarUrl, channelName, channelId, messageId);
                                }
                            }
                        }
                    } else if (state.tab == Tab.FILES) {
                        if (message.attachments != null) {
                            for (Attachment attachment : message.attachments) {
                                if (attachment == null || attachment.url == null) continue;
                                addFileItem(state, attachment.url, attachment.filename, attachment.size, attachment.content_type, authorName, authorAvatarUrl, channelName, channelId, messageId);
                            }
                        }
                    } else if (state.tab == Tab.LINKS) {
                        Set<String> addedLinksInMessage = new HashSet<>();
                        if (message.embeds != null) {
                            for (Embed embed : message.embeds) {
                                if (embed == null || embed.url == null) continue;
                                addLinkItem(state, embed.url, embed.title, embed.description, authorName, authorAvatarUrl, channelName, channelId, messageId);
                                addedLinksInMessage.add(embed.url);
                            }
                        }
                        if (message.content != null && !message.content.isEmpty()) {
                            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                                     "https?://[\\w\\d\\-_]+(?:\\.[\\w\\d\\-_]+)+(?:/[\\w\\d\\-./?%&=]*)?",
                                     java.util.regex.Pattern.CASE_INSENSITIVE
                             ).matcher(message.content);
                            while (matcher.find()) {
                                String url = matcher.group();
                                if (!addedLinksInMessage.contains(url)) {
                                    addLinkItem(state, url, null, null, authorName, authorAvatarUrl, channelName, channelId, messageId);
                                    addedLinksInMessage.add(url);
                                }
                            }
                        }
                    }
                }
            }
        }

        state.offset += PAGE_SIZE;
        state.canLoadMore = messageCount >= PAGE_SIZE && (tab.total_results <= 0 || state.offset < tab.total_results);
        
        if (state.items.size() > oldSize) {
            if (state.adapter != null) {
                state.adapter.notifyItemRangeInserted(oldSize, state.items.size() - oldSize);
            }
        }

        updateLoadingState(state);
    }

    private void addMedia(TabState state, String previewUrl, String url, String contentType, String filename, Integer width, Integer height, String authorName, String authorAvatarUrl, String channelName, long channelId, long messageId) {
        if (url == null || previewUrl == null) return;
        if (!state.seenUrls.add(url)) return;

        boolean video = isVideo(contentType, filename);
        state.items.add(new MediaItem(previewUrl, url, filename, video, width, height, null, null, null, null, authorName, authorAvatarUrl, channelName, channelId, messageId));
    }

    private void addFileItem(TabState state, String url, String filename, Long size, String contentType, String authorName, String authorAvatarUrl, String channelName, long channelId, long messageId) {
        if (url == null) return;
        if (!state.seenUrls.add(url)) return;
        state.items.add(new MediaItem(null, url, filename, false, null, null, size, contentType, null, null, authorName, authorAvatarUrl, channelName, channelId, messageId));
    }

    private void addLinkItem(TabState state, String url, String title, String description, String authorName, String authorAvatarUrl, String channelName, long channelId, long messageId) {
        if (url == null) return;
        if (!state.seenUrls.add(url)) return;
        state.items.add(new MediaItem(null, url, null, false, null, null, null, null, title, description, authorName, authorAvatarUrl, channelName, channelId, messageId));
    }

    private boolean isVideo(String contentType, String filename) {
        return (contentType != null && contentType.toLowerCase().contains("video"))
                || (filename != null && filename.toLowerCase().matches(".*\\.(mp4|mov|webm|mkv)$"));
    }

    private String videoPreviewUrl(String url) {
        if (url == null) return null;
        String preview = url.replace("https://cdn.discordapp.com/", "https://media.discordapp.net/");
        String separator = preview.contains("?") ? "&" : "?";
        return preview + separator + "format=webp&width=320&height=320";
    }

    private void openMedia(View view, MediaItem selected) {
        WidgetMedia.Companion.launch(view.getContext(), selected.toAttachment());
    }

    private void setStatus(TabState state, String text) {
        if (state.status == null) return;
        state.status.setText(text);
        state.status.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateLoadingState(TabState state) {
        if (state.spinner == null) return;

        if (state.loading) {
            if (state.items.isEmpty()) {
                state.spinner.setVisibility(View.VISIBLE);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) state.spinner.getLayoutParams();
                lp.gravity = Gravity.CENTER;
                lp.bottomMargin = 0;
                state.spinner.setLayoutParams(lp);
            } else {
                state.spinner.setVisibility(View.GONE);
            }

            if (state.status != null) {
                state.status.setVisibility(View.GONE);
            }
        } else {
            state.spinner.setVisibility(View.GONE);

            if (state.status != null) {
                String emptyMsg = state.items.isEmpty() ? state.getEmptyMessage() : "";
                state.status.setText(emptyMsg);
                state.status.setVisibility(emptyMsg.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }

        if (state.adapter != null) {
            state.adapter.notifyDataSetChanged();
        }
    }

    private class ChannelTabsPagerAdapter extends PagerAdapter {
        private final android.content.Context ctx;

        ChannelTabsPagerAdapter(android.content.Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public int getCount() {
            return tabStates.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TabState state = tabStates[position];
            
            FrameLayout root = new FrameLayout(ctx);
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            state.recycler = new RecyclerView(ctx);
            if (state.tab == Tab.MEDIA && isMediaGrid) {
                androidx.recyclerview.widget.GridLayoutManager glm = new androidx.recyclerview.widget.GridLayoutManager(ctx, 3);
                glm.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (state.adapter != null && state.adapter.getItemViewType(position) == VIEW_TYPE_FOOTER) {
                            return 3;
                        }
                        return 1;
                    }
                });
                state.recycler.setLayoutManager(glm);
            } else {
                state.recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(ctx));
            }
            
            state.adapter = new MediaAdapter(state);
            state.recycler.setAdapter(state.adapter);
            state.recycler.setNestedScrollingEnabled(true);
            state.recycler.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            
            state.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy <= 0 || !state.canLoadMore || state.loading) return;
                    var layoutManager = (androidx.recyclerview.widget.LinearLayoutManager) state.recycler.getLayoutManager();
                    if (layoutManager == null) return;
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    if (state.items.size() > 0 && lastVisible >= state.items.size() - 4) {
                        loadNextPage(state);
                    }
                }
            });
            root.addView(state.recycler);

            state.status = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_SubText);
            state.status.setGravity(Gravity.CENTER);
            state.status.setPadding(DimenUtils.dpToPx(24), DimenUtils.dpToPx(24), DimenUtils.dpToPx(24), DimenUtils.dpToPx(24));
            FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
            );
            state.status.setLayoutParams(statusParams);
            state.status.setVisibility(View.GONE);
            root.addView(state.status);

            state.spinner = new ProgressBar(ctx);
            FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
            );
            state.spinner.setLayoutParams(spinnerParams);
            state.spinner.setVisibility(View.GONE);

            int accentColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive);
            if (accentColor != 0) {
                try {
                    state.spinner.getIndeterminateDrawable().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
                } catch (Throwable ignored) {}
            }
            root.addView(state.spinner);

            container.addView(root);

            // Fetch initial page if we haven't loaded anything yet
            if (state.items.isEmpty() && !state.loading && state.canLoadMore) {
                loadNextPage(state);
            } else {
                updateLoadingState(state);
            }

            return root;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
            tabStates[position].recycler = null;
            tabStates[position].adapter = null;
            tabStates[position].status = null;
            tabStates[position].spinner = null;
        }
    }

    private static class MediaItem {
        final String previewUrl;
        final String url;
        final String filename;
        final boolean video;
        final Integer width;
        final Integer height;

        // Files fields
        final Long fileSize;
        final String contentType;

        // Links fields
        final String title;
        final String description;
        final String authorName;
        final String authorAvatarUrl;

        // Metadata and Navigation fields
        final String channelName;
        final long channelId;
        final long messageId;

        MediaItem(String previewUrl, String url, String filename, boolean video, Integer width, Integer height,
                  Long fileSize, String contentType, String title, String description, String authorName, String authorAvatarUrl,
                  String channelName, long channelId, long messageId) {
            this.previewUrl = previewUrl;
            this.url = url;
            this.filename = filename;
            this.video = video;
            this.width = width;
            this.height = height;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.title = title;
            this.description = description;
            this.authorName = authorName;
            this.authorAvatarUrl = authorAvatarUrl;
            this.channelName = channelName;
            this.channelId = channelId;
            this.messageId = messageId;
        }
        boolean hasImagePreview() {
            if (previewUrl == null) return false;
            String lower = previewUrl.toLowerCase();
            return lower.contains("format=webp")
                    || lower.contains("format=jpg")
                    || lower.contains("format=jpeg")
                    || !lower.matches(".*\\.(mp4|mov|webm|mkv)(\\?.*)?$");
        }
        MessageAttachment toAttachment() {
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("id", 0L);
            attachment.put("size", 0L);
            attachment.put("url", url);
            attachment.put("proxyUrl", video ? url : previewUrl);
            attachment.put("proxy_url", video ? url : previewUrl);
            attachment.put("filename", filename == null ? (video ? "video.mp4" : "image") : filename);
            attachment.put("width", width);
            attachment.put("height", height);
            return GsonUtils.fromJson(GsonUtils.toJson(attachment), MessageAttachment.class);
        }
    }

    private LinearLayout createCardContainer(android.content.Context ctx) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        
        RecyclerView.LayoutParams cardParams = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, DimenUtils.dpToPx(12));
        container.setLayoutParams(cardParams);
        container.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(12), DimenUtils.dpToPx(12), DimenUtils.dpToPx(12));
        
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(DimenUtils.dpToPx(8));
        cardBg.setColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundSecondary));
        container.setBackground(cardBg);
        
        return container;
    }

    private LinearLayout createMetadataView(android.content.Context ctx, SimpleDraweeView[] avatarViewOut, TextView[] textViewsOut) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, DimenUtils.dpToPx(8));
        layout.setLayoutParams(lp);

        SimpleDraweeView avatar = new SimpleDraweeView(ctx);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(
                DimenUtils.dpToPx(24),
                DimenUtils.dpToPx(24)
        );
        avatarLp.setMargins(0, 0, DimenUtils.dpToPx(8), 0);
        avatar.setLayoutParams(avatarLp);
        
        try {
            Object hierarchy = avatar.getHierarchy();
            Class<?> genericHierarchyClass = Class.forName("com.facebook.drawee.generic.GenericDraweeHierarchy");
            Class<?> roundingParamsClass = Class.forName("com.facebook.drawee.generic.RoundingParams");
            Object roundingParams = roundingParamsClass.getMethod("asCircle").invoke(null);
            genericHierarchyClass.getMethod("setRoundingParams", roundingParamsClass).invoke(hierarchy, roundingParams);
        } catch (Throwable ignored) {}

        avatar.setClipToOutline(true);
        avatar.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                int size = DimenUtils.dpToPx(24);
                outline.setOval(0, 0, size, size);
            }
        });
        
        layout.addView(avatar);
        
        TextView metaText = new TextView(ctx);
        metaText.setTextSize(12f);
        metaText.setTypeface(Typeface.DEFAULT_BOLD);
        metaText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorTextMuted));
        
        layout.addView(metaText);
        
        if (avatarViewOut != null && avatarViewOut.length > 0) {
            avatarViewOut[0] = avatar;
        }
        if (textViewsOut != null && textViewsOut.length > 0) {
            textViewsOut[0] = metaText;
        }
        
        return layout;
    }

    private void setupMetadataClick(View view, MediaItem item) {
        view.setOnClickListener(v -> {
            if (item.channelId != 0 && item.messageId != 0) {
                try {
                    com.discord.stores.StoreStream.getMessagesLoader().jumpToMessage(item.channelId, item.messageId);
                    closePage();
                } catch (Throwable t) {
                    Utils.showToast("Failed to jump to message");
                }
            }
        });
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final TabState state;

        MediaAdapter(TabState state) {
            this.state = state;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == state.items.size()) {
                return VIEW_TYPE_FOOTER;
            }
            if (state.tab == Tab.MEDIA && isMediaGrid) {
                return VIEW_TYPE_MEDIA_GRID;
            }
            return state.tab.ordinal();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            android.content.Context ctx = parent.getContext();
            if (viewType == VIEW_TYPE_FOOTER) {
                FrameLayout container = new FrameLayout(ctx);
                container.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        DimenUtils.dpToPx(60)
                ));
                ProgressBar pb = new ProgressBar(ctx);
                FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                );
                pb.setLayoutParams(pbParams);
                int accentColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive);
                if (accentColor != 0) {
                    try {
                        pb.getIndeterminateDrawable().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
                    } catch (Throwable ignored) {}
                }
                container.addView(pb);
                return new FooterViewHolder(container);
            } else if (viewType == VIEW_TYPE_MEDIA_GRID) {
                FrameLayout gridWrapper = new FrameLayout(ctx);
                int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
                int size = (screenWidth - DimenUtils.dpToPx(24) - DimenUtils.dpToPx(8)) / 3;
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(size, size);
                lp.setMargins(DimenUtils.dpToPx(2), DimenUtils.dpToPx(2), DimenUtils.dpToPx(2), DimenUtils.dpToPx(2));
                gridWrapper.setLayoutParams(lp);
                return new MediaGridViewHolder(gridWrapper);
            } else if (viewType == Tab.MEDIA.ordinal()) {
                LinearLayout container = createCardContainer(ctx);
                return new MediaViewHolder(container);
            } else if (viewType == Tab.FILES.ordinal()) {
                LinearLayout container = createCardContainer(ctx);
                return new FileViewHolder(container);
            } else {
                LinearLayout container = createCardContainer(ctx);
                return new LinkViewHolder(container);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof FooterViewHolder) {
                return;
            }
            MediaItem item = state.items.get(position);
            if (holder instanceof MediaViewHolder) {
                ((MediaViewHolder) holder).bind(item);
            } else if (holder instanceof MediaGridViewHolder) {
                ((MediaGridViewHolder) holder).bind(item);
            } else if (holder instanceof FileViewHolder) {
                ((FileViewHolder) holder).bind(item);
            } else if (holder instanceof LinkViewHolder) {
                ((LinkViewHolder) holder).bind(item);
            }
        }

        @Override
        public int getItemCount() {
            int count = state.items.size();
            if (state.loading && !state.items.isEmpty()) {
                count++;
            }
            return count;
        }
    }

    private class MediaGridViewHolder extends RecyclerView.ViewHolder {
        private final SimpleDraweeView image;
        private final SimpleDraweeView avatar;
        private final TextView playBadge;

        MediaGridViewHolder(FrameLayout wrapper) {
            super(wrapper);
            android.content.Context ctx = wrapper.getContext();

            image = new SimpleDraweeView(ctx);
            image.setBackgroundColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundSecondary));
            
            try {
                Object hierarchy = image.getHierarchy();
                Class<?> scaleTypeClass = Class.forName("com.facebook.drawee.drawable.ScalingUtils$ScaleType");
                Object centerCrop = scaleTypeClass.getField("CENTER_CROP").get(null);
                hierarchy.getClass().getMethod("setActualImageScaleType", scaleTypeClass).invoke(hierarchy, centerCrop);
                
                Class<?> roundingParamsClass = Class.forName("com.facebook.drawee.generic.RoundingParams");
                Object roundingParams = roundingParamsClass.getMethod("fromCornersRadius", float.class).invoke(null, (float) DimenUtils.dpToPx(6));
                hierarchy.getClass().getMethod("setRoundingParams", roundingParamsClass).invoke(hierarchy, roundingParams);
            } catch (Throwable ignored) {}

            wrapper.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            FrameLayout avatarFrame = new FrameLayout(ctx);
            android.graphics.drawable.GradientDrawable avatarBg = new android.graphics.drawable.GradientDrawable();
            avatarBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            avatarBg.setColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundPrimary));
            avatarFrame.setBackground(avatarBg);
            avatarFrame.setPadding(DimenUtils.dpToPx(2), DimenUtils.dpToPx(2), DimenUtils.dpToPx(2), DimenUtils.dpToPx(2));
            avatarFrame.setClipToOutline(true);
            avatarFrame.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    int size = DimenUtils.dpToPx(26);
                    outline.setOval(0, 0, size, size);
                }
            });

            FrameLayout.LayoutParams avatarFrameParams = new FrameLayout.LayoutParams(
                    DimenUtils.dpToPx(26),
                    DimenUtils.dpToPx(26),
                    Gravity.TOP | Gravity.START
            );
            avatarFrameParams.setMargins(DimenUtils.dpToPx(5), DimenUtils.dpToPx(5), 0, 0);
            wrapper.addView(avatarFrame, avatarFrameParams);

            avatar = new SimpleDraweeView(ctx);
            try {
                Object hierarchy = avatar.getHierarchy();
                Class<?> genericHierarchyClass = Class.forName("com.facebook.drawee.generic.GenericDraweeHierarchy");
                Class<?> roundingParamsClass = Class.forName("com.facebook.drawee.generic.RoundingParams");
                Object roundingParams = roundingParamsClass.getMethod("asCircle").invoke(null);
                genericHierarchyClass.getMethod("setRoundingParams", roundingParamsClass).invoke(hierarchy, roundingParams);
            } catch (Throwable ignored) {}
            avatar.setClipToOutline(true);
            avatar.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    int size = DimenUtils.dpToPx(22);
                    outline.setOval(0, 0, size, size);
                }
            });
            avatarFrame.addView(avatar, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            playBadge = new TextView(ctx);
            playBadge.setText("▶");
            playBadge.setGravity(Gravity.CENTER);
            playBadge.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive));
            playBadge.setTextSize(14f);
            playBadge.setBackgroundColor(0x77000000);
            
            FrameLayout.LayoutParams playParams = new FrameLayout.LayoutParams(
                    DimenUtils.dpToPx(24),
                    DimenUtils.dpToPx(24),
                    Gravity.CENTER
            );
            playBadge.setLayoutParams(playParams);
            
            android.graphics.drawable.GradientDrawable roundBg = new android.graphics.drawable.GradientDrawable();
            roundBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            roundBg.setColor(0x77000000);
            playBadge.setBackground(roundBg);
            
            playBadge.setVisibility(View.GONE);
            wrapper.addView(playBadge);
        }

        void bind(MediaItem item) {
            if (!item.video || item.hasImagePreview()) {
                MGImages.setImage(image, item.previewUrl);
            } else {
                image.setController(null);
            }

            if (item.authorAvatarUrl != null) {
                avatar.setImageURI(android.net.Uri.parse(item.authorAvatarUrl));
            } else {
                avatar.setController(null);
            }

            if (item.video) {
                playBadge.setVisibility(View.VISIBLE);
            } else {
                playBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> openMedia(v, item));
            
            itemView.setOnLongClickListener(v -> {
                if (item.channelId != 0 && item.messageId != 0) {
                    try {
                        com.discord.stores.StoreStream.getMessagesLoader().jumpToMessage(item.channelId, item.messageId);
                        closePage();
                        return true;
                    } catch (Throwable t) {
                        Utils.showToast("Failed to jump to message");
                    }
                }
                return false;
            });
        }
    }

    private class MediaViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout cardContainer;
        private final LinearLayout metadataLayout;
        private final SimpleDraweeView avatarImage;
        private final TextView metadataText;
        private final FrameLayout mediaWrapper;
        private final SimpleDraweeView image;
        private final TextView play;
        private final TextView badge;

        MediaViewHolder(LinearLayout container) {
            super(container);
            this.cardContainer = container;
            android.content.Context ctx = container.getContext();
            
            SimpleDraweeView[] avatarOut = new SimpleDraweeView[1];
            TextView[] out = new TextView[1];
            metadataLayout = createMetadataView(ctx, avatarOut, out);
            avatarImage = avatarOut[0];
            metadataText = out[0];
            container.addView(metadataLayout);

            mediaWrapper = new FrameLayout(ctx);
            LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    DimenUtils.dpToPx(200)
            );
            mediaWrapper.setLayoutParams(wrapperParams);

            image = new SimpleDraweeView(ctx);
            image.setBackgroundColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundSecondary));
            try {
                Object hierarchy = image.getHierarchy();
                Class<?> scaleTypeClass = Class.forName("com.facebook.drawee.drawable.ScalingUtils$ScaleType");
                Object centerCrop = scaleTypeClass.getField("CENTER_CROP").get(null);
                hierarchy.getClass().getMethod("setActualImageScaleType", scaleTypeClass).invoke(hierarchy, centerCrop);
            } catch (Throwable ignored) {}
            
            mediaWrapper.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            int textColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive);

            play = new TextView(ctx);
            play.setText("PLAY");
            play.setGravity(Gravity.CENTER);
            play.setTextColor(textColor);
            play.setTextSize(18f);
            play.setTypeface(Typeface.DEFAULT_BOLD);
            play.setBackgroundColor(0x55000000);
            play.setVisibility(View.GONE);
            mediaWrapper.addView(play, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            badge = new TextView(ctx);
            badge.setText("VIDEO");
            badge.setTextColor(textColor);
            badge.setTextSize(10f);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setBackgroundColor(0xaa000000);
            badge.setPadding(DimenUtils.dpToPx(5), DimenUtils.dpToPx(2), DimenUtils.dpToPx(5), DimenUtils.dpToPx(2));
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END
            );
            badgeParams.setMargins(0, 0, DimenUtils.dpToPx(6), DimenUtils.dpToPx(6));
            badge.setVisibility(View.GONE);
            mediaWrapper.addView(badge, badgeParams);

            container.addView(mediaWrapper);
        }

        void bind(MediaItem item) {
            String channelLabel = item.channelName != null ? "#" + item.channelName : "#channel";
            String authorLabel = item.authorName != null ? item.authorName : "Unknown";
            metadataText.setText(channelLabel + " • " + authorLabel);

            if (item.authorAvatarUrl != null) {
                avatarImage.setImageURI(android.net.Uri.parse(item.authorAvatarUrl));
            } else {
                avatarImage.setController(null);
            }

            setupMetadataClick(metadataLayout, item);

            if (!item.video || item.hasImagePreview()) {
                MGImages.setImage(image, item.previewUrl);
            } else {
                image.setController(null);
            }

            if (item.video) {
                play.setVisibility(View.VISIBLE);
                badge.setVisibility(View.VISIBLE);
            } else {
                play.setVisibility(View.GONE);
                badge.setVisibility(View.GONE);
            }

            mediaWrapper.setOnClickListener(v -> openMedia(v, item));
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout cardContainer;
        private final LinearLayout metadataLayout;
        private final SimpleDraweeView avatarImage;
        private final TextView metadataText;
        private final LinearLayout fileContentLayout;
        private final TextView iconText;
        private final TextView nameText;
        private final TextView detailsText;

        FileViewHolder(LinearLayout container) {
            super(container);
            this.cardContainer = container;
            android.content.Context ctx = container.getContext();

            SimpleDraweeView[] avatarOut = new SimpleDraweeView[1];
            TextView[] out = new TextView[1];
            metadataLayout = createMetadataView(ctx, avatarOut, out);
            avatarImage = avatarOut[0];
            metadataText = out[0];
            container.addView(metadataLayout);

            fileContentLayout = new LinearLayout(ctx);
            fileContentLayout.setOrientation(LinearLayout.HORIZONTAL);
            fileContentLayout.setGravity(Gravity.CENTER_VERTICAL);
            fileContentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            iconText = new TextView(ctx);
            iconText.setGravity(Gravity.CENTER);
            iconText.setTypeface(Typeface.DEFAULT_BOLD);
            iconText.setTextSize(11f);
            iconText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive));
            
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundTertiary));
            iconText.setBackground(circle);
            
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    DimenUtils.dpToPx(40),
                    DimenUtils.dpToPx(40)
            );
            iconParams.setMargins(0, 0, DimenUtils.dpToPx(12), 0);
            fileContentLayout.addView(iconText, iconParams);

            LinearLayout textLayout = new LinearLayout(ctx);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            fileContentLayout.addView(textLayout, layoutParams);

            nameText = new TextView(ctx);
            nameText.setTypeface(Typeface.DEFAULT_BOLD);
            nameText.setTextSize(14f);
            nameText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive));
            nameText.setSingleLine(true);
            nameText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textLayout.addView(nameText);

            detailsText = new TextView(ctx);
            detailsText.setTextSize(12f);
            detailsText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorTextMuted));
            textLayout.addView(detailsText);

            container.addView(fileContentLayout);
        }

        void bind(MediaItem item) {
            String channelLabel = item.channelName != null ? "#" + item.channelName : "#channel";
            String authorLabel = item.authorName != null ? item.authorName : "Unknown";
            metadataText.setText(channelLabel + " • " + authorLabel);

            if (item.authorAvatarUrl != null) {
                avatarImage.setImageURI(android.net.Uri.parse(item.authorAvatarUrl));
            } else {
                avatarImage.setController(null);
            }

            setupMetadataClick(metadataLayout, item);

            String ext = "";
            if (item.filename != null) {
                int lastDot = item.filename.lastIndexOf('.');
                if (lastDot != -1 && lastDot < item.filename.length() - 1) {
                    ext = item.filename.substring(lastDot + 1).toUpperCase();
                }
            }
            if (ext.isEmpty() || ext.length() > 4) ext = "FILE";
            iconText.setText(ext);

            nameText.setText(item.filename != null ? item.filename : "Unnamed File");

            String sizeStr = "";
            if (item.fileSize != null && item.fileSize > 0) {
                long size = item.fileSize;
                if (size < 1024) {
                    sizeStr = size + " B";
                } else if (size < 1024 * 1024) {
                    sizeStr = String.format(java.util.Locale.ENGLISH, "%.1f KB", size / 1024.0);
                } else {
                    sizeStr = String.format(java.util.Locale.ENGLISH, "%.1f MB", size / (1024.0 * 1024.0));
                }
            }

            detailsText.setText(sizeStr);

            fileContentLayout.setOnClickListener(v -> {
                if (item.url != null) {
                    Utils.launchUrl(item.url);
                }
            });
        }
    }

    private class LinkViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout cardContainer;
        private final LinearLayout metadataLayout;
        private final SimpleDraweeView avatarImage;
        private final TextView metadataText;
        private final LinearLayout linkContentLayout;
        private final TextView iconText;
        private final TextView titleText;
        private final TextView contextText;

        LinkViewHolder(LinearLayout container) {
            super(container);
            this.cardContainer = container;
            android.content.Context ctx = container.getContext();

            SimpleDraweeView[] avatarOut = new SimpleDraweeView[1];
            TextView[] out = new TextView[1];
            metadataLayout = createMetadataView(ctx, avatarOut, out);
            avatarImage = avatarOut[0];
            metadataText = out[0];
            container.addView(metadataLayout);

            linkContentLayout = new LinearLayout(ctx);
            linkContentLayout.setOrientation(LinearLayout.HORIZONTAL);
            linkContentLayout.setGravity(Gravity.CENTER_VERTICAL);
            linkContentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            iconText = new TextView(ctx);
            iconText.setGravity(Gravity.CENTER);
            iconText.setTypeface(Typeface.DEFAULT_BOLD);
            iconText.setTextSize(11f);
            iconText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive));
            iconText.setText("LINK");
            
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundTertiary));
            iconText.setBackground(circle);
            
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    DimenUtils.dpToPx(40),
                    DimenUtils.dpToPx(40)
            );
            iconParams.setMargins(0, 0, DimenUtils.dpToPx(12), 0);
            linkContentLayout.addView(iconText, iconParams);

            LinearLayout textLayout = new LinearLayout(ctx);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            linkContentLayout.addView(textLayout, layoutParams);

            titleText = new TextView(ctx);
            titleText.setTypeface(Typeface.DEFAULT_BOLD);
            titleText.setTextSize(14f);
            titleText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveActive));
            titleText.setSingleLine(true);
            titleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textLayout.addView(titleText);

            contextText = new TextView(ctx);
            contextText.setTextSize(12f);
            contextText.setTextColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorTextMuted));
            contextText.setSingleLine(true);
            contextText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textLayout.addView(contextText);

            container.addView(linkContentLayout);
        }

        void bind(MediaItem item) {
            String channelLabel = item.channelName != null ? "#" + item.channelName : "#channel";
            String authorLabel = item.authorName != null ? item.authorName : "Unknown";
            metadataText.setText(channelLabel + " • " + authorLabel);

            if (item.authorAvatarUrl != null) {
                avatarImage.setImageURI(android.net.Uri.parse(item.authorAvatarUrl));
            } else {
                avatarImage.setController(null);
            }

            setupMetadataClick(metadataLayout, item);

            String domain = "";
            try {
                android.net.Uri uri = android.net.Uri.parse(item.url);
                domain = uri.getHost();
            } catch (Exception ignored) {}
            if (domain == null) domain = "";

            if (item.title != null && !item.title.trim().isEmpty()) {
                titleText.setText(item.title);
            } else {
                titleText.setText(!domain.isEmpty() ? domain : "Link");
            }

            contextText.setText(item.url);

            linkContentLayout.setOnClickListener(v -> {
                if (item.url != null) {
                    Utils.launchUrl(item.url);
                }
            });
        }
    }

    private static class SearchResponse {
        Tabs tabs;
    }

    private static class Tabs {
        SearchTab media;
        SearchTab files;
        SearchTab links;
    }

    private static class SearchTab {
        Message[][] messages;
        int total_results;
        Channel[] channels;
    }

    private static class Channel {
        long id;
        String name;
    }

    private static class Message {
        long id;
        long channel_id;
        Attachment[] attachments;
        Embed[] embeds;
        String content;
        User author;
        Boolean hit;
    }

    private static class User {
        long id;
        String username;
        String avatar;
        String discriminator;

        public String getAvatarUrl() {
            if (avatar != null && !avatar.isEmpty()) {
                String ext = avatar.startsWith("a_") ? "gif" : "png";
                return "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + "." + ext + "?size=128";
            } else {
                long index = 0;
                try {
                    if (discriminator != null && !discriminator.equals("0")) {
                        index = Long.parseLong(discriminator) % 5;
                    } else {
                        index = (id >> 22) % 6;
                    }
                } catch (Throwable ignored) {
                    index = (id >> 22) % 6;
                }
                return "https://cdn.discordapp.com/embed/avatars/" + index + ".png";
            }
        }
    }

    private static class Attachment {
        String url;
        String proxy_url;
        String filename;
        String content_type;
        Integer width;
        Integer height;
        Long size;
    }

    private static class Embed {
        EmbedMedia image;
        EmbedMedia thumbnail;
        EmbedMedia video;
        String url;
        String title;
        String description;
    }

    private static class EmbedMedia {
        String url;
        String proxy_url;
        Integer width;
        Integer height;
    }
}
