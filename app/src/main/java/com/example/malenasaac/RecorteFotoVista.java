package com.example.malenasaac;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Recorte libre: mueve el recuadro o estirá las esquinas para sacar el sobrante. */
public final class RecorteFotoVista extends View {
    private static final float MIN_LADO = 48f;
    private Bitmap bitmap;
    private final RectF imagenEnVista = new RectF();
    private final RectF recorte = new RectF();
    private final Paint oscuro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mango = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int modo;
    private float lastX;
    private float lastY;

    public RecorteFotoVista(Context context) {
        super(context);
        oscuro.setColor(0x99000000);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(4f);
        borde.setColor(0xffffffff);
        mango.setColor(0xffffffff);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        requestLayout();
        post(this::acomodarRecorte);
        invalidate();
    }

    public Bitmap recortar() {
        if (bitmap == null || bitmap.isRecycled() || imagenEnVista.width() <= 0) return null;
        float sx = bitmap.getWidth() / imagenEnVista.width();
        float sy = bitmap.getHeight() / imagenEnVista.height();
        int left = clamp((int) Math.floor((recorte.left - imagenEnVista.left) * sx), 0, bitmap.getWidth() - 1);
        int top = clamp((int) Math.floor((recorte.top - imagenEnVista.top) * sy), 0, bitmap.getHeight() - 1);
        int right = clamp((int) Math.ceil((recorte.right - imagenEnVista.left) * sx), left + 1, bitmap.getWidth());
        int bottom = clamp((int) Math.ceil((recorte.bottom - imagenEnVista.top) * sy), top + 1, bitmap.getHeight());
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        acomodarRecorte();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || bitmap.isRecycled() || imagenEnVista.isEmpty()) return;
        canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), imagenEnVista, null);
        canvas.drawRect(0, 0, getWidth(), recorte.top, oscuro);
        canvas.drawRect(0, recorte.top, recorte.left, recorte.bottom, oscuro);
        canvas.drawRect(recorte.right, recorte.top, getWidth(), recorte.bottom, oscuro);
        canvas.drawRect(0, recorte.bottom, getWidth(), getHeight(), oscuro);
        canvas.drawRect(recorte, borde);
        float r = 14f;
        canvas.drawCircle(recorte.left, recorte.top, r, mango);
        canvas.drawCircle(recorte.right, recorte.top, r, mango);
        canvas.drawCircle(recorte.left, recorte.bottom, r, mango);
        canvas.drawCircle(recorte.right, recorte.bottom, r, mango);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                modo = modoEn(x, y);
                lastX = x;
                lastY = y;
                return modo != 0;
            case MotionEvent.ACTION_MOVE:
                if (modo == 0) return false;
                float dx = x - lastX;
                float dy = y - lastY;
                lastX = x;
                lastY = y;
                aplicarGesto(dx, dy);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                modo = 0;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void acomodarRecorte() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float escala = Math.min(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
        float w = bitmap.getWidth() * escala;
        float h = bitmap.getHeight() * escala;
        float left = (getWidth() - w) / 2f;
        float top = (getHeight() - h) / 2f;
        imagenEnVista.set(left, top, left + w, top + h);
        float margen = Math.min(w, h) * 0.08f;
        recorte.set(imagenEnVista.left + margen, imagenEnVista.top + margen,
                imagenEnVista.right - margen, imagenEnVista.bottom - margen);
    }

    private int modoEn(float x, float y) {
        float toque = 36f;
        if (cerca(x, y, recorte.left, recorte.top, toque)) return 2;
        if (cerca(x, y, recorte.right, recorte.top, toque)) return 3;
        if (cerca(x, y, recorte.left, recorte.bottom, toque)) return 4;
        if (cerca(x, y, recorte.right, recorte.bottom, toque)) return 5;
        if (recorte.contains(x, y)) return 1;
        return 0;
    }

    private void aplicarGesto(float dx, float dy) {
        RectF r = new RectF(recorte);
        if (modo == 1) {
            r.offset(dx, dy);
            if (r.left < imagenEnVista.left) r.offset(imagenEnVista.left - r.left, 0);
            if (r.top < imagenEnVista.top) r.offset(0, imagenEnVista.top - r.top);
            if (r.right > imagenEnVista.right) r.offset(imagenEnVista.right - r.right, 0);
            if (r.bottom > imagenEnVista.bottom) r.offset(0, imagenEnVista.bottom - r.bottom);
        } else {
            if (modo == 2 || modo == 4) r.left = clamp(r.left + dx, imagenEnVista.left, r.right - MIN_LADO);
            if (modo == 3 || modo == 5) r.right = clamp(r.right + dx, r.left + MIN_LADO, imagenEnVista.right);
            if (modo == 2 || modo == 3) r.top = clamp(r.top + dy, imagenEnVista.top, r.bottom - MIN_LADO);
            if (modo == 4 || modo == 5) r.bottom = clamp(r.bottom + dy, r.top + MIN_LADO, imagenEnVista.bottom);
        }
        recorte.set(r);
    }

    private static boolean cerca(float x, float y, float cx, float cy, float radio) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radio * radio;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
