package com.example.malenasaac;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PDF siempre A4 vertical.
 * Picto real = 1/10 del ancho A4 (se reduce solo si algún marco no entra);
 * automáticos misma altura y ancho variable; renglón con wrap; alineación a la izquierda;
 * frases repartidas en vertical con separación mínima entre marcos.
 */
public final class FrasePdfExporter {
    private static final String DIR_PICTOS = "pictos";
    /** A4 a 72 dpi (puntos PDF). */
    private static final int A4_ANCHO = 595;
    private static final int A4_ALTO = 842;
    private static final float MARGEN = 24f;
    private static final float GAP = 3.5f;
    private static final float NUM_ANCHO = 28f;
    /** Referencia: 1/10 del ancho A4. */
    private static final float LADO_BASE = A4_ANCHO / 10f;
    /** Separación mínima vertical entre bordes de marcos consecutivos. */
    private static final float SEPARACION_MIN = 14f;
    private static final float PAD_MARCO = 8f;
    private static final int TAM_DECODE = 256;

    private FrasePdfExporter() { }

    /** Guarda el PDF en Descargas/Malena Comunicador y devuelve su Uri. */
    public static Uri exportar(Context context, List<PhraseRecord> frases) throws IOException {
        if (frases == null || frases.isEmpty()) throw new IOException("No hay frases seleccionadas");

        PdfDocument documento = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(A4_ANCHO, A4_ALTO, 1).create();
        PdfDocument.Page pagina = documento.startPage(info);
        Canvas canvas = pagina.getCanvas();

        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(0xffffffff);
        canvas.drawRect(0, 0, A4_ANCHO, A4_ALTO, fondo);

        int n = frases.size();
        float xPictos = MARGEN + NUM_ANCHO;
        float anchoUtil = A4_ANCHO - xPictos - MARGEN;
        float huecos = Math.max(0, n - 1) * SEPARACION_MIN;
        float banda = (A4_ALTO - MARGEN * 2f - huecos) / n;

        float lado = LADO_BASE;
        List<BloqueFrase> bloques = medirTodas(frases, anchoUtil, lado);
        // Solo reduce si hace falta; itera porque con varios renglones el padding/gaps
        // no escalan igual que el picto y una sola pasada a veces no alcanza.
        for (int intento = 0; intento < 8; intento++) {
            float maxMarco = 0f;
            for (BloqueFrase b : bloques) {
                maxMarco = Math.max(maxMarco, Math.max(b.alto, 24f) + PAD_MARCO * 2f);
            }
            if (maxMarco <= banda + 0.5f) break;
            float factor = (banda / maxMarco) * 0.97f;
            if (factor >= 0.999f) break;
            lado = Math.max(18f, lado * factor);
            bloques = medirTodas(frases, anchoUtil, lado);
        }

        Paint bordeMarco = new Paint(Paint.ANTI_ALIAS_FLAG);
        bordeMarco.setStyle(Paint.Style.STROKE);
        bordeMarco.setStrokeWidth(1.6f);
        bordeMarco.setColor(0xff455a64);

        for (int i = 0; i < n; i++) {
            float bandaTop = MARGEN + i * (banda + SEPARACION_MIN);
            BloqueFrase bloque = bloques.get(i);
            float contenidoAlto = Math.max(bloque.alto, 24f);
            float marcoAlto = Math.min(contenidoAlto + PAD_MARCO * 2f, banda);
            float marcoTop = bandaTop + (banda - marcoAlto) / 2f;
            RectF marco = new RectF(MARGEN, marcoTop, A4_ANCHO - MARGEN, marcoTop + marcoAlto);
            canvas.drawRoundRect(marco, 12f, 12f, bordeMarco);

            float altoUtilMarco = Math.max(0f, marcoAlto - PAD_MARCO * 2f);
            float yBloque = marcoTop + PAD_MARCO + Math.max(0f, (altoUtilMarco - bloque.alto) / 2f);
            float cyNumero = marcoTop + marcoAlto / 2f;
            dibujarNumero(canvas, i + 1, cyNumero, lado);
            canvas.save();
            canvas.clipRect(marco.left + 2f, marco.top + 2f, marco.right - 2f, marco.bottom - 2f);
            dibujarBloque(context, canvas, bloque, xPictos, yBloque, lado);
            canvas.restore();
        }

        documento.finishPage(pagina);

        String nombre = "frases_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".pdf";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return guardarEnDescargas(context, documento, nombre);
            return guardarEnDescargasAntiguas(documento, nombre);
        } finally {
            documento.close();
        }
    }

    private static List<BloqueFrase> medirTodas(List<PhraseRecord> frases, float anchoUtil, float lado) {
        List<BloqueFrase> bloques = new ArrayList<>();
        for (PhraseRecord frase : frases) bloques.add(medirFrase(frase, anchoUtil, lado));
        return bloques;
    }

    private static void dibujarNumero(Canvas canvas, int numero, float cy, float lado) {
        Paint numFondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        numFondo.setColor(0xff496f88);
        TextPaint numTexto = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        numTexto.setColor(0xffffffff);
        float radio = Math.max(9f, Math.min(12f, lado * 0.22f));
        numTexto.setTextSize(radio * 1.25f);
        numTexto.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        numTexto.setTextAlign(Paint.Align.CENTER);
        canvas.drawCircle(MARGEN + radio, cy, radio, numFondo);
        Paint.FontMetrics fm = numTexto.getFontMetrics();
        canvas.drawText(String.valueOf(numero), MARGEN + radio, cy - (fm.ascent + fm.descent) / 2f, numTexto);
    }

    private static BloqueFrase medirFrase(PhraseRecord frase, float anchoUtil, float lado) {
        BloqueFrase bloque = new BloqueFrase();
        List<PhraseRecord.Item> items = frase.items;
        if (items == null || items.isEmpty()) return bloque;

        Linea linea = new Linea();
        for (PhraseRecord.Item item : items) {
            Casilla casilla = medirCasilla(item, lado);
            float extra = linea.casillas.isEmpty() ? 0f : GAP;
            if (!linea.casillas.isEmpty() && linea.ancho + extra + casilla.ancho > anchoUtil) {
                bloque.lineas.add(linea);
                bloque.alto += (bloque.lineas.size() > 1 ? GAP : 0f) + linea.alto;
                linea = new Linea();
                extra = 0f;
            }
            linea.casillas.add(casilla);
            linea.ancho += extra + casilla.ancho;
            linea.alto = Math.max(linea.alto, casilla.alto);
        }
        if (!linea.casillas.isEmpty()) {
            bloque.lineas.add(linea);
            bloque.alto += (bloque.lineas.size() > 1 ? GAP : 0f) + linea.alto;
        }
        return bloque;
    }

    private static Casilla medirCasilla(PhraseRecord.Item item, float lado) {
        Casilla c = new Casilla();
        c.item = item;
        c.alto = lado;
        if (item.automatico) {
            c.ancho = anchoAutomatico(item.etiqueta, lado);
        } else {
            c.ancho = lado;
        }
        return c;
    }

    /** Misma altura de referencia que el real; el ancho crece con el texto. */
    private static float anchoAutomatico(String etiqueta, float lado) {
        String texto = etiqueta == null ? "" : etiqueta.trim();
        if (texto.isEmpty()) return lado * 0.7f;
        TextPaint medida = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        medida.setTextSize(lado * 0.22f);
        float textoAncho = medida.measureText(texto) + lado * 0.2f;
        float min = lado * 0.55f;
        float max = A4_ANCHO - MARGEN * 2 - NUM_ANCHO;
        return Math.max(min, Math.min(max, textoAncho));
    }

    private static void dibujarBloque(Context context, Canvas canvas, BloqueFrase bloque,
                                      float x0, float y0, float lado) {
        float y = y0;
        for (int li = 0; li < bloque.lineas.size(); li++) {
            Linea linea = bloque.lineas.get(li);
            if (li > 0) y += GAP;
            float x = x0;
            for (Casilla casilla : linea.casillas) {
                float top = y + (linea.alto - casilla.alto) / 2f;
                dibujarItem(context, canvas, casilla.item, x, top, casilla.ancho, casilla.alto, lado);
                x += casilla.ancho + GAP;
            }
            y += linea.alto;
        }
    }

    private static void dibujarItem(Context context, Canvas canvas, PhraseRecord.Item item,
                                    float x, float y, float ancho, float alto, float lado) {
        if (item.automatico) {
            dibujarAutomatico(canvas, item.etiqueta, x, y, ancho, alto, lado);
            return;
        }
        Bitmap bitmap = decodificar(context, item.archivo);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        RectF dest = new RectF(x, y, x + ancho, y + alto);
        Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(1.2f);
        borde.setColor(0xff263238);
        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(0xffffffff);
        canvas.drawRoundRect(dest, 5f, 5f, fondo);
        if (bitmap != null) {
            RectF img = new RectF(x + 2.5f, y + 2.5f, x + ancho - 2.5f, y + alto - 2.5f);
            canvas.drawBitmap(bitmap, null, img, paint);
            if (!bitmap.isRecycled()) bitmap.recycle();
        }
        canvas.drawRoundRect(dest, 5f, 5f, borde);
        if (item.negado) {
            Paint raya = new Paint(Paint.ANTI_ALIAS_FLAG);
            raya.setColor(0xccb00020);
            raya.setStrokeWidth(Math.max(2.5f, lado * 0.06f));
            canvas.drawLine(x + 5f, y + alto / 2f, x + ancho - 5f, y + alto / 2f, raya);
        }
    }

    private static void dibujarAutomatico(Canvas canvas, String etiqueta, float x, float y,
                                          float ancho, float alto, float lado) {
        String texto = etiqueta == null ? "" : etiqueta.trim();
        float padV = alto * 0.18f;
        RectF dest = new RectF(x, y + padV, x + ancho, y + alto - padV);
        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(0xffeceff1);
        Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(1f);
        borde.setColor(0x88263238);
        canvas.drawRoundRect(dest, 7f, 7f, fondo);
        canvas.drawRoundRect(dest, 7f, 7f, borde);

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xff546e7a);
        tp.setTextAlign(Paint.Align.CENTER);
        float tam = Math.min(lado * 0.22f, dest.height() * 0.45f);
        tp.setTextSize(tam);
        while (tam > 5f && tp.measureText(texto) > dest.width() - 6f) {
            tam -= 0.4f;
            tp.setTextSize(tam);
        }
        Paint.FontMetrics fm = tp.getFontMetrics();
        float cy = dest.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(texto, dest.centerX(), cy, tp);
    }

    private static Bitmap decodificar(Context context, String archivo) {
        if (archivo == null || archivo.isEmpty()) return null;
        String ruta = DIR_PICTOS + "/" + archivo;
        try {
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            try (InputStream stream = context.getAssets().open(ruta)) {
                BitmapFactory.decodeStream(stream, null, limites);
            }
            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestraPara(limites.outWidth, limites.outHeight, TAM_DECODE);
            opciones.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream stream = context.getAssets().open(ruta)) {
                return BitmapFactory.decodeStream(stream, null, opciones);
            }
        } catch (IOException | OutOfMemoryError ignored) {
            return null;
        }
    }

    private static int muestraPara(int ancho, int alto, int maximo) {
        int muestra = 1;
        while (ancho / muestra > maximo || alto / muestra > maximo) muestra *= 2;
        return muestra;
    }

    private static Uri guardarEnDescargas(Context context, PdfDocument documento, String nombre) throws IOException {
        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Downloads.DISPLAY_NAME, nombre);
        valores.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        valores.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Malena Comunicador");
        valores.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
        if (uri == null) throw new IOException("No se pudo crear el PDF");
        try {
            escribir(context.getContentResolver(), uri, documento);
            valores.clear();
            valores.put(MediaStore.Downloads.IS_PENDING, 0);
            context.getContentResolver().update(uri, valores, null, null);
            return uri;
        } catch (IOException error) {
            context.getContentResolver().delete(uri, null, null);
            throw error;
        }
    }

    @SuppressWarnings("deprecation")
    private static Uri guardarEnDescargasAntiguas(PdfDocument documento, String nombre) throws IOException {
        File carpeta = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Malena Comunicador");
        if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("No se pudo crear la carpeta");
        File archivo = new File(carpeta, nombre);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            documento.writeTo(salida);
        }
        return Uri.fromFile(archivo);
    }

    private static void escribir(ContentResolver resolver, Uri uri, PdfDocument documento) throws IOException {
        try (OutputStream salida = resolver.openOutputStream(uri)) {
            if (salida == null) throw new IOException("No se pudo abrir el PDF");
            documento.writeTo(salida);
        }
    }

    private static final class Casilla {
        PhraseRecord.Item item;
        float ancho;
        float alto;
    }

    private static final class Linea {
        final List<Casilla> casillas = new ArrayList<>();
        float ancho;
        float alto;
    }

    private static final class BloqueFrase {
        final List<Linea> lineas = new ArrayList<>();
        float alto;
    }
}
