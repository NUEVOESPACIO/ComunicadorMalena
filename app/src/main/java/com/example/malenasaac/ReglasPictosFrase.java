package com.example.malenasaac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reglas específicas de casos de uso para ALGORITMOPICTOSFRASE.
 * Agregar nuevas reglas en {@link #reglas()}.
 */
public final class ReglasPictosFrase {
    private static final Set<String> CONECTORES = new HashSet<>(Arrays.asList(
            "EL", "LA", "LOS", "LAS", "UN", "UNA", "UNOS", "UNAS",
            "DE", "DEL", "Y", "E", "O", "U", "A", "EN", "CON", "POR", "PARA", "QUE",
            "ESTOY", "ESTAS", "ESTAMOS", "SON", "SER",
            "MUY", "MAS", "MUCHO", "POCO", "TAL", "VEZ", "COMO", "SI", "YA",
            "ME", "TE", "LE", "LO", "MI", "TU", "SU", "QUIERO", "QUIERES"
    ));

    /** Palabras que indican que el habla trata de comida (para desambiguar papa/papas). */
    private static final Set<String> INDICADORES_COMIDA = new HashSet<>(Arrays.asList(
            "COMIDA", "ALIMENTOS", "BEBIDAS", "COMER", "DESAYUNO", "ALMUERZO", "CENA", "MERIENDA",
            "PAN", "LECHE", "ARROZ", "ENSALADA", "SOPA", "PIZZA", "HAMBURGUESA", "FRUTAS", "POSTRES",
            "GALLETITAS", "DULCE", "CARNE", "POLLO", "PESCADO", "HUEVO", "QUESO", "PURE", "FRITA", "FRITAS",
            "SANDWICH", "TORTA", "FLAN", "CEREALES", "HELADO", "JUGO", "CHOCOLATE", "MAGALENA",
            "POROTOS", "LENTEJAS", "FESTEJAR", "PANADERIA", "DESAYUNAR", "ALMORZAR", "CENAR",
            "HORNEAR", "COCINAR", "MILANESA", "EMPANADA", "FIDEO", "FIDEOS", "YOGUR", "MANTECA",
            "LOMO", "SALCHICAS", "SALCHICA", "ENSARTAR", "COCINA", "PLATO", "VASO", "CUCHARA"
    ));

    /** Palabra hablada → nombre base del picto (sin extensión). */
    private static final Map<String, String> SINONIMOS = new HashMap<>();
    /** Frase hablada (palabras unidas con espacio) → nombre base del picto. */
    private static final Map<String, String> FRASES = new HashMap<>();
    /**
     * Prefijos de negación: se consumen sin picto y el siguiente match queda negado.
     * Orden: más largos primero.
     */
    private static final String[] PREFIJOS_NEGACION = {
            "NO VAMOS A",
            "NO TENEMOS",
            "NO QUEREMOS",
            "NO PODEMOS",
            "NO VAMOS",
            "NO HAY",
            "NO ES",
            "NO ESTA",
            "NO ESTOY",
            "NO SOMOS",
            "NO"
    };

    static {
        agregarSinonimo("PRENDIDO", "encendido");
        agregarSinonimo("PRENDIDA", "encendido");
        agregarSinonimo("PRENDIDOS", "encendido");
        agregarSinonimo("APAGADA", "apagado");
        agregarSinonimo("APAGADOS", "apagado");
        agregarSinonimo("OSCURO", "osuro");
        agregarSinonimo("OSCURA", "osuro");
        agregarSinonimo("BUEN", "buena");
        agregarSinonimo("BUENO", "buena");
        agregarSinonimo("BUENOS", "buena");
        agregarSinonimo("BUENAS", "buena");
        agregarSinonimo("FACIL", "facil");
        agregarSinonimo("FACILES", "facil");
        agregarSinonimo("DIFICIL", "dificil");
        agregarSinonimo("DIFICILES", "dificil");
        agregarSinonimo("JOVENES", "joven");
        agregarSinonimo("FUERTES", "fuerte");
        agregarSinonimo("DEBIL", "flojo");
        agregarSinonimo("DEBILES", "flojo");
        agregarSinonimo("RAPIDOS", "rapido");
        agregarSinonimo("LENTOS", "lento");
        agregarSinonimo("BONITOS", "bonito");
        agregarSinonimo("GUAPOS", "guapo");
        agregarSinonimo("FEOS", "feo");
        agregarSinonimo("FEAS", "feo");
        agregarSinonimo("MALOS", "malo");
        agregarSinonimo("MALAS", "malo");
        agregarSinonimo("VIEJOS", "viejo");
        agregarSinonimo("VIEJAS", "viejo");
        agregarSinonimo("NUEVOS", "nuevo");
        agregarSinonimo("NUEVAS", "nuevo");
        agregarSinonimo("GORDOS", "gordo");
        agregarSinonimo("GORDAS", "gordo");
        agregarSinonimo("DELGADOS", "delgado");
        agregarSinonimo("DELGADAS", "delgado");
        agregarSinonimo("MOJADOS", "mojado");
        agregarSinonimo("MOJADAS", "mojado");
        agregarSinonimo("SECOS", "seco");
        agregarSinonimo("SECAS", "seco");
        agregarSinonimo("SUCIOS", "sucio");
        agregarSinonimo("SUCIAS", "sucio");
        agregarSinonimo("LIMPIOS", "limpio");
        agregarSinonimo("LIMPIAS", "limpio");
        agregarSinonimo("ROTOS", "roto");
        agregarSinonimo("ROTAS", "roto");
        agregarSinonimo("BLANDOS", "blando");
        agregarSinonimo("BLANDAS", "blando");
        agregarSinonimo("DUROS", "duro");
        agregarSinonimo("DURAS", "duro");

        agregarFrase("DE MADERA", "madera");
        agregarFrase("DE METAL", "metal");
        agregarFrase("DE PLASTICO", "plastico");
        agregarFrase("DE CARTON", "carton");
        agregarFrase("DE VIDRIO", "vidrio");
        agregarFrase("MUY RAPIDO", "rapido");
        agregarFrase("MUY LENTO", "lento");
        agregarFrase("ESTA ROTO", "roto");
        agregarFrase("ESTA ROTA", "roto");
        agregarFrase("ESTA LIMPIO", "limpio");
        agregarFrase("ESTA LIMPIA", "limpio");
        agregarFrase("ESTA SUCIO", "sucio");
        agregarFrase("ESTA SUCIA", "sucio");
        agregarFrase("ESTA MOJADO", "mojado");
        agregarFrase("ESTA MOJADA", "mojado");

        agregarSinonimo("RATO", "despues");
        agregarFrase("EN UN RATO", "despues");
        agregarFrase("DENTRO DE UN RATO", "despues");
        agregarFrase("MAS TARDE", "despues");

        agregarSinonimo("ES", "es");
        agregarSinonimo("ESTA", "esta");

        agregarSinonimo("JULY", "mama");
        agregarSinonimo("JULIANA", "mama");
        agregarSinonimo("JULI", "mama");
        agregarSinonimo("MADRE", "mama");

        agregarSinonimo("SEBASTIAN", "papa");
        agregarSinonimo("PADRE", "papa");

        agregarSinonimo("SOFI", "fonoaudiologa");
        agregarSinonimo("SOFIA", "fonoaudiologa");
    }

    private ReglasPictosFrase() { }

    private static void agregarSinonimo(String palabra, String basePicto) {
        SINONIMOS.put(AlgoritmoPictosFrase.normalizar(palabra), basePicto.toLowerCase(Locale.ROOT));
    }

    private static void agregarFrase(String frase, String basePicto) {
        FRASES.put(AlgoritmoPictosFrase.normalizar(frase), basePicto.toLowerCase(Locale.ROOT));
    }

    static boolean esConector(String palabra) {
        return CONECTORES.contains(AlgoritmoPictosFrase.normalizar(palabra));
    }

    /** Cuántas palabras consume un prefijo de negación empezando en {@code indice}, o 0 si no hay. */
    static int consumirPrefijoNegacion(String[] palabras, int indice) {
        if (indice >= palabras.length) return 0;
        if (!"NO".equals(AlgoritmoPictosFrase.normalizar(palabras[indice]))) return 0;
        for (String prefijo : PREFIJOS_NEGACION) {
            String[] partes = prefijo.split("\\s+");
            if (indice + partes.length > palabras.length) continue;
            boolean coincide = true;
            for (int j = 0; j < partes.length; j++) {
                if (!partes[j].equals(AlgoritmoPictosFrase.normalizar(palabras[indice + j]))) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) return partes.length;
        }
        return 0;
    }

    static List<ReglaPictoFrase> reglas() {
        List<ReglaPictoFrase> reglas = new ArrayList<>();
        reglas.add(new ReglaPalabraEs());
        reglas.add(new ReglaContextoPapaPapas());
        reglas.add(new ReglaFrasesCompuestas());
        reglas.add(new ReglaSinonimos());
        return reglas;
    }

    static CoincidenciaPicto aplicar(String[] palabras, int indice, List<String> pictos,
            AlgoritmoPictosFrase.ProveedorNombre nombres) {
        for (ReglaPictoFrase regla : reglas()) {
            CoincidenciaPicto coincidencia = regla.resolver(palabras, indice, pictos, nombres);
            if (coincidencia != null) return coincidencia;
        }
        return null;
    }

    static String buscarArchivoPorBase(List<String> pictos, AlgoritmoPictosFrase.ProveedorNombre nombres, String base) {
        String baseNorm = AlgoritmoPictosFrase.normalizar(base);
        for (String archivo : pictos) {
            String nombre = AlgoritmoPictosFrase.normalizar(nombres.nombre(archivo));
            if (nombre.equals(baseNorm)) return archivo;
        }
        String porStem = buscarArchivoPorStemExacto(pictos, baseNorm);
        if (porStem != null) return porStem;
        for (String archivo : pictos) {
            String nombre = AlgoritmoPictosFrase.normalizar(nombres.nombre(archivo));
            if (nombre.startsWith(baseNorm + " ")) return archivo;
        }
        for (String archivo : pictos) {
            String nombreArchivo = archivo.toLowerCase(Locale.ROOT);
            if (nombreArchivo.startsWith(base.toLowerCase(Locale.ROOT))) return archivo;
        }
        return null;
    }

    /** Coincide solo con el nombre de archivo (sin extensión), no con prefijos parciales. */
    static String buscarArchivoPorStemExacto(List<String> pictos, String stemNorm) {
        if (stemNorm == null || stemNorm.isEmpty()) return null;
        for (String archivo : pictos) {
            if (stemArchivoNormalizado(archivo).equals(stemNorm)) return archivo;
        }
        return null;
    }

    static boolean esTemaComida(String[] palabras, int indiceExcluir) {
        for (int i = 0; i < palabras.length; i++) {
            if (i == indiceExcluir) continue;
            String palabra = AlgoritmoPictosFrase.normalizar(palabras[i]);
            if (palabra.isEmpty() || esConector(palabra)) continue;
            if (INDICADORES_COMIDA.contains(palabra)) return true;
        }
        return false;
    }

    /**
     * Busca picto por palabra hablada, probando o/a indistintamente.
     * Si existen ambas formas, elige la que coincide con la terminación hablada.
     * Si no hay coincidencia directa, aplica variantes morfológicas (sufijos, género plural).
     */
    static String buscarArchivoPorGenero(List<String> pictos, AlgoritmoPictosFrase.ProveedorNombre nombres,
            String palabra) {
        String palabraNorm = AlgoritmoPictosFrase.normalizar(palabra);
        if (palabraNorm.isEmpty()) return null;

        if ("ES".equals(palabraNorm)) {
            return buscarArchivoPorStemExacto(pictos, "ES");
        }

        String resultado = buscarArchivoPorGeneroDirecto(pictos, nombres, palabraNorm);
        if (resultado != null) return resultado;

        if (palabraNorm.length() > 2 && palabraNorm.endsWith("ES")) {
            resultado = buscarArchivoPorGeneroDirecto(pictos, nombres,
                    palabraNorm.substring(0, palabraNorm.length() - 2));
            if (resultado != null) return resultado;
        }

        resultado = buscarArchivoPorGeneroDirecto(pictos, nombres, palabraNorm + "ES");
        if (resultado != null) return resultado;

        resultado = buscarArchivoPorGeneroDirecto(pictos, nombres, palabraNorm + "S");
        if (resultado != null) return resultado;

        resultado = buscarConPictoSinSFinal(pictos, nombres, palabraNorm);
        if (resultado != null) return resultado;

        if (palabraNorm.length() > 2 && palabraNorm.endsWith("AS")) {
            resultado = buscarArchivoPorGeneroDirecto(pictos, nombres,
                    palabraNorm.substring(0, palabraNorm.length() - 2) + "OS");
            if (resultado != null) return resultado;
        }

        if (palabraNorm.length() > 2 && palabraNorm.endsWith("OS")) {
            resultado = buscarArchivoPorGeneroDirecto(pictos, nombres,
                    palabraNorm.substring(0, palabraNorm.length() - 2) + "AS");
            if (resultado != null) return resultado;
        }

        return null;
    }

    private static String buscarArchivoPorGeneroDirecto(List<String> pictos,
            AlgoritmoPictosFrase.ProveedorNombre nombres, String palabraNorm) {
        String exacto = buscarArchivoPorBase(pictos, nombres, palabraNorm);
        String alternativa = alternativaGenero(palabraNorm);
        String alterno = alternativa != null ? buscarArchivoPorBase(pictos, nombres, alternativa) : null;

        if (exacto != null && alterno != null) return exacto;
        if (exacto != null) return exacto;
        return alterno;
    }

    /** Si el picto termina en S, intenta emparejar quitando esa S del nombre del picto. */
    private static String buscarConPictoSinSFinal(List<String> pictos,
            AlgoritmoPictosFrase.ProveedorNombre nombres, String palabraNorm) {
        for (String archivo : pictos) {
            String nombre = AlgoritmoPictosFrase.normalizar(nombres.nombre(archivo));
            if (nombre.length() > 1 && nombre.endsWith("S")) {
                String sinS = nombre.substring(0, nombre.length() - 1);
                if (coincideConNombre(palabraNorm, sinS)) return archivo;
            }
            String stem = stemArchivoNormalizado(archivo);
            if (stem.length() > 1 && stem.endsWith("S")) {
                String sinS = stem.substring(0, stem.length() - 1);
                if (coincideConNombre(palabraNorm, sinS)) return archivo;
            }
        }
        return null;
    }

    private static boolean coincideConNombre(String palabraNorm, String nombreNorm) {
        if (nombreNorm.equals(palabraNorm)) return true;
        String altPalabra = alternativaGenero(palabraNorm);
        if (altPalabra != null && nombreNorm.equals(altPalabra)) return true;
        String altNombre = alternativaGenero(nombreNorm);
        return altNombre != null && altNombre.equals(palabraNorm);
    }

    private static String stemArchivoNormalizado(String archivo) {
        String lower = archivo.toLowerCase(Locale.ROOT);
        int p = lower.lastIndexOf('.');
        String base = p > 0 ? lower.substring(0, p) : lower;
        base = base.replaceAll("\\s*\\(\\d+\\)", "").trim();
        return AlgoritmoPictosFrase.normalizar(base);
    }

    private static String alternativaGenero(String palabra) {
        if (palabra.length() < 2) return null;
        char ultima = palabra.charAt(palabra.length() - 1);
        if (ultima == 'O') return palabra.substring(0, palabra.length() - 1) + 'A';
        if (ultima == 'A') return palabra.substring(0, palabra.length() - 1) + 'O';
        return null;
    }

    /** Palabra exacta "es" (como token aislado) → es.png, sin mezclar con "es gracioso", etc. */
    private static final class ReglaPalabraEs implements ReglaPictoFrase {
        @Override
        public CoincidenciaPicto resolver(String[] palabras, int indice, List<String> pictos,
                AlgoritmoPictosFrase.ProveedorNombre nombres) {
            if (indice >= palabras.length) return null;
            if (!"ES".equals(AlgoritmoPictosFrase.normalizar(palabras[indice]))) return null;
            String archivo = buscarArchivoPorStemExacto(pictos, "ES");
            if (archivo == null) return null;
            return new CoincidenciaPicto(archivo, false, 1);
        }
    }

    /** Desambigua papa (papá) vs papas según si el habla trata de comida. */
    private static final class ReglaContextoPapaPapas implements ReglaPictoFrase {
        @Override
        public CoincidenciaPicto resolver(String[] palabras, int indice, List<String> pictos,
                AlgoritmoPictosFrase.ProveedorNombre nombres) {
            if (indice >= palabras.length) return null;
            String palabra = AlgoritmoPictosFrase.normalizar(palabras[indice]);
            if (!"PAPAS".equals(palabra) && !"PAPA".equals(palabra)) return null;

            boolean temaComida = esTemaComida(palabras, indice);
            String stem = temaComida ? "papas" : "papa";
            String archivo = buscarArchivoPorStemExacto(pictos, stem);
            if (archivo == null) return null;
            return new CoincidenciaPicto(archivo, false, 1);
        }
    }

    /** Frases fijas definidas en {@link #FRASES}. */
    private static final class ReglaFrasesCompuestas implements ReglaPictoFrase {
        @Override
        public CoincidenciaPicto resolver(String[] palabras, int indice, List<String> pictos,
                AlgoritmoPictosFrase.ProveedorNombre nombres) {
            return resolverFrase(palabras, indice, pictos, nombres, false);
        }
    }

    private static CoincidenciaPicto resolverFrase(String[] palabras, int indice, List<String> pictos,
            AlgoritmoPictosFrase.ProveedorNombre nombres, boolean negado) {
        for (Map.Entry<String, String> entrada : FRASES.entrySet()) {
            String[] partes = entrada.getKey().split("\\s+");
            if (indice + partes.length > palabras.length) continue;
            boolean coincide = true;
            for (int i = 0; i < partes.length; i++) {
                if (!partes[i].equals(AlgoritmoPictosFrase.normalizar(palabras[indice + i]))) {
                    coincide = false;
                    break;
                }
            }
            if (!coincide) continue;
            String ultimaPalabra = partes[partes.length - 1];
            String archivo = buscarArchivoPorGenero(pictos, nombres, ultimaPalabra);
            if (archivo == null) archivo = buscarArchivoPorGenero(pictos, nombres, entrada.getValue());
            if (archivo != null) return new CoincidenciaPicto(archivo, negado, partes.length);
        }
        return null;
    }

    /** Palabras alternativas que apuntan al mismo picto. */
    private static final class ReglaSinonimos implements ReglaPictoFrase {
        @Override
        public CoincidenciaPicto resolver(String[] palabras, int indice, List<String> pictos,
                AlgoritmoPictosFrase.ProveedorNombre nombres) {
            if (indice >= palabras.length) return null;
            String palabra = AlgoritmoPictosFrase.normalizar(palabras[indice]);
            String archivo = resolverPalabra(palabra, pictos, nombres);
            if (archivo == null) return null;
            return new CoincidenciaPicto(archivo, false, 1);
        }
    }

    private static String resolverPalabra(String palabraNormalizada, List<String> pictos,
            AlgoritmoPictosFrase.ProveedorNombre nombres) {
        String base = SINONIMOS.get(palabraNormalizada);
        if (base != null) {
            String archivo = buscarArchivoPorGenero(pictos, nombres, base);
            if (archivo != null) return archivo;
        }
        return buscarArchivoPorGenero(pictos, nombres, palabraNormalizada);
    }
}
