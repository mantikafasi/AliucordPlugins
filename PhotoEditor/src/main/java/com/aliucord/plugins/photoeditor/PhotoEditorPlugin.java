package com.aliucord.plugins.photoeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.DimenUtils;
import com.discord.api.channel.Channel;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.attachment.MessageAttachmentType;
import com.discord.api.sticker.Sticker;
import com.discord.models.domain.emoji.Emoji;
import com.discord.models.message.Message;
import com.discord.stores.StoreStream;
import com.discord.utilities.stickers.StickerUtils;
import com.discord.widgets.chat.input.WidgetChatInputAttachments;
import com.discord.widgets.chat.input.attachments.AttachmentBottomSheet;
import com.discord.widgets.chat.input.emoji.EmojiPickerContextType;
import com.discord.widgets.chat.input.emoji.EmojiPickerNavigator;
import com.discord.widgets.chat.input.sticker.StickerPickerViewModel;
import com.discord.widgets.chat.input.sticker.WidgetStickerPickerSheet;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment;
import com.discord.widgets.chat.list.entries.AttachmentEntry;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.media.WidgetMedia;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionAggregator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XposedBridge;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder;
import kotlin.Unit;

@AliucordPlugin
@SuppressWarnings("unused")
public class PhotoEditorPlugin extends Plugin {
    private final ArrayDeque<View> overlayRedoStack = new ArrayDeque<>();

    public PhotoEditorPlugin() {
        settingsTab = new SettingsTab(PhotoEditorSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
    }

    public interface SpoilerToggleListener {
        void onSpoilerToggled(boolean isSpoiler);
    }

    View createToolbar(Context context, PhotoEditor editor, PhotoEditorView editorView, Dialog dialog, Attachment<?>[] currentAttachment, EditRequest editRequest, PhotoFilter[] sessionFilter, boolean[] isCustomFilter, float[] customFilterValues) {
        LinearLayout rootContainer = new LinearLayout(context);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setBackgroundColor(0xff1e1f22);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rootContainer.setElevation(dp(8));
        }

        android.widget.HorizontalScrollView mainScroll = new android.widget.HorizontalScrollView(context);
        mainScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout mainToolbar = new LinearLayout(context);
        mainToolbar.setOrientation(LinearLayout.HORIZONTAL);
        mainToolbar.setGravity(Gravity.CENTER_VERTICAL);
        mainToolbar.setPadding(dp(8), dp(12), dp(8), dp(12));
        mainScroll.addView(mainToolbar, new android.widget.HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        FrameLayout subToolbarContainer = new FrameLayout(context);
        subToolbarContainer.setVisibility(View.INVISIBLE);
        subToolbarContainer.setBackgroundColor(0xff2b2d31);

        rootContainer.addView(subToolbarContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
        ));
        rootContainer.addView(mainScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        List<View> mainButtons = new ArrayList<>();
        List<View> toolButtons = new ArrayList<>();
        List<View> filterButtons = new ArrayList<>();

        // --- DRAW SUB-TOOLBAR ---
        android.widget.FrameLayout drawScroll = new android.widget.FrameLayout(context);
        LinearLayout drawToolbar = new LinearLayout(context);
        drawToolbar.setOrientation(LinearLayout.HORIZONTAL);
        drawToolbar.setGravity(Gravity.CENTER_VERTICAL);
        drawToolbar.setPadding(dp(8), dp(7), dp(8), dp(7));
        drawScroll.addView(drawToolbar, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        View penButton = iconButton(context, "Pen", "ic_edit_24dp", null);
        penButton.setOnClickListener(v -> {
            selectOnly(toolButtons, penButton);
            editor.setBrushDrawingMode(true);
            applyBrush(editor);
            applyBrushLayerMode(editorView, true);
        });
        toolButtons.add(penButton);
        drawToolbar.addView(penButton);

        View eraseButton = iconButton(context, "Erase", "ic_delete_24dp", null);
        eraseButton.setOnClickListener(v -> {
            selectOnly(toolButtons, eraseButton);
            editor.setBrushDrawingMode(true);
            editor.brushEraser();
            applyBrushLayerMode(editorView, true);
        });
        toolButtons.add(eraseButton);
        drawToolbar.addView(eraseButton);

        drawToolbar.addView(groupSeparator(context));

        LinearLayout sliderContainer = new LinearLayout(context);
        sliderContainer.setOrientation(LinearLayout.HORIZONTAL);
        sliderContainer.setGravity(Gravity.CENTER_VERTICAL);
        sliderContainer.setPadding(0, 0, dp(8), 0);

        TextView sizeLabel = new TextView(context);
        sizeLabel.setTextColor(Color.WHITE);
        sizeLabel.setTextSize(14f);
        sizeLabel.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        // initialize label text with current brushSize
        sizeLabel.setSingleLine(true);
        sizeLabel.setText(brushSize + " px");

        sizeLabel.setPadding(dp(8), 0, dp(4), 0);
        sliderContainer.addView(sizeLabel, new LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.WRAP_CONTENT));

        com.google.android.material.slider.Slider brushSlider = PhotoEditorUi.createDiscordSlider(context, 0, 75, brushSize - 5);
        brushSlider.setPadding(dp(8), 0, dp(8), 0);

        brushSlider.addOnChangeListener((slider, value, fromUser) -> {
            int newSize = Math.round(value) + 5;
            brushSize = newSize;
            sizeLabel.setText(newSize + " px");
            editor.setBrushSize((float) newSize);
            editor.setBrushEraserSize((float) newSize);
        });

        sliderContainer.addView(brushSlider, new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        drawToolbar.addView(sliderContainer);

        drawToolbar.addView(groupSeparator(context));

        final View currentColorBtn = colorSwatch(context, brushColor, null);
        currentColorBtn.setOnClickListener(v -> {
            PhotoEditorColorPicker.show(context, editorView, editor, brushColor, COLORS, newColor -> {
                brushColor = newColor;
                textColor = newColor;
                setColorSwatchSelected(currentColorBtn, newColor, true);
                editor.setBrushDrawingMode(true);
                applyBrush(editor);
                applyBrushLayerMode(editorView, true);
            });
        });
        setColorSwatchSelected(currentColorBtn, brushColor, true);
        drawToolbar.addView(currentColorBtn);

        // --- FILTER SUB-TOOLBAR ---
        android.widget.HorizontalScrollView filterScroll = new android.widget.HorizontalScrollView(context);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filterToolbar = new LinearLayout(context);
        filterToolbar.setOrientation(LinearLayout.HORIZONTAL);
        filterToolbar.setGravity(Gravity.CENTER_VERTICAL);
        filterToolbar.setPadding(dp(8), dp(7), dp(8), dp(7));
        filterScroll.addView(filterToolbar, new android.widget.HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (PhotoFilter filter : FILTERS) {
            View fBtn = iconButton(context, PhotoEditorUtils.humanize(filter.name()), null, null);
            fBtn.setTag(filter);
            filterButtons.add(fBtn);
            fBtn.setOnClickListener(v -> {
                selectOnly(filterButtons, fBtn);
                isCustomFilter[0] = false;
                for (int i=0; i<editorView.getChildCount(); i++) {
                    android.view.View child = editorView.getChildAt(i);
                    if (child.getClass().getName().contains("ImageFilterView")) {
                        child.setVisibility(android.view.View.VISIBLE);
                    }
                }
                editorView.setLayerType(android.view.View.LAYER_TYPE_NONE, null);
                editorView.getSource().clearColorFilter();
                sessionFilter[0] = filter;
                selectedFilter = filter;
                editor.setFilterEffect(filter);
                boolean isGlOnly = isGlOnlyFilter(filter);
                boolean cantSaveGl = isGlOnly && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N;
                if (cantSaveGl)
                    Toast.makeText(context, "This filter requires Android 7.0+ to save properly", Toast.LENGTH_LONG).show();
            });
            filterToolbar.addView(fBtn);

            if (filter == PhotoFilter.NONE) {
                View customBtn = iconButton(context, "Custom", null, null);
                customBtn.setTag("CUSTOM");
                filterButtons.add(customBtn);
                customBtn.setOnClickListener(v -> {
                    selectOnly(filterButtons, customBtn);
                    isCustomFilter[0] = true;
                    editor.setFilterEffect(PhotoFilter.NONE);
                    sessionFilter[0] = PhotoFilter.NONE;
                    selectedFilter = PhotoFilter.NONE;
                    PhotoEditorFilterDialog.show(context, editorView, settings, customFilterValues);
                });
                filterToolbar.addView(customBtn);
            }
        }

        // --- MAIN TOOLBAR ---
        View undoMainBtn = iconButton(context, "Undo", "ic_reply_24dp", v -> {
            if (!editor.undo()) {
                for (int i = editorView.getChildCount() - 1; i >= 0; i--) {
                    View child = editorView.getChildAt(i);
                    Object tag = child.getTag();
                    if (OVERLAY_IMAGE.equals(tag) || OVERLAY_TEXT.equals(tag) || OVERLAY_EMOJI.equals(tag)) {
                        editorView.removeViewAt(i);
                        overlayRedoStack.push(child);
                        break;
                    }
                }
            }
        });
        mainToolbar.addView(undoMainBtn);

        View redoMainBtn = iconButton(context, "Redo", "ic_reply_24dp", v -> {
            if (!editor.redo() && !overlayRedoStack.isEmpty()) {
                View overlay = overlayRedoStack.pop();
                if (overlay.getParent() == null) editorView.addView(overlay, overlay.getLayoutParams());
                overlay.bringToFront();
            }
        });
        if (redoMainBtn instanceof android.view.ViewGroup && ((android.view.ViewGroup) redoMainBtn).getChildCount() > 0) {
            ((android.view.ViewGroup) redoMainBtn).getChildAt(0).setScaleX(-1f);
        }
        mainToolbar.addView(redoMainBtn);

        View clearMainBtn = iconButton(context, "Reset", "ucrop_ic_reset", v -> clearAllEditorOverlays(editor, editorView));
        mainToolbar.addView(clearMainBtn);

        Runnable stateUpdater = new Runnable() {
            @Override
            public void run() {
                if (undoMainBtn.getParent() == null) return;

                boolean hasOverlay = false;
                for (int i = 0; i < editorView.getChildCount(); i++) {
                    View child = editorView.getChildAt(i);
                    Object tag = child.getTag();
                    if (OVERLAY_IMAGE.equals(tag) || OVERLAY_TEXT.equals(tag) || OVERLAY_EMOJI.equals(tag)) {
                        hasOverlay = true;
                        break;
                    }
                }

                boolean canUndo = editor.isUndoAvailable();
                boolean canRedo = editor.isRedoAvailable() || !overlayRedoStack.isEmpty();

                boolean undoActive = hasOverlay || canUndo;
                boolean redoActive = canRedo;
                boolean clearActive = hasOverlay || canUndo || canRedo;

                if (undoMainBtn instanceof android.view.ViewGroup && ((android.view.ViewGroup) undoMainBtn).getChildCount() > 0) {
                    ((android.view.ViewGroup) undoMainBtn).getChildAt(0).setAlpha(undoActive ? 1f : 0.3f);
                }
                undoMainBtn.setEnabled(undoActive);

                if (redoMainBtn instanceof android.view.ViewGroup && ((android.view.ViewGroup) redoMainBtn).getChildCount() > 0) {
                    ((android.view.ViewGroup) redoMainBtn).getChildAt(0).setAlpha(redoActive ? 1f : 0.3f);
                }
                redoMainBtn.setEnabled(redoActive);

                if (clearMainBtn instanceof android.view.ViewGroup && ((android.view.ViewGroup) clearMainBtn).getChildCount() > 0) {
                    ((android.view.ViewGroup) clearMainBtn).getChildAt(0).setAlpha(clearActive ? 1f : 0.3f);
                }
                clearMainBtn.setEnabled(clearActive);

                undoMainBtn.postDelayed(this, 300);
            }
        };
        undoMainBtn.post(stateUpdater);

        mainToolbar.addView(groupSeparator(context));

        View drawMainBtn = iconButton(context, "Draw", "ic_edit_24dp", null);
        drawMainBtn.setOnClickListener(v -> {
            selectOnly(mainButtons, drawMainBtn);
            subToolbarContainer.removeAllViews();
            subToolbarContainer.addView(drawScroll);
            subToolbarContainer.setVisibility(View.VISIBLE);
            selectOnly(toolButtons, penButton);
            editor.setBrushDrawingMode(true);
            applyBrush(editor);
            applyBrushLayerMode(editorView, true);
        });
        mainButtons.add(drawMainBtn);
        mainToolbar.addView(drawMainBtn);

        View textMainBtn = iconButton(context, "Text", "ic_text_image_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.INVISIBLE);
            editor.setBrushDrawingMode(false);
            PhotoEditorTextDialog.show(this, context, editor, editorView);
        });
        mainToolbar.addView(textMainBtn);

        View emojiMainBtn = iconButton(context, "Emoji", "ic_emoji_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.INVISIBLE);
            editor.setBrushDrawingMode(false);
            showDiscordEmojiPicker(context, editor, editorView);
        });
        mainToolbar.addView(emojiMainBtn);

        View stickerMainBtn = iconButton(context, "Sticker", "ic_sticker_icon_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.INVISIBLE);
            editor.setBrushDrawingMode(false);
            showDiscordStickerPicker(context, editor, editorView);
        });
        mainToolbar.addView(stickerMainBtn);

        View imageMainBtn = iconButton(context, "Image", "ic_photo_grey_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.INVISIBLE);
            editor.setBrushDrawingMode(false);
            PhotoEditorImagePicker.show(this, context, editor, editorView);
        });
        mainToolbar.addView(imageMainBtn);

        View cropMainBtn = iconButton(context, "Crop", "ucrop_ic_crop", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.INVISIBLE);
            editor.setBrushDrawingMode(false);
            PhotoEditorCropDialog.show(this, context, editorView.getSource(), editorView);
        });
        mainToolbar.addView(cropMainBtn);

        int filterIconId = Utils.getResId("ic_flare_24dp", "drawable");
        if (filterIconId == 0) filterIconId = Utils.getResId("ic_auto_fix_high", "drawable");
        if (filterIconId == 0) filterIconId = Utils.getResId("ic_filter_list_grey_24dp", "drawable");
        String filterIconName = filterIconId != 0 ? context.getResources().getResourceEntryName(filterIconId) : "ic_filter_list_grey_24dp";
        View filterMainBtn = iconButton(context, "Filter", filterIconName, null);
        filterMainBtn.setOnClickListener(v -> {
            selectOnly(mainButtons, filterMainBtn);
            subToolbarContainer.removeAllViews();
            subToolbarContainer.addView(filterScroll);
            subToolbarContainer.setVisibility(View.VISIBLE);
            editor.setBrushDrawingMode(false);
            for (int i=0; i<filterButtons.size(); i++) {
                Object tag = filterButtons.get(i).getTag();
                if ((isCustomFilter[0] && "CUSTOM".equals(tag)) || (!isCustomFilter[0] && tag == sessionFilter[0])) {
                    selectOnly(filterButtons, filterButtons.get(i));
                    break;
                }
            }
        });
        mainButtons.add(filterMainBtn);
        mainToolbar.addView(filterMainBtn);

        // Initial state
        setToolbarButtonSelected(drawMainBtn, true);
        subToolbarContainer.addView(drawScroll);
        subToolbarContainer.setVisibility(View.VISIBLE);
        setToolbarButtonSelected(penButton, true);
        editor.setBrushDrawingMode(true);
        applyBrush(editor);
        applyBrushLayerMode(editorView, true);

        return rootContainer;
    }

    static final String OVERLAY_TEXT = "photoeditor:text";
    static final String OVERLAY_EMOJI = "photoeditor:emoji";
    static final String OVERLAY_IMAGE = "photoeditor:image";
    private static final String MEDIA_EDIT_BUTTON_TAG = "photoeditor:media-edit-button";

    static final int[] COLORS = {
            Color.WHITE,
            Color.BLACK,
            0xffef4444,
            0xfff97316,
            0xffeab308,
            0xff22c55e,
            0xff38bdf8,
            0xffa855f7
    };

    private static final PhotoFilter[] FILTERS = {
            PhotoFilter.NONE,
            PhotoFilter.AUTO_FIX,
            PhotoFilter.BRIGHTNESS,
            PhotoFilter.CONTRAST,
            PhotoFilter.DOCUMENTARY,
            PhotoFilter.DUE_TONE,
            PhotoFilter.FILL_LIGHT,
            PhotoFilter.FISH_EYE,
            PhotoFilter.GRAIN,
            PhotoFilter.GRAY_SCALE,
            PhotoFilter.LOMISH,
            PhotoFilter.NEGATIVE,
            PhotoFilter.POSTERIZE,
            PhotoFilter.SATURATE,
            PhotoFilter.SEPIA,
            PhotoFilter.SHARPEN,
            PhotoFilter.TEMPERATURE,
            PhotoFilter.TINT,
            PhotoFilter.VIGNETTE
    };

    private final Map<Attachment<?>, EditRequest> editRequests = new WeakHashMap<>();
    private final Map<String, SentImageContext> sentImageContexts = new java.util.LinkedHashMap<String, SentImageContext>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SentImageContext> eldest) {
            return size() > 160;
        }
    };
    private WeakReference<WidgetChatInputAttachments> latestChatInputAttachments = new WeakReference<>(null);
    private WeakReference<SelectionAggregator<?>> latestAggregator = new WeakReference<>(null);
    private boolean editorStickerPickerOpen;

    void setEditorStickerPickerOpen(boolean open) {
        editorStickerPickerOpen = open;
    }
    private int brushColor = Color.WHITE;
    private int textColor = Color.WHITE;
    private int brushSize = 24;
    private PhotoFilter selectedFilter = PhotoFilter.NONE;

    @Override
    public void start(Context context) throws Throwable {

        patcher.patch(
                Class.forName("com.discord.widgets.chat.input.WidgetChatInputAttachments$createPreviewAdapter$onAttachmentSelected$1").getDeclaredMethod(
                        "invoke",
                        SelectionAggregator.class,
                        Attachment.class
                ),
                new InsteadHook(callFrame -> {
                    SelectionAggregator<?> aggregator = (SelectionAggregator<?>) callFrame.args[0];
                    Attachment<?> attachment = (Attachment<?>) callFrame.args[1];
                    latestAggregator = new WeakReference<>(aggregator);
                    registerEditRequest(aggregator, attachment);

                    FragmentActivity activity = findFragmentActivity(Utils.getAppActivity());
                    if (settings.getBool("quick_edit", false) && activity != null && isLikelyImage(attachment)) {
                        EditRequest editReq = editRequests.get(attachment);
                        com.aliucord.Utils.mainThread.postDelayed(() -> PhotoEditorSession.open(this, activity, attachment, editReq, aggregator), 160);
                        return null;
                    }
                    try {
                        return XposedBridge.invokeOriginalMethod(callFrame.method, callFrame.thisObject, callFrame.args);
                    } catch (Throwable t) {
                        logger.error(t);
                        return null;
                    }
                })
        );

        patcher.patch(
                AttachmentBottomSheet.class.getDeclaredMethod("onViewCreated", View.class, Bundle.class),
                new Hook(callFrame -> addEditRow((AttachmentBottomSheet) callFrame.thisObject, (View) callFrame.args[0]))
        );

        patcher.patch(
                WidgetChatInputAttachments.class.getDeclaredConstructor(FlexInputFragment.class),
                new Hook(callFrame -> latestChatInputAttachments = new WeakReference<>((WidgetChatInputAttachments) callFrame.thisObject))
        );

        for (java.lang.reflect.Method method : StickerPickerViewModel.class.getDeclaredMethods()) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 1 && (paramTypes[0] == Sticker.class || paramTypes[0] == Object.class)) {
                Class<?> retType = method.getReturnType();
                if (retType == boolean.class || retType == Boolean.class || retType == void.class) {
                    patcher.patch(method, new InsteadHook(callFrame -> {
                        if (editorStickerPickerOpen) {
                            if (retType == boolean.class || retType == Boolean.class) return true;
                            return null;
                        }
                        try {
                            return XposedBridge.invokeOriginalMethod(callFrame.method, callFrame.thisObject, callFrame.args);
                        } catch (Throwable throwable) {
                            throw new RuntimeException(throwable);
                        }
                    }));
                }
            }
        }

        try {
            java.lang.reflect.Field fMessageManager = StickerPickerViewModel.class.getDeclaredField("messageManager");
            Class<?> messageManagerClass = fMessageManager.getType();

            if (messageManagerClass != null) {
                for (java.lang.reflect.Method method : messageManagerClass.getDeclaredMethods()) {
                    Class<?> mRetType = method.getReturnType();
                    if ((mRetType == void.class || mRetType == boolean.class || mRetType == Boolean.class) && method.getParameterTypes().length > 5 && method.getName().contains("sendMessage")) {
                        patcher.patch(method, new InsteadHook(callFrame -> {
                            if (editorStickerPickerOpen) {
                                if (mRetType == boolean.class || mRetType == Boolean.class) return false;
                                return null; // for god sake.
                            }
                            try {
                                return XposedBridge.invokeOriginalMethod(callFrame.method, callFrame.thisObject, callFrame.args);
                            } catch (Throwable throwable) {
                                throw new RuntimeException(throwable);
                            }
                        }));
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to hook MessageManager", t);
        }

        patcher.patch(
                WidgetChatListAdapterItemAttachment.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class),
                new Hook(callFrame -> {
                    if (callFrame.args[1] instanceof AttachmentEntry)
                        registerSentImageContext((AttachmentEntry) callFrame.args[1]);
                })
        );

        patcher.patch(
                WidgetMedia.class.getDeclaredMethod("onViewBoundOrOnResume"),
                new Hook(callFrame -> addMediaEditButton((WidgetMedia) callFrame.thisObject))
        );
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
        editRequests.clear();
        sentImageContexts.clear();
        latestChatInputAttachments.clear();
        editorStickerPickerOpen = false;
    }

    private void registerEditRequest(SelectionAggregator<?> aggregator, Attachment<?> attachment) {
        if (aggregator == null || attachment == null) return;
        editRequests.put(attachment, (oldAttachment, newAttachment) -> {
            try {
                replaceAttachment(aggregator, oldAttachment, newAttachment);
            } catch (Throwable t) {
                logger.error("Failed to replace attachment", t);
            }
        });
    }

    private void registerSentImageContext(AttachmentEntry entry) {
        if (entry == null)
            return;

        MessageAttachment attachment = entry.getAttachment();
        Message message = entry.getMessage();
        if (attachment == null || message == null || !isEditableMessageAttachment(attachment))
            return;

        SentImageContext context = new SentImageContext(message, attachment);
        putSentImageContext(attachment.c(), context);
        putSentImageContext(attachment.f(), context);
    }

    private void putSentImageContext(String url, SentImageContext context) {
        String key = normalizeMediaUrl(url);
        if (key != null && !key.isEmpty())
            sentImageContexts.put(key, context);
    }

    private boolean isEditableMessageAttachment(MessageAttachment attachment) {
        if (attachment == null)
            return false;
        if (attachment.e() == MessageAttachmentType.IMAGE)
            return true;

        String name = attachment.a();
        String url = attachment.c();
        return isLikelyImageName(name) || isLikelyImageName(url);
    }

    private void addMediaEditButton(WidgetMedia media) {
        try {
            Intent intent = media.getMostRecentIntent();
            String imageUrl = intent != null ? intent.getStringExtra("INTENT_IMAGE_URL") : null;
            SentImageContext imageContext = sentImageContexts.get(normalizeMediaUrl(imageUrl));
            if (imageContext == null)
                return;

            View root = media.getView();
            if (!(root instanceof ViewGroup))
                return;

            View existing = root.findViewWithTag(MEDIA_EDIT_BUTTON_TAG);
            if (existing != null) {
                existing.setOnClickListener(view -> openSentImageEditor(view.getContext(), imageContext));
                return;
            }

            android.widget.ImageView edit = new android.widget.ImageView(root.getContext());
            edit.setTag(MEDIA_EDIT_BUTTON_TAG);
            edit.setContentDescription("Edit Image");
            int iconId = Utils.getResId("ic_edit_24dp", "drawable");
            edit.setImageResource(iconId != 0 ? iconId : android.R.drawable.ic_menu_edit);
            edit.setColorFilter(Color.WHITE);
            edit.setPadding(dp(9), dp(9), dp(9), dp(9));
            edit.setOnClickListener(view -> openSentImageEditor(view.getContext(), imageContext));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                edit.setElevation(dp(8));

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(0xdd2b2d31);
            bg.setStroke(dp(1), 0x40ffffff);
            edit.setBackground(bg);

            ViewGroup.LayoutParams rawParams;
            if (root instanceof FrameLayout) {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.RIGHT);
                params.setMargins(0, dp(58), dp(12), 0);
                rawParams = params;
            } else {
                rawParams = new ViewGroup.LayoutParams(dp(44), dp(44));
            }
            ((ViewGroup) root).addView(edit, rawParams);
        } catch (Throwable throwable) {
            logger.error("Failed to add media edit button", throwable);
        }
    }

    private void openSentImageEditor(Context context, SentImageContext sentImageContext) {
        FragmentActivity activity = findFragmentActivity(context);
        if (activity == null)
            activity = findFragmentActivity(Utils.getAppActivity());
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Toast.makeText(context, "Could not open image editor", Toast.LENGTH_SHORT).show();
            return;
        }

        FragmentActivity finalActivity = activity;
        Toast.makeText(context, "Loading image...", Toast.LENGTH_SHORT).show();
        Utils.threadPool.execute(() -> {
            try {
                File source = downloadSentImage(finalActivity, sentImageContext);
                Attachment<?> attachment = new Attachment<>(
                        source.getAbsolutePath().hashCode(),
                        Uri.fromFile(source),
                        source.getName(),
                        null,
                        false
                );
                Utils.mainThread.post(() -> PhotoEditorSession.open(this, finalActivity, attachment, (oldAttachment, newAttachment) -> addEditedReplyAttachment(finalActivity, sentImageContext.message, newAttachment), null));
            } catch (Throwable throwable) {
                logger.error("Failed to download sent image for editing", throwable);
                Utils.mainThread.post(() -> Toast.makeText(context, "Failed to load image for editing", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private File downloadSentImage(Context context, SentImageContext sentImageContext) throws Exception {
        File dir = new File(context.getCacheDir(), "photoeditor-sent");
        if (!dir.exists() && !dir.mkdirs())
            dir = context.getCacheDir();

        String displayName = PhotoEditorUtils.editedFileName(sentImageContext.displayName).replace("-edited-", "-source-");
        File output = new File(dir, displayName);
        try (InputStream input = new java.net.URL(sentImageContext.url).openConnection().getInputStream();
             FileOutputStream outputStream = new FileOutputStream(output, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1)
                outputStream.write(buffer, 0, read);
        }
        return output;
    }

    private void addEditedReplyAttachment(Context context, Message message, Attachment<?> edited) {
        try {
            Channel channel = StoreStream.getChannels().findChannelById(message.getChannelId());
            if (channel != null)
                StoreStream.Companion.getPendingReplies().onCreatePendingReply(channel, message, false, true);

            WidgetChatInputAttachments attachments = latestChatInputAttachments.get();
            if (attachments == null) {
                Toast.makeText(context, "Could not find chat input", Toast.LENGTH_SHORT).show();
                return;
            }

            attachments.addExternalAttachment((Attachment<? extends Object>) edited);
            Toast.makeText(context, "Edited image added as reply attachment", Toast.LENGTH_SHORT).show();
        } catch (Throwable throwable) {
            logger.error("Failed to attach edited reply image", throwable);
            Toast.makeText(context, "Failed to add edited image", Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeMediaUrl(String url) {
        if (url == null)
            return null;
        int query = url.indexOf('?');
        return query >= 0 ? url.substring(0, query) : url;
    }

    private void addEditRow(AttachmentBottomSheet sheet, View root) {
        try {
            Attachment<?> attachment = sheet.getAttachment();
            EditRequest editRequest = editRequests.get(attachment);
            if (attachment == null || editRequest == null || !isLikelyImage(attachment))
                return;

            ViewGroup parent = findAttachmentMenuParent(root);
            View remove = root.findViewById(Utils.getResId("attachment_remove_item", "id"));
            View spoiler = root.findViewById(Utils.getResId("attachment_mark_spoiler", "id"));
            if (!(parent instanceof ConstraintLayout) || remove == null || spoiler == null)
                return;

            Context context = root.getContext();
            AppCompatTextView edit = new AppCompatTextView(context);
            edit.setId(View.generateViewId());
            edit.setText("Edit Image");
            if (spoiler instanceof TextView) {
                TextView s = (TextView) spoiler;
                edit.setTextColor(s.getTextColors());
                edit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, s.getTextSize());
                edit.setTypeface(s.getTypeface());
                edit.setBackground(s.getBackground() != null ? s.getBackground().getConstantState().newDrawable() : null);
                edit.setPadding(s.getPaddingLeft(), s.getPaddingTop(), s.getPaddingRight(), s.getPaddingBottom());
                edit.setCompoundDrawablesWithIntrinsicBounds(Utils.getResId("ic_edit_24dp", "drawable"), 0, 0, 0);
                edit.setCompoundDrawablePadding(s.getCompoundDrawablePadding());
            } else {
                edit.setTextColor(Color.WHITE);
                edit.setPadding(dp(16), dp(16), dp(16), dp(16));
                edit.setBackgroundColor(0xff2b2d31);
            }
            edit.setGravity(Gravity.CENTER_VERTICAL);
            edit.setOnClickListener(view -> {
                FragmentActivity activity = findFragmentActivity(view.getContext());
                sheet.dismiss();
                com.aliucord.Utils.mainThread.postDelayed(() -> PhotoEditorSession.open(this, activity, attachment, editRequest, latestAggregator.get()), 160);
            });

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToBottom = spoiler.getId();
            ((ConstraintLayout) parent).addView(edit, params);

            ViewGroup.LayoutParams removeParams = remove.getLayoutParams();
            if (removeParams instanceof ConstraintLayout.LayoutParams) {
                ((ConstraintLayout.LayoutParams) removeParams).topToBottom = edit.getId();
                remove.setLayoutParams(removeParams);
            }
        } catch (Throwable throwable) {
            logger.error("Failed to add PhotoEditor attachment action", throwable);
        }
    }

    private ViewGroup findAttachmentMenuParent(View root) {
        View remove = root.findViewById(Utils.getResId("attachment_remove_item", "id"));
        return remove != null && remove.getParent() instanceof ViewGroup ? (ViewGroup) remove.getParent() : null;
    }

    private View groupSeparator(Context context) {
        View sep = new View(context);
        sep.setBackgroundColor(0xff3f4147);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(28));
        params.setMargins(dp(4), 0, dp(8), 0);
        sep.setLayoutParams(params);
        return sep;
    }

    View iconButton(Context context, String label, String drawableName, View.OnClickListener listener) {
        int resId = 0;
        if (drawableName != null) {
            resId = Utils.getResId(drawableName, "drawable");
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(8), dp(8), dp(8), dp(8));
        container.setMinimumWidth(dp(48));
        container.setMinimumHeight(dp(48));
        container.setContentDescription(label);

        setToolbarButtonSelected(container, false);
        if (listener != null)
            container.setOnClickListener(listener);

        container.setOnLongClickListener(v -> {
            Toast.makeText(context, label, Toast.LENGTH_SHORT).show();
            return true;
        });

        addPressAnimation(container);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(5), 0);
        container.setLayoutParams(params);

        if (resId != 0) {
            android.widget.ImageView icon = new android.widget.ImageView(context);
            icon.setImageResource(resId);
            icon.setColorFilter(Color.WHITE);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(21), dp(21));
            icon.setLayoutParams(iconParams);
            container.addView(icon);
        } else {
            TextView text = new TextView(context);
            text.setText(label);
            text.setTextColor(Color.WHITE);
            text.setTextSize(22f);  // large enough to fill the button box
            text.setGravity(Gravity.CENTER);
            text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            text.setPadding(dp(2), 0, dp(2), 0);
            container.addView(text);
        }

        return container;
    }



    View colorSwatch(Context context, int color, View.OnClickListener listener) {
        View swatch = new View(context);
        setColorSwatchSelected(swatch, color, color == brushColor);
        swatch.setOnClickListener(listener);
        addPressAnimation(swatch);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(32), dp(32));
        params.setMargins(0, 0, dp(8), 0);
        swatch.setLayoutParams(params);
        return swatch;
    }

    private void setToolbarButtonSelected(View button, boolean selected) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(selected ? 0xcc5865f2 : 0x00000000);
        bg.setCornerRadius(dp(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(0x40ffffff);
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(rippleColor, bg, null);
            button.setBackground(ripple);
        } else {
            button.setBackground(bg);
        }


        if (button instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) button;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView)
                    ((TextView) child).setTextColor(selected ? Color.WHITE : Color.parseColor("#dbdee1"));
                if (child instanceof android.widget.ImageView)
                    ((android.widget.ImageView) child).setColorFilter(selected ? Color.WHITE : Color.parseColor("#dbdee1"));
            }
        }
    }

    void setColorSwatchSelected(View swatch, int color, boolean selected) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dp(selected ? 3 : 1), selected ? Color.WHITE : Color.parseColor("#4e5058"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(0x40ffffff);
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(rippleColor, drawable, null);
            swatch.setBackground(ripple);
        } else {
            swatch.setBackground(drawable);
        }
    }

    private void selectOnly(List<View> buttons, View selected) {
        for (View button : buttons)
            setToolbarButtonSelected(button, button == selected);
    }

    void selectColorOnly(List<View> swatches, int selectedColor) {
        for (View swatch : swatches) {
            Object tag = swatch.getTag();
            if (tag instanceof Integer)
                setColorSwatchSelected(swatch, (Integer) tag, ((Integer) tag) == selectedColor);
        }
    }

    Dialog customDialog(Context context, String titleText, View customView) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(20), dp(20), dp(20));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xff313338);
        bg.setCornerRadius(dp(12));
        container.setBackground(bg);

        // Add title
        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, dp(16));
        title.setLayoutParams(titleParams);
        container.addView(title);

        // Add custom view
        container.addView(customView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        dialog.setContentView(container);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
        }
        return dialog;
    }

    // showFilterDialog removed because it is now rendered inline within the toolbar

    /** Returns true for filters that rely purely on GL shaders and have no ColorMatrix equivalent. */
    com.aliucord.api.SettingsAPI getSettings() { return settings; }

    boolean isGlOnlyFilter(PhotoFilter filter) {
        return filter == PhotoFilter.GRAIN
            || filter == PhotoFilter.FISH_EYE
            || filter == PhotoFilter.VIGNETTE
            || filter == PhotoFilter.SHARPEN;
    }

    int getTextColor() { return textColor; }
    void setTextColor(int color) { textColor = color; }

    LinearLayout createBottomSheetContainer(Context context) {
        return createBottomSheetContainer(context, "Add Text");
    }

    LinearLayout createBottomSheetContainer(Context context, String titleText) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(16));

        android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
        rootBg.setColor(0xff313338);
        rootBg.setCornerRadii(new float[]{dp(16), dp(16), dp(16), dp(16), 0, 0, 0, 0});
        root.setBackground(rootBg);

        View handle = new View(context);
        android.graphics.drawable.GradientDrawable handleBg = new android.graphics.drawable.GradientDrawable();
        handleBg.setColor(0xff4e5058);
        handleBg.setCornerRadius(dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, dp(14));
        root.addView(handle, handleParams);

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, dp(14));
        root.addView(title, titleParams);
        return root;
    }

    void showKeyboardDialog(Dialog dialog, View content, EditText input) {
        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.setOnShowListener(ignored -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null) {
                shownWindow.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                WindowManager.LayoutParams attributes = shownWindow.getAttributes();
                attributes.y = dp(72);
                shownWindow.setAttributes(attributes);
                shownWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                shownWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            input.requestFocus();
        });
        dialog.show();
    }

    private void showDiscordEmojiPicker(Context context, PhotoEditor editor, PhotoEditorView editorView) {
        FragmentActivity activity = findFragmentActivity(context);
        if (activity == null)
            activity = findFragmentActivity(Utils.getAppActivity());

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Toast.makeText(context, "Could not open Discord emoji picker", Toast.LENGTH_SHORT).show();
            return;
        }

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.isDestroyed() || fragmentManager.isStateSaved()) {
            Toast.makeText(context, "Could not open Discord emoji picker", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            EmojiPickerNavigator.launchBottomSheet(
                    fragmentManager,
                    emoji -> addPickedEmoji(context, editor, editorView, emoji),
                    EmojiPickerContextType.Chat.INSTANCE,
                    null
            );
        } catch (Throwable throwable) {
            logger.error("Failed to launch Discord emoji picker", throwable);
            Toast.makeText(context, "Failed to open Discord emoji picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDiscordStickerPicker(Context context, PhotoEditor editor, PhotoEditorView editorView) {
        FragmentActivity activity = findFragmentActivity(context);
        if (activity == null)
            activity = findFragmentActivity(Utils.getAppActivity());

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Toast.makeText(context, "Could not open Discord sticker picker", Toast.LENGTH_SHORT).show();
            return;
        }

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.isDestroyed() || fragmentManager.isStateSaved()) {
            Toast.makeText(context, "Could not open Discord sticker picker", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            editorStickerPickerOpen = true;
            WidgetStickerPickerSheet.Companion.show(
                    fragmentManager,
                    sticker -> addPickedSticker(context, editor, editorView, sticker),
                    null,
                    null,
                    () -> {
                        return Unit.a;
                    }
            );
        } catch (Throwable throwable) {
            editorStickerPickerOpen = false;
            logger.error("Failed to launch Discord sticker picker", throwable);
            Toast.makeText(context, "Failed to open Discord sticker picker", Toast.LENGTH_SHORT).show();
        }
    }

    FragmentActivity findFragmentActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof FragmentActivity)
                return (FragmentActivity) current;
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof FragmentActivity ? (FragmentActivity) current : null;
    }

    private void addPickedEmoji(Context context, PhotoEditor editor, PhotoEditorView editorView, Emoji emoji) {
        if (emoji == null)
            return;

        try {
            String replacement = emoji.getMessageContentReplacement();

            String imageUri = emoji.getImageUri(false, 256, context);
            if (imageUri == null || imageUri.isEmpty()) {
                Toast.makeText(context, "Emoji has no image", Toast.LENGTH_SHORT).show();
                return;
            }

            Utils.threadPool.execute(() -> {
                try {
                    InputStream input;
                    if (imageUri.startsWith("http")) {
                        input = new java.net.URL(imageUri).openConnection().getInputStream();
                    } else if (imageUri.startsWith("res:///")) {
                        int resId = Integer.parseInt(imageUri.substring(7));
                        input = context.getResources().openRawResource(resId);
                    } else {
                        input = context.getContentResolver().openInputStream(android.net.Uri.parse(imageUri));
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (input != null) input.close();
                    if (bitmap == null) {
                        Utils.mainThread.post(() -> Toast.makeText(context, "Failed to load emoji", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    Utils.mainThread.post(() -> {
                        try {
                            editor.setBrushDrawingMode(false);
                            addBitmapOverlay(editorView, bitmap);
                        } catch (Throwable throwable) {
                            logger.error("Failed to add Discord emoji image", throwable);
                            Toast.makeText(context, "Failed to add emoji", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Throwable throwable) {
                    logger.error("Failed to load Discord emoji image", throwable);
                    Utils.mainThread.post(() -> Toast.makeText(context, "Failed to load emoji", Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Throwable throwable) {
            logger.error("Failed to add picked Discord emoji", throwable);
            Toast.makeText(context, "Failed to add emoji", Toast.LENGTH_SHORT).show();
        }
    }

    private void addPickedSticker(Context context, PhotoEditor editor, PhotoEditorView editorView, Sticker sticker) {
        if (sticker == null)
            return;

        try {
            String imageUri = StickerUtils.INSTANCE.getCDNAssetUrl(sticker, 320, false);
            if (imageUri == null || imageUri.isEmpty()) {
                Toast.makeText(context, "Sticker has no image", Toast.LENGTH_SHORT).show();
                return;
            }

            if (imageUri.endsWith(".json")) {
                Toast.makeText(context, "Lottie stickers are not supported yet", Toast.LENGTH_SHORT).show();
                return;
            }

            Utils.threadPool.execute(() -> {
                try (InputStream input = new java.net.URL(imageUri).openConnection().getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap == null) {
                        Utils.mainThread.post(() -> Toast.makeText(context, "Failed to load sticker", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    Utils.mainThread.post(() -> {
                        try {
                            editor.setBrushDrawingMode(false);
                            addBitmapOverlay(editorView, bitmap);
                        } catch (Throwable throwable) {
                            logger.error("Failed to add Discord sticker image", throwable);
                            Toast.makeText(context, "Failed to add sticker", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Throwable throwable) {
                    logger.error("Failed to load Discord sticker image", throwable);
                    Utils.mainThread.post(() -> Toast.makeText(context, "Failed to load sticker", Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Throwable throwable) {
            logger.error("Failed to add picked Discord sticker", throwable);
            Toast.makeText(context, "Failed to add sticker", Toast.LENGTH_SHORT).show();
        }
    }

    void fitEditorToBitmap(PhotoEditorView editorView, View editorHolder, Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0)
            return;

        Runnable fit = () -> {
            int holderWidth = editorHolder.getWidth();
            int holderHeight = editorHolder.getHeight();
            if (holderWidth <= 0 || holderHeight <= 0)
                return;

            float imageRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
            int targetWidth = holderWidth;
            int targetHeight = Math.round(targetWidth / imageRatio);
            if (targetHeight > holderHeight) {
                targetHeight = holderHeight;
                targetWidth = Math.round(targetHeight * imageRatio);
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    Math.max(1, targetWidth),
                    Math.max(1, targetHeight),
                    Gravity.CENTER
            );
            editorView.setLayoutParams(params);
            fillEditorBaseLayers(editorView);
            editorView.requestLayout();
        };

        if (editorHolder.getWidth() > 0 && editorHolder.getHeight() > 0) {
            fit.run();
        }
        editorHolder.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int newWidth = right - left;
            int newHeight = bottom - top;
            int oldWidth = oldRight - oldLeft;
            int oldHeight = oldBottom - oldTop;
            if (newWidth > 0 && newHeight > 0 && (newWidth != oldWidth || newHeight != oldHeight)) {
                fit.run();
            }
        });
    }

    EditText dialogInput(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(false);
        int padding = dp(16);
        input.setPadding(padding, dp(8), padding, dp(8));
        return input;
    }



    private void applyBrushLayerMode(ja.burhanrashid52.photoeditor.PhotoEditorView editorView, boolean isDrawing) {
        int mode = settings.getInt("brush_layer_mode", 2); // 0: Behind, 1: Front, 2: Dynamic
        View drawingView = null;
        for (int i = 0; i < editorView.getChildCount(); i++) {
            View child = editorView.getChildAt(i);
            if (child.getClass().getName().contains("DrawingView")) {
                drawingView = child;
                break;
            }
        }
        if (drawingView != null) {
            if (mode == 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawingView.setElevation(0f);
                }
            } else if (mode == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawingView.setElevation(100f);
                } else {
                    drawingView.bringToFront();
                }
            } else if (mode == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawingView.setElevation(0f);
                }
                if (isDrawing) {
                    drawingView.bringToFront();
                }
            }
        }
    }

    void applyBrush(PhotoEditor editor) {
        editor.setShape(new ShapeBuilder()
                .withShapeColor(brushColor)
                .withShapeSize(brushSize));
    }

    void loadImage(Uri uri, PhotoEditorView editorView, View editorHolder, ProgressBar progressBar) {
        Utils.threadPool.execute(() -> {
            try {
                Bitmap bitmap = decodeBitmap(uri);
                if (bitmap == null)
                    throw new IllegalStateException("Image could not be decoded");

                Utils.mainThread.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    editorView.getSource().setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                    editorView.getSource().setAdjustViewBounds(false);
                    editorView.getSource().setImageBitmap(bitmap);
                    fillEditorBaseLayers(editorView);
                    fitEditorToBitmap(editorView, editorHolder, bitmap);
                });
            } catch (Throwable throwable) {
                logger.error(throwable);
                Utils.mainThread.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(editorView.getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap decodeBitmap(Uri uri) throws Exception {
        Context context = Utils.getAppContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
        }

        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream);
        }
    }

        void addBitmapOverlay(PhotoEditorView editorView, Bitmap bitmap) {
        android.widget.ImageView imageView = new android.widget.ImageView(editorView.getContext());
        imageView.setTag(OVERLAY_IMAGE);
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

            int viewW = editorView.getWidth();
            int viewH = editorView.getHeight();
            if (viewW == 0)
                viewW = android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels;
            if (viewH == 0)
                viewH = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;

            int maxBound = Math.min(viewW, viewH) / 2;
            if (maxBound < dp(100)) maxBound = dp(100);

            int bmpW = bitmap.getWidth();
            int bmpH = bitmap.getHeight();
            float ratio = (bmpH == 0) ? 1f : (float) bmpW / bmpH;

            int bound = Math.min(Math.max(bmpW, bmpH), maxBound);
            if (bound < dp(64)) bound = dp(64);

            int finalW, finalH;
            if (bmpW > bmpH) {
                finalW = bound;
                finalH = (int) (bound / ratio);
            } else {
                finalH = bound;
                finalW = (int) (bound * ratio);
            }

            android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(finalW, finalH);
        params.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT, android.widget.RelativeLayout.TRUE);
        imageView.setLayoutParams(params);
        addManualOverlay(editorView, imageView);
    }

    void fillEditorBaseLayers(PhotoEditorView editorView) {
        for (int i = 0; i < editorView.getChildCount(); i++) {
            View child = editorView.getChildAt(i);
            String className = child.getClass().getName();
            if (child == editorView.getSource()
                    || className.contains("DrawingView")
                    || className.contains("ImageFilterView")) {
                android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                params.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT, android.widget.RelativeLayout.TRUE);
                child.setLayoutParams(params);
            }
        }
    }

    void addTextOverlay(PhotoEditorView editorView, String text, int color, android.graphics.Typeface typeface, float textSize, boolean isUnderlined) {
        TextView textView = new TextView(editorView.getContext());
        textView.setTag(OVERLAY_TEXT);
        textView.setText(text);
        textView.setTextColor(color);
        textView.setTextSize(textSize);
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(typeface);
        if (isUnderlined) {
            textView.setPaintFlags(textView.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        }
        textView.setShadowLayer(dp(2), dp(1), dp(1), 0xcc000000);
        textView.setPadding(dp(8), dp(4), dp(8), dp(4));
        addManualOverlay(editorView, textView);
    }

    private void addManualOverlay(PhotoEditorView editorView, View overlay) {
        overlayRedoStack.clear();
        Runnable add = () -> {
            if (overlay.getParent() == null) {
                ViewGroup.LayoutParams existing = overlay.getLayoutParams();
                android.widget.RelativeLayout.LayoutParams params;
                if (existing instanceof android.widget.RelativeLayout.LayoutParams) {
                    params = (android.widget.RelativeLayout.LayoutParams) existing;
                } else {
                    params = new android.widget.RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT, android.widget.RelativeLayout.TRUE);
                }
                editorView.addView(overlay, params);
            }

            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(1f);
            overlay.setOnTouchListener(new PhotoEditorOverlayTouchListener(this, editorView));
            overlay.bringToFront();
            overlay.requestLayout();
            Toast.makeText(editorView.getContext(), "Added to image. Drag to move, pinch to scale.", Toast.LENGTH_SHORT).show();
        };

        if (editorView.getWidth() > 0 && editorView.getHeight() > 0)
            add.run();
        else
            editorView.post(add);
    }

    private void clearAllEditorOverlays(PhotoEditor editor, PhotoEditorView editorView) {
        overlayRedoStack.clear();
        try {
            editor.clearAllViews();
        } catch (Throwable throwable) {
            logger.error("Failed to clear PhotoEditor views", throwable);
        }

        for (int i = editorView.getChildCount() - 1; i >= 0; i--) {
            View child = editorView.getChildAt(i);
            Object tag = child.getTag();
            if (OVERLAY_TEXT.equals(tag) || OVERLAY_EMOJI.equals(tag) || OVERLAY_IMAGE.equals(tag))
                editorView.removeViewAt(i);
        }
    }

    void replaceAttachment(SelectionAggregator<?> aggregator, Attachment<?> original, Attachment<?> edited) {
        try {
            Method remove = SelectionAggregator.class.getDeclaredMethod("removeItem", Attachment.class);
            remove.setAccessible(true);
            remove.invoke(aggregator, original);

            Method add = SelectionAggregator.class.getDeclaredMethod("addItem", Attachment.class);
            add.setAccessible(true);
            add.invoke(aggregator, edited);
        } catch (Throwable throwable) {
            logger.error("Failed to replace edited attachment", throwable);
            Utils.showToast("Failed to replace attachment");
        }
    }

    private boolean isLikelyImage(Attachment<?> attachment) {
        String name = attachment.getDisplayName();
        return isLikelyImageName(name);
    }

    private boolean isLikelyImageName(String name) {
        if (name == null)
            return false;

        String lower = normalizeMediaUrl(name).toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif")
                || lower.endsWith(".heic")
                || lower.endsWith(".heif");
    }

    void logError(Throwable throwable) { logger.error(throwable); }
    void logError(String message, Throwable throwable) { logger.error(message, throwable); }

    int dp(int value) {
        return DimenUtils.dpToPx(value);
    }

    private void addRipple(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            view.setBackgroundResource(outValue.resourceId);
        }
    }

    void addRippleBorderless(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            view.setBackgroundResource(outValue.resourceId);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    void addPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    interface EditRequest {
        void onEdited(Attachment<?> oldAttachment, Attachment<?> newAttachment);
    }

    private class SentImageContext {
        private final Message message;
        private final String url;
        private final String displayName;

        private SentImageContext(Message message, MessageAttachment attachment) {
            this.message = message;
            String bestUrl = attachment.c();
            if (bestUrl == null || bestUrl.isEmpty())
                bestUrl = attachment.f();
            this.url = bestUrl;
            String name = attachment.a();
            this.displayName = name == null || name.trim().isEmpty() ? "image.png" : name;
        }
    }
}
