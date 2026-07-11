package com.aliucord.plugins.photoeditor;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aliucord.Utils;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionAggregator;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;

final class PhotoEditorSession {
    private static final int MAX_ACTIVITY_RESOLVE_ATTEMPTS = 5;
    private static final long ACTIVITY_RETRY_DELAY_MS = 100L;

    private PhotoEditorSession() {}

    static void open(
            PhotoEditorPlugin owner,
            Activity passedActivity,
            Attachment<?> attachment,
            PhotoEditorPlugin.EditRequest editRequest,
            SelectionAggregator<?> aggregator
    ) {
        open(owner, passedActivity, attachment, editRequest, aggregator, 0);
    }

    private static void open(
            PhotoEditorPlugin owner,
            Activity passedActivity,
            Attachment<?> attachment,
            PhotoEditorPlugin.EditRequest editRequest,
            SelectionAggregator<?> aggregator,
            int resolveAttempt
    ) {
        Activity activity = resolveActivity(passedActivity);
        if (activity == null) {
            if (resolveAttempt < MAX_ACTIVITY_RESOLVE_ATTEMPTS) {
                Utils.mainThread.postDelayed(
                        () -> open(owner, passedActivity, attachment, editRequest, aggregator, resolveAttempt + 1),
                        ACTIVITY_RETRY_DELAY_MS
                );
                return;
            }

            Activity appActivity = Utils.getAppActivity();
            if (appActivity != null) {
                Toast.makeText(appActivity, "Could not open image editor (activity null or destroyed)", Toast.LENGTH_SHORT).show();
            }
            owner.logError(
                    "Could not open PhotoEditor because no live activity was available",
                    new IllegalStateException("Activity unavailable after retry")
            );
            return;
        }
        if (attachment.getUri() == null) {
            Toast.makeText(activity, "Attachment has no URI", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        PhotoEditorView editorView = new PhotoEditorView(activity);
        PhotoEditor editor = new PhotoEditor.Builder(activity, editorView)
                .setPinchTextScalable(true)
                .setClipSourceImage(false)
                .build();

        Attachment<?>[] currentAttachment = {attachment};
        PhotoFilter[] sessionFilter = {PhotoFilter.NONE};
        boolean[] isCustomFilter = {false};
        float[] customFilterValues = {0f, 1f, 1f, 0f, 0f, 0f};

        FrameLayout spoilerOverlay = new FrameLayout(activity);
        spoilerOverlay.setBackgroundColor(0x99000000);

        TextView spoilerText = new TextView(activity);
        spoilerText.setText("SPOILER");
        spoilerText.setTextColor(Color.WHITE);
        spoilerText.setTextSize(24f);
        spoilerText.setTypeface(null, android.graphics.Typeface.BOLD);
        spoilerText.setPadding(owner.dp(20), owner.dp(8), owner.dp(20), owner.dp(8));

        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setColor(0x80000000);
        pill.setCornerRadius(owner.dp(20));
        spoilerText.setBackground(pill);

        spoilerOverlay.addView(spoilerText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        spoilerOverlay.setVisibility(attachment.getSpoiler() ? View.VISIBLE : View.GONE);

        FrameLayout root = new FrameLayout(activity) {
            @Override
            public boolean dispatchTouchEvent(android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN &&
                        spoilerOverlay.getVisibility() == View.VISIBLE && spoilerOverlay.getAlpha() == 1f) {
                    spoilerOverlay.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                        spoilerOverlay.setVisibility(View.GONE);
                        spoilerOverlay.setAlpha(1f);
                    }).start();
                }
                return super.dispatchTouchEvent(event);
            }
        };
        root.setBackgroundColor(0xff111214);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View header = PhotoEditorHeader.create(owner, activity, currentAttachment, aggregator, view -> dialog.dismiss(), isSpoiler -> {
            if (isSpoiler) {
                spoilerOverlay.setVisibility(View.VISIBLE);
                spoilerOverlay.setAlpha(0f);
                spoilerOverlay.animate().alpha(1f).setDuration(150).setListener(null).start();
            } else if (spoilerOverlay.getVisibility() == View.VISIBLE) {
                spoilerOverlay.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    spoilerOverlay.setVisibility(View.GONE);
                    spoilerOverlay.setAlpha(1f);
                }).start();
            }
        });

        android.widget.ImageView saveButton = new android.widget.ImageView(activity);
        int saveId = Utils.getResId("ic_check_white_24dp", "drawable");
        saveButton.setImageResource(saveId != 0 ? saveId : android.R.drawable.ic_menu_save);
        saveButton.setColorFilter(Color.WHITE);
        saveButton.setPadding(owner.dp(8), owner.dp(8), owner.dp(8), owner.dp(8));
        owner.addRippleBorderless(saveButton);
        owner.addPressAnimation(saveButton);
        saveButton.setOnClickListener(view -> PhotoEditorSaver.save(
                owner,
                activity,
                editor,
                editorView,
                currentAttachment,
                editRequest,
                dialog,
                sessionFilter,
                isCustomFilter,
                customFilterValues
        ));

        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(owner.dp(40), owner.dp(40));
        saveParams.setMargins(owner.dp(8), 0, 0, 0);
        ((LinearLayout) header).addView(saveButton, saveParams);

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

        content.addView(owner.createToolbar(
                activity,
                editor,
                editorView,
                dialog,
                currentAttachment,
                editRequest,
                sessionFilter,
                isCustomFilter,
                customFilterValues
        ), new LinearLayout.LayoutParams(
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
            if (shownWindow != null) {
                shownWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            owner.loadImage(currentAttachment[0].getUri(), editorView, editorHolder, progressBar);
            owner.applyBrush(editor);
        });
        dialog.setOnDismissListener(ignored -> owner.setEditorStickerPickerOpen(false));

        try {
            owner.setEditorStickerPickerOpen(true);
            dialog.show();
        } catch (android.view.WindowManager.BadTokenException exception) {
            owner.setEditorStickerPickerOpen(false);
            owner.logError("Failed to show PhotoEditor dialog because the activity window token is invalid", exception);
            Toast.makeText(activity, "Could not open image editor", Toast.LENGTH_SHORT).show();
        }
    }

    private static Activity resolveActivity(Activity passedActivity) {
        if (isUsable(passedActivity)) {
            return passedActivity;
        }
        Activity currentActivity = Utils.getAppActivity();
        return isUsable(currentActivity) ? currentActivity : null;
    }

    private static boolean isUsable(Activity activity) {
        return activity != null &&
                !activity.isFinishing() &&
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed());
    }
}
