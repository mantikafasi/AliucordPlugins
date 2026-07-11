package com.aliucord.plugins.photoeditor;

import android.content.Context;
import android.widget.Toast;

import com.aliucord.Utils;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

final class PhotoEditorImagePicker {
    private PhotoEditorImagePicker() {}

    static void show(PhotoEditorPlugin owner, Context context, PhotoEditor editor, PhotoEditorView editorView) {
        androidx.fragment.app.FragmentActivity activity = owner.findFragmentActivity(context);
        if (activity == null) activity = owner.findFragmentActivity(Utils.getAppActivity());
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
                            com.aliucord.Utils.mainThread.post(() -> owner.addBitmapOverlay(editorView, bmp));
                        }
                    } catch (Throwable t) {
                        owner.logError("Failed to load picked image", t);
                        com.aliucord.Utils.mainThread.post(() -> android.widget.Toast.makeText(context, "Failed to load image", android.widget.Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });

        activity.getSupportFragmentManager().beginTransaction().add(fragment, "IMAGE_PICKER").commitAllowingStateLoss();
    }

}
