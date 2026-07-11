package com.aliucord.plugins.photoeditor;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliucord.Utils;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionAggregator;

final class PhotoEditorHeader {
    private PhotoEditorHeader() {}

    static View create(PhotoEditorPlugin owner, Context context, Attachment<?>[] currentAttachment, SelectionAggregator<?> aggregator, View.OnClickListener onClose, PhotoEditorPlugin.SpoilerToggleListener spoilerListener) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(0xff1e1f22);
        header.setPadding(owner.dp(16), owner.dp(10), owner.dp(16), owner.dp(10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            header.setElevation(owner.dp(4));
        }

        android.widget.ImageView closeBtn = new android.widget.ImageView(context);
        int closeId = Utils.getResId("ic_close_24dp", "drawable");
        closeBtn.setImageResource(closeId != 0 ? closeId : android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setColorFilter(Color.WHITE);
        closeBtn.setPadding(owner.dp(8), owner.dp(8), owner.dp(8), owner.dp(8));
        closeBtn.setOnClickListener(onClose);
        owner.addRippleBorderless(closeBtn);
        owner.addPressAnimation(closeBtn);

        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(owner.dp(40), owner.dp(40));
        closeParams.setMargins(0, 0, owner.dp(8), 0);
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
            spoilerBtn.setPadding(owner.dp(8), owner.dp(8), owner.dp(8), owner.dp(8));
            owner.addRippleBorderless(spoilerBtn);
            owner.addPressAnimation(spoilerBtn);

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
                    owner.replaceAttachment(aggregator, currentAttachment[0], edited);
                    currentAttachment[0] = edited;
                    if (spoilerListener != null) {
                        spoilerListener.onSpoilerToggled(isSpoiler[0]);
                    }
                } catch (Throwable t) {
                    owner.logError("Failed to toggle spoiler", t);
                }
            });

            LinearLayout.LayoutParams spoilerParams = new LinearLayout.LayoutParams(owner.dp(40), owner.dp(40));
            spoilerParams.setMargins(0, 0, owner.dp(8), 0);
            header.addView(spoilerBtn, spoilerParams);

            // Delete button
            android.widget.ImageView deleteBtn = new android.widget.ImageView(context);
            int deleteId = Utils.getResId("ic_delete_24dp", "drawable");
            deleteBtn.setImageResource(deleteId != 0 ? deleteId : android.R.drawable.ic_menu_delete);
            deleteBtn.setColorFilter(0xffda373c);
            deleteBtn.setPadding(owner.dp(8), owner.dp(8), owner.dp(8), owner.dp(8));
            owner.addRippleBorderless(deleteBtn);
            owner.addPressAnimation(deleteBtn);

            deleteBtn.setOnClickListener(v -> {
                try {
                    java.lang.reflect.Method remove = SelectionAggregator.class.getDeclaredMethod("removeItem", Attachment.class);
                    remove.setAccessible(true);
                    remove.invoke(aggregator, currentAttachment[0]);
                    onClose.onClick(v);
                } catch (Throwable t) {
                    owner.logError("Failed to delete attachment", t);
                }
            });

            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(owner.dp(40), owner.dp(40));
            header.addView(deleteBtn, deleteParams);
        }

        return header;
    }

}
