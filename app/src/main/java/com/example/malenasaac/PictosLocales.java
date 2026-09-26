package com.example.malenasaac;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Pictogramas personalizados en almacenamiento interno de la aplicación. */
public final class PictosLocales {
    public static final String CARPETA = "pictos_locales";
    public static final String CARPETA_PUBLICA = "Malena Comunicador/Pictos";
    public static final int LADO_MAXIMO = 512;

    private PictosLocales() { }

    public static String rutaPublicaVisible() {
        return ExportadorPublico.rutaVisible(CARPETA_PUBLICA);
    }

    /** Copia los pictos propios a Descargas/Malena Comunicador/Pictos, reemplazando los del mismo nombre. */
    public static ExportadorPublico.Resultado exportarPublico(Context context) throws IOException {
        return ExportadorPublico.exportar(context, listar(context), CARPETA_PUBLICA);
    }

    public static File carpeta(Context context) {
        File dir = new File(context.getFilesDir(), CARPETA);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static List<File> listar(Context context) {
        File[] files = carpeta(context).listFiles();
        List<File> r = new ArrayList<>();
        if (files == null) return r;
        for (File f : files) {
            if (f.isFile() && esImagen(f.getName())) r.add(f);
        }
        Collections.sort(r, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return r;
    }

    public static boolean esImagen(String nombre) {
        if (nombre == null) return false;
        String n = nombre.toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp");
    }

    public static String baseSinSufijo(String archivo) {
        if (archivo == null) return "";
        int p = archivo.lastIndexOf('.');
        String base = p > 0 ? archivo.substring(0, p) : archivo;
        base = base.replaceAll("\\s*\\(\\d+\\)", "").trim();
        return sanitizarBase(base);
    }

    public static String sanitizarBase(String texto) {
        if (texto == null) return "";
        String limpio = AlgoritmoPictosFrase.normalizar(texto).toLowerCase(Locale.ROOT).trim();
        limpio = limpio.replaceAll("[^a-z0-9 ]", "");
        return limpio.replaceAll("\\s+", " ").trim();
    }

    public static List<String> nombresArchivo(List<File> archivos) {
        List<String> r = new ArrayList<>();
        for (File f : archivos) r.add(f.getName());
        return r;
    }

    /** El menor nombre libre: casa.png, casa (2).png, casa (3).png... */
    public static String siguienteNombrePng(Collection<String> ocupados, String nombreVisible) {
        String base = sanitizarBase(nombreVisible);
        if (base.isEmpty()) base = "picto";
        String exacto = base + ".png";
        if (!contiene(ocupados, exacto)) return exacto;
        int n = 2;
        while (contiene(ocupados, base + " (" + n + ").png")) n++;
        return base + " (" + n + ").png";
    }

    /**
     * Une assets (prioridad) con archivos locales. Si el nombre ya existe,
     * el local entra al catálogo con el menor sufijo (n) libre.
     */
    public static Map<String, File> unificarConAssets(List<String> nombresAssets, List<File> locales) {
        Set<String> ocupados = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (nombresAssets != null) ocupados.addAll(nombresAssets);
        Map<String, File> mapa = new LinkedHashMap<>();
        for (File f : locales) {
            String nombre = f.getName();
            if (!contiene(ocupados, nombre)) {
                mapa.put(nombre, f);
                ocupados.add(nombre);
            } else {
                String libre = siguienteNombrePng(ocupados, baseSinSufijo(nombre));
                mapa.put(libre, f);
                ocupados.add(libre);
            }
        }
        return mapa;
    }

    public static Bitmap procesarComoPicto(Bitmap origen) {
        if (origen == null) return null;
        int w = origen.getWidth();
        int h = origen.getHeight();
        float scale = Math.min(1f, LADO_MAXIMO / (float) Math.max(w, h));
        int nw = Math.max(1, Math.round(w * scale));
        int nh = Math.max(1, Math.round(h * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(origen, nw, nh, true);
        if (scaled.getConfig() == Bitmap.Config.ARGB_8888) return scaled;
        Bitmap argb = scaled.copy(Bitmap.Config.ARGB_8888, false);
        if (scaled != origen) scaled.recycle();
        return argb;
    }

    public static File guardarPng(Context context, Bitmap bitmap, String nombreArchivo) throws IOException {
        File destino = new File(carpeta(context), nombreArchivo);
        try (FileOutputStream out = new FileOutputStream(destino)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("No se pudo guardar PNG");
            }
        }
        return destino;
    }

    public static File renombrar(File original, String nuevoNombre) {
        if (original == null || nuevoNombre == null || nuevoNombre.isEmpty()) return original;
        File dest = new File(original.getParentFile(), nuevoNombre);
        if (original.getName().equals(nuevoNombre)) return original;
        return original.renameTo(dest) ? dest : original;
    }

    public static Bitmap decodificarUri(Context context, Uri uri, int maxLado) {
        if (uri == null) return null;
        try {
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(stream, null, limites);
            }
            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestraPara(limites.outWidth, limites.outHeight, maxLado);
            opciones.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap;
            try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(stream, null, opciones);
            }
            return rotarSegunExif(context, uri, bitmap);
        } catch (IOException | OutOfMemoryError ignored) {
            return null;
        }
    }

    public static Bitmap decodificarArchivo(File file, int maxLado) {
        if (file == null || !file.isFile()) return null;
        try {
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), limites);
            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestraPara(limites.outWidth, limites.outHeight, maxLado);
            opciones.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opciones);
        } catch (OutOfMemoryError ignored) {
            return null;
        }
    }

    private static Bitmap rotarSegunExif(Context context, Uri uri, Bitmap bitmap) {
        if (bitmap == null) return null;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return bitmap;
            ExifInterface exif = new ExifInterface(stream);
            int orientacion = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int grados = 0;
            if (orientacion == ExifInterface.ORIENTATION_ROTATE_90) grados = 90;
            else if (orientacion == ExifInterface.ORIENTATION_ROTATE_180) grados = 180;
            else if (orientacion == ExifInterface.ORIENTATION_ROTATE_270) grados = 270;
            if (grados == 0) return bitmap;
            Matrix m = new Matrix();
            m.postRotate(grados);
            Bitmap rotado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            if (rotado != bitmap) bitmap.recycle();
            return rotado;
        } catch (IOException ignored) {
            return bitmap;
        }
    }

    private static int muestraPara(int ancho, int alto, int maximo) {
        int muestra = 1;
        int tope = Math.max(maximo, 1);
        while (ancho / muestra > tope || alto / muestra > tope) muestra *= 2;
        return muestra;
    }

    private static boolean contiene(Collection<String> ocupados, String nombre) {
        if (ocupados == null || nombre == null) return false;
        for (String o : ocupados) {
            if (o != null && o.equalsIgnoreCase(nombre)) return true;
        }
        return false;
    }
}
