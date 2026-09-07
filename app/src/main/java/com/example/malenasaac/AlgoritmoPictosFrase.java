package com.example.malenasaac;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cruza nombres de pictogramas con texto reconocido por voz. */
public final class AlgoritmoPictosFrase {
    private static final float SIMILITUD_MINIMA = 0.80f;

    private AlgoritmoPictosFrase() { }

    public interface ProveedorNombre {
        String nombre(String archivo);
    }

    /**
     * Devuelve pictogramas que coinciden con el texto, en el orden en que aparecen
     * al recorrer las palabras de izquierda a derecha.
     */
    public static List<CoincidenciaPicto> coincidencias(String textoReconocido, List<String> pictos,
            ProveedorNombre nombres) {
        return recorrer(textoReconocido, pictos, nombres, false);
    }

    /**
     * Igual que {@link #coincidencias}, pero intercala pictos automáticos (texto)
     * para conectores y palabras sin match, pensado para el tren de reproducción.
     */
    public static List<CoincidenciaPicto> secuenciaConAutomaticos(String textoReconocido, List<String> pictos,
            ProveedorNombre nombres) {
        return recorrer(textoReconocido, pictos, nombres, true);
    }

    private static List<CoincidenciaPicto> recorrer(String textoReconocido, List<String> pictos,
            ProveedorNombre nombres, boolean incluirAutomaticos) {
        List<CoincidenciaPicto> resultados = new ArrayList<>();
        String[] palabras = palabras(normalizar(textoReconocido));
        if (palabras.length == 0 || pictos.isEmpty()) return resultados;

        int maxPalabras = maxPalabrasPicto(pictos, nombres);
        boolean proximoNegado = false;

        for (int i = 0; i < palabras.length; i++) {
            int prefijoNegacion = ReglasPictosFrase.consumirPrefijoNegacion(palabras, i);
            if (prefijoNegacion > 0) {
                proximoNegado = true;
                i += prefijoNegacion - 1;
                continue;
            }

            // Antes de conectores/reglas: "a veces", "se va", etc. como bloque de texto.
            int soloAutomatico = ReglasPictosFrase.consumirSoloAutomatico(palabras, i);
            if (soloAutomatico > 0) {
                if (incluirAutomaticos) {
                    resultados.add(CoincidenciaPicto.automatico(unir(palabras, i, soloAutomatico)));
                }
                proximoNegado = false;
                i += soloAutomatico - 1;
                continue;
            }

            if (ReglasPictosFrase.esConector(palabras[i])) {
                if (incluirAutomaticos) resultados.add(CoincidenciaPicto.automatico(palabras[i]));
                continue;
            }

            CoincidenciaPicto porRegla = ReglasPictosFrase.aplicar(palabras, i, pictos, nombres);
            if (porRegla != null) {
                resultados.add(aplicarNegacion(proximoNegado, porRegla));
                proximoNegado = false;
                i += porRegla.palabrasConsumidas - 1;
                continue;
            }

            CoincidenciaPicto porSimilitud = buscarPorSimilitudEnVentana(palabras, i, maxPalabras, pictos, nombres);
            if (porSimilitud != null) {
                resultados.add(aplicarNegacion(proximoNegado, porSimilitud));
                proximoNegado = false;
                i += porSimilitud.palabrasConsumidas - 1;
            } else if (incluirAutomaticos) {
                resultados.add(CoincidenciaPicto.automatico(palabras[i]));
                proximoNegado = false;
            }
        }
        return resultados;
    }

    private static CoincidenciaPicto aplicarNegacion(boolean negado, CoincidenciaPicto coincidencia) {
        if (!negado || coincidencia.automatico) return coincidencia;
        return new CoincidenciaPicto(coincidencia.archivo, true, coincidencia.palabrasConsumidas);
    }

    private static int maxPalabrasPicto(List<String> pictos, ProveedorNombre nombres) {
        int maxPalabras = 1;
        for (String archivo : pictos) {
            int cantidad = palabras(normalizar(nombres.nombre(archivo))).length;
            if (cantidad > maxPalabras) maxPalabras = cantidad;
        }
        return maxPalabras;
    }

    private static CoincidenciaPicto buscarPorSimilitudEnVentana(String[] palabras, int inicio, int maxPalabras,
            List<String> pictos, ProveedorNombre nombres) {
        String mejorArchivo = null;
        float mejorSimilitud = -1f;
        int mejorLargo = 0;

        for (int largo = Math.min(maxPalabras, palabras.length - inicio); largo >= 1; largo--) {
            String segmento = unir(palabras, inicio, largo);
            if (largo == 1) {
                String porGenero = ReglasPictosFrase.buscarArchivoPorGenero(pictos, nombres, segmento);
                if (porGenero != null) return new CoincidenciaPicto(porGenero, false, 1);
            }
            for (String archivo : pictos) {
                String nombrePicto = normalizar(nombres.nombre(archivo));
                if (nombrePicto.isEmpty()) continue;
                if (ReglasPictosFrase.esPictoRestringido(archivo)
                        && !ReglasPictosFrase.segmentoPermitePictoRestringido(segmento, archivo)) {
                    continue;
                }
                float similitud = similitud(segmento, nombrePicto);
                if (similitud >= SIMILITUD_MINIMA && (similitud > mejorSimilitud
                        || (similitud == mejorSimilitud && largo > mejorLargo))) {
                    mejorSimilitud = similitud;
                    mejorLargo = largo;
                    mejorArchivo = archivo;
                }
            }
            if (mejorArchivo != null) return new CoincidenciaPicto(mejorArchivo, false, mejorLargo);
        }
        return null;
    }

    static String buscarPorSimilitud(String segmento, List<String> pictos, ProveedorNombre nombres) {
        String porGenero = ReglasPictosFrase.buscarArchivoPorGenero(pictos, nombres, segmento);
        if (porGenero != null) return porGenero;

        String mejorArchivo = null;
        float mejorSimilitud = -1f;
        String segmentoNorm = normalizar(segmento);
        for (String archivo : pictos) {
            String nombrePicto = normalizar(nombres.nombre(archivo));
            if (nombrePicto.isEmpty()) continue;
            if (ReglasPictosFrase.esPictoRestringido(archivo)
                    && !ReglasPictosFrase.segmentoPermitePictoRestringido(segmentoNorm, archivo)) {
                continue;
            }
            float similitud = similitud(segmentoNorm, nombrePicto);
            if (similitud >= SIMILITUD_MINIMA && similitud > mejorSimilitud) {
                mejorSimilitud = similitud;
                mejorArchivo = archivo;
            }
        }
        return mejorArchivo;
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        String limpio = texto.toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD);
        return limpio.replaceAll("\\p{M}", "");
    }

    private static String[] palabras(String texto) {
        if (texto.isEmpty()) return new String[0];
        return texto.split("\\s+");
    }

    private static String unir(String[] palabras, int inicio, int largo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < largo; i++) {
            if (i > 0) sb.append(' ');
            sb.append(palabras[inicio + i]);
        }
        return sb.toString();
    }

    /** Similitud 0..1 basada en distancia de Levenshtein (1 = igual). */
    static float similitud(String a, String b) {
        if (a.equals(b)) return 1f;
        if (a.isEmpty() || b.isEmpty()) return 0f;
        int distancia = distanciaLevenshtein(a, b);
        return 1f - (float) distancia / Math.max(a.length(), b.length());
    }

    private static int distanciaLevenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + costo);
            }
        }
        return dp[a.length()][b.length()];
    }
}
