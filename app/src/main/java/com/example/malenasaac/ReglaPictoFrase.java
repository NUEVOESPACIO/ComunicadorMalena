package com.example.malenasaac;

import java.util.List;

/** Regla específica para resolver pictogramas desde texto reconocido. */
public interface ReglaPictoFrase {
    /**
     * @return coincidencia si la regla aplica en {@code indice}, o {@code null} si no.
     */
    CoincidenciaPicto resolver(String[] palabras, int indice, List<String> pictos,
            AlgoritmoPictosFrase.ProveedorNombre nombres);
}
