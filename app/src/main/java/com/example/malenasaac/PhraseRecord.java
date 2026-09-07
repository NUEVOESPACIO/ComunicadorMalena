package com.example.malenasaac;

import java.util.ArrayList;
import java.util.List;

/** Una frase guardada como secuencia ordenada de pictogramas. */
public class PhraseRecord {
    public final List<Item> items;
    public int reproducciones;

    public PhraseRecord(List<Item> items) {
        this(items, 0);
    }

    public PhraseRecord(List<Item> items, int reproducciones) {
        this.items = new ArrayList<>(items);
        this.reproducciones = Math.max(0, reproducciones);
    }

    public static class Item {
        public final String archivo;
        public boolean negado;
        /** Palabra sin picto real: se guarda en la frase, no se muestra en la lista de Frases. */
        public final boolean automatico;
        public final String etiqueta;

        public Item(String archivo, boolean negado) {
            this(archivo, negado, false, null);
        }

        public Item(String archivo, boolean negado, boolean automatico, String etiqueta) {
            this.archivo = archivo == null ? "" : archivo;
            this.negado = negado;
            this.automatico = automatico;
            this.etiqueta = etiqueta;
        }

        public static Item automatico(String palabra) {
            return new Item("", false, true, palabra);
        }
    }
}
