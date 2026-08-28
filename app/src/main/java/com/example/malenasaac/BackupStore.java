package com.example.malenasaac;

import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;

/** Respaldo fuera de los datos privados de la aplicación. */
public final class BackupStore {
    private static final String PREFS = "comunicador_visual";
    private static final String KEY_URI = "uri_respaldo";
    private static final String NOMBRE = "frases_malena.json";
    private BackupStore() { }

    public static void usarDestino(Context context, Uri uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URI, uri.toString()).apply();
    }

    public static void guardar(Context context, String json) throws IOException {
        Uri existente = uriGuardada(context);
        if (existente != null) {
            try { escribir(context.getContentResolver(), existente, json); return; }
            catch (IOException ignored) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_URI).apply(); }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) guardarEnDescargas(context, json);
        else guardarEnDescargasAntiguas(json);
    }

    private static Uri uriGuardada(Context context) {
        String texto = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URI, null);
        return texto == null ? null : Uri.parse(texto);
    }

    private static void guardarEnDescargas(Context context, String json) throws IOException {
        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Downloads.DISPLAY_NAME, NOMBRE);
        valores.put(MediaStore.Downloads.MIME_TYPE, "application/json");
        valores.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Malena Comunicador");
        valores.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
        if (uri == null) throw new IOException("No se pudo crear el respaldo");
        try {
            escribir(context.getContentResolver(), uri, json);
            valores.clear(); valores.put(MediaStore.Downloads.IS_PENDING, 0);
            context.getContentResolver().update(uri, valores, null, null);
            usarDestino(context, uri);
        } catch (IOException error) { context.getContentResolver().delete(uri, null, null); throw error; }
    }

    @SuppressWarnings("deprecation") private static void guardarEnDescargasAntiguas(String json) throws IOException {
        File carpeta = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Malena Comunicador");
        if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("No se pudo crear la carpeta de respaldo");
        try (FileOutputStream salida = new FileOutputStream(new File(carpeta, NOMBRE))) { salida.write(json.getBytes("UTF-8")); }
    }

    private static void escribir(ContentResolver resolver, Uri uri, String json) throws IOException {
        try (OutputStream salida = resolver.openOutputStream(uri, "wt")) {
            if (salida == null) throw new IOException("No se pudo abrir el respaldo");
            salida.write(json.getBytes("UTF-8"));
        }
    }
}
