package com.aliucord.plugins.mediafavorites;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.aliucord.Utils;
import com.discord.utilities.dimen.DimenUtils;
import com.discord.utilities.images.MGImages;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FavoriteGridAdapter extends RecyclerView.Adapter<FavoriteGridAdapter.ViewHolder> {
    private static final Map<String, Bitmap> VIDEO_THUMBNAILS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> VIDEO_THUMBNAILS_LOADING = new ConcurrentHashMap<>();

    private List<MediaFavorites.FavoriteAttachment> items;
    private final OnFavoriteClickListener listener;
    private final OnFavoriteLongClickListener longClickListener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(MediaFavorites.FavoriteAttachment fav);
    }

    public interface OnFavoriteLongClickListener {
        void onFavoriteLongClick(MediaFavorites.FavoriteAttachment fav);
    }

    public FavoriteGridAdapter(List<MediaFavorites.FavoriteAttachment> items, OnFavoriteClickListener listener) {
        this(items, listener, null);
    }

    public FavoriteGridAdapter(
            List<MediaFavorites.FavoriteAttachment> items,
            OnFavoriteClickListener listener,
            OnFavoriteLongClickListener longClickListener
    ) {
        this.items = items;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void setItems(List<MediaFavorites.FavoriteAttachment> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        FrameLayout root = new FrameLayout(parent.getContext());
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        SimpleDraweeView image = new SimpleDraweeView(parent.getContext());
        image.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);

        int padding = DimenUtils.dpToPixels(3);
        image.setPadding(padding, padding, padding, padding);
        root.addView(image);

        ImageView videoThumbnail = new ImageView(parent.getContext());
        videoThumbnail.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        videoThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        videoThumbnail.setVisibility(View.GONE);
        root.addView(videoThumbnail);

        ImageView icon = new ImageView(parent.getContext());
        icon.setColorFilter(Color.WHITE);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                DimenUtils.dpToPixels(32),
                DimenUtils.dpToPixels(32),
                android.view.Gravity.CENTER
        );
        root.addView(icon, iconLp);

        TextView label = new TextView(parent.getContext());
        label.setTextColor(Color.WHITE);
        label.setTextSize(11f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(android.view.Gravity.CENTER);
        label.setMaxLines(2);
        label.setPadding(
                DimenUtils.dpToPixels(8),
                DimenUtils.dpToPixels(54),
                DimenUtils.dpToPixels(8),
                DimenUtils.dpToPixels(8)
        );
        root.addView(label, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        return new ViewHolder(root, image, videoThumbnail, icon, label);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MediaFavorites.FavoriteAttachment fav = items.get(position);
        boolean image = isImage(fav);
        boolean video = isVideo(fav);
        String mediaUrl = getMediaUrl(fav);
        holder.boundKey = mediaUrl;

        holder.image.setVisibility(image ? View.VISIBLE : View.GONE);
        holder.videoThumbnail.setVisibility(View.GONE);
        holder.videoThumbnail.setImageDrawable(null);
        holder.icon.setVisibility(image ? View.GONE : View.VISIBLE);
        holder.label.setVisibility(image ? View.GONE : View.VISIBLE);
        holder.itemView.setBackground(image ? null : makeMediaTileBackground());

        if (image) {
            MGImages.setImage$default(
                holder.image,
                Collections.singletonList(mediaUrl),
                0, 0, false, null, null, null, 252, null
            );
        } else {
            holder.icon.setImageResource(video
                    ? com.lytefast.flexinput.R.e.ic_play_circle_outline_white_24dp
                    : com.lytefast.flexinput.R.e.ic_file_audio);
            holder.label.setText((video ? "VIDEO" : "AUDIO") + "\n" + getDisplayName(fav));
            if (video) bindVideoThumbnail(holder, mediaUrl);
        }

        ViewGroup.LayoutParams currentLp = holder.itemView.getLayoutParams();
        StaggeredGridLayoutManager.LayoutParams lp;
        if (currentLp instanceof StaggeredGridLayoutManager.LayoutParams) {
            lp = (StaggeredGridLayoutManager.LayoutParams) currentLp;
        } else {
            lp = new StaggeredGridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        if (fav.height > 0 && fav.width > 0) {
            float ratio = (float) fav.height / fav.width;
            int colWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels /
                    Math.max(2, holder.itemView.getResources().getDisplayMetrics().widthPixels / DimenUtils.dpToPixels(164));
            lp.height = (int) (colWidth * ratio);
        } else if (!image) {
            lp.height = DimenUtils.dpToPixels(video ? 128 : 96);
        } else {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        holder.itemView.setLayoutParams(lp);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(fav);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener == null) return false;
            longClickListener.onFavoriteLongClick(fav);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static GradientDrawable makeMediaTileBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(47, 49, 54));
        bg.setCornerRadius(DimenUtils.dpToPixels(6));
        return bg;
    }

    private static String getDisplayName(MediaFavorites.FavoriteAttachment fav) {
        return fav.filename == null || fav.filename.trim().isEmpty() ? "media" : fav.filename;
    }

    private static String getMediaUrl(MediaFavorites.FavoriteAttachment fav) {
        return fav.proxy_url != null ? fav.proxy_url : fav.url;
    }

    private static void bindVideoThumbnail(ViewHolder holder, String url) {
        if (url == null || url.isEmpty()) return;

        Bitmap cached = VIDEO_THUMBNAILS.get(url);
        if (cached != null) {
            showVideoThumbnail(holder, cached);
            return;
        }

        if (VIDEO_THUMBNAILS_LOADING.put(url, true) != null) return;

        Utils.threadPool.execute(() -> {
            Bitmap thumbnail = extractVideoThumbnail(url);
            if (thumbnail != null) {
                VIDEO_THUMBNAILS.put(url, thumbnail);
            }
            VIDEO_THUMBNAILS_LOADING.remove(url);

            if (thumbnail == null) return;
            Utils.mainThread.post(() -> {
                if (!url.equals(holder.boundKey)) return;
                showVideoThumbnail(holder, thumbnail);
            });
        });
    }

    private static void showVideoThumbnail(ViewHolder holder, Bitmap thumbnail) {
        holder.videoThumbnail.setImageBitmap(thumbnail);
        holder.videoThumbnail.setVisibility(View.VISIBLE);
        holder.label.setVisibility(View.GONE);
        holder.itemView.setBackgroundColor(Color.BLACK);
    }

    private static Bitmap extractVideoThumbnail(String url) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(url, new HashMap<String, String>());
            Bitmap frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) frame = retriever.getFrameAtTime();
            return scaleThumbnail(frame);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }
    }

    private static Bitmap scaleThumbnail(Bitmap thumbnail) {
        if (thumbnail == null) return null;

        int maxWidth = DimenUtils.dpToPixels(320);
        if (thumbnail.getWidth() <= maxWidth || thumbnail.getWidth() <= 0 || thumbnail.getHeight() <= 0) {
            return thumbnail;
        }

        int height = Math.max(1, Math.round((float) thumbnail.getHeight() * maxWidth / thumbnail.getWidth()));
        return Bitmap.createScaledBitmap(thumbnail, maxWidth, height, true);
    }

    private static boolean isImage(MediaFavorites.FavoriteAttachment fav) {
        String filename = fav.filename == null ? "" : fav.filename.toLowerCase();
        return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                filename.endsWith(".gif") || filename.endsWith(".webp") || filename.endsWith(".bmp") ||
                filename.endsWith(".tiff") || filename.endsWith(".tif");
    }

    private static boolean isVideo(MediaFavorites.FavoriteAttachment fav) {
        String filename = fav.filename == null ? "" : fav.filename.toLowerCase();
        return filename.endsWith(".mp4") || filename.endsWith(".webm") || filename.endsWith(".mov") ||
                filename.endsWith(".avi") || filename.endsWith(".mkv") || filename.endsWith(".m4v") ||
                filename.endsWith(".mpg") || filename.endsWith(".mpeg") || filename.endsWith(".wmv");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final SimpleDraweeView image;
        final ImageView videoThumbnail;
        final ImageView icon;
        final TextView label;
        String boundKey;

        ViewHolder(View itemView, SimpleDraweeView image, ImageView videoThumbnail, ImageView icon, TextView label) {
            super(itemView);
            this.image = image;
            this.videoThumbnail = videoThumbnail;
            this.icon = icon;
            this.label = label;
        }
    }
}
