package com.example.malenasaac;

/** Resultado de cruzar voz con un pictograma, o palabra sin match (picto automático). */
public final class CoincidenciaPicto {
    public final String archivo;
    public final boolean negado;
    public final int palabrasConsumidas;
    /** Si true, no hay archivo: se muestra {@link #texto} como picto débil. */
    public final boolean automatico;
    public final String texto;

    public CoincidenciaPicto(String archivo, boolean negado, int palabrasConsumidas) {
        this(archivo, negado, palabrasConsumidas, false, null);
    }

    public CoincidenciaPicto(String archivo, boolean negado, int palabrasConsumidas,
            boolean automatico, String texto) {
        this.archivo = archivo;
        this.negado = negado;
        this.palabrasConsumidas = palabrasConsumidas;
        this.automatico = automatico;
        this.texto = texto;
    }

    static CoincidenciaPicto automatico(String palabra) {
        return new CoincidenciaPicto(null, false, 1, true, palabra);
    }
}
