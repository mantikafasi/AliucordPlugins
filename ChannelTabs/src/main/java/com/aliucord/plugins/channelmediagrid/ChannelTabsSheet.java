package com.aliucord.plugins.channelmediagrid;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private long guildId;
    private long channelId;
    private final ArrayList<MediaItem> items = new ArrayList<>();
    private final Set<String> seenUrls = new HashSet<>();
    private RecyclerView recycler;
    private MediaAdapter adapter;
    private TextView status;
    private int offset;
    private boolean loading;
    private boolean canLoadMore = true;

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
        setActionBarTitle("Channel Media");
        setActionBarDisplayHomeAsUpEnabled(true);
        setOnBackPressed(() -> {
            closePage();
            return true;
        });

        var ctx = view.getContext();
        LinearLayout root = (LinearLayout) view;
        root.setBackgroundColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundPrimary));
        root.setPadding(DimenUtils.dpToPx(12), DimenUtils.dpToPx(8), DimenUtils.dpToPx(12), 0);

        TextView title = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText("<  Channel Media");
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, DimenUtils.dpToPx(8), 0, DimenUtils.dpToPx(8));
        title.setOnClickListener(v -> closePage());
        root.addView(title);

        status = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_SubText);
        status.setText("Loading media...");
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, DimenUtils.dpToPx(12), 0, DimenUtils.dpToPx(12));
        root.addView(status);

        recycler = new RecyclerView(ctx);
        GridLayoutManager layoutManager = new GridLayoutManager(ctx, 3);
        recycler.setLayoutManager(layoutManager);
        adapter = new MediaAdapter();
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(true);
        recycler.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || !canLoadMore || loading) return;
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= items.size() - 4) loadNextPage();
            }
        });
        root.addView(recycler);

        loadNextPage();
    }

    private void closePage() {
        try {
            getParentFragmentManager().popBackStack();
        } catch (Throwable ignored) {
            try {
                getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    private void loadNextPage() {
        if (loading || !canLoadMore) return;
        loading = true;
        setStatus("Loading media...");

        Utils.threadPool.execute(() -> {
            try {
                SearchResponse response = fetchPage();
                Utils.mainThread.post(() -> applyResponse(response));
            } catch (Throwable throwable) {
                Utils.mainThread.post(() -> {
                    if (!isAdded()) return;
                    loading = false;
                    setStatus("Failed to load media");
                    Utils.showToast("Failed to load channel media");
                });
            }
        });
    }

    private SearchResponse fetchPage() throws Exception {
        Map<String, Object> mediaTab = new HashMap<>();
        mediaTab.put("limit", PAGE_SIZE);
        mediaTab.put("has", new String[]{"image", "video"});
        mediaTab.put("sort_by", "timestamp");
        mediaTab.put("sort_order", "desc");
        if (offset > 0) mediaTab.put("offset", offset);

        Map<String, Object> tabs = new HashMap<>();
        tabs.put("media", mediaTab);

        Map<String, Object> body = new HashMap<>();
        body.put("tabs", tabs);
        body.put("channel_ids", new String[]{String.valueOf(channelId)});
        body.put("include_nsfw", true);
        body.put("track_exact_total_hits", false);

        Http.Response response = Http.Request
                .newDiscordRNRequest("/guilds/" + guildId + "/messages/search/tabs", "POST")
                .executeWithJson(body);
        return GsonUtils.fromJson(response.text(), SearchResponse.class);
    }

    private void applyResponse(SearchResponse response) {
        if (!isAdded()) return;
        loading = false;

        SearchTab media = response == null || response.tabs == null ? null : response.tabs.media;
        if (media == null) {
            canLoadMore = false;
            setStatus(items.isEmpty() ? "No media found" : "");
            return;
        }

        int oldSize = items.size();
        int messageCount = 0;
        
        if (media.messages != null) {
            for (Message[] group : media.messages) {
                if (group == null) continue;
                messageCount += group.length;
                for (Message message : group) {
                    if (message == null) continue;
                    
                    if (message.attachments != null) {
                        for (Attachment attachment : message.attachments) {
                            if (attachment == null || attachment.url == null) continue;
                            boolean video = isVideo(attachment.content_type, attachment.filename);
                            String preview = video
                                    ? videoPreviewUrl(attachment.proxy_url != null ? attachment.proxy_url : attachment.url)
                                    : (attachment.proxy_url != null ? attachment.proxy_url : attachment.url);
                            addMedia(preview, attachment.url, attachment.content_type, attachment.filename, attachment.width, attachment.height);
                        }
                    }
                    if (message.embeds != null) {
                        for (Embed embed : message.embeds) {
                            if (embed == null) continue;
                            if (embed.video != null && embed.video.url != null) {
                                String preview = embed.thumbnail != null && embed.thumbnail.url != null
                                        ? (embed.thumbnail.proxy_url != null ? embed.thumbnail.proxy_url : embed.thumbnail.url)
                                        : videoPreviewUrl(embed.video.url);
                                addMedia(preview, embed.video.url, "video", null, embed.video.width, embed.video.height);
                                continue;
                            }
                            if (embed.image != null && embed.image.url != null) {
                                addMedia(embed.image.proxy_url != null ? embed.image.proxy_url : embed.image.url, embed.image.url, "image", null, embed.image.width, embed.image.height);
                            } else if (embed.thumbnail != null && embed.thumbnail.url != null) {
                                addMedia(embed.thumbnail.proxy_url != null ? embed.thumbnail.proxy_url : embed.thumbnail.url, embed.thumbnail.url, "image", null, embed.thumbnail.width, embed.thumbnail.height);
                            }
                        }
                    }
                }
            }
        }

        offset += PAGE_SIZE;
        canLoadMore = messageCount >= PAGE_SIZE && (media.total_results <= 0 || offset < media.total_results);
        
        if (items.size() > oldSize) {
            if (adapter != null) adapter.notifyItemRangeInserted(oldSize, items.size() - oldSize);
        }

        setStatus(items.isEmpty() ? "No media found" : "");
    }

    private void addMedia(String previewUrl, String url, String contentType, String filename, Integer width, Integer height) {
        if (url == null || previewUrl == null) return;
        if (!seenUrls.add(url)) return;

        boolean video = isVideo(contentType, filename);
        items.add(new MediaItem(previewUrl, url, filename, video, width, height));
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

    private void setStatus(String text) {
        if (status == null) return;
        status.setText(text);
        status.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static class MediaItem {
        final String previewUrl;
        final String url;
        final String filename;
        final boolean video;
        final Integer width;
        final Integer height;

        MediaItem(String previewUrl, String url, String filename, boolean video, Integer width, Integer height) {
            this.previewUrl = previewUrl;
            this.url = url;
            this.filename = filename;
            this.video = video;
            this.width = width;
            this.height = height;
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

    private class MediaAdapter extends RecyclerView.Adapter<MediaViewHolder> {
        @Override
        public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout tile = new FrameLayout(parent.getContext());
            int size = (getResources().getDisplayMetrics().widthPixels - DimenUtils.dpToPx(36)) / 3;
            tile.setLayoutParams(new RecyclerView.LayoutParams(size, size));
            tile.setPadding(DimenUtils.dpToPx(3), DimenUtils.dpToPx(3), DimenUtils.dpToPx(3), DimenUtils.dpToPx(3));
            return new MediaViewHolder(tile);
        }

        @Override
        public void onBindViewHolder(MediaViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class MediaViewHolder extends RecyclerView.ViewHolder {
        private final SimpleDraweeView image;
        private final TextView play;
        private final TextView badge;

        MediaViewHolder(FrameLayout tile) {
            super(tile);
            
            int textColor = ColorCompat.getThemedColor(tile.getContext(), com.lytefast.flexinput.R.b.colorInteractiveActive);

            image = new SimpleDraweeView(tile.getContext());
            image.setBackgroundColor(ColorCompat.getThemedColor(tile.getContext(), com.lytefast.flexinput.R.b.colorBackgroundSecondary));
            tile.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            play = new TextView(tile.getContext());
            play.setText("PLAY");
            play.setGravity(Gravity.CENTER);
            play.setTextColor(textColor);
            play.setTextSize(18f);
            play.setTypeface(Typeface.DEFAULT_BOLD);
            play.setBackgroundColor(0x55000000);
            play.setVisibility(View.GONE);
            tile.addView(play, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            badge = new TextView(tile.getContext());
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
            tile.addView(badge, badgeParams);
        }

        void bind(MediaItem item) {
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

            itemView.setOnClickListener(v -> openMedia(v, item));
        }
    }

    private static class SearchResponse {
        Tabs tabs;
    }

    private static class Tabs {
        SearchTab media;
    }

    private static class SearchTab {
        Message[][] messages;
        int total_results;
    }

    private static class Message {
        Attachment[] attachments;
        Embed[] embeds;
    }

    private static class Attachment {
        String url;
        String proxy_url;
        String filename;
        String content_type;
        Integer width;
        Integer height;
    }

    private static class Embed {
        EmbedMedia image;
        EmbedMedia thumbnail;
        EmbedMedia video;
    }

    private static class EmbedMedia {
        String url;
        String proxy_url;
        Integer width;
        Integer height;
    }
}
