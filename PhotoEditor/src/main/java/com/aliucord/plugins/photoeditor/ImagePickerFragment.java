package com.aliucord.plugins.photoeditor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

public final class ImagePickerFragment extends androidx.fragment.app.Fragment {
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
