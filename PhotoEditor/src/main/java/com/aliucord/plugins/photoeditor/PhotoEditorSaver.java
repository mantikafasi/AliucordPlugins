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

final class PhotoEditorSaver {
    private PhotoEditorSaver() {}

    static void save(PhotoEditorPlugin owner, Context context, PhotoEditor editor, PhotoEditorView editorView, Attachment<?>[] currentAttachment, PhotoEditorPlugin.EditRequest editRequest, Dialog dialog, PhotoFilter[] sessionFilter, boolean[] isCustomFilter, float[] customFilterValues) {
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
            boolean isGlOnly = owner.isGlOnlyFilter(activeFilter);

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
                            writeBitmapToFile(owner, context, currentAttachment, editRequest, dialog, activeFilter, glBitmap);
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
            float[] matrix = isCustomFilter != null && isCustomFilter[0] ? PhotoEditorUtils.buildCustomMatrix(customFilterValues) : PhotoEditorUtils.getColorMatrixForFilter(activeFilter);

            boolean applyToEverything = owner.getSettings().getInt("filter_apply_mode", 0) == 1;

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

            writeBitmapToFile(owner, context, currentAttachment, editRequest, dialog, activeFilter, saveBitmap);
        } catch (Throwable throwable) {
            owner.logError(throwable);
            Toast.makeText(context, "Failed to render image: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void writeBitmapToFile(PhotoEditorPlugin owner, Context context, Attachment<?>[] currentAttachment, PhotoEditorPlugin.EditRequest editRequest, Dialog dialog, PhotoFilter activeFilter, Bitmap finalBitmap) {
        Utils.threadPool.execute(() -> {
            try {
                Attachment<?> original = currentAttachment[0];
                File output = PhotoEditorUtils.nextOutputFile(context, original.getDisplayName());
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
                        ? " (" + PhotoEditorUtils.humanize(activeFilter.name()) + " filter)" : "";
                Utils.mainThread.post(() -> {
                    editRequest.onEdited(original, edited);
                    Toast.makeText(context, "Saved" + filterDesc, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            } catch (Throwable throwable) {
                owner.logError(throwable);
                Utils.mainThread.post(() -> Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Maps a PhotoFilter to a 4×5 ColorMatrix array for software rendering.
     * Returns null for NONE or filters without a meaningful color matrix equivalent.
     */
}
