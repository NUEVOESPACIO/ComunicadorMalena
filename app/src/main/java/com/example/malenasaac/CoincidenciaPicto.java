package com.example.malenasaac;

/** Resultado de cruzar voz con un pictograma. */
public final class CoincidenciaPicto {
    public final String archivo;
    public final boolean negado;
    public final int palabrasConsumidas;

    public CoincidenciaPicto(String archivo, boolean negado, int palabrasConsumidas) {
        this.archivo = archivo;
        this.negado = negado;
        this.palabrasConsumidas = palabrasConsumidas;
    }
}
