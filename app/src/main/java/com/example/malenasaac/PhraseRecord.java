package com.example.malenasaac;
import java.util.*;
/** Una frase guardada como secuencia ordenada de pictogramas. */
public class PhraseRecord { public final List<Item> items; public int reproducciones; public PhraseRecord(List<Item> items){this(items,0);} public PhraseRecord(List<Item> items,int reproducciones){this.items=new ArrayList<>(items);this.reproducciones=Math.max(0,reproducciones);} public static class Item { public final String archivo; public boolean negado; public Item(String archivo,boolean negado){this.archivo=archivo;this.negado=negado;} } }
