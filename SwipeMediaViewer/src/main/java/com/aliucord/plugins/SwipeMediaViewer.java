package com.aliucord.plugins;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

import b.f.g.e.v;

import com.facebook.samples.zoomable.ZoomableDraweeView;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.attachment.MessageAttachmentType;
import com.discord.databinding.WidgetChatListAdapterItemAttachmentBinding;
import com.discord.models.message.Message;
import com.discord.utilities.images.MGImages;
import com.discord.widgets.chat.list.InlineMediaView;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment;
import com.discord.widgets.chat.list.entries.AttachmentEntry;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.media.WidgetMedia;

import java.util.ArrayList;
import java.util.List;

@AliucordPlugin
@SuppressWarnings("unused")
public class SwipeMediaViewer extends Plugin {
    public static final String SHOW_ARROWS_KEY = "showArrows";
    private static final long ANIMATION_MS = 180L;
    private static final float COMMIT_FRACTION = 0.12f;
    private static SwipeMediaViewer instance;
    private static List<MessageAttachment> activeAttachments = new ArrayList<>();
    private static int activeIndex = -1;

    @Override
    public void start(Context context) throws Throwable {
        instance = this;
        settingsTab = new SettingsTab(SwipeMediaViewerSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patcher.patch(
                WidgetChatListAdapterItemAttachment.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class),
                new Hook(callFrame -> {
                    try {
                        AttachmentEntry entry = (AttachmentEntry) callFrame.args[1];
                        MessageAttachment attachment = entry.getAttachment();
                        if (!isSwipeableMedia(attachment))
                            return;

                        List<MessageAttachment> attachments = swipeableAttachments(entry.getMessage());
                        if (attachments.isEmpty())
                            return;

                        WidgetChatListAdapterItemAttachmentBinding binding =
                                WidgetChatListAdapterItemAttachment.access$getBinding$p((WidgetChatListAdapterItemAttachment) callFrame.thisObject);

                        View.OnClickListener listener = view -> launchGroup(view.getContext(), attachments, attachment);
                        binding.h.setOnClickListener(listener);
                        binding.d.setOnClickListener(listener);
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                })
        );

        patcher.patch(
                WidgetMedia.class.getDeclaredMethod("onViewBoundOrOnResume"),
                new Hook(callFrame -> {
                    try {
                        WidgetMedia media = (WidgetMedia) callFrame.thisObject;
                        if (activeAttachments.size() <= 1 || activeIndex < 0)
                            return;

                        var binding = WidgetMedia.access$getBinding$p(media);
                        binding.a.setBackgroundColor(Color.BLACK);
                        media.requireActivity().getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                        updateOverlay(media);

                        View.OnTouchListener listener = new SwipeTouchListener(media);
                        binding.a.setOnTouchListener(listener);
                        binding.d.setOnTouchListener(listener);
                        binding.g.setOnTouchListener(listener);
                        binding.f.setOnTouchListener(listener);
                        View surface = binding.g.getVideoSurfaceView();
                        if (surface != null)
                            surface.setOnTouchListener(listener);
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                })
        );
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
        activeAttachments.clear();
        activeIndex = -1;
        instance = null;
    }

    private static List<MessageAttachment> swipeableAttachments(Message message) {
        List<MessageAttachment> result = new ArrayList<>();
        List<MessageAttachment> attachments = message.getAttachments();
        if (attachments == null)
            return result;

        for (MessageAttachment attachment : attachments) {
            if (isSwipeableMedia(attachment))
                result.add(attachment);
        }
        return result;
    }

    private static boolean isSwipeableMedia(MessageAttachment attachment) {
        if (attachment == null)
            return false;

        MessageAttachmentType type = attachment.e();
        return type == MessageAttachmentType.IMAGE || type == MessageAttachmentType.VIDEO;
    }

    private static void launchGroup(Context context, List<MessageAttachment> attachments, MessageAttachment attachment) {
        activeAttachments = new ArrayList<>(attachments);
        activeIndex = 0;
        String targetUrl = String.valueOf(attachment.c());
        for (int i = 0; i < activeAttachments.size(); i++) {
            if (String.valueOf(activeAttachments.get(i).c()).equals(targetUrl)) {
                activeIndex = i;
                break;
            }
        }
        WidgetMedia.Companion.launch(context, attachment);
    }

    private static void updateOverlay(WidgetMedia media) {
        var binding = WidgetMedia.access$getBinding$p(media);
        FrameLayout root = binding.a;
        Context context = root.getContext();

        TextView count = root.findViewWithTag("SwipeMediaViewerCount");
        if (count == null) {
            count = new TextView(context);
            count.setTag("SwipeMediaViewerCount");
            count.setTextColor(Color.WHITE);
            count.setTextSize(14f);
            count.setTypeface(Typeface.DEFAULT_BOLD);
            count.setGravity(Gravity.CENTER);
            count.setBackgroundColor(0x66000000);
            int horizontal = DimenUtils.dpToPx(10);
            int vertical = DimenUtils.dpToPx(4);
            count.setPadding(horizontal, vertical, horizontal, vertical);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            );
            params.bottomMargin = DimenUtils.dpToPx(24);
            root.addView(count, params);
        }
        count.setText((activeIndex + 1) + "/" + activeAttachments.size());
        count.bringToFront();

        boolean showArrows = instance != null && instance.settings.getBool(SHOW_ARROWS_KEY, false);
        configureArrow(media, root, "SwipeMediaViewerLeft", "<", Gravity.START | Gravity.CENTER_VERTICAL, -1, showArrows && activeIndex > 0);
        configureArrow(media, root, "SwipeMediaViewerRight", ">", Gravity.END | Gravity.CENTER_VERTICAL, 1, showArrows && activeIndex < activeAttachments.size() - 1);
    }

    private static void configureArrow(WidgetMedia media, FrameLayout root, String tag, String text, int gravity, int offset, boolean visible) {
        TextView arrow = root.findViewWithTag(tag);
        if (arrow == null) {
            arrow = new TextView(root.getContext());
            arrow.setTag(tag);
            arrow.setText(text);
            arrow.setTextColor(Color.WHITE);
            arrow.setTextSize(34f);
            arrow.setTypeface(Typeface.DEFAULT_BOLD);
            arrow.setGravity(Gravity.CENTER);
            arrow.setBackgroundColor(0x66000000);
            int size = DimenUtils.dpToPx(48);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size, gravity);
            params.leftMargin = DimenUtils.dpToPx(12);
            params.rightMargin = DimenUtils.dpToPx(12);
            root.addView(arrow, params);
        }

        arrow.setVisibility(visible ? View.VISIBLE : View.GONE);
        arrow.setOnClickListener(view -> new SwipeTouchListener(media).openOffset(view, offset, true));
        arrow.bringToFront();
    }

    private static class SwipeTouchListener implements View.OnTouchListener {
        private final WidgetMedia media;
        private final int touchSlop;
        private View previewView;
        private int previewOffset;
        private int dragOffset;
        private boolean swiping;
        private float downX;
        private float downY;

        SwipeTouchListener(WidgetMedia media) {
            this.media = media;
            this.touchSlop = ViewConfiguration.get(media.requireContext()).getScaledTouchSlop();
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getPointerCount() > 1) {
                resetDrag();
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    swiping = false;
                    dragOffset = 0;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float moveDx = event.getRawX() - downX;
                    float moveDy = event.getRawY() - downY;
                    if (isCurrentImageZoomed()) {
                        swiping = false;
                        dragOffset = 0;
                        return false;
                    }

                    if (!swiping && (Math.abs(moveDx) > touchSlop || Math.abs(moveDy) > touchSlop)) {
                        if (Math.abs(moveDx) > Math.abs(moveDy)) {
                            int offset = moveDx < 0 ? 1 : -1;
                            if (!canOpenOffset(offset))
                                return false;

                            swiping = true;
                            dragOffset = offset;
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                        } else {
                            swiping = true;
                        }
                    }
                    if (swiping) {
                        if (dragOffset != 0)
                            dragHorizontal(moveDx);
                        else
                            dragVertical(moveDy);
                    }
                    return swiping;
                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    if (Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop) {
                        view.performClick();
                        return false;
                    }

                    if (isCurrentImageZoomed())
                        return false;

                    if (dragOffset == 0) {
                        if (Math.abs(dy) > touchSlop) {
                            dismiss(view, dy);
                            return true;
                        }
                        resetDrag();
                        return false;
                    }

                    int offset = dragOffset;
                    int width = WidgetMedia.access$getBinding$p(media).a.getWidth();
                    
                    boolean commit = false;
                    if (dragOffset > 0 && dx < -width * COMMIT_FRACTION) commit = true;
                    if (dragOffset < 0 && dx > width * COMMIT_FRACTION) commit = true;

                    if (commit)
                        openOffset(view, offset, false);
                    else
                        resetDrag();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    resetDrag();
                    return false;
                default:
                    return false;
            }
        }

        private void dragHorizontal(float dx) {
            var binding = WidgetMedia.access$getBinding$p(media);
            FrameLayout root = binding.a;
            int width = root.getWidth();
            if (width <= 0)
                return;

            int offset = dragOffset != 0 ? dragOffset : (dx < 0 ? 1 : -1);
            int nextIndex = activeIndex + offset;
            if (nextIndex < 0 || nextIndex >= activeAttachments.size())
                return;

            if (offset > 0)
                dx = Math.max(-width, Math.min(0f, dx));
            else
                dx = Math.min(width, Math.max(0f, dx));

            ensurePreview(root, nextIndex, offset);
            binding.d.setTranslationX(dx);
            binding.g.setTranslationX(dx);
            binding.e.setTranslationX(dx);
            previewView.setTranslationX((offset > 0 ? width : -width) + dx);
        }

        private void dragVertical(float dy) {
            var binding = WidgetMedia.access$getBinding$p(media);
            binding.a.setTranslationY(dy);
            int height = binding.a.getHeight();
            if (height > 0) {
                float progress = Math.min(1f, Math.abs(dy) / height);
                int alpha = (int) ((1f - progress) * 255);
                binding.a.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
                media.requireActivity().getWindow().getDecorView().setBackgroundColor(Color.argb(alpha, 0, 0, 0));
            }
        }

        private boolean canOpenOffset(int offset) {
            int nextIndex = activeIndex + offset;
            return nextIndex >= 0 && nextIndex < activeAttachments.size();
        }

        private boolean isCurrentImageZoomed() {
            if (activeIndex < 0 || activeIndex >= activeAttachments.size())
                return false;

            if (activeAttachments.get(activeIndex).e() != MessageAttachmentType.IMAGE)
                return false;

            var image = WidgetMedia.access$getBinding$p(media).d;
            return image.computeHorizontalScrollRange() > image.computeHorizontalScrollExtent() + touchSlop ||
                    image.computeVerticalScrollRange() > image.computeVerticalScrollExtent() + touchSlop;
        }

        private void ensurePreview(FrameLayout root, int nextIndex, int offset) {
            if (previewView != null && previewOffset == offset)
                return;

            removePreview();
            previewOffset = offset;
            MessageAttachment attachment = activeAttachments.get(nextIndex);
            if (attachment.e() == MessageAttachmentType.IMAGE) {
                ZoomableDraweeView imagePreview = new ZoomableDraweeView(root.getContext(), null);
                imagePreview.setIsLongpressEnabled(false);
                MGImages.setScaleType(imagePreview, v.l);
                MGImages.setImage(imagePreview, attachment.c());
                previewView = imagePreview;
            } else {
                InlineMediaView videoPreview = new InlineMediaView(root.getContext());
                previewView = videoPreview;
            }
            previewView.setBackgroundColor(Color.BLACK);
            previewView.setTag("SwipeMediaViewerPreview");
            root.addView(previewView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            if (previewView instanceof InlineMediaView)
                ((InlineMediaView) previewView).updateUIWithAttachment(attachment, root.getWidth(), root.getHeight(), true);
        }

        private void resetDrag() {
            var binding = WidgetMedia.access$getBinding$p(media);
            binding.d.animate().translationX(0f).setDuration(ANIMATION_MS).start();
            binding.g.animate().translationX(0f).setDuration(ANIMATION_MS).start();
            binding.e.animate().translationX(0f).setDuration(ANIMATION_MS).start();
            binding.a.animate().translationY(0f).setDuration(ANIMATION_MS).start();
            binding.a.setBackgroundColor(Color.BLACK);
            media.requireActivity().getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            if (previewView != null) {
                int width = binding.a.getWidth();
                int target = previewOffset > 0 ? width : -width;
                previewView.animate()
                        .translationX(target)
                        .setDuration(ANIMATION_MS)
                        .withEndAction(this::removePreview)
                        .start();
            }
        }

        private void removePreview() {
            if (previewView == null)
                return;

            FrameLayout root = WidgetMedia.access$getBinding$p(media).a;
            root.removeView(previewView);
            previewView = null;
            previewOffset = 0;
        }

        private void openOffset(View view, int offset, boolean animateFromRest) {
            int nextIndex = activeIndex + offset;
            if (nextIndex < 0 || nextIndex >= activeAttachments.size())
                return;

            MessageAttachment next = activeAttachments.get(nextIndex);
            Activity currentActivity = media.requireActivity();
            View root = WidgetMedia.access$getBinding$p(media).a;
            int width = root.getWidth();
            if (animateFromRest) {
                ensurePreview((FrameLayout) root, nextIndex, offset);
                previewView.setTranslationX(offset > 0 ? width : -width);
            }

            activeIndex = nextIndex;

            var binding = WidgetMedia.access$getBinding$p(media);
            binding.d.animate().translationX(offset > 0 ? -width : width).setDuration(ANIMATION_MS).start();
            binding.g.animate().translationX(offset > 0 ? -width : width).setDuration(ANIMATION_MS).start();
            binding.e.animate().translationX(offset > 0 ? -width : width).setDuration(ANIMATION_MS).start();
            Runnable finishSwap = () -> {
                WidgetMedia.Companion.launch(view.getContext(), next);
                currentActivity.overridePendingTransition(0, 0);
                currentActivity.finish();
                currentActivity.overridePendingTransition(0, 0);
            };

            if (previewView != null) {
                previewView.animate()
                        .translationX(0f)
                        .setDuration(ANIMATION_MS)
                        .withEndAction(finishSwap)
                        .start();
            } else {
                root.postDelayed(finishSwap, ANIMATION_MS);
            }
        }

        private void dismiss(View view, float dy) {
            Activity activity = media.requireActivity();
            View root = WidgetMedia.access$getBinding$p(media).a;
            int height = root.getHeight();
            root.animate()
                    .translationY(dy > 0 ? height : -height)
                    .setDuration(ANIMATION_MS)
                    .withEndAction(() -> {
                activity.finish();
                activity.overridePendingTransition(0, 0);
            })
                    .start();
        }
    }
}
