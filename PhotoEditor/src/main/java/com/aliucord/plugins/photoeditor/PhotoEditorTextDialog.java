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

final class PhotoEditorTextDialog {
    private PhotoEditorTextDialog() {}

    static void show(PhotoEditorPlugin owner, Context context, PhotoEditor editor, PhotoEditorView editorView) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = owner.createBottomSheetContainer(context);

        // State variables
        final int[] currentTextColor = {owner.getTextColor()};
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

        EditText input = owner.dialogInput(context, "Enter your text...");
        android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
        inputBg.setColor(0xff1e1f22);
        inputBg.setCornerRadius(owner.dp(8));
        inputBg.setStroke(owner.dp(1), Color.parseColor("#3f4147"));
        input.setBackground(inputBg);
        input.setTextColor(currentTextColor[0]);
        input.setHintTextColor(Color.parseColor("#80848e"));
        input.setTextSize(currentTextSize[0]);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(owner.dp(12), owner.dp(12), owner.dp(12), owner.dp(12));

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
        inputParams.setMargins(0, 0, 0, owner.dp(16));
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
        fontSearch.setDropDownHeight(owner.dp(150)); // Constrain height so it doesn't hide behind keyboard
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
        searchParams.setMargins(0, 0, 0, owner.dp(12));
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
        boldToggle.setPadding(owner.dp(8), owner.dp(4), owner.dp(16), owner.dp(4));
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
        italicToggle.setPadding(owner.dp(16), owner.dp(4), owner.dp(16), owner.dp(4));
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
        underlineToggle.setPadding(owner.dp(16), owner.dp(4), owner.dp(16), owner.dp(4));
        underlineToggle.setOnClickListener(v -> {
            isUnderlined[0] = !isUnderlined[0];
            underlineToggle.setTextColor(isUnderlined[0] ? Color.WHITE : Color.GRAY);
            updateTypeface.run();
        });
        styleTogglesRow.addView(underlineToggle);

        LinearLayout.LayoutParams togglesRowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        togglesRowParams.setMargins(0, 0, 0, owner.dp(12));
        scrollContent.addView(styleTogglesRow, togglesRowParams);

        // Size & Color Row
        LinearLayout styleRow = new LinearLayout(context);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleRow.setGravity(Gravity.CENTER_VERTICAL);
        styleRow.setPadding(0, 0, 0, owner.dp(16));

        TextView sizeLabel = new TextView(context);
        sizeLabel.setText("Size " + (int) currentTextSize[0]);
        sizeLabel.setTextColor(Color.parseColor("#b5bac1"));
        sizeLabel.setTextSize(14f);
        sizeLabel.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        sizeLabel.setMinimumWidth(owner.dp(65)); // Prevent layout jumping
        styleRow.addView(sizeLabel);

        com.google.android.material.slider.Slider sizeSlider = PhotoEditorUi.createDiscordSlider(context, 0, 110, currentTextSize[0] - 10);
        sizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentTextSize[0] = Math.round(value) + 10f;
            sizeLabel.setText("Size " + (int) currentTextSize[0]);
            updateTypeface.run();
        });
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        styleRow.addView(sizeSlider, sliderParams);

        View colorBtn = owner.colorSwatch(context, currentTextColor[0], null);
        colorBtn.setOnClickListener(v -> {
            PhotoEditorColorPicker.show(context, editorView, editor, currentTextColor[0], PhotoEditorPlugin.COLORS, newColor -> {
                currentTextColor[0] = newColor;
                owner.setTextColor(newColor); // Also sync global text color
                owner.setColorSwatchSelected(colorBtn, newColor, true);
                updateTypeface.run();
            });
        });
        LinearLayout.LayoutParams colorBtnParams = new LinearLayout.LayoutParams(owner.dp(32), owner.dp(32));
        colorBtnParams.setMargins(owner.dp(12), 0, 0, 0);
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
        cancelBtn.setPadding(owner.dp(16), owner.dp(11), owner.dp(16), owner.dp(11));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancelBtn);

        TextView addBtn = new TextView(context);
        addBtn.setText("Add");
        addBtn.setTextColor(Color.WHITE);
        addBtn.setTextSize(14f);
        addBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setPadding(owner.dp(18), owner.dp(11), owner.dp(18), owner.dp(11));

        android.graphics.drawable.GradientDrawable addBg = new android.graphics.drawable.GradientDrawable();
        addBg.setColor(0xff5865f2);
        addBg.setCornerRadius(owner.dp(8));
        addBtn.setBackground(addBg);

        addBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                try {
                    editor.setBrushDrawingMode(false);
                    owner.addTextOverlay(editorView, text, currentTextColor[0], finalTypeface[0], currentTextSize[0], isUnderlined[0]);
                } catch (Throwable t) {
                    owner.logError("Failed to add text to canvas", t);
                    android.widget.Toast.makeText(context, "Failed to add text: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            }
            dialog.dismiss();
        });

        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        addParams.setMargins(owner.dp(8), 0, 0, 0);
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
                attributes.y = owner.dp(72);
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

}
