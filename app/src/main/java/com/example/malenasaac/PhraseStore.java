package com.example.malenasaac;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.ArrayList;
import java.util.List;

/** Persistencia local, liviana y sin permisos adicionales. */
public final class PhraseStore {
    private static final String PREFS = "comunicador_visual";
    private static final String KEY = "frases";

    private PhraseStore() { }

    public static List<PhraseRecord> cargar(Context c) {
        try {
            return desdeJson(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static void guardar(Context c, List<PhraseRecord> frases) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, aJson(frases)).apply();
    }

    /** Acepta el formato anterior (un arreglo) y el respaldo versionado actual. */
    public static List<PhraseRecord> desdeJson(String texto) throws JSONException {
        List<PhraseRecord> r = new ArrayList<>();
        Object raiz = new JSONTokener(texto).nextValue();
        JSONArray fs = raiz instanceof JSONObject
                ? ((JSONObject) raiz).getJSONArray("frases")
                : (JSONArray) raiz;
        for (int i = 0; i < fs.length(); i++) {
            Object entrada = fs.get(i);
            JSONArray origen;
            int reproducciones = 0;
            if (entrada instanceof JSONObject) {
                JSONObject frase = (JSONObject) entrada;
                origen = frase.getJSONArray("items");
                reproducciones = frase.optInt("reproducciones", 0);
            } else {
                origen = (JSONArray) entrada;
            }
            List<PhraseRecord.Item> items = new ArrayList<>();
            for (int j = 0; j < origen.length(); j++) {
                JSONObject x = origen.getJSONObject(j);
                if (x.optBoolean("automatico", false)) {
                    items.add(PhraseRecord.Item.automatico(x.optString("etiqueta", "")));
                } else {
                    items.add(new PhraseRecord.Item(x.getString("archivo"), x.optBoolean("negado")));
                }
            }
            if (!items.isEmpty()) r.add(new PhraseRecord(items, reproducciones));
        }
        return r;
    }

    public static String aJson(List<PhraseRecord> frases) {
        JSONArray destino = new JSONArray();
        try {
            for (PhraseRecord f : frases) {
                JSONArray items = new JSONArray();
                for (PhraseRecord.Item i : f.items) {
                    JSONObject x = new JSONObject();
                    x.put("archivo", i.archivo);
                    x.put("negado", i.negado);
                    if (i.automatico) {
                        x.put("automatico", true);
                        x.put("etiqueta", i.etiqueta == null ? "" : i.etiqueta);
                    }
                    items.put(x);
                }
                JSONObject frase = new JSONObject();
                frase.put("items", items);
                frase.put("reproducciones", f.reproducciones);
                destino.put(frase);
            }
            JSONObject raiz = new JSONObject();
            raiz.put("version", 2);
            raiz.put("frases", destino);
            return raiz.toString();
        } catch (JSONException impossible) {
            return "{\"version\":2,\"frases\":[]}";
        }
    }
}
