package com.example.malenasaac;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/** Copia archivos internos a Descargas/&lt;subcarpeta&gt;, reemplazando los que tengan el mismo nombre. */
public final class ExportadorPublico {

    private ExportadorPublico() { }

    public static String rutaVisible(String subcarpeta) {
        return Environment.DIRECTORY_DOWNLOADS + "/" + subcarpeta;
    }

    public static Resultado exportar(Context context, List<File> archivos, String subcarpeta) throws IOException {
        Resultado resultado = new Resultado();
        resultado.rutaVisible = rutaVisible(subcarpeta);
        if (archivos.isEmpty()) return resultado;

        for (File archivo : archivos) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportarMediaStore(context, archivo, subcarpeta);
                } else {
                    exportarLegacy(archivo, subcarpeta);
                }
                resultado.exportados++;
            } catch (IOException e) {
                resultado.fallidos++;
            }
        }
        if (resultado.exportados == 0 && resultado.fallidos > 0) {
            throw new IOException("No se pudieron exportar los archivos");
        }
        return resultado;
    }

    public static String mimeDe(String nombre) {
        String n = nombre.toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static void exportarMediaStore(Context context, File origen, String subcarpeta) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String nombre = origen.getName();
        String relative = rutaVisible(subcarpeta);
        borrarSiExisteMediaStore(resolver, nombre, relative);

        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Downloads.DISPLAY_NAME, nombre);
        valores.put(MediaStore.Downloads.MIME_TYPE, mimeDe(nombre));
        valores.put(MediaStore.Downloads.RELATIVE_PATH, relative);
        valores.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
        if (uri == null) throw new IOException("No se pudo crear " + nombre);
        try {
            try (InputStream in = new FileInputStream(origen);
                 OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new IOException("No se pudo abrir destino");
                copiarStreams(in, out);
            }
            valores.clear();
            valores.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, valores, null, null);
        } catch (IOException error) {
            resolver.delete(uri, null, null);
            throw error;
        }
    }

    private static void borrarSiExisteMediaStore(ContentResolver resolver, String nombre, String relative) {
        String[] proyeccion = { MediaStore.Downloads._ID };
        String seleccion = MediaStore.Downloads.DISPLAY_NAME + "=? AND "
                + MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
        String[] args = { nombre, relative + "%" };
        try (Cursor c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, proyeccion, seleccion, args, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                long id = c.getLong(0);
                Uri uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));
                resolver.delete(uri, null, null);
            }
        } catch (RuntimeException ignored) { }
    }

    @SuppressWarnings("deprecation")
    private static void exportarLegacy(File origen, String subcarpeta) throws IOException {
        File carpeta = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subcarpeta);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta pública");
        }
        copiar(origen, new File(carpeta, origen.getName()));
    }

    public static final class Resultado {
        public int exportados;
        public int fallidos;
        public String rutaVisible;
    }

    static void copiar(File origen, File destino) throws IOException {
        try (FileInputStream in = new FileInputStream(origen);
             FileOutputStream out = new FileOutputStream(destino)) {
            copiarStreams(in, out);
        }
    }

    private static void copiarStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        out.flush();
    }
}
