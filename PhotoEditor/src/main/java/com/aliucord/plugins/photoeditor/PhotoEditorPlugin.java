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

    public PhotoEditorPlugin() {
        settingsTab = new SettingsTab(PhotoEditorSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
    }

    public interface SpoilerToggleListener {
        void onSpoilerToggled(boolean isSpoiler);
    }

    private View createToolbar(Context context, PhotoEditor editor, PhotoEditorView editorView, Dialog dialog, Attachment<?>[] currentAttachment, EditRequest editRequest, PhotoFilter[] sessionFilter, boolean[] isCustomFilter, float[] customFilterValues) {
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
        subToolbarContainer.setVisibility(View.GONE);
        subToolbarContainer.setBackgroundColor(0xff2b2d31);
        
        rootContainer.addView(subToolbarContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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
        
        // initialize label text with current brushSize
        String initLabelStr = "S";
        if (brushSize > 45) initLabelStr = "L";
        else if (brushSize > 20) initLabelStr = "M";
        sizeLabel.setText(brushSize + "px (" + initLabelStr + ")");
        
        sizeLabel.setPadding(dp(8), 0, dp(4), 0);
        sliderContainer.addView(sizeLabel);

        android.widget.SeekBar brushSlider = new android.widget.SeekBar(context);
        brushSlider.setMax(75); // Range: 5 to 80
        brushSlider.setProgress(brushSize - 5);
        brushSlider.setPadding(dp(8), 0, dp(8), 0);
        
        brushSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int newSize = progress + 5;
                brushSize = newSize;
                
                String labelStr = "S";
                if (newSize > 45) labelStr = "L";
                else if (newSize > 20) labelStr = "M";
                
                sizeLabel.setText(newSize + "px (" + labelStr + ")");
                
                editor.setBrushSize((float) newSize);
                editor.setBrushEraserSize((float) newSize);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        sliderContainer.addView(brushSlider, new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        drawToolbar.addView(sliderContainer);

        drawToolbar.addView(groupSeparator(context));

        final View currentColorBtn = colorSwatch(context, brushColor, null);
        currentColorBtn.setOnClickListener(v -> {
            showColorPickerDialog(context, editorView, editor, brushColor, newColor -> {
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
            View fBtn = iconButton(context, humanize(filter.name()), null, null);
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
                    showCustomFilterDialog(context, editorView, customFilterValues);
                });
                filterToolbar.addView(customBtn);
            }
        }

        // --- MAIN TOOLBAR ---
        View undoMainBtn = iconButton(context, "Undo", "ic_reply_24dp", v -> {
            boolean undidOverlay = false;
            for (int i = editorView.getChildCount() - 1; i >= 0; i--) {
                View child = editorView.getChildAt(i);
                Object tag = child.getTag();
                if (OVERLAY_IMAGE.equals(tag) || OVERLAY_TEXT.equals(tag) || OVERLAY_EMOJI.equals(tag)) {
                    editorView.removeViewAt(i);
                    undidOverlay = true;
                    break;
                }
            }
            if (!undidOverlay) editor.undo();
        });
        mainToolbar.addView(undoMainBtn);

        View redoMainBtn = iconButton(context, "Redo", "ic_reply_24dp", v -> editor.redo());
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
                
                boolean canUndo = false;
                boolean canRedo = false;
                try {
                    java.lang.reflect.Method isUndoAvailable = editor.getClass().getDeclaredMethod("isUndoAvailable");
                    canUndo = (boolean) isUndoAvailable.invoke(editor);
                    java.lang.reflect.Method isRedoAvailable = editor.getClass().getDeclaredMethod("isRedoAvailable");
                    canRedo = (boolean) isRedoAvailable.invoke(editor);
                } catch (Throwable ignored) {}
                
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

                undoMainBtn.postDelayed(this, 100);
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
            subToolbarContainer.setVisibility(View.GONE);
            editor.setBrushDrawingMode(false);
            showTextDialog(context, editor, editorView);
        });
        mainToolbar.addView(textMainBtn);

        View emojiMainBtn = iconButton(context, "Emoji", "ic_emoji_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.GONE);
            editor.setBrushDrawingMode(false);
            showDiscordEmojiPicker(context, editor, editorView);
        });
        mainToolbar.addView(emojiMainBtn);

        View stickerMainBtn = iconButton(context, "Sticker", "ic_sticker_icon_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.GONE);
            editor.setBrushDrawingMode(false);
            showDiscordStickerPicker(context, editor, editorView);
        });
        mainToolbar.addView(stickerMainBtn);

        View imageMainBtn = iconButton(context, "Image", "ic_photo_grey_24dp", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.GONE);
            editor.setBrushDrawingMode(false);
            showImagePicker(context, editor, editorView);
        });
        mainToolbar.addView(imageMainBtn);

        View cropMainBtn = iconButton(context, "Crop", "ucrop_ic_crop", v -> {
            selectOnly(mainButtons, null);
            subToolbarContainer.setVisibility(View.GONE);
            editor.setBrushDrawingMode(false);
            showCropDialog(context, editorView.getSource(), editorView);
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

    private static final String OVERLAY_TEXT = "photoeditor:text";
    private static final String OVERLAY_EMOJI = "photoeditor:emoji";
    private static final String OVERLAY_IMAGE = "photoeditor:image";
    private static final String MEDIA_EDIT_BUTTON_TAG = "photoeditor:media-edit-button";

    private static final int[] COLORS = {
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

                    FragmentActivity activity = findFragmentActivity(getRealActivity());
                    if (settings.getBool("quick_edit", false) && activity != null && isLikelyImage(attachment)) {
                        com.aliucord.Utils.mainThread.postDelayed(() -> openEditor(activity, attachment, editRequests.get(attachment), aggregator), 160);
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
            if (paramTypes.length >= 1 && (paramTypes[0] == Sticker.class || paramTypes[0] == Object.class)) {
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
            activity = findFragmentActivity(getRealActivity());
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
                Utils.mainThread.post(() -> openEditor(finalActivity, attachment, (oldAttachment, newAttachment) -> addEditedReplyAttachment(finalActivity, sentImageContext.message, newAttachment), null));
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

        String displayName = editedFileName(sentImageContext.displayName).replace("-edited-", "-source-");
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
                com.aliucord.Utils.mainThread.postDelayed(() -> openEditor(activity, attachment, editRequest, latestAggregator.get()), 160);
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

    private Activity getRealActivity() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            java.util.Map<?, ?> activities = (java.util.Map<?, ?>) activitiesField.get(activityThread);
            
            for (Object activityRecord : activities.values()) {
                java.lang.reflect.Field activityField = activityRecord.getClass().getDeclaredField("activity");
                activityField.setAccessible(true);
                Activity activity = (Activity) activityField.get(activityRecord);
                if (activity != null && !activity.isFinishing() && (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed())) {
                    return activity;
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to get active activity from ActivityThread", t);
        }
        return com.aliucord.Utils.getAppActivity();
    }

    private void openEditor(Activity passedActivity, Attachment<?> attachment, EditRequest editRequest, SelectionAggregator<?> aggregator) {
        final Activity activity;
        if (passedActivity == null || passedActivity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && passedActivity.isDestroyed())) {
            activity = getRealActivity();
        } else {
            activity = passedActivity;
        }
        if (activity == null || activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
            android.widget.Toast.makeText(getRealActivity(), "Could not open image editor (activity null or destroyed)", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (attachment.getUri() == null) {
            android.widget.Toast.makeText(activity, "Attachment has no URI", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);

        PhotoEditorView editorView = new PhotoEditorView(activity);
        PhotoEditor editor = new PhotoEditor.Builder(activity, editorView)
                .setPinchTextScalable(true)
                .setClipSourceImage(false)
                .build();

        final Attachment<?>[] currentAttachment = {attachment};
        final PhotoFilter[] sessionFilter = {PhotoFilter.NONE};
        final boolean[] isCustomFilter = {false};
        final float[] customFilterValues = new float[]{0f, 1f, 1f, 0f, 0f, 0f}; // Brightness, Contrast, Saturation, Hue, Temp, Tint

        FrameLayout spoilerOverlay = new FrameLayout(activity);
        spoilerOverlay.setBackgroundColor(0x99000000);
        
        TextView spoilerText = new TextView(activity);
        spoilerText.setText("SPOILER");
        spoilerText.setTextColor(Color.WHITE);
        spoilerText.setTextSize(24f);
        spoilerText.setTypeface(null, android.graphics.Typeface.BOLD);
        spoilerText.setPadding(dp(20), dp(8), dp(20), dp(8));
        
        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setColor(0x80000000);
        pill.setCornerRadius(dp(20));
        spoilerText.setBackground(pill);
        
        spoilerOverlay.addView(spoilerText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        spoilerOverlay.setVisibility(attachment.getSpoiler() ? View.VISIBLE : View.GONE);

        FrameLayout root = new FrameLayout(activity) {
            @Override
            public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
                if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    if (spoilerOverlay.getVisibility() == View.VISIBLE && spoilerOverlay.getAlpha() == 1f) {
                        spoilerOverlay.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                            spoilerOverlay.setVisibility(View.GONE);
                            spoilerOverlay.setAlpha(1f);
                        }).start();
                    }
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        root.setBackgroundColor(0xff111214);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View header = createHeader(activity, currentAttachment, aggregator, view -> dialog.dismiss(), isSpoiler -> {
            if (isSpoiler) {
                spoilerOverlay.setVisibility(View.VISIBLE);
                spoilerOverlay.setAlpha(0f);
                spoilerOverlay.animate().alpha(1f).setDuration(150).setListener(null).start();
            } else {
                if (spoilerOverlay.getVisibility() == View.VISIBLE) {
                    spoilerOverlay.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                        spoilerOverlay.setVisibility(View.GONE);
                        spoilerOverlay.setAlpha(1f);
                    }).start();
                }
            }
        });
        
        android.widget.ImageView saveBtn = new android.widget.ImageView(activity);
        int saveId = Utils.getResId("ic_check_white_24dp", "drawable");
        saveBtn.setImageResource(saveId != 0 ? saveId : android.R.drawable.ic_menu_save);
        saveBtn.setColorFilter(Color.WHITE);
        saveBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        addRippleBorderless(saveBtn);
        addPressAnimation(saveBtn);
        saveBtn.setOnClickListener(v -> saveImage(activity, editor, editorView, currentAttachment, editRequest, dialog, sessionFilter, isCustomFilter, customFilterValues));
        
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        saveParams.setMargins(dp(8), 0, 0, 0);
        ((LinearLayout) header).addView(saveBtn, saveParams);

        content.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        FrameLayout editorHolder = new FrameLayout(activity);
        editorHolder.setBackgroundColor(Color.BLACK);
        content.addView(editorHolder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        editorHolder.addView(editorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        ));

        editorHolder.addView(spoilerOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ProgressBar progressBar = new ProgressBar(activity);
        editorHolder.addView(progressBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));

        content.addView(createToolbar(activity, editor, editorView, dialog, currentAttachment, editRequest, sessionFilter, isCustomFilter, customFilterValues), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        dialog.setOnShowListener(ignored -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null)
                shownWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            loadImage(currentAttachment[0].getUri(), editorView, editorHolder, progressBar);
            applyBrush(editor);
        });
        dialog.setOnDismissListener(ignored -> {
            editorStickerPickerOpen = false;
        });

        editorStickerPickerOpen = true;
        try {
            dialog.show();
        } catch (android.view.WindowManager.BadTokenException badTokenException) {
            logger.error("Failed to show PhotoEditor dialog because the activity window token is invalid", badTokenException);
            Toast.makeText(activity, "Could not open image editor", Toast.LENGTH_SHORT).show();
        }
    }

    private View createHeader(Context context, Attachment<?>[] currentAttachment, SelectionAggregator<?> aggregator, View.OnClickListener onClose, SpoilerToggleListener spoilerListener) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(0xff1e1f22);
        header.setPadding(dp(16), dp(10), dp(16), dp(10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            header.setElevation(dp(4));
        }

        android.widget.ImageView closeBtn = new android.widget.ImageView(context);
        int closeId = Utils.getResId("ic_close_24dp", "drawable");
        closeBtn.setImageResource(closeId != 0 ? closeId : android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setColorFilter(Color.WHITE);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        closeBtn.setOnClickListener(onClose);
        addRippleBorderless(closeBtn);
        addPressAnimation(closeBtn);

        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        closeParams.setMargins(0, 0, dp(8), 0);
        closeBtn.setLayoutParams(closeParams);
        header.addView(closeBtn);

        TextView title = new TextView(context);
        String displayName = currentAttachment[0].getDisplayName();
        title.setText(displayName == null ? "PhotoEditor" : displayName);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        header.addView(title);

        if (aggregator != null) {
            // Spoiler toggle
            android.widget.ImageView spoilerBtn = new android.widget.ImageView(context);
            int eyeOpenId = Utils.getResId("ic_eye_24dp", "drawable");
            int eyeClosedId = Utils.getResId("ic_eye_closed_24dp", "drawable");
            if (eyeOpenId == 0) eyeOpenId = android.R.drawable.ic_menu_view;
            if (eyeClosedId == 0) eyeClosedId = android.R.drawable.ic_menu_view;

            boolean[] isSpoiler = {currentAttachment[0].getSpoiler()};
            spoilerBtn.setImageResource(isSpoiler[0] ? eyeClosedId : eyeOpenId);
            spoilerBtn.setColorFilter(isSpoiler[0] ? 0xffda373c : Color.WHITE);
            spoilerBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
            addRippleBorderless(spoilerBtn);
            addPressAnimation(spoilerBtn);
            
            final int finalEyeOpen = eyeOpenId;
            final int finalEyeClosed = eyeClosedId;

            spoilerBtn.setOnClickListener(v -> {
                isSpoiler[0] = !isSpoiler[0];
                spoilerBtn.setImageResource(isSpoiler[0] ? finalEyeClosed : finalEyeOpen);
                spoilerBtn.setColorFilter(isSpoiler[0] ? 0xffda373c : Color.WHITE);
                
                try {
                    Attachment<?> edited = new Attachment<>(
                            currentAttachment[0].getId(),
                            currentAttachment[0].getUri(),
                            currentAttachment[0].getDisplayName(),
                            null,
                            isSpoiler[0]
                    );
                    replaceAttachment(aggregator, currentAttachment[0], edited);
                    currentAttachment[0] = edited;
                    if (spoilerListener != null) {
                        spoilerListener.onSpoilerToggled(isSpoiler[0]);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to toggle spoiler", t);
                }
            });

            LinearLayout.LayoutParams spoilerParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            spoilerParams.setMargins(0, 0, dp(8), 0);
            header.addView(spoilerBtn, spoilerParams);

            // Delete button
            android.widget.ImageView deleteBtn = new android.widget.ImageView(context);
            int deleteId = Utils.getResId("ic_delete_24dp", "drawable");
            deleteBtn.setImageResource(deleteId != 0 ? deleteId : android.R.drawable.ic_menu_delete);
            deleteBtn.setColorFilter(0xffda373c);
            deleteBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
            addRippleBorderless(deleteBtn);
            addPressAnimation(deleteBtn);

            deleteBtn.setOnClickListener(v -> {
                try {
                    java.lang.reflect.Method remove = SelectionAggregator.class.getDeclaredMethod("removeItem", Attachment.class);
                    remove.setAccessible(true);
                    remove.invoke(aggregator, currentAttachment[0]);
                    onClose.onClick(v);
                } catch (Throwable t) {
                    logger.error("Failed to delete attachment", t);
                }
            });

            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            header.addView(deleteBtn, deleteParams);
        }

        return header;
    }

    private View groupSeparator(Context context) {
        View sep = new View(context);
        sep.setBackgroundColor(0xff3f4147);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(28));
        params.setMargins(dp(4), 0, dp(8), 0);
        sep.setLayoutParams(params);
        return sep;
    }

    private View iconButton(Context context, String label, String drawableName, View.OnClickListener listener) {
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



    private View colorSwatch(Context context, int color, View.OnClickListener listener) {
        View swatch = new View(context);
        setColorSwatchSelected(swatch, color, color == brushColor);
        swatch.setOnClickListener(listener);
        addPressAnimation(swatch);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(32), dp(32));
        params.setMargins(0, 0, dp(8), 0);
        swatch.setLayoutParams(params);
        return swatch;
    }
    
    interface ColorCallback {
        void onColorPicked(int color);
    }
    
    private void showColorPickerDialog(Context context, PhotoEditorView editorView, PhotoEditor editor, int initialColor, ColorCallback callback) {
        Dialog dialog = new Dialog(context);
        
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        
        android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
        rootBg.setColor(0xff2b2d31);
        rootBg.setCornerRadius(dp(16));
        root.setBackground(rootBg);
        
        // 1. Title Row (Title + Eyedropper)
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(context);
        title.setText("Custom Color");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        android.widget.ImageView eyedropper = new android.widget.ImageView(context);
        int dropperId = Utils.getResId("ic_colorize_24dp", "drawable");
        if (dropperId == 0) dropperId = Utils.getResId("ic_edit_24dp", "drawable");
        eyedropper.setImageResource(dropperId);
        eyedropper.setColorFilter(Color.WHITE);
        eyedropper.setPadding(dp(8), dp(8), dp(8), dp(8));
        addRippleBorderless(eyedropper);
        titleRow.addView(eyedropper);
        root.addView(titleRow);

        // Eyedropper Logic
        eyedropper.setOnClickListener(v -> {
            dialog.dismiss();
            android.widget.Toast.makeText(context, "Tap anywhere on the image to pick a color!", android.widget.Toast.LENGTH_SHORT).show();
            
            editor.setBrushDrawingMode(false);
            editorView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, android.view.MotionEvent event) {
                    int action = event.getAction();
                    if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_MOVE || action == android.view.MotionEvent.ACTION_UP) {
                        try {
                            android.widget.ImageView source = editorView.getSource();
                            android.graphics.drawable.BitmapDrawable drawable = (android.graphics.drawable.BitmapDrawable) source.getDrawable();
                            android.graphics.Bitmap bitmap = drawable.getBitmap();
                            
                            android.graphics.Matrix inverse = new android.graphics.Matrix();
                            source.getImageMatrix().invert(inverse);
                            float[] pts = {event.getX(), event.getY()};
                            inverse.mapPoints(pts);
                            
                            int x = (int) pts[0];
                            int y = (int) pts[1];
                            
                            if (x >= 0 && y >= 0 && x < bitmap.getWidth() && y < bitmap.getHeight()) {
                                int pixel = bitmap.getPixel(x, y);
                                callback.onColorPicked(pixel);
                            }
                        } catch (Throwable t) {
                            // Silently ignore out of bounds during drag
                        }
                        
                        if (action == android.view.MotionEvent.ACTION_UP) {
                            editorView.setOnTouchListener(null);
                            editor.setBrushDrawingMode(true);
                        }
                        return true;
                    }
                    return false;
                }
            });
        });
        
        // 2. Preview Box and HEX
        LinearLayout previewRow = new LinearLayout(context);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        previewRow.setPadding(0, dp(16), 0, dp(16));
        
        View previewBox = new View(context);
        android.graphics.drawable.GradientDrawable boxBg = new android.graphics.drawable.GradientDrawable();
        boxBg.setCornerRadius(dp(8));
        boxBg.setColor(initialColor);
        previewBox.setBackground(boxBg);
        previewRow.addView(previewBox, new LinearLayout.LayoutParams(dp(48), dp(48)));
        
        android.widget.EditText hexInput = new android.widget.EditText(context);
        hexInput.setTextColor(Color.WHITE);
        hexInput.setText(String.format("#%08X", initialColor));
        hexInput.setSingleLine(true);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexParams.setMargins(dp(16), 0, 0, 0);
        previewRow.addView(hexInput, hexParams);
        root.addView(previewRow);
        
        // 3. HSV Sliders
        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);
        
        TextView hueLabel = new TextView(context); hueLabel.setText("Hue"); hueLabel.setTextColor(Color.LTGRAY); root.addView(hueLabel);
        android.widget.SeekBar hueSlider = new android.widget.SeekBar(context); hueSlider.setMax(360); hueSlider.setProgress((int) hsv[0]); root.addView(hueSlider);
        
        TextView satLabel = new TextView(context); satLabel.setText("Saturation"); satLabel.setTextColor(Color.LTGRAY); satLabel.setPadding(0, dp(8), 0, 0); root.addView(satLabel);
        android.widget.SeekBar satSlider = new android.widget.SeekBar(context); satSlider.setMax(100); satSlider.setProgress((int) (hsv[1] * 100)); root.addView(satSlider);
        
        TextView valLabel = new TextView(context); valLabel.setText("Lightness"); valLabel.setTextColor(Color.LTGRAY); valLabel.setPadding(0, dp(8), 0, 0); root.addView(valLabel);
        android.widget.SeekBar valSlider = new android.widget.SeekBar(context); valSlider.setMax(100); valSlider.setProgress((int) (hsv[2] * 100)); root.addView(valSlider);
        
        TextView alphaLabel = new TextView(context); alphaLabel.setText("Opacity"); alphaLabel.setTextColor(Color.LTGRAY); alphaLabel.setPadding(0, dp(8), 0, 0); root.addView(alphaLabel);
        android.widget.SeekBar alphaSlider = new android.widget.SeekBar(context); alphaSlider.setMax(255); alphaSlider.setProgress(Color.alpha(initialColor)); root.addView(alphaSlider);
        
        // 4. Presets Horizontal Scroll
        android.widget.HorizontalScrollView presetScroll = new android.widget.HorizontalScrollView(context);
        presetScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presetContainer = new LinearLayout(context);
        presetContainer.setOrientation(LinearLayout.HORIZONTAL);
        presetContainer.setPadding(0, dp(16), 0, dp(16));
        for (int c : COLORS) {
            View swatch = new View(context);
            android.graphics.drawable.GradientDrawable swBg = new android.graphics.drawable.GradientDrawable();
            swBg.setCornerRadius(dp(16)); swBg.setColor(c);
            swatch.setBackground(swBg);
            LinearLayout.LayoutParams swParams = new LinearLayout.LayoutParams(dp(32), dp(32));
            swParams.setMargins(0, 0, dp(8), 0);
            swatch.setLayoutParams(swParams);
            swatch.setOnClickListener(v -> {
                Color.colorToHSV(c, hsv);
                hueSlider.setProgress((int) hsv[0]);
                satSlider.setProgress((int) (hsv[1] * 100));
                valSlider.setProgress((int) (hsv[2] * 100));
                alphaSlider.setProgress(Color.alpha(c));
            });
            presetContainer.addView(swatch);
        }
        presetScroll.addView(presetContainer);
        root.addView(presetScroll);
        
        // Updates
        final int[] currentColor = {initialColor};
        
        Runnable updateColor = () -> {
            try {
                hsv[0] = hueSlider.getProgress();
                hsv[1] = satSlider.getProgress() / 100f;
                hsv[2] = valSlider.getProgress() / 100f;
                int alpha = alphaSlider.getProgress();
                currentColor[0] = Color.HSVToColor(alpha, hsv);
                boxBg.setColor(currentColor[0]);
                previewBox.invalidate();
                if (!hexInput.hasFocus()) {
                    hexInput.setText(String.format("#%08X", currentColor[0]));
                }
            } catch (Exception e) {}
        };
        
        android.widget.SeekBar.OnSeekBarChangeListener sliderListener = new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) { updateColor.run(); }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        };
        hueSlider.setOnSeekBarChangeListener(sliderListener);
        satSlider.setOnSeekBarChangeListener(sliderListener);
        valSlider.setOnSeekBarChangeListener(sliderListener);
        alphaSlider.setOnSeekBarChangeListener(sliderListener);
        
        hexInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (hexInput.hasFocus() && s.toString().startsWith("#")) {
                    try {
                        int c = Color.parseColor(s.toString());
                        currentColor[0] = c;
                        boxBg.setColor(c);
                        Color.colorToHSV(c, hsv);
                        hueSlider.setProgress((int) hsv[0]);
                        satSlider.setProgress((int) (hsv[1] * 100));
                        valSlider.setProgress((int) (hsv[2] * 100));
                        alphaSlider.setProgress(Color.alpha(c));
                    } catch (Exception ignored) {}
                }
            }
        });
        
        // Buttons
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.RIGHT);
        
        TextView cancel = new TextView(context); cancel.setText("Cancel"); cancel.setTextColor(Color.GRAY); cancel.setPadding(dp(16), dp(8), dp(16), dp(8));
        cancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(cancel);
        
        TextView apply = new TextView(context); apply.setText("Select"); apply.setTextColor(0xff5865f2); apply.setPadding(dp(16), dp(8), dp(16), dp(8)); apply.setTypeface(null, android.graphics.Typeface.BOLD);
        apply.setOnClickListener(v -> {
            callback.onColorPicked(currentColor[0]);
            dialog.dismiss();
        });
        btnRow.addView(apply);
        
        root.addView(btnRow);
        
        dialog.setContentView(root);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.show();
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

    private void setColorSwatchSelected(View swatch, int color, boolean selected) {
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

    private void selectColorOnly(List<View> swatches, int selectedColor) {
        for (View swatch : swatches) {
            Object tag = swatch.getTag();
            if (tag instanceof Integer)
                setColorSwatchSelected(swatch, (Integer) tag, ((Integer) tag) == selectedColor);
        }
    }

    private Dialog customDialog(Context context, String titleText, View customView) {
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

    private void showImagePicker(Context context, PhotoEditor editor, PhotoEditorView editorView) {
        androidx.fragment.app.FragmentActivity activity = findFragmentActivity(context);
        if (activity == null) activity = findFragmentActivity(getRealActivity());
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            android.widget.Toast.makeText(context, "Could not open image picker", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        ImagePickerFragment fragment = new ImagePickerFragment();
        fragment.setListener(uri -> {
            if (uri != null) {
                com.aliucord.Utils.threadPool.execute(() -> {
                    try {
                        android.graphics.Bitmap bmp = android.provider.MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                        if (bmp != null) {
                            com.aliucord.Utils.mainThread.post(() -> addBitmapOverlay(editorView, bmp));
                        }
                    } catch (Throwable t) {
                        logger.error("Failed to load picked image", t);
                        com.aliucord.Utils.mainThread.post(() -> android.widget.Toast.makeText(context, "Failed to load image", android.widget.Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });

        activity.getSupportFragmentManager().beginTransaction().add(fragment, "IMAGE_PICKER").commitAllowingStateLoss();
    }

    // showFilterDialog removed because it is now rendered inline within the toolbar

    /** Returns true for filters that rely purely on GL shaders and have no ColorMatrix equivalent. */
    private boolean isGlOnlyFilter(PhotoFilter filter) {
        return filter == PhotoFilter.GRAIN
            || filter == PhotoFilter.FISH_EYE
            || filter == PhotoFilter.VIGNETTE
            || filter == PhotoFilter.SHARPEN;
    }

    private void showTextDialog(Context context, PhotoEditor editor, PhotoEditorView editorView) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = createBottomSheetContainer(context);
        
        // State variables
        final int[] currentTextColor = {textColor};
        final float[] currentTextSize = {28f};
        final String[] currentFontFamily = {"sans-serif"};
        final boolean[] isBold = {false};
        final boolean[] isItalic = {false};
        final boolean[] isUnderlined = {false};
        final android.graphics.Typeface[] finalTypeface = {android.graphics.Typeface.DEFAULT};

        final java.util.Map<String, String> fontPathMap = new java.util.HashMap<>();
        String[] defaultFonts = {"sans-serif", "sans-serif-thin", "sans-serif-light", "sans-serif-medium", "sans-serif-black", "sans-serif-condensed", "sans-serif-condensed-light", "sans-serif-condensed-medium", "serif", "monospace", "serif-monospace", "casual", "cursive", "sans-serif-smallcaps"};
        final java.util.List<String> fontList = new java.util.ArrayList<>(java.util.Arrays.asList(defaultFonts));
        
        class FontScanner {
            void scan(java.io.File dir, int depth) {
                if (depth > 2 || dir == null || !dir.exists() || !dir.isDirectory()) return;
                java.io.File[] files = dir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.isDirectory()) {
                            scan(f, depth + 1);
                        } else if (f.isFile() && (f.getName().endsWith(".ttf") || f.getName().endsWith(".otf"))) {
                            String name = f.getName();
                            String cleanName = name.substring(0, name.lastIndexOf('.'));
                            fontList.add(cleanName);
                            fontPathMap.put(cleanName, f.getAbsolutePath());
                        }
                    }
                }
            }
        }
        new FontScanner().scan(new java.io.File("/system/fonts"), 0);
        new FontScanner().scan(new java.io.File(Environment.getExternalStorageDirectory(), "Fonts"), 0);
        new FontScanner().scan(new java.io.File(Environment.getExternalStorageDirectory(), "Aliucord/fonts"), 0);

        EditText input = dialogInput(context, "Enter your text...");
        android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
        inputBg.setColor(0xff1e1f22);
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(1), Color.parseColor("#3f4147"));
        input.setBackground(inputBg);
        input.setTextColor(currentTextColor[0]);
        input.setHintTextColor(Color.parseColor("#80848e"));
        input.setTextSize(currentTextSize[0]);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));

        Runnable updateTypeface = () -> {
            int style = android.graphics.Typeface.NORMAL;
            if (isBold[0] && isItalic[0]) style = android.graphics.Typeface.BOLD_ITALIC;
            else if (isBold[0]) style = android.graphics.Typeface.BOLD;
            else if (isItalic[0]) style = android.graphics.Typeface.ITALIC;
            
            try {
                if (fontPathMap.containsKey(currentFontFamily[0])) {
                    java.io.File f = new java.io.File(fontPathMap.get(currentFontFamily[0]));
                    if (f.exists()) {
                        finalTypeface[0] = android.graphics.Typeface.createFromFile(f);
                        if (style != android.graphics.Typeface.NORMAL) {
                            finalTypeface[0] = android.graphics.Typeface.create(finalTypeface[0], style);
                        }
                    } else {
                        finalTypeface[0] = android.graphics.Typeface.create(currentFontFamily[0], style);
                    }
                } else if (currentFontFamily[0].endsWith(".ttf") || currentFontFamily[0].endsWith(".otf")) {
                    java.io.File f = new java.io.File("/system/fonts/" + currentFontFamily[0]);
                    if (f.exists()) {
                        finalTypeface[0] = android.graphics.Typeface.createFromFile(f);
                        if (style != android.graphics.Typeface.NORMAL) {
                            finalTypeface[0] = android.graphics.Typeface.create(finalTypeface[0], style);
                        }
                    } else {
                        finalTypeface[0] = android.graphics.Typeface.create(currentFontFamily[0], style);
                    }
                } else {
                    finalTypeface[0] = android.graphics.Typeface.create(currentFontFamily[0], style);
                }
            } catch (Exception e) {
                finalTypeface[0] = android.graphics.Typeface.create("sans-serif", style);
            }
            input.setTypeface(finalTypeface[0]);
            input.setTextSize(currentTextSize[0]);
            input.setTextColor(currentTextColor[0]);
            
            if (isUnderlined[0]) {
                input.setPaintFlags(input.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            } else {
                input.setPaintFlags(input.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            }
        };

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, dp(16));
        input.setLayoutParams(inputParams);
        layout.addView(input);
        
        // Wrap everything else in a ScrollView to prevent keyboard overlap
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        
        // Font Selection Row
        android.widget.AutoCompleteTextView fontSearch = new android.widget.AutoCompleteTextView(context);
        
        int bgColor = 0xff313338;
        int textColorPrimary = Color.WHITE;
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            int bgAttr = Utils.getResId("colorBackgroundFloating", "attr");
            if (bgAttr != 0 && context.getTheme().resolveAttribute(bgAttr, typedValue, true)) {
                bgColor = typedValue.data;
            } else if (context.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
                bgColor = typedValue.data;
            }
            
            int textAttr = Utils.getResId("colorTextNormal", "attr");
            if (textAttr != 0 && context.getTheme().resolveAttribute(textAttr, typedValue, true)) {
                textColorPrimary = typedValue.data;
            } else if (context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)) {
                textColorPrimary = typedValue.data;
            }
        } catch (Exception ignored) {}
        final int finalTextColor = textColorPrimary;
        
        fontSearch.setTextColor(textColorPrimary);
        fontSearch.setHint("Search font (e.g., sans-serif)");
        fontSearch.setHintTextColor(Color.GRAY);
        fontSearch.setSingleLine(true);
        fontSearch.setText("sans-serif");
        fontSearch.setThreshold(0); // Show dropdown even when empty
        fontSearch.setDropDownHeight(dp(150)); // Constrain height so it doesn't hide behind keyboard
        fontSearch.setDropDownBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor)); // Match theme
        fontSearch.setOnClickListener(v -> fontSearch.showDropDown());
        fontSearch.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) fontSearch.showDropDown(); });
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, fontList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(finalTextColor);
                return view;
            }
        };
        fontSearch.setAdapter(adapter);
        
        fontSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                currentFontFamily[0] = s.toString().trim();
                updateTypeface.run();
            }
        });
        fontSearch.setOnItemClickListener((parent, view, position, id) -> {
            currentFontFamily[0] = adapter.getItem(position);
            updateTypeface.run();
        });
        
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 0, 0, dp(12));
        scrollContent.addView(fontSearch, searchParams);
        
        // Style Toggles Row
        LinearLayout styleTogglesRow = new LinearLayout(context);
        styleTogglesRow.setOrientation(LinearLayout.HORIZONTAL);
        styleTogglesRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        
        // Bold Toggle
        TextView boldToggle = new TextView(context);
        boldToggle.setText("B");
        boldToggle.setTextColor(Color.GRAY);
        boldToggle.setTextSize(18f);
        boldToggle.setTypeface(null, android.graphics.Typeface.BOLD);
        boldToggle.setPadding(dp(8), dp(4), dp(16), dp(4));
        boldToggle.setOnClickListener(v -> {
            isBold[0] = !isBold[0];
            boldToggle.setTextColor(isBold[0] ? Color.WHITE : Color.GRAY);
            updateTypeface.run();
        });
        styleTogglesRow.addView(boldToggle);
        
        // Italic Toggle
        TextView italicToggle = new TextView(context);
        italicToggle.setText("I");
        italicToggle.setTextColor(Color.GRAY);
        italicToggle.setTextSize(18f);
        italicToggle.setTypeface(null, android.graphics.Typeface.ITALIC);
        italicToggle.setPadding(dp(16), dp(4), dp(16), dp(4));
        italicToggle.setOnClickListener(v -> {
            isItalic[0] = !isItalic[0];
            italicToggle.setTextColor(isItalic[0] ? Color.WHITE : Color.GRAY);
            updateTypeface.run();
        });
        styleTogglesRow.addView(italicToggle);

        // Underline Toggle
        TextView underlineToggle = new TextView(context);
        underlineToggle.setText("U");
        underlineToggle.setTextColor(Color.GRAY);
        underlineToggle.setTextSize(18f);
        underlineToggle.setPaintFlags(underlineToggle.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        underlineToggle.setPadding(dp(16), dp(4), dp(16), dp(4));
        underlineToggle.setOnClickListener(v -> {
            isUnderlined[0] = !isUnderlined[0];
            underlineToggle.setTextColor(isUnderlined[0] ? Color.WHITE : Color.GRAY);
            updateTypeface.run();
        });
        styleTogglesRow.addView(underlineToggle);
        
        LinearLayout.LayoutParams togglesRowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        togglesRowParams.setMargins(0, 0, 0, dp(12));
        scrollContent.addView(styleTogglesRow, togglesRowParams);
        
        // Size & Color Row
        LinearLayout styleRow = new LinearLayout(context);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleRow.setGravity(Gravity.CENTER_VERTICAL);
        styleRow.setPadding(0, 0, 0, dp(16));
        
        TextView sizeLabel = new TextView(context);
        sizeLabel.setText("Size " + (int) currentTextSize[0]);
        sizeLabel.setTextColor(Color.parseColor("#b5bac1"));
        sizeLabel.setTextSize(14f);
        sizeLabel.setMinimumWidth(dp(65)); // Prevent layout jumping
        styleRow.addView(sizeLabel);
        
        android.widget.SeekBar sizeSlider = new android.widget.SeekBar(context);
        sizeSlider.setMax(110); // 10 to 120
        sizeSlider.setProgress((int) currentTextSize[0] - 10);
        sizeSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                currentTextSize[0] = progress + 10f;
                sizeLabel.setText("Size " + (int) currentTextSize[0]);
                updateTypeface.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        styleRow.addView(sizeSlider, sliderParams);
        
        View colorBtn = colorSwatch(context, currentTextColor[0], null);
        colorBtn.setOnClickListener(v -> {
            showColorPickerDialog(context, editorView, editor, currentTextColor[0], newColor -> {
                currentTextColor[0] = newColor;
                textColor = newColor; // Also sync global text color
                setColorSwatchSelected(colorBtn, newColor, true);
                updateTypeface.run();
            });
        });
        LinearLayout.LayoutParams colorBtnParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        colorBtnParams.setMargins(dp(12), 0, 0, 0);
        colorBtn.setLayoutParams(colorBtnParams);
        styleRow.addView(colorBtn);
        
        scrollContent.addView(styleRow);
        
        scrollView.addView(scrollContent);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // We use weight=1f so that if the keyboard pushes it, it shrinks and becomes scrollable
        scrollParams.weight = 1f;
        layout.addView(scrollView, scrollParams);

        updateTypeface.run();

        // Buttons
        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.RIGHT);

        TextView cancelBtn = new TextView(context);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(Color.parseColor("#dbdee1"));
        cancelBtn.setTextSize(14f);
        cancelBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setPadding(dp(16), dp(11), dp(16), dp(11));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancelBtn);

        TextView addBtn = new TextView(context);
        addBtn.setText("Add");
        addBtn.setTextColor(Color.WHITE);
        addBtn.setTextSize(14f);
        addBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setPadding(dp(18), dp(11), dp(18), dp(11));

        android.graphics.drawable.GradientDrawable addBg = new android.graphics.drawable.GradientDrawable();
        addBg.setColor(0xff5865f2);
        addBg.setCornerRadius(dp(8));
        addBtn.setBackground(addBg);

        addBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                try {
                    editor.setBrushDrawingMode(false);
                    addTextOverlay(editorView, text, currentTextColor[0], finalTypeface[0], currentTextSize[0], isUnderlined[0]);
                } catch (Throwable t) {
                    logger.error("Failed to add text to canvas", t);
                    android.widget.Toast.makeText(context, "Failed to add text: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            }
            dialog.dismiss();
        });

        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        addParams.setMargins(dp(8), 0, 0, 0);
        addBtn.setLayoutParams(addParams);
        buttons.addView(addBtn);

        layout.addView(buttons);

        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null)
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

    private ja.burhanrashid52.photoeditor.TextStyleBuilder textStyle(int color, float sizeSp) {
        ja.burhanrashid52.photoeditor.TextStyleBuilder builder = new ja.burhanrashid52.photoeditor.TextStyleBuilder();
        builder.withTextColor(color);
        builder.withTextSize(sizeSp);
        builder.withGravity(Gravity.CENTER);
        builder.withTextShadow(dp(1), dp(1), dp(2), 0xcc000000);
        return builder;
    }

    private LinearLayout createBottomSheetContainer(Context context) {
        return createBottomSheetContainer(context, "Add Text");
    }

    private LinearLayout createBottomSheetContainer(Context context, String titleText) {
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

    private void showKeyboardDialog(Dialog dialog, View content, EditText input) {
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
            activity = findFragmentActivity(getRealActivity());

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
            activity = findFragmentActivity(getRealActivity());

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

    private FragmentActivity findFragmentActivity(Context context) {
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

    public static class CropOverlayView extends android.view.View {
        private final Paint maskPaint = new Paint();
        private final Paint borderPaint = new Paint();
        private final Paint handlePaint = new Paint();
        private final RectF cropRect = new RectF();
        private final float handleRadius = DimenUtils.dpToPx(8);
        private int activeHandle = -1; // 0: TL, 1: TR, 2: BL, 3: BR, 4: Center/Move
        private float lastX, lastY;

        public CropOverlayView(Context context) {
            super(context);
            maskPaint.setColor(0xaa000000); // 66% opacity dark mask
            maskPaint.setStyle(Paint.Style.FILL);

            borderPaint.setColor(Color.WHITE);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(DimenUtils.dpToPx(2));

            handlePaint.setColor(Color.WHITE);
            handlePaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float marginX = w * 0.1f;
            float marginY = h * 0.1f;
            cropRect.set(marginX, marginY, w - marginX, h - marginY);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();

            // Draw dark mask outside crop area
            canvas.drawRect(0, 0, w, cropRect.top, maskPaint);
            canvas.drawRect(0, cropRect.bottom, w, h, maskPaint);
            canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, maskPaint);
            canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, maskPaint);

            // Draw crop border
            canvas.drawRect(cropRect, borderPaint);

            // Draw handles at 4 corners
            canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint);
            canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint);
            canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint);
            canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint);
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    lastY = y;
                    activeHandle = getTouchedHandle(x, y);
                    return activeHandle != -1;

                case MotionEvent.ACTION_MOVE:
                    if (activeHandle != -1) {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        moveHandle(activeHandle, dx, dy);
                        lastX = x;
                        lastY = y;
                        invalidate();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeHandle = -1;
                    break;
            }
            return super.onTouchEvent(event);
        }

        private int getTouchedHandle(float x, float y) {
            float touchTreshold = handleRadius * 2.5f;
            if (distance(x, y, cropRect.left, cropRect.top) < touchTreshold) return 0; // TL
            if (distance(x, y, cropRect.right, cropRect.top) < touchTreshold) return 1; // TR
            if (distance(x, y, cropRect.left, cropRect.bottom) < touchTreshold) return 2; // BL
            if (distance(x, y, cropRect.right, cropRect.bottom) < touchTreshold) return 3; // BR
            if (cropRect.contains(x, y)) return 4; // Center
            return -1;
        }

        private void moveHandle(int handle, float dx, float dy) {
            float minSize = handleRadius * 4;
            int w = getWidth();
            int h = getHeight();

            if (handle == 4) { // Move whole cropRect
                if (cropRect.left + dx >= 0 && cropRect.right + dx <= w) {
                    cropRect.left += dx;
                    cropRect.right += dx;
                }
                if (cropRect.top + dy >= 0 && cropRect.bottom + dy <= h) {
                    cropRect.top += dy;
                    cropRect.bottom += dy;
                }
                return;
            }

            float newLeft = cropRect.left;
            float newRight = cropRect.right;
            float newTop = cropRect.top;
            float newBottom = cropRect.bottom;

            if (handle == 0 || handle == 2) newLeft = Math.max(0, Math.min(cropRect.right - minSize, cropRect.left + dx));
            if (handle == 1 || handle == 3) newRight = Math.min(w, Math.max(cropRect.left + minSize, cropRect.right + dx));
            if (handle == 0 || handle == 1) newTop = Math.max(0, Math.min(cropRect.bottom - minSize, cropRect.top + dy));
            if (handle == 2 || handle == 3) newBottom = Math.min(h, Math.max(cropRect.top + minSize, cropRect.bottom + dy));

            cropRect.set(newLeft, newTop, newRight, newBottom);
        }

        private float distance(float x1, float y1, float x2, float y2) {
            return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        }

        public RectF getCropRectPercent() {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return new RectF(0.1f, 0.1f, 0.9f, 0.9f);
            return new RectF(
                    cropRect.left / w,
                    cropRect.top / h,
                    cropRect.right / w,
                    cropRect.bottom / h
            );
        }
    }

    private void fitEditorToBitmap(PhotoEditorView editorView, View editorHolder, Bitmap bitmap) {
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

    private void showCropDialog(Context context, android.widget.ImageView targetView, PhotoEditorView editorView) {
        try {
            android.graphics.drawable.BitmapDrawable drawable = (android.graphics.drawable.BitmapDrawable) targetView.getDrawable();
            if (drawable == null) {
                Toast.makeText(context, "No image to crop", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap src = drawable.getBitmap();
            if (src == null) {
                Toast.makeText(context, "No bitmap to crop", Toast.LENGTH_SHORT).show();
                return;
            }

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);

            TextView info = new TextView(context);
            info.setText("Drag corners to resize. Drag center to move crop area:");
            info.setTextColor(Color.parseColor("#dbdee1"));
            info.setTextSize(12f);
            info.setPadding(0, 0, 0, dp(12));
            layout.addView(info);

            float[] rotation = {0f};
            boolean[] flip = {false, false};
            
            // Create downscaled base for smooth preview dragging
            int maxDim = Math.max(src.getWidth(), src.getHeight());
            float downscale = maxDim > 800 ? 800f / maxDim : 1f;
            Bitmap previewBase = downscale == 1f ? src : Bitmap.createScaledBitmap(src, (int)(src.getWidth() * downscale), (int)(src.getHeight() * downscale), true);

            Bitmap[] currentPreview = {previewBase};

            // Container for image and crop overlay
            FrameLayout container = new FrameLayout(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int width = View.MeasureSpec.getSize(widthMeasureSpec);
                    int height = (int) (width * ((float) currentPreview[0].getHeight() / currentPreview[0].getWidth()));
                    int maxHeight = dp(350);
                    if (height > maxHeight) {
                        height = maxHeight;
                        width = (int) (height * ((float) currentPreview[0].getWidth() / currentPreview[0].getHeight()));
                    }
                    int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                    int hSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                    super.onMeasure(wSpec, hSpec);
                }
            };
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            containerParams.setMargins(0, 0, 0, dp(16));
            containerParams.gravity = Gravity.CENTER_HORIZONTAL;
            container.setLayoutParams(containerParams);

            android.widget.ImageView previewImage = new android.widget.ImageView(context);
            previewImage.setImageBitmap(currentPreview[0]);
            previewImage.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
            container.addView(previewImage, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            CropOverlayView cropOverlay = new CropOverlayView(context);
            container.addView(cropOverlay, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            layout.addView(container);

            Runnable updatePreview = () -> {
                android.graphics.Matrix m = new android.graphics.Matrix();
                m.postRotate(rotation[0]);
                if (flip[0]) m.postScale(-1, 1);
                if (flip[1]) m.postScale(1, -1);
                currentPreview[0] = Bitmap.createBitmap(previewBase, 0, 0, previewBase.getWidth(), previewBase.getHeight(), m, true);
                previewImage.setImageBitmap(currentPreview[0]);
                container.requestLayout();
            };

            // Rotation Controls
            LinearLayout rotateControls = new LinearLayout(context);
            rotateControls.setOrientation(LinearLayout.VERTICAL);
            rotateControls.setPadding(0, dp(8), 0, dp(16));

            LinearLayout sliderRow = new LinearLayout(context);
            sliderRow.setOrientation(LinearLayout.HORIZONTAL);
            sliderRow.setGravity(Gravity.CENTER_VERTICAL);
            
            TextView degreeLabel = new TextView(context);
            degreeLabel.setText("0°");
            degreeLabel.setTextColor(Color.WHITE);
            degreeLabel.setMinWidth(dp(40));
            degreeLabel.setGravity(Gravity.CENTER);
            sliderRow.addView(degreeLabel);

            android.widget.SeekBar rotateSlider = new android.widget.SeekBar(context);
            rotateSlider.setMax(360);
            rotateSlider.setProgress(180);
            rotateSlider.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rotateSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        rotation[0] = progress - 180f;
                        degreeLabel.setText((int)rotation[0] + "°");
                        updatePreview.run();
                    }
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });
            sliderRow.addView(rotateSlider);
            rotateControls.addView(sliderRow);

            LinearLayout mirrorRow = new LinearLayout(context);
            mirrorRow.setOrientation(LinearLayout.HORIZONTAL);
            mirrorRow.setGravity(Gravity.CENTER);
            mirrorRow.setPadding(0, dp(8), 0, 0);

            View flipHBtn = iconButton(context, "Flip H", "ic_swap_horiz_24dp", v -> {
                flip[0] = !flip[0];
                updatePreview.run();
            });
            mirrorRow.addView(flipHBtn);

            View flipVBtn = iconButton(context, "Flip V", "ic_swap_vert_24dp", v -> {
                flip[1] = !flip[1];
                updatePreview.run();
            });
            mirrorRow.addView(flipVBtn);

            View rotate90Btn = iconButton(context, "Rotate 90°", "ucrop_ic_rotate", v -> {
                float newRot = rotation[0] + 90f;
                if (newRot > 180f) newRot -= 360f;
                rotation[0] = newRot;
                rotateSlider.setProgress((int)newRot + 180);
                degreeLabel.setText((int)newRot + "°");
                updatePreview.run();
            });
            mirrorRow.addView(rotate90Btn);

            rotateControls.addView(mirrorRow);
            layout.addView(rotateControls);

            Dialog dialog[] = new Dialog[1];

            LinearLayout buttons = new LinearLayout(context);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.RIGHT);

            TextView cancelBtn = new TextView(context);
            cancelBtn.setText("Cancel");
            cancelBtn.setTextColor(Color.parseColor("#dbdee1"));
            cancelBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
            cancelBtn.setOnClickListener(v -> dialog[0].dismiss());
            buttons.addView(cancelBtn);

            TextView applyBtn = new TextView(context);
            applyBtn.setText("Crop");
            applyBtn.setTextColor(Color.WHITE);
            applyBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            applyBtn.setPadding(dp(16), dp(10), dp(16), dp(10));

            android.graphics.drawable.GradientDrawable applyBg = new android.graphics.drawable.GradientDrawable();
            applyBg.setColor(0xff5865f2);
            applyBg.setCornerRadius(dp(6));
            applyBtn.setBackground(applyBg);

            applyBtn.setOnClickListener(v -> {
                try {
                    // 1. Generate full-res rotated bitmap
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.postRotate(rotation[0]);
                    if (flip[0]) m.postScale(-1, 1);
                    if (flip[1]) m.postScale(1, -1);
                    Bitmap finalRotatedSrc = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);

                    RectF percent = cropOverlay.getCropRectPercent();
                    int w = finalRotatedSrc.getWidth();
                    int h = finalRotatedSrc.getHeight();

                    int left = (int) (w * percent.left);
                    int top = (int) (h * percent.top);
                    int right = (int) (w * percent.right);
                    int bottom = (int) (h * percent.bottom);

                    int cropWidth = right - left;
                    int cropHeight = bottom - top;

                    if (cropWidth > 10 && cropHeight > 10) {
                        Bitmap cropped = Bitmap.createBitmap(finalRotatedSrc, left, top, cropWidth, cropHeight);
                        targetView.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                        targetView.setImageBitmap(cropped);
                        
                        if (targetView == editorView.getSource()) {
                            fillEditorBaseLayers(editorView);
                            if (editorView.getParent() instanceof View)
                                fitEditorToBitmap(editorView, (View) editorView.getParent(), cropped);
                        } else {
                            targetView.requestLayout();
                        }
                        Toast.makeText(context, "Cropped successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable e) {
                    logger.error(e);
                    Toast.makeText(context, "Crop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                dialog[0].dismiss();
            });

            LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            applyParams.setMargins(dp(8), 0, 0, 0);
            applyBtn.setLayoutParams(applyParams);
            buttons.addView(applyBtn);

            layout.addView(buttons);

            dialog[0] = customDialog(context, "Crop Image", layout);
            dialog[0].show();
        } catch (Throwable t) {
            logger.error("Failed to show crop dialog", t);
        }
    }

    private EditText dialogInput(Context context, String hint) {
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

    private void applyBrush(PhotoEditor editor) {
        editor.setShape(new ShapeBuilder()
                .withShapeColor(brushColor)
                .withShapeSize(brushSize));
    }

    private void loadImage(Uri uri, PhotoEditorView editorView, View editorHolder, ProgressBar progressBar) {
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

        private void addBitmapOverlay(PhotoEditorView editorView, Bitmap bitmap) {
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

    private void fillEditorBaseLayers(PhotoEditorView editorView) {
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

    private void addTextOverlay(PhotoEditorView editorView, String text, int color, android.graphics.Typeface typeface, float textSize, boolean isUnderlined) {
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

    private void showOverlayOptionsDialog(Context context, View viewToRemove, android.view.ViewGroup parent) {
        android.app.Dialog dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xff313338);
        bg.setCornerRadius(dp(16));
        layout.setBackground(bg);

        android.widget.TextView title = new android.widget.TextView(context);
        title.setText("Overlay Options");
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTextSize(20f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));

        android.widget.TextView message = new android.widget.TextView(context);
        message.setText("What would you like to do with this item?");
        message.setTextColor(android.graphics.Color.parseColor("#dbdee1"));
        message.setTextSize(16f);
        message.setPadding(0, 0, 0, dp(24));

        android.widget.LinearLayout buttons = new android.widget.LinearLayout(context);
        buttons.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        buttons.setGravity(android.view.Gravity.END);
        buttons.setPadding(0, dp(8), 0, 0);

        android.widget.TextView cancel = new android.widget.TextView(context);
        cancel.setText("Cancel");
        cancel.setTextColor(android.graphics.Color.WHITE);
        cancel.setTextSize(14f);
        cancel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cancel.setPadding(dp(20), dp(10), dp(20), dp(10));
        cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel);

        if (viewToRemove instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) viewToRemove;
            if (iv.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                android.widget.TextView crop = new android.widget.TextView(context);
                crop.setText("Crop");
                crop.setTextColor(android.graphics.Color.WHITE);
                crop.setTextSize(14f);
                crop.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                crop.setPadding(dp(24), dp(10), dp(24), dp(10));

                android.graphics.drawable.GradientDrawable cropBg = new android.graphics.drawable.GradientDrawable();
                cropBg.setColor(0xff5865f2);
                cropBg.setCornerRadius(dp(6));
                crop.setBackground(cropBg);

                android.widget.LinearLayout.LayoutParams cropParams = new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cropParams.setMargins(dp(8), 0, dp(8), 0);
                crop.setLayoutParams(cropParams);

                crop.setOnClickListener(v -> {
                    dialog.dismiss();
                    showCropDialog(context, iv, (PhotoEditorView) parent);
                });
                buttons.addView(crop);
            }
        }

        android.widget.TextView delete = new android.widget.TextView(context);
        delete.setText("Delete");
        delete.setTextColor(android.graphics.Color.WHITE);
        delete.setTextSize(14f);
        delete.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        delete.setPadding(dp(24), dp(10), dp(24), dp(10));

        android.graphics.drawable.GradientDrawable deleteBg = new android.graphics.drawable.GradientDrawable();
        deleteBg.setColor(0xffda373c);
        deleteBg.setCornerRadius(dp(6));
        delete.setBackground(deleteBg);

        android.widget.LinearLayout.LayoutParams delParams = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        delParams.setMargins(dp(8), 0, 0, 0);
        delete.setLayoutParams(delParams);

        delete.setOnClickListener(v -> {
            parent.removeView(viewToRemove);
            dialog.dismiss();
        });
        buttons.addView(delete);

        layout.addView(title);
        layout.addView(message);
        layout.addView(buttons);

        dialog.setContentView(layout);
        android.view.Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            win.setLayout((int) (android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels * 0.9f), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void addManualOverlay(PhotoEditorView editorView, View overlay) {
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
            overlay.setOnTouchListener(new OverlayTouchListener(editorView));
            overlay.bringToFront();
            overlay.requestLayout();
            Toast.makeText(editorView.getContext(), "Added to image. Drag to move, pinch to scale.", Toast.LENGTH_SHORT).show();
        };

        if (editorView.getWidth() > 0 && editorView.getHeight() > 0)
            add.run();
        else
            editorView.post(add);
    }

    private void showEditTextOverlayDialog(Context context, TextView target) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = createBottomSheetContainer(context, "Edit Text");
        EditText input = dialogInput(context, "Enter your text...");
        input.setText(target.getText());
        input.setSelectAllOnFocus(false);
        input.setSelection(input.getText().length());

        android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
        inputBg.setColor(0xff1e1f22);
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(1), Color.parseColor("#3f4147"));
        input.setBackground(inputBg);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#80848e"));
        input.setTextSize(16f);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, dp(16));
        layout.addView(input, inputParams);

        final int[] chosenColor = {target.getCurrentTextColor()};
        TextView colorLabel = new TextView(context);
        colorLabel.setText("Color");
        colorLabel.setTextColor(Color.parseColor("#b5bac1"));
        colorLabel.setTextSize(12f);
        colorLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        colorLabel.setPadding(0, 0, 0, dp(8));
        layout.addView(colorLabel);

        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER_VERTICAL);
        List<View> swatches = new ArrayList<>();
        for (int color : COLORS) {
            View swatch = colorSwatch(context, color, view -> {
                chosenColor[0] = color;
                selectColorOnly(swatches, color);
            });
            swatch.setTag(color);
            swatches.add(swatch);
            colorRow.addView(swatch);
        }
        selectColorOnly(swatches, chosenColor[0]);
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        colorParams.setMargins(0, 0, 0, dp(18));
        layout.addView(colorRow, colorParams);

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.RIGHT);

        TextView deleteBtn = dialogButton(context, "Delete", Color.parseColor("#f23f42"), false);
        deleteBtn.setOnClickListener(v -> {
            ViewGroup parent = (ViewGroup) target.getParent();
            if (parent != null)
                parent.removeView(target);
            dialog.dismiss();
        });
        buttons.addView(deleteBtn);

        TextView cancelBtn = dialogButton(context, "Cancel", Color.parseColor("#dbdee1"), false);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancelBtn);

        TextView saveBtn = dialogButton(context, "Save", Color.WHITE, true);
        saveBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                target.setText(text);
                target.setTextColor(chosenColor[0]);
                target.requestLayout();
            }
            dialog.dismiss();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        saveParams.setMargins(dp(8), 0, 0, 0);
        saveBtn.setLayoutParams(saveParams);
        buttons.addView(saveBtn);

        layout.addView(buttons);
        showKeyboardDialog(dialog, layout, input);
    }

    private TextView dialogButton(Context context, String text, int color, boolean primary) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(color);
        button.setTextSize(14f);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        if (primary) {
            bg.setColor(0xff5865f2);
        } else {
            bg.setColor(Color.TRANSPARENT);
        }
        bg.setCornerRadius(dp(8));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(0x40ffffff);
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(rippleColor, bg, null);
            button.setBackground(ripple);
        } else {
            button.setBackground(bg);
        }
        
        addPressAnimation(button);
        return button;
    }

    public static class ImagePickerFragment extends androidx.fragment.app.Fragment {
        private OnImagePickedListener listener;

        public void setListener(OnImagePickedListener listener) {
            this.listener = listener;
        }

        @Override
        public void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 54321);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 54321) {
                if (resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
                    if (listener != null) listener.onImagePicked(data.getData());
                } else {
                    if (listener != null) listener.onImagePicked(null);
                }
                listener = null;
                getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            }
        }

        public interface OnImagePickedListener {
            void onImagePicked(android.net.Uri uri);
        }
    }

    private class OverlayTouchListener implements View.OnTouchListener {
        private final View parent;
        private float downRawX;
        private float downRawY;
        private float startX;
        private float startY;
        private float startDistance;
        private float startScale;
        private long downTime;
        private boolean moved;
        private int mode;
        private Runnable longClickRunnable;

        private OverlayTouchListener(View parent) {
            this.parent = parent;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.bringToFront();
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = view.getX();
                    startY = view.getY();
                    downTime = System.currentTimeMillis();
                    moved = false;
                    mode = 0;

                    if (longClickRunnable != null) view.removeCallbacks(longClickRunnable);
                    longClickRunnable = () -> {
                        if (!moved) {
                            showOverlayOptionsDialog(view.getContext(), view, (android.view.ViewGroup) parent);
                        }
                    };
                    view.postDelayed(longClickRunnable, 500);
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (longClickRunnable != null) view.removeCallbacks(longClickRunnable);
                    if (event.getPointerCount() == 2) {
                        startDistance = pointerDistance(event);
                        startScale = view.getScaleX();
                        mode = 1;
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mode == 1 && event.getPointerCount() >= 2) {
                        float newDist = pointerDistance(event);
                        if (newDist > 10f) {
                            float scale = (newDist / startDistance) * startScale;
                            view.setScaleX(Math.max(0.1f, scale));
                            view.setScaleY(Math.max(0.1f, scale));
                        }
                    } else if (mode == 0) {
                        float deltaX = event.getRawX() - downRawX;
                        float deltaY = event.getRawY() - downRawY;
                        if (Math.abs(deltaX) > dp(4) || Math.abs(deltaY) > dp(4)) {
                            moved = true;
                            if (longClickRunnable != null) view.removeCallbacks(longClickRunnable);
                        }
                        float nextX = startX + deltaX;
                        float nextY = startY + deltaY;
                        float maxX = Math.max(0, parent.getWidth() - view.getWidth());
                        float maxY = Math.max(0, parent.getHeight() - view.getHeight());
                        view.setX(Math.max(0, Math.min(maxX, nextX)));
                        view.setY(Math.max(0, Math.min(maxY, nextY)));
                    }
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerCount() <= 2)
                        mode = 1;
                    return true;
                case MotionEvent.ACTION_UP:
                    if (longClickRunnable != null) view.removeCallbacks(longClickRunnable);
                    if (!moved && System.currentTimeMillis() - downTime < 350 && OVERLAY_TEXT.equals(view.getTag()) && view instanceof android.widget.TextView)
                        showEditTextOverlayDialog(view.getContext(), (android.widget.TextView) view);
                    mode = 0;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    if (longClickRunnable != null) view.removeCallbacks(longClickRunnable);
                    mode = 0;
                    return true;
                default:
                    return true;
            }
        }

        private float pointerDistance(MotionEvent event) {
            if (event.getPointerCount() < 2)
                return 0f;
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    private void clearAllEditorOverlays(PhotoEditor editor, PhotoEditorView editorView) {
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

    private void saveImage(Context context, PhotoEditor editor, PhotoEditorView editorView, Attachment<?>[] currentAttachment, EditRequest editRequest, Dialog dialog, PhotoFilter[] sessionFilter, boolean[] isCustomFilter, float[] customFilterValues) {
        try {
            editor.clearHelperBox();

            // Obtain source bitmap
            Bitmap srcBitmap = null;
            android.graphics.drawable.Drawable srcDrawable = editorView.getSource().getDrawable();
            if (srcDrawable instanceof android.graphics.drawable.BitmapDrawable) {
                srcBitmap = ((android.graphics.drawable.BitmapDrawable) srcDrawable).getBitmap();
            }
            if (srcBitmap == null || srcBitmap.getWidth() <= 0 || srcBitmap.getHeight() <= 0) {
                throw new IllegalStateException("Invalid source bitmap");
            }

            PhotoFilter activeFilter = (sessionFilter != null && sessionFilter[0] != null)
                    ? sessionFilter[0] : PhotoFilter.NONE;
            boolean isGlOnly = isGlOnlyFilter(activeFilter);

            if (isGlOnly && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.view.SurfaceView glView = null;
                for (int i = 0; i < editorView.getChildCount(); i++) {
                    View child = editorView.getChildAt(i);
                    if (child instanceof android.view.SurfaceView) {
                        glView = (android.view.SurfaceView) child;
                        break;
                    }
                }
                if (glView != null && glView.getWidth() > 0 && glView.getHeight() > 0) {
                    Bitmap glBitmap = Bitmap.createBitmap(glView.getWidth(), glView.getHeight(), Bitmap.Config.ARGB_8888);
                    android.view.PixelCopy.request(glView, glBitmap, copyResult -> {
                        if (copyResult == android.view.PixelCopy.SUCCESS) {
                            Canvas canvas = new Canvas(glBitmap);
                            // Draw overlays on top, unscaled (since glView and children share the same layout space)
                            for (int i = 0; i < editorView.getChildCount(); i++) {
                                View child = editorView.getChildAt(i);
                                if (child == editorView.getSource() || child instanceof android.view.SurfaceView) continue;
                                canvas.save();
                                canvas.translate(child.getX(), child.getY());
                                float pivX = child.getPivotX();
                                float pivY = child.getPivotY();
                                canvas.scale(child.getScaleX(), child.getScaleY(), pivX, pivY);
                                if (child.getRotation() != 0f) canvas.rotate(child.getRotation(), pivX, pivY);
                                child.draw(canvas);
                                canvas.restore();
                            }
                            writeBitmapToFile(context, currentAttachment, editRequest, dialog, activeFilter, glBitmap);
                        } else {
                            Toast.makeText(context, "Failed to capture GL filter: " + copyResult, Toast.LENGTH_SHORT).show();
                        }
                    }, new android.os.Handler(android.os.Looper.getMainLooper()));
                    return; // Async save handling
                }
            }

            // Normal save for NONE or ColorMatrix filters (Full-res)
            int outW = srcBitmap.getWidth();
            int outH = srcBitmap.getHeight();
            Bitmap saveBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(saveBitmap);

            Paint srcPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            float[] matrix = isCustomFilter != null && isCustomFilter[0] ? buildCustomMatrix(customFilterValues) : getColorMatrixForFilter(activeFilter);
            
            boolean applyToEverything = settings.getInt("filter_apply_mode", 0) == 1;
            
            if (matrix != null && !applyToEverything) {
                srcPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
            }
            canvas.drawBitmap(srcBitmap, 0, 0, srcPaint);

            float scaleX = (float) outW / Math.max(1, editorView.getWidth());
            float scaleY = (float) outH / Math.max(1, editorView.getHeight());
            canvas.save();
            canvas.scale(scaleX, scaleY);
            for (int i = 0; i < editorView.getChildCount(); i++) {
                View child = editorView.getChildAt(i);
                if (child == editorView.getSource()) continue;
                if (child.getClass().getName().contains("ImageFilterView")) continue;
                canvas.save();
                canvas.translate(child.getX(), child.getY());
                float pivX = child.getPivotX();
                float pivY = child.getPivotY();
                canvas.scale(child.getScaleX(), child.getScaleY(), pivX, pivY);
                if (child.getRotation() != 0f) canvas.rotate(child.getRotation(), pivX, pivY);
                child.draw(canvas);
                canvas.restore();
            }
            canvas.restore();

            if (matrix != null && applyToEverything) {
                Bitmap finalSaveBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
                Canvas finalCanvas = new Canvas(finalSaveBitmap);
                Paint finalPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                finalPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
                finalCanvas.drawBitmap(saveBitmap, 0, 0, finalPaint);
                saveBitmap.recycle();
                saveBitmap = finalSaveBitmap;
            }

            writeBitmapToFile(context, currentAttachment, editRequest, dialog, activeFilter, saveBitmap);
        } catch (Throwable throwable) {
            logger.error(throwable);
            Toast.makeText(context, "Failed to render image: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeBitmapToFile(Context context, Attachment<?>[] currentAttachment, EditRequest editRequest, Dialog dialog, PhotoFilter activeFilter, Bitmap finalBitmap) {
        Utils.threadPool.execute(() -> {
            try {
                Attachment<?> original = currentAttachment[0];
                File output = nextOutputFile(context, original.getDisplayName());
                try (FileOutputStream stream = new FileOutputStream(output)) {
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                }

                Attachment<?> edited = new Attachment<>(
                        output.getAbsolutePath().hashCode(),
                        Uri.fromFile(output),
                        output.getName(),
                        null,
                        original.getSpoiler()
                );

                String filterDesc = (activeFilter != null && activeFilter != PhotoFilter.NONE)
                        ? " (" + humanize(activeFilter.name()) + " filter)" : "";
                Utils.mainThread.post(() -> {
                    editRequest.onEdited(original, edited);
                    Toast.makeText(context, "Saved" + filterDesc, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            } catch (Throwable throwable) {
                logger.error(throwable);
                Utils.mainThread.post(() -> Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Maps a PhotoFilter to a 4×5 ColorMatrix array for software rendering.
     * Returns null for NONE or filters without a meaningful color matrix equivalent.
     */
    private float[] getColorMatrixForFilter(PhotoFilter filter) {
        if (filter == null || filter == PhotoFilter.NONE) return null;
        switch (filter) {
            case GRAY_SCALE:
                return new float[]{
                    0.21f, 0.72f, 0.07f, 0, 0,
                    0.21f, 0.72f, 0.07f, 0, 0,
                    0.21f, 0.72f, 0.07f, 0, 0,
                    0,     0,     0,     1, 0
                };
            case SEPIA:
                return new float[]{
                    0.393f, 0.769f, 0.189f, 0, 0,
                    0.349f, 0.686f, 0.168f, 0, 0,
                    0.272f, 0.534f, 0.131f, 0, 0,
                    0,      0,      0,      1, 0
                };
            case NEGATIVE:
                return new float[]{
                    -1,  0,  0, 0, 255,
                     0, -1,  0, 0, 255,
                     0,  0, -1, 0, 255,
                     0,  0,  0, 1,   0
                };
            case BRIGHTNESS:
                return new float[]{
                    1, 0, 0, 0, 55,
                    0, 1, 0, 0, 55,
                    0, 0, 1, 0, 55,
                    0, 0, 0, 1,  0
                };
            case CONTRAST: {
                float s = 1.5f, t = (-.5f * s + .5f) * 255f;
                return new float[]{
                    s, 0, 0, 0, t,
                    0, s, 0, 0, t,
                    0, 0, s, 0, t,
                    0, 0, 0, 1, 0
                };
            }
            case SATURATE:
                return new float[]{
                     1.8f, -0.4f, -0.4f, 0, 0,
                    -0.4f,  1.8f, -0.4f, 0, 0,
                    -0.4f, -0.4f,  1.8f, 0, 0,
                     0,     0,     0,    1, 0
                };
            case TEMPERATURE:
                // Warm: boost red, reduce blue
                return new float[]{
                    1.15f, 0,    0,     0,  10,
                    0,     1.0f, 0,     0,   0,
                    0,     0,    0.75f, 0, -15,
                    0,     0,    0,     1,   0
                };
            case TINT:
                // Subtle cool tint
                return new float[]{
                    0.9f, 0,    0,    0, 0,
                    0,    1.0f, 0,    0, 5,
                    0,    0,    1.1f, 0, 10,
                    0,    0,    0,    1, 0
                };
            case DUE_TONE:
                // Duotone: map to teal-purple
                return new float[]{
                    0.3f, 0.5f, 0.2f, 0, 20,
                    0.1f, 0.4f, 0.5f, 0, 30,
                    0.4f, 0.2f, 0.4f, 0, 40,
                    0,    0,    0,    1,  0
                };
            case LOMISH:
                // Lo-fi: faded, slightly warm
                return new float[]{
                    0.9f, 0.1f, 0,    0, 20,
                    0,    0.85f,0.1f, 0, 15,
                    0.1f, 0,    0.8f, 0, 10,
                    0,    0,    0,    1,  0
                };
            case POSTERIZE: {
                // Crude posterize via high contrast + slight desaturate
                float ps = 2.5f, pt = (-.5f * ps + .5f) * 255f;
                return new float[]{
                    ps, 0,  0,  0, pt,
                    0,  ps, 0,  0, pt,
                    0,  0,  ps, 0, pt,
                    0,  0,  0,  1,  0
                };
            }
            case GRAIN:
                // Grain is random, can't do with ColorMatrix — return null
                return null;
            case FILL_LIGHT:
                return new float[]{
                    1, 0, 0, 0, 30,
                    0, 1, 0, 0, 30,
                    0, 0, 1, 0, 30,
                    0, 0, 0, 1,  0
                };
            case FISH_EYE:
                // Geometric distortion — not possible with ColorMatrix
                return null;
            case VIGNETTE:
                // Radial effect — not possible with ColorMatrix
                return null;
            case DOCUMENTARY:
                // Desaturated + high contrast
                return new float[]{
                    0.33f, 0.50f, 0.17f, 0,  0,
                    0.33f, 0.50f, 0.17f, 0,  0,
                    0.33f, 0.50f, 0.17f, 0,  0,
                    0,     0,     0,     1, -5
                };
            case SHARPEN:
                return null; // Convolution kernel — not possible with ColorMatrix
            case AUTO_FIX:
                return new float[]{
                    1.1f, 0,    0,    0, 5,
                    0,    1.1f, 0,    0, 5,
                    0,    0,    1.1f, 0, 5,
                    0,    0,    0,    1, 0
                };
            default:
                return null;
        }
    }

    private File nextOutputFile(Context context, String displayName) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PhotoEditor");
        if (!dir.exists() && !dir.mkdirs())
            dir = context.getCacheDir();
        return new File(dir, editedFileName(displayName));
    }

    private String editedFileName(String displayName) {
        String baseName = displayName == null ? "image" : displayName.replaceAll("(?i)\\.[a-z0-9]{1,5}$", "");
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (baseName.isEmpty())
            baseName = "image";
        return baseName + "-edited-" + System.currentTimeMillis() + ".png";
    }

    private void replaceAttachment(SelectionAggregator<?> aggregator, Attachment<?> original, Attachment<?> edited) {
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

    @SuppressLint("DefaultLocale")
    private String humanize(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replace('_', ' ');
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }

    private int dp(int value) {
        return DimenUtils.dpToPx(value);
    }

    private void addRipple(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            view.setBackgroundResource(outValue.resourceId);
        }
    }

    private void addRippleBorderless(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.util.TypedValue outValue = new android.util.TypedValue();
            view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            view.setBackgroundResource(outValue.resourceId);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addPressAnimation(View view) {
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

    private float[] buildCustomMatrix(float[] values) {
        float brightness = values[0];
        float contrast = values[1];
        float saturation = values[2];
        float hue = values[3];
        float temp = values[4];
        float tint = values[5];

        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        
        // Brightness & Contrast
        float t = (1f - contrast) * 128f + brightness;
        android.graphics.ColorMatrix cmContrast = new android.graphics.ColorMatrix(new float[]{
            contrast, 0, 0, 0, t,
            0, contrast, 0, 0, t,
            0, 0, contrast, 0, t,
            0, 0, 0, 1, 0
        });
        cm.postConcat(cmContrast);

        // Saturation
        android.graphics.ColorMatrix cmSat = new android.graphics.ColorMatrix();
        cmSat.setSaturation(saturation);
        cm.postConcat(cmSat);

        // Hue
        if (hue != 0f) {
            float cos = (float) Math.cos(hue * Math.PI / 180f);
            float sin = (float) Math.sin(hue * Math.PI / 180f);
            float lumR = 0.213f, lumG = 0.715f, lumB = 0.072f;
            android.graphics.ColorMatrix cmHue = new android.graphics.ColorMatrix(new float[]{
                lumR + cos * (1 - lumR) + sin * (-lumR), lumG + cos * (-lumG) + sin * (-lumG), lumB + cos * (-lumB) + sin * (1 - lumB), 0, 0,
                lumR + cos * (-lumR) + sin * (0.143f), lumG + cos * (1 - lumG) + sin * (0.140f), lumB + cos * (-lumB) + sin * (-0.283f), 0, 0,
                lumR + cos * (-lumR) + sin * (-(1 - lumR)), lumG + cos * (-lumG) + sin * (lumG), lumB + cos * (1 - lumB) + sin * (lumB), 0, 0,
                0, 0, 0, 1, 0
            });
            cm.postConcat(cmHue);
        }

        // Temperature (Warm/Cool) & Tint (Green/Magenta)
        if (temp != 0f || tint != 0f) {
            float rTemp = temp > 0 ? temp * 0.1f : 0;
            float bTemp = temp < 0 ? -temp * 0.1f : 0;
            float gTint = tint > 0 ? tint * 0.1f : 0;
            float rTint = tint < 0 ? -tint * 0.1f : 0;
            float bTint = tint < 0 ? -tint * 0.1f : 0;
            
            android.graphics.ColorMatrix cmTempTint = new android.graphics.ColorMatrix(new float[]{
                1f + rTemp + rTint, 0, 0, 0, 0,
                0, 1f + gTint, 0, 0, 0,
                0, 0, 1f + bTemp + bTint, 0, 0,
                0, 0, 0, 1, 0
            });
            cm.postConcat(cmTempTint);
        }

        return cm.getArray();
    }

    private void showCustomFilterDialog(Context context, PhotoEditorView editorView, float[] customFilterValues) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = createBottomSheetContainer(context);
        android.widget.ScrollView scrollContent = new android.widget.ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        Runnable updateFilter = () -> {
            for (int i=0; i<editorView.getChildCount(); i++) {
                android.view.View child = editorView.getChildAt(i);
                if (child.getClass().getName().contains("ImageFilterView")) {
                    child.setVisibility(android.view.View.GONE);
                }
            }
            float[] matrix = buildCustomMatrix(customFilterValues);
            boolean applyToEverything = settings.getInt("filter_apply_mode", 0) == 1;
            if (applyToEverything) {
                Paint p = new Paint();
                p.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
                editorView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, p);
                editorView.getSource().clearColorFilter();
            } else {
                editorView.setLayerType(android.view.View.LAYER_TYPE_NONE, null);
                editorView.getSource().setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
            }
        };

        String[] labels = {"Brightness", "Contrast", "Saturation", "Hue", "Temperature", "Tint"};
        float[] mins = {-100f, 0f, 0f, -180f, -50f, -50f};
        float[] maxs = {100f, 2f, 2f, 180f, 50f, 50f};
        float[] defaults = {0f, 1f, 1f, 0f, 0f, 0f};

        for (int i = 0; i < 6; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, dp(16));

            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            
            TextView label = new TextView(context);
            label.setText(labels[i]);
            label.setTextColor(Color.WHITE);
            label.setTextSize(14f);
            
            TextView valueText = new TextView(context);
            valueText.setText(String.format(java.util.Locale.US, "%.2f", customFilterValues[i]));
            valueText.setTextColor(Color.GRAY);
            valueText.setTextSize(12f);
            
            LinearLayout.LayoutParams lblParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            header.addView(label, lblParams);
            header.addView(valueText);
            
            row.addView(header);

            android.widget.SeekBar slider = new android.widget.SeekBar(context);
            slider.setMax(200); // Normalize to 0-200
            
            float current = customFilterValues[i];
            float range = maxs[i] - mins[i];
            int progress = (int) (((current - mins[i]) / range) * 200f);
            slider.setProgress(progress);

            slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int prog, boolean fromUser) {
                    if (fromUser) {
                        float val = mins[index] + ((float) prog / 200f) * range;
                        customFilterValues[index] = val;
                        valueText.setText(String.format(java.util.Locale.US, "%.2f", val));
                        updateFilter.run();
                    }
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });
            row.addView(slider);
            content.addView(row);
        }
        
        TextView resetBtn = new TextView(context);
        resetBtn.setText("Reset Custom Filter");
        resetBtn.setTextColor(0xffda373c);
        resetBtn.setTextSize(14f);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        resetBtn.setOnClickListener(v -> {
            System.arraycopy(defaults, 0, customFilterValues, 0, defaults.length);
            updateFilter.run();
            dialog.dismiss();
            showCustomFilterDialog(context, editorView, customFilterValues); // Reopen to refresh sliders
        });
        content.addView(resetBtn);

        scrollContent.addView(content);
        layout.addView(scrollContent, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(300)
        ));

        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private interface EditRequest {
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










