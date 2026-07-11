package com.aliucord.plugins.photoeditor;

import android.view.MotionEvent;
import android.view.View;

    final class PhotoEditorOverlayTouchListener implements View.OnTouchListener {
        private final PhotoEditorPlugin owner;
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

        PhotoEditorOverlayTouchListener(PhotoEditorPlugin owner, View parent) {
            this.owner = owner;
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
                            PhotoEditorOverlayDialogs.showOptions(owner,view.getContext(), view, (android.view.ViewGroup) parent);
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
                        if (Math.abs(deltaX) > owner.dp(4) || Math.abs(deltaY) > owner.dp(4)) {
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
                    if (!moved && System.currentTimeMillis() - downTime < 350 && PhotoEditorPlugin.OVERLAY_TEXT.equals(view.getTag()) && view instanceof android.widget.TextView)
                        PhotoEditorOverlayDialogs.showTextEditor(owner,view.getContext(), (android.widget.TextView) view);
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
