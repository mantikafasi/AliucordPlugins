package com.aliucord.plugins.photoeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.aliucord.utils.DimenUtils;

public final class CropOverlayView extends android.view.View {
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
