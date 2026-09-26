package com.example.malenasaac;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Audios de pictogramas en almacenamiento interno.
 * Convención: casa.png → casa.mp3 (misma base, sin extensión de imagen).
 */
public final class AudiosPictos {
    public static final String CARPETA = "audios_pictos";
    public static final String CARPETA_PUBLICA = "Malena Comunicador/Audios";

    private AudiosPictos() { }

    public static File carpeta(Context context) {
        File dir = new File(context.getFilesDir(), CARPETA);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Nombre visible / base del archivo de audio: sin extensión de imagen. */
    public static String baseDePicto(String archivoPicto) {
        if (archivoPicto == null) return "";
        int p = archivoPicto.lastIndexOf('.');
        return p > 0 ? archivoPicto.substring(0, p) : archivoPicto;
    }

    public static String nombreMp3(String archivoPicto) {
        return baseDePicto(archivoPicto) + ".mp3";
    }

    public static File archivoPara(Context context, String archivoPicto) {
        return new File(carpeta(context), nombreMp3(archivoPicto));
    }

    public static boolean existe(Context context, String archivoPicto) {
        File f = archivoPara(context, archivoPicto);
        return f.isFile() && f.length() > 0;
    }

    public static File tempGrabacion(Context context) {
        return new File(carpeta(context), "_grabacion_temp.mp3");
    }

    public static void borrarTemp(Context context) {
        File temp = tempGrabacion(context);
        if (temp.exists()) //noinspection ResultOfMethodCallIgnored
            temp.delete();
    }

    /** Reemplaza el audio definitivo con el archivo temporal (si existe). */
    public static boolean confirmarTemp(Context context, String archivoPicto) {
        File temp = tempGrabacion(context);
        if (!temp.isFile() || temp.length() == 0) return false;
        File destino = archivoPara(context, archivoPicto);
        if (destino.exists()) //noinspection ResultOfMethodCallIgnored
            destino.delete();
        if (temp.renameTo(destino)) return destino.isFile();
        try {
            ExportadorPublico.copiar(temp, destino);
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            return destino.isFile() && destino.length() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean esImagenCatalogo(String nombre) {
        if (nombre == null) return false;
        String n = nombre.toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp");
    }

    public static List<File> listar(Context context) {
        File[] files = carpeta(context).listFiles();
        List<File> r = new ArrayList<>();
        if (files == null) return r;
        for (File f : files) {
            if (!f.isFile()) continue;
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (!n.endsWith(".mp3")) continue;
            if (n.startsWith("_grabacion_temp")) continue;
            if (f.length() <= 0) continue;
            r.add(f);
        }
        Collections.sort(r, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return r;
    }

    public static String rutaPublicaVisible() {
        return ExportadorPublico.rutaVisible(CARPETA_PUBLICA);
    }

    /**
     * Copia todos los MP3 internos a Descargas/Malena Comunicador/Audios.
     * Si un archivo ya existe con el mismo nombre, lo reemplaza.
     */
    public static ExportadorPublico.Resultado exportarPublico(Context context) throws IOException {
        return ExportadorPublico.exportar(context, listar(context), CARPETA_PUBLICA);
    }
}
