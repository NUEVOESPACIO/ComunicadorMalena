package com.example.malenasaac;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.util.TypedValue;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Comunicador visual con composición y reproducción de frases. */
public class MainActivity extends Activity {
    private static final int MAX_PICTOS = 7;
    /** Tamaño máximo de decodificación para miniaturas de lista (70 dp). */
    private static final int TAM_MINIATURA_LISTA = 256;
    /** Tamaño máximo de decodificación para reproducción de frases. */
    private static final int TAM_PICTO_REPRODUCCION = 512;
    private static final int ELEGIR_RESPALDO = 41;
    private static final int PERMISO_DESCARGAS = 42;
    private static final String DIR = "pictos";
    private static final int[] COLORES_PALABRAS = {0xffe3f4e8, 0xffe1f0fa, 0xfffff5c9, 0xffffe3ee};
    private static final int[] COLORES_FRASES = {0xffeee8fb, 0xffffeadb, 0xffdef3f1, 0xffe7edf9};
    /** Estilos visuales de teclas del teclado predictivo. */
    private static final int TECLA_NORMAL = 0;
    private static final int TECLA_PULSADA = 1;
    private static final int TECLA_ULTIMA = 2;
    private LinearLayout contenido, lista;
    private ScrollView scrollLista;
    private Button tabPalabras, tabFrases;
    private ImageButton salir;
    private final List<String> pictos = new ArrayList<>();
    private final List<PhraseRecord> frases = new ArrayList<>();
    private final List<PhraseRecord.Item> borrador = new ArrayList<>();
    private boolean palabras = true;
    private int frasesDescartadasAlCargar;
    /** Prefijo elegido en el teclado predictivo de pictogramas. */
    private String filtroTeclado = "";
    /** Catálogo de pictos buscables, precalculado al iniciar. */
    private final List<String> catalogoBuscable = new ArrayList<>();
    /** Nombres visibles cacheados por archivo. */
    private final Map<String, String> nombresCache = new HashMap<>();
    /** Miniaturas decodificadas para evitar releer assets en cada tecla. */
    private LruCache<String, Bitmap> cacheMiniaturas;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); cargarDatos(); crearVista(); mostrarPalabras(); ocultarBarras();
        if (frasesDescartadasAlCargar > 0) {
            Toast.makeText(this, "Se quitaron " + frasesDescartadasAlCargar + " frases con pictogramas que ya no existen.", Toast.LENGTH_LONG).show();
            guardarRespaldo();
        }
        if (esInstalacionNueva() && frases.isEmpty()) contenido.post(this::ofrecerRestauracion);
    }
    @Override protected void onResume() { super.onResume(); ocultarBarras(); }
    @Override public void onWindowFocusChanged(boolean foco) { super.onWindowFocusChanged(foco); if (foco) ocultarBarras(); }

    private void cargarDatos() {
        try { String[] nombres = getAssets().list(DIR); if (nombres != null) { pictos.addAll(Arrays.asList(nombres)); Collections.sort(pictos, String.CASE_INSENSITIVE_ORDER); } } catch (IOException ignored) { }
        for (String archivo : pictos) {
            if (!esCaptura(archivo)) catalogoBuscable.add(archivo);
            nombresCache.put(archivo, calcularNombre(archivo));
        }
        Collections.sort(catalogoBuscable, String.CASE_INSENSITIVE_ORDER);
        int cacheBytes = (int) (Runtime.getRuntime().maxMemory() / 8);
        cacheMiniaturas = new LruCache<String, Bitmap>(cacheBytes) {
            @Override protected int sizeOf(String clave, Bitmap valor) { return valor.getByteCount(); }
        };
        precargarMiniaturas();
        List<PhraseRecord> cargadas = PhraseStore.cargar(this);
        List<PhraseRecord> validas = validarFrases(cargadas);
        frasesDescartadasAlCargar = cargadas.size() - validas.size();
        frases.addAll(validas);
        if (frasesDescartadasAlCargar > 0) PhraseStore.guardar(this, frases);
    }

    /** Decodifica miniaturas en segundo plano para que la primera tecla ya las tenga listas. */
    private void precargarMiniaturas() {
        new Thread(() -> {
            for (String archivo : catalogoBuscable) {
                if (cacheMiniaturas.get(archivo) != null) continue;
                Bitmap bitmap = decodificarPicto(archivo, TAM_MINIATURA_LISTA);
                if (bitmap != null) cacheMiniaturas.put(archivo, bitmap);
            }
        }).start();
    }
    private void crearVista() {
        FrameLayout raiz = new FrameLayout(this); raiz.setBackgroundColor(0xfff7f8fa);
        contenido = new LinearLayout(this); contenido.setOrientation(LinearLayout.VERTICAL); raiz.addView(contenido, new FrameLayout.LayoutParams(-1,-1));
        LinearLayout tabs = new LinearLayout(this); tabs.setPadding(dp(10),dp(10),dp(10),dp(6));
        tabPalabras = tab("Palabras", v -> mostrarPalabras()); tabFrases = tab("Frases", v -> mostrarFrases());
        tabs.addView(tabPalabras, peso(1,-2,dp(4))); tabs.addView(tabFrases,peso(1,-2,dp(4)));
        salir = new ImageButton(this); salir.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); salir.setContentDescription("Salir de la aplicación"); salir.setBackgroundColor(Color.TRANSPARENT); salir.setVisibility(View.GONE); salir.setOnClickListener(v -> finishAffinity()); tabs.addView(salir, fijo(dp(52),dp(52),0)); contenido.addView(tabs);
        scrollLista = new ScrollView(this); lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); lista.setPadding(dp(10),dp(4),dp(10),dp(4)); scrollLista.addView(lista); contenido.addView(scrollLista, expandirEnVertical());
        setContentView(raiz);
    }
    private Button tab(String texto, View.OnClickListener accion) { Button b = new Button(this); b.setText(texto.toUpperCase(Locale.ROOT)); b.setTextSize(18); b.setTextColor(0xff263238); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setOnClickListener(accion); return b; }

    private void mostrarPalabras() {
        palabras=true; actualizarTabs(); lista.removeAllViews();
        int indice=0;
        // Las tarjetas ya elegidas siempre se muestran primero, incluso si la misma se eligió más de una vez.
        for(PhraseRecord.Item item:borrador) lista.addView(filaPalabra(item, indice++));
        agregarTecladoYCandidatos();
        scrollLista.scrollTo(0, 0);
    }

    /** Actualiza solo teclado y candidatos, sin reconstruir las tarjetas ya elegidas. */
    private void actualizarTrasTecla() {
        int indiceTeclado = borrador.size();
        while (lista.getChildCount() > indiceTeclado) lista.removeViewAt(indiceTeclado);
        agregarTecladoYCandidatos();
        scrollLista.scrollTo(0, 0);
    }

    private void agregarTecladoYCandidatos() {
        lista.addView(botonPlayFrase());
        lista.addView(tecladoPredictivo(catalogoBuscable));
        if (!filtroTeclado.isEmpty()) {
            int candidato = 0;
            for (String archivo : coincidenciasTeclado()) lista.addView(filaCandidato(archivo, candidato++));
        }
    }

    /** PLAY a todo el ancho, entre pictos elegidos y teclado. */
    private View botonPlayFrase() {
        Button play = tecla("", TECLA_NORMAL, v -> enviar());
        play.setEnabled(true); play.setAlpha(1f);
        Drawable iconoPlay = getResources().getDrawable(android.R.drawable.ic_media_play, getTheme()).mutate();
        iconoPlay.setTint(0xff263238);
        int ladoIcono = dp(14);
        iconoPlay.setBounds(0, 0, ladoIcono, ladoIcono);
        SpannableStringBuilder etiquetaPlay = new SpannableStringBuilder("PLAY ");
        int inicioIcono = etiquetaPlay.length();
        etiquetaPlay.append('\uFFFC');
        etiquetaPlay.setSpan(new ImageSpan(iconoPlay, ImageSpan.ALIGN_CENTER), inicioIcono, etiquetaPlay.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        play.setText(etiquetaPlay);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(40));
        params.setMargins(0, dp(3), 0, dp(3));
        play.setLayoutParams(params);
        return play;
    }
    private void mostrarFrases() {
        palabras=false; actualizarTabs(); lista.removeAllViews();
        if(frases.isEmpty()) { TextView v=new TextView(this); v.setText("Todavía no hay frases guardadas."); v.setTextSize(18); v.setGravity(Gravity.CENTER); v.setPadding(0,dp(35),0,0); lista.addView(v); }
        int indice=0; for(PhraseRecord f:frases) lista.addView(filaFrase(f, indice++));
    }
    private void actualizarTabs() { tabPalabras.setEnabled(!palabras); tabFrases.setEnabled(palabras); tabPalabras.setAlpha(palabras?1:.72f); tabFrases.setAlpha(palabras?.72f:1); tabPalabras.setBackground(fondoSolapa(palabras)); tabFrases.setBackground(fondoSolapa(!palabras)); salir.setVisibility(palabras ? View.GONE : View.VISIBLE); }
    private View filaPalabra(PhraseRecord.Item item, int indice) {
        LinearLayout fila=nuevaFila(); fila.setBackground(fondoPalabra(indice, true));
        // Número de orden arriba a la izquierda (~1/4 de la altura de la tarjeta).
        int altoTarjeta = dp(70) + dp(12);
        int altoNumero = altoTarjeta / 4;
        int anchoNumero = dp(22);
        TextView numero = new TextView(this);
        numero.setText(String.valueOf(indice + 1));
        numero.setTextColor(0xff263238);
        numero.setTypeface(Typeface.DEFAULT_BOLD);
        numero.setGravity(Gravity.CENTER);
        numero.setTextSize(TypedValue.COMPLEX_UNIT_PX, altoNumero * .78f);
        LinearLayout.LayoutParams numParams = fijo(anchoNumero, altoNumero, 0);
        numParams.gravity = Gravity.TOP;
        fila.addView(numero, numParams);
        FrameLayout icono = new FrameLayout(this); ImageView imagen=imagen(item.archivo); icono.addView(imagen,new FrameLayout.LayoutParams(-1,-1));
        View raya=new View(this); raya.setBackgroundColor(0xccb00020); raya.setVisibility(item.negado?View.VISIBLE:View.GONE); FrameLayout.LayoutParams pr=new FrameLayout.LayoutParams(-1,dp(5),Gravity.CENTER);pr.setMargins(dp(5),0,dp(5),0);icono.addView(raya,pr);
        icono.setContentDescription((item.negado ? "Afirmar " : "Negar ") + nombre(item.archivo));
        icono.setOnClickListener(v->{item.negado=!item.negado;raya.setVisibility(item.negado?View.VISIBLE:View.GONE);icono.setContentDescription((item.negado ? "Afirmar " : "Negar ") + nombre(item.archivo));});
        fila.addView(icono,fijo(dp(70),dp(70),dp(4)));
        TextView nombre=new TextView(this); nombre.setText(nombre(item.archivo)); nombre.setTextSize(20f * 1.12f); nombre.setTypeface(nombre.getTypeface(), Typeface.BOLD); nombre.setGravity(Gravity.CENTER_VERTICAL); fila.addView(nombre,peso(1,-1,dp(4)));
        ImageButton papelera=papelera("Quitar una selección"); papelera.setOnClickListener(v->quitar(item)); fila.addView(papelera,fijo(dp(54),dp(54),0)); return fila;
    }
    private View filaCandidato(String archivo, int indice) {
        LinearLayout fila = nuevaFila(); fila.setBackground(fondoPalabra(indice, false));
        ImageView icono = imagen(archivo); fila.addView(icono, fijo(dp(70), dp(70), dp(4)));
        TextView etiqueta = new TextView(this); etiqueta.setText(nombreConPrefijoResaltado(archivo)); etiqueta.setTextSize(20); etiqueta.setGravity(Gravity.CENTER_VERTICAL);
        fila.addView(etiqueta, peso(1, -1, dp(4)));
        View.OnClickListener elegir = v -> agregar(archivo);
        fila.setOnClickListener(elegir); icono.setOnClickListener(elegir); etiqueta.setOnClickListener(elegir);
        fila.setContentDescription("Agregar " + nombre(archivo));
        return fila;
    }

    /** Resalta en negrita las letras ya tipadas en el teclado. */
    private CharSequence nombreConPrefijoResaltado(String archivo) {
        String texto = nombre(archivo);
        int largo = Math.min(filtroTeclado.length(), texto.length());
        if (largo <= 0) return texto;
        SpannableStringBuilder etiqueta = new SpannableStringBuilder(texto);
        etiqueta.setSpan(new StyleSpan(Typeface.BOLD), 0, largo, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        etiqueta.setSpan(new RelativeSizeSpan(1.12f), 0, largo, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return etiqueta;
    }
    private View filaFrase(PhraseRecord frase, int indice) {
        LinearLayout fila=nuevaFila(); fila.setBackground(fondoFrase(indice, false));
        HorizontalScrollView h=new HorizontalScrollView(this); h.setHorizontalScrollBarEnabled(false); h.setFillViewport(true);
        LinearLayout iconos=new LinearLayout(this); iconos.setGravity(Gravity.CENTER_VERTICAL); h.addView(iconos);
        h.post(()->dibujarIconosDeFrase(iconos,h,frase));
        fila.addView(h,peso(1,dp(82),0));
        ImageButton play = new ImageButton(this); play.setImageResource(android.R.drawable.ic_media_play);
        play.setContentDescription("Reproducir frase"); play.setBackgroundColor(Color.TRANSPARENT);
        play.setOnClickListener(v -> ejecutarFrase(frase)); fila.addView(play,fijo(dp(54),dp(54),0));
        ImageButton borrar=papelera("Eliminar frase"); borrar.setOnClickListener(v->confirmar(frase)); fila.addView(borrar,fijo(dp(54),dp(54),0)); return fila;
    }
    private void dibujarIconosDeFrase(LinearLayout destino, View espacio, PhraseRecord frase) {
        destino.removeAllViews();
        int margen = Math.max(1, Math.round(getResources().getDisplayMetrics().density * .5f));
        int cantidad = frase.items.size();
        int lado = dp(56);
        if (cantidad > 0 && espacio.getWidth() > 0) lado = Math.min(lado, Math.max(dp(30), (espacio.getWidth() - cantidad * margen * 2) / cantidad));
        for (PhraseRecord.Item item : frase.items) destino.addView(picto(item, false, lado, margen));
    }
    private LinearLayout nuevaFila() { LinearLayout f=new LinearLayout(this); f.setGravity(Gravity.CENTER_VERTICAL); f.setPadding(dp(8),dp(6),dp(8),dp(6)); f.setBackground(fondo()); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(3),0,dp(3));f.setLayoutParams(p);return f; }
    private void agregar(String archivo) {
        if(borrador.size()==MAX_PICTOS){Toast.makeText(this,"Podés seleccionar hasta 7 pictogramas.",Toast.LENGTH_SHORT).show();return;}
        borrador.add(new PhraseRecord.Item(archivo,false)); filtroTeclado=""; mostrarPalabras();
        scrollLista.scrollTo(0, 0);
    }
    private void quitar(PhraseRecord.Item item) { borrador.remove(item); filtroTeclado=""; mostrarPalabras(); }
    private View picto(PhraseRecord.Item item, boolean editable, int lado) { return picto(item, editable, lado, dp(3), false); }
    private View picto(PhraseRecord.Item item, boolean editable, int lado, int margen) { return picto(item, editable, lado, margen, false); }
    private View picto(PhraseRecord.Item item, boolean editable, int lado, int margen, boolean altaResolucion) {
        FrameLayout caja=new FrameLayout(this); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(lado,lado);p.setMargins(margen,margen,margen,margen);caja.setLayoutParams(p);
        ImageView img = altaResolucion ? imagenGrande(item.archivo) : imagen(item.archivo);
        img.setPadding(dp(4),dp(4),dp(4),dp(4));caja.addView(img,new FrameLayout.LayoutParams(-1,-1));
        View raya=new View(this);raya.setBackgroundColor(0xccb00020);raya.setVisibility(item.negado?View.VISIBLE:View.GONE);FrameLayout.LayoutParams pr=new FrameLayout.LayoutParams(-1,dp(4),Gravity.CENTER);pr.setMargins(dp(5),0,dp(5),0);caja.addView(raya,pr);
        if(editable)caja.setOnClickListener(v->{item.negado=!item.negado;raya.setVisibility(item.negado?View.VISIBLE:View.GONE);}); return caja;
    }
    private ImageView imagen(String archivo) {
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        img.setAdjustViewBounds(true);
        img.setBackground(bordePicto());
        img.setPadding(dp(3), dp(3), dp(3), dp(3));
        img.setContentDescription(nombre(archivo));
        Bitmap bitmap = obtenerMiniatura(archivo);
        if (bitmap != null) img.setImageBitmap(bitmap);
        return img;
    }

    private ImageView imagenGrande(String archivo) {
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        img.setAdjustViewBounds(true);
        img.setBackground(bordePicto());
        img.setPadding(dp(3), dp(3), dp(3), dp(3));
        img.setContentDescription(nombre(archivo));
        Bitmap bitmap = decodificarPicto(archivo, TAM_PICTO_REPRODUCCION);
        if (bitmap != null) img.setImageBitmap(bitmap);
        return img;
    }

    private Bitmap obtenerMiniatura(String archivo) {
        Bitmap cached = cacheMiniaturas.get(archivo);
        if (cached != null) return cached;
        Bitmap bitmap = decodificarPicto(archivo, TAM_MINIATURA_LISTA);
        if (bitmap != null) cacheMiniaturas.put(archivo, bitmap);
        return bitmap;
    }

    private Bitmap decodificarPicto(String archivo, int tamMaximo) {
        String ruta = DIR + "/" + archivo;
        try {
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            try (InputStream stream = getAssets().open(ruta)) {
                BitmapFactory.decodeStream(stream, null, limites);
            }
            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestraPara(limites.outWidth, limites.outHeight, tamMaximo);
            opciones.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream stream = getAssets().open(ruta)) {
                return BitmapFactory.decodeStream(stream, null, opciones);
            }
        } catch (IOException | OutOfMemoryError ignored) {
            return null;
        }
    }

    private int muestraPara(int ancho, int alto, int maximo) {
        int muestra = 1;
        while (ancho / muestra > maximo || alto / muestra > maximo) muestra *= 2;
        return muestra;
    }

    private void enviar() {
        List<PhraseRecord.Item> items=copiar(borrador); if(items.isEmpty()){Toast.makeText(this,"Seleccioná al menos un pictograma.",Toast.LENGTH_SHORT).show();return;}
        // Si ya está en Frases, solo pasa a ser la última ejecutada; si no, se agrega.
        marcarUltimaEjecucion(items);
        PhraseStore.guardar(this,frases); guardarRespaldo();
        ejecutar(items,()->mostrarPalabras());
    }
    private void ejecutarFrase(PhraseRecord frase) {
        // Queda en Frases como última ejecutada y se carga en Palabras para editar/reejecutar.
        frases.remove(frase); frases.add(0, frase);
        PhraseStore.guardar(this,frases); guardarRespaldo();
        List<PhraseRecord.Item> items = copiar(frase.items);
        ejecutar(items, () -> {
            borrador.clear();
            borrador.addAll(copiar(items));
            filtroTeclado = "";
            mostrarPalabras();
        });
    }

    /** Mueve la frase existente al tope o la agrega si es nueva. */
    private void marcarUltimaEjecucion(List<PhraseRecord.Item> items) {
        for (int i = 0; i < frases.size(); i++) {
            if (mismosItems(frases.get(i).items, items)) {
                PhraseRecord existente = frases.remove(i);
                frases.add(0, existente);
                return;
            }
        }
        frases.add(0, new PhraseRecord(items));
    }

    private static boolean mismosItems(List<PhraseRecord.Item> a, List<PhraseRecord.Item> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            PhraseRecord.Item x = a.get(i), y = b.get(i);
            if (!x.archivo.equals(y.archivo) || x.negado != y.negado) return false;
        }
        return true;
    }
    private void ejecutar(List<PhraseRecord.Item> items, Runnable fin) {
        contenido.setVisibility(View.GONE); FrameLayout raiz=(FrameLayout)contenido.getParent(); raiz.setClipChildren(false); raiz.setClipToPadding(false); LinearLayout tren=new LinearLayout(this);tren.setGravity(Gravity.CENTER_VERTICAL);tren.setClipChildren(false);tren.setClipToPadding(false);for(PhraseRecord.Item i:items)tren.addView(picto(i,false,dp(185),dp(3),true)); int anchoTren=items.size()*(dp(185)+dp(6)); raiz.addView(tren,new FrameLayout.LayoutParams(anchoTren,dp(210),Gravity.CENTER_VERTICAL));
        tren.post(()->{
            float inicio=getResources().getDisplayMetrics().widthPixels;
            final boolean[] terminado={false}; final ObjectAnimator[] automatico={null};
            Runnable finalizar=()->{if(terminado[0])return;terminado[0]=true;raiz.setOnTouchListener(null);raiz.removeView(tren);contenido.setVisibility(View.VISIBLE);fin.run();};
            tren.setTranslationX(inicio);
            actualizarEscalaCentrada(tren);
            automatico[0]=animarTren(tren,inicio,-tren.getWidth(),2200L+items.size()*900L,finalizar);
            final float[] inicioDedo={0},inicioTren={0}; final boolean[] arrastrando={false};
            View.OnTouchListener controlarConDedo=(v,event)->{
                switch(event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if(automatico[0]!=null){automatico[0].cancel();automatico[0]=null;}
                        inicioDedo[0]=event.getRawX(); inicioTren[0]=tren.getTranslationX(); arrastrando[0]=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float desplazamiento=event.getRawX()-inicioDedo[0];
                        if(Math.abs(desplazamiento)>getTouchSlop())arrastrando[0]=true;
                        if(arrastrando[0]){
                            float posicion=Math.max(-tren.getWidth(),Math.min(inicio, inicioTren[0]+desplazamiento)); tren.setTranslationX(posicion); actualizarEscalaCentrada(tren);
                            if(posicion<=-tren.getWidth() || posicion>=inicio) finalizar.run();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Un toque breve conserva la reproducción automática; un arrastre queda bajo control manual.
                        if(!arrastrando[0]&&!terminado[0]) automatico[0]=animarTren(tren,tren.getTranslationX(),-tren.getWidth(),2200L+items.size()*900L,finalizar);
                        return true;
                }
                return true;
            };
            raiz.setOnTouchListener(controlarConDedo); tren.setOnTouchListener(controlarConDedo);
        });
    }

    private ObjectAnimator animarTren(LinearLayout tren,float desde,float hasta,long duracion,Runnable finalizar) {
        ObjectAnimator animacion=ObjectAnimator.ofFloat(tren,View.TRANSLATION_X,desde,hasta); final boolean[] cancelada={false};
        animacion.setDuration(duracion); animacion.addUpdateListener(a->actualizarEscalaCentrada(tren)); animacion.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationCancel(Animator a){cancelada[0]=true;}@Override public void onAnimationEnd(Animator a){if(!cancelada[0])finalizar.run();}}); animacion.start(); return animacion;
    }

    /** Agranda hasta un 15 % el picto que se acerca al centro de la pantalla. */
    private void actualizarEscalaCentrada(LinearLayout tren) {
        float centroPantalla=getResources().getDisplayMetrics().widthPixels/2f;
        float rango=Math.max(1f,centroPantalla);
        for(int i=0;i<tren.getChildCount();i++){
            View picto=tren.getChildAt(i); float centroPicto=tren.getX()+picto.getLeft()+picto.getWidth()/2f;
            float cercania=Math.max(0f,1f-Math.abs(centroPicto-centroPantalla)/rango); float escala=1f+.15f*cercania;
            picto.setScaleX(escala); picto.setScaleY(escala);
            // La profundidad sigue el crecimiento: el más cercano al centro tapa a sus vecinos.
            picto.setTranslationZ(dp(1)+dp(20)*cercania);
        }
    }

    private float getTouchSlop(){return ViewConfiguration.get(this).getScaledTouchSlop();}
    private void confirmar(PhraseRecord frase){new AlertDialog.Builder(this).setTitle("Eliminar frase").setMessage("¿Seguro que querés borrar esta frase?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{frases.remove(frase);PhraseStore.guardar(this,frases);guardarRespaldo();mostrarFrases();}).show();}

    private void ofrecerRestauracion() {
        new AlertDialog.Builder(this).setTitle("¿Restaurar frases guardadas?")
                .setMessage("Si usaste la aplicación antes, elegí el archivo frases_malena.json de Descargas para recuperar tus frases.")
                .setNegativeButton("Ahora no", null).setPositiveButton("Buscar archivo", (d, w) -> elegirRespaldo()).show();
    }

    private void elegirRespaldo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, ELEGIR_RESPALDO);
    }

    @Override protected void onActivityResult(int codigo, int resultado, Intent datos) {
        super.onActivityResult(codigo, resultado, datos);
        if (codigo != ELEGIR_RESPALDO || resultado != RESULT_OK || datos == null || datos.getData() == null) return;
        Uri uri = datos.getData();
        int permisos = datos.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try { getContentResolver().takePersistableUriPermission(uri, permisos); } catch (SecurityException ignored) { }
        new Thread(() -> {
            try {
                List<PhraseRecord> leidas = PhraseStore.desdeJson(leerTexto(uri));
                List<PhraseRecord> validas = validarFrases(leidas);
                int descartadas = leidas.size() - validas.size();
                runOnUiThread(() -> confirmarRestauracion(validas, descartadas, uri));
            } catch (Exception error) { runOnUiThread(() -> Toast.makeText(this, "No se pudo leer ese respaldo.", Toast.LENGTH_LONG).show()); }
        }).start();
    }

    private void confirmarRestauracion(List<PhraseRecord> validas, int descartadas, Uri uri) {
        String mensaje = "Se encontraron " + validas.size() + " frases válidas." + (descartadas == 0 ? "" : " Se descartarán " + descartadas + " frases con pictogramas eliminados.");
        new AlertDialog.Builder(this).setTitle("Restaurar frases").setMessage(mensaje).setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar", (d, w) -> { frases.clear(); frases.addAll(validas); PhraseStore.guardar(this, frases); BackupStore.usarDestino(this, uri); guardarRespaldo(); mostrarFrases(); }).show();
    }

    private List<PhraseRecord> validarFrases(List<PhraseRecord> origen) {
        Set<String> existentes = new HashSet<>(pictos); List<PhraseRecord> validas = new ArrayList<>();
        for (PhraseRecord frase : origen) { boolean valida = !frase.items.isEmpty(); for (PhraseRecord.Item item : frase.items) if (!existentes.contains(item.archivo)) { valida = false; break; } if (valida) validas.add(frase); }
        return validas;
    }

    private String leerTexto(Uri uri) throws IOException {
        StringBuilder texto = new StringBuilder();
        try (InputStream entrada = getContentResolver().openInputStream(uri); BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, "UTF-8"))) { String linea; while ((linea = lector.readLine()) != null) texto.append(linea); }
        return texto.toString();
    }

    private boolean esInstalacionNueva() {
        try { android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0); return info.firstInstallTime == info.lastUpdateTime; }
        catch (PackageManager.NameNotFoundException ignored) { return false; }
    }

    private void guardarRespaldo() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISO_DESCARGAS); return; }
        String json = PhraseStore.aJson(new ArrayList<>(frases));
        new Thread(() -> { try { BackupStore.guardar(this, json); } catch (IOException ignored) { } }).start();
    }

    @Override public void onRequestPermissionsResult(int codigo, String[] permisos, int[] resultados) { super.onRequestPermissionsResult(codigo, permisos, resultados); if (codigo == PERMISO_DESCARGAS && resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED) guardarRespaldo(); }

    /** Ordena las opciones no elegidas, priorizando las sugerencias aprendidas. */
    private List<String> palabrasDisponiblesOrdenadas() {
        List<String> orden = palabrasSugeridas();
        List<String> disponibles = new ArrayList<>(pictos);
        for (PhraseRecord.Item item : borrador) disponibles.remove(item.archivo);
        disponibles.removeAll(orden);
        Collections.sort(disponibles, String.CASE_INSENSITIVE_ORDER);
        orden.addAll(disponibles);
        return orden;
    }

    /** Sugerencias basadas en las frases guardadas, sin completar con el índice alfabético. */
    private List<String> palabrasSugeridas() {
        List<String> orden = new ArrayList<>();
        List<String> disponibles = new ArrayList<>(pictos);
        for (PhraseRecord.Item item : borrador) disponibles.remove(item.archivo);
        if (!borrador.isEmpty()) {
            agregarCadenaDeSugerencias(orden, disponibles, borrador.get(borrador.size() - 1).archivo);
        } else {
            String primero = masUsado(disponibles);
            if (primero != null) {
                orden.add(primero);
                disponibles.remove(primero);
                agregarCadenaDeSugerencias(orden, disponibles, primero);
            }
        }
        return orden;
    }

    /**
     * Oferta inicial: todos los pictos distintos que ya forman parte de frases
     * guardadas. Se ordenan por uso, descontando los de la frase en formación.
     */
    private List<String> pictosUsadosOrdenados(List<String> disponibles) {
        List<String> orden = new ArrayList<>();
        for (String archivo : disponibles) if (usosEnFrases(archivo) > 0) orden.add(archivo);
        Collections.sort(orden, (a, b) -> {
            int comparacion = Integer.compare(usoAjustado(b), usoAjustado(a));
            return comparacion != 0 ? comparacion : a.compareToIgnoreCase(b);
        });
        return orden;
    }

    private int usosEnFrases(String archivo) {
        int cantidad = 0;
        for (PhraseRecord frase : frases)
            for (PhraseRecord.Item item : frase.items)
                if (item.archivo.equals(archivo)) cantidad++;
        return cantidad;
    }

    private int usoAjustado(String archivo) {
        int cantidad = usosEnFrases(archivo);
        for (PhraseRecord.Item item : borrador) if (item.archivo.equals(archivo)) cantidad--;
        return Math.max(0, cantidad);
    }

    /** Teclado QWERTY que muestra pulsadas las letras que forman el filtro actual. */
    private View tecladoPredictivo(List<String> disponibles) {
        boolean lleno = borrador.size() >= MAX_PICTOS;
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(6), dp(8), dp(6)); panel.setBackground(fondoTeclado());
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.setMargins(0, dp(5), 0, dp(5)); panel.setLayoutParams(panelParams);

        LinearLayout controles = new LinearLayout(this); controles.setGravity(Gravity.CENTER);
        Button liberar = tecla("Soltar todas", TECLA_NORMAL, lleno ? null : v -> liberarFiltro());
        boolean soltarActivo = !lleno && !filtroTeclado.isEmpty();
        liberar.setEnabled(soltarActivo); liberar.setAlpha(soltarActivo ? 1f : .35f);
        controles.addView(liberar, peso(1, dp(28), dp(1)));
        panel.addView(controles);

        Set<Character> iniciales = caracteresIniciales(disponibles);
        if (lleno) {
            agregarFilaTeclado(panel, "QWERTYUIOP", Collections.emptySet(), iniciales, false);
            agregarFilaTeclado(panel, "ASDFGHJKLÑ", Collections.emptySet(), iniciales, false);
            agregarFilaTeclado(panel, "ZXCVBNM ", Collections.emptySet(), iniciales, false);
        } else {
            Set<Character> siguientes = siguientesCaracteres(disponibles);
            agregarFilaTeclado(panel, "QWERTYUIOP", siguientes, iniciales, true);
            agregarFilaTeclado(panel, "ASDFGHJKLÑ", siguientes, iniciales, true);
            agregarFilaTeclado(panel, "ZXCVBNM ", siguientes, iniciales, true);
        }
        return panel;
    }

    private void liberarFiltro() { filtroTeclado=""; actualizarTrasTecla(); }

    /** Espacio se muestra como "_" para que la tecla sea visible. */
    private String etiquetaTecla(char letra) { return letra == ' ' ? "_" : String.valueOf(letra); }

    private void borrarUltimaTecla() {
        if (filtroTeclado.isEmpty()) return;
        filtroTeclado = filtroTeclado.substring(0, filtroTeclado.length() - 1);
        actualizarTrasTecla();
    }

    private void agregarFilaTeclado(LinearLayout panel, String letras, Set<Character> siguientes, Set<Character> iniciales, boolean habilitado) {
        LinearLayout fila = new LinearLayout(this); fila.setGravity(Gravity.CENTER);
        char ultima = filtroTeclado.isEmpty() ? 0 : filtroTeclado.charAt(filtroTeclado.length() - 1);
        for (int i=0; i<letras.length(); i++) {
            char letra = letras.charAt(i);
            String etiqueta = etiquetaTecla(letra);
            if (!habilitado) {
                Button inactiva = tecla(etiqueta, TECLA_NORMAL, null);
                inactiva.setEnabled(false); inactiva.setAlpha(.35f);
                fila.addView(inactiva, peso(1, dp(31), dp(1)));
                continue;
            }
            int seleccionadas = cantidadDeLetra(letra);
            boolean disponible = siguientes.contains(letra);
            // Espacio siempre forma parte del teclado (pictos con varias palabras).
            boolean enTecladoOriginal = iniciales.contains(letra) || letra == ' ';
            // El teclado original (iniciales) se mantiene: si no hay próxima
            // ocurrencia la tecla se inhibe, no desaparece. Excepción: si la
            // letra ya fue pulsada y sigue siendo válida, se agrega una copia activa.
            if (!enTecladoOriginal && !disponible && seleccionadas == 0) continue;
            for (int copia=0; copia<seleccionadas; copia++) {
                // Solo la copia que representa la última letra tipada se puede deshacer.
                boolean esUltima = letra == ultima && copia == seleccionadas - 1;
                if (esUltima) {
                    Button deshacer = tecla(etiqueta, TECLA_ULTIMA, v -> borrarUltimaTecla());
                    deshacer.setContentDescription("Borrar última letra");
                    fila.addView(deshacer, peso(1, dp(31), dp(1)));
                } else {
                    Button marcada = tecla(etiqueta, TECLA_PULSADA, null);
                    marcada.setEnabled(false); fila.addView(marcada, peso(1, dp(31), dp(1)));
                }
            }
            if (disponible) {
                Button opcion = tecla(etiqueta, TECLA_NORMAL, v -> { filtroTeclado += letra; actualizarTrasTecla(); });
                fila.addView(opcion, peso(1, dp(31), dp(1)));
            } else if (seleccionadas == 0) {
                Button inactiva = tecla(etiqueta, TECLA_NORMAL, null);
                inactiva.setEnabled(false); inactiva.setAlpha(.35f);
                fila.addView(inactiva, peso(1, dp(31), dp(1)));
            }
        }
        if (fila.getChildCount() > 0) panel.addView(fila);
    }

    private Set<Character> caracteresIniciales(List<String> disponibles) {
        Set<Character> resultado = new HashSet<>();
        for (String archivo : disponibles) {
            String texto = nombre(archivo);
            if (!texto.isEmpty()) resultado.add(texto.charAt(0));
        }
        return resultado;
    }

    private int cantidadDeLetra(char letra) {
        int cantidad = 0;
        for (int i=0; i<filtroTeclado.length(); i++) if (filtroTeclado.charAt(i) == letra) cantidad++;
        return cantidad;
    }

    private Set<Character> siguientesCaracteres(List<String> disponibles) {
        Set<Character> resultado = new HashSet<>();
        for (String archivo : disponibles) {
            String texto = nombre(archivo);
            if (texto.startsWith(filtroTeclado) && texto.length() > filtroTeclado.length()) resultado.add(texto.charAt(filtroTeclado.length()));
        }
        return resultado;
    }

    private boolean esCaptura(String archivo) { return archivo.regionMatches(true, 0, "Captura", 0, 7); }
    private List<String> coincidenciasTeclado() {
        List<String> r = new ArrayList<>();
        if (filtroTeclado.isEmpty()) return r;
        for (String archivo : catalogoBuscable) if (nombre(archivo).startsWith(filtroTeclado)) r.add(archivo);
        return r;
    }

    private Button tecla(String texto, int estilo, View.OnClickListener accion) {
        Button boton = new Button(this);
        if (estilo == TECLA_ULTIMA) {
            SpannableStringBuilder etiqueta = new SpannableStringBuilder(texto);
            etiqueta.setSpan(new UnderlineSpan(), 0, etiqueta.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            boton.setText(etiqueta);
        } else boton.setText(texto);
        boton.setTextSize(texto.length() == 1 ? 15 : 11); boton.setAllCaps(false);
        boton.setTextColor(estilo == TECLA_NORMAL ? 0xff263238 : Color.WHITE);
        boton.setPadding(0, 0, 0, 0); boton.setGravity(Gravity.CENTER);
        boton.setBackground(fondoTecla(estilo == TECLA_ULTIMA ? TECLA_PULSADA : estilo));
        if (accion != null) boton.setOnClickListener(accion); return boton;
    }

    private void agregarCadenaDeSugerencias(List<String> orden, List<String> disponibles, String anterior) {
        String actual = anterior;
        while (!disponibles.isEmpty()) {
            String siguiente = sucesorMasUsado(actual, disponibles);
            if (siguiente == null) return;
            orden.add(siguiente);
            disponibles.remove(siguiente);
            actual = siguiente;
        }
    }

    private String masUsado(List<String> candidatos) {
        String mejor = null;
        int mayorCantidad = 0;
        for (String candidato : candidatos) {
            int cantidad = 0;
            for (PhraseRecord frase : frases)
                for (PhraseRecord.Item item : frase.items)
                    if (item.archivo.equals(candidato)) cantidad++;
            if (cantidad > mayorCantidad || (cantidad == mayorCantidad && cantidad > 0
                    && (mejor == null || candidato.compareToIgnoreCase(mejor) < 0))) {
                mayorCantidad = cantidad;
                mejor = candidato;
            }
        }
        return mayorCantidad == 0 ? null : mejor;
    }

    private String sucesorMasUsado(String anterior, List<String> candidatos) {
        String mejor = null;
        int mayorCantidad = 0;
        for (String candidato : candidatos) {
            int cantidad = 0;
            for (PhraseRecord frase : frases) {
                for (int i = 1; i < frase.items.size(); i++) {
                    if (frase.items.get(i - 1).archivo.equals(anterior)
                            && frase.items.get(i).archivo.equals(candidato)) cantidad++;
                }
            }
            if (cantidad > mayorCantidad || (cantidad == mayorCantidad && cantidad > 0
                    && (mejor == null || candidato.compareToIgnoreCase(mejor) < 0))) {
                mayorCantidad = cantidad;
                mejor = candidato;
            }
        }
        return mayorCantidad == 0 ? null : mejor;
    }

    private static List<PhraseRecord.Item> copiar(List<PhraseRecord.Item> origen){List<PhraseRecord.Item> r=new ArrayList<>();for(PhraseRecord.Item i:origen)r.add(new PhraseRecord.Item(i.archivo,i.negado));return r;}
    private String nombre(String archivo) {
        String cached = nombresCache.get(archivo);
        return cached != null ? cached : calcularNombre(archivo);
    }
    private String calcularNombre(String archivo){
        int p=archivo.lastIndexOf('.');
        String base=(p>0?archivo.substring(0,p):archivo).replace('_',' ');
        // Oculta sufijos tipo (1), (2), etc. en el texto visible.
        base=base.replaceAll("\\s*\\(\\d+\\)","").trim();
        return base.toUpperCase(Locale.ROOT);
    }
    private ImageButton papelera(String d){ImageButton b=new ImageButton(this);b.setImageResource(android.R.drawable.ic_menu_delete);b.setContentDescription(d);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private GradientDrawable fondo(){GradientDrawable f=new GradientDrawable();f.setColor(Color.WHITE);f.setCornerRadius(dp(10));f.setStroke(dp(1),0x22000000);return f;}
    private GradientDrawable fondoPalabra(int indice, boolean seleccionada){GradientDrawable f=new GradientDrawable();int color=COLORES_PALABRAS[indice%COLORES_PALABRAS.length];if(seleccionada)color=oscurecer(color,.86f);f.setColor(color);f.setCornerRadius(dp(12));f.setStroke(dp(seleccionada?2:1),seleccionada?0xff55616a:0x4437464f);return f;}
    private int oscurecer(int color, float factor){return Color.rgb(Math.round(Color.red(color)*factor),Math.round(Color.green(color)*factor),Math.round(Color.blue(color)*factor));}
    private GradientDrawable fondoFrase(int indice, boolean seleccionada){GradientDrawable f=new GradientDrawable();int color=COLORES_FRASES[indice%COLORES_FRASES.length];f.setColor(seleccionada?oscurecer(color,.72f):color);f.setCornerRadius(dp(16));f.setStroke(dp(seleccionada?3:1),seleccionada?0xff382060:0xff685c7a);return f;}
    private GradientDrawable bordePicto(){GradientDrawable f=new GradientDrawable();f.setColor(Color.WHITE);f.setCornerRadius(dp(10));f.setStroke(dp(2),Color.BLACK);return f;}
    private GradientDrawable fondoTeclado(){GradientDrawable f=new GradientDrawable();f.setColor(0xffedf3f7);f.setCornerRadius(dp(12));f.setStroke(dp(2),0xff718596);return f;}
    private GradientDrawable fondoTecla(int estilo){
        GradientDrawable f=new GradientDrawable();
        if (estilo == TECLA_PULSADA) { f.setColor(0xff496f88); f.setCornerRadius(dp(7)); f.setStroke(dp(1),0xff294b62); }
        else { f.setColor(0xffffffff); f.setCornerRadius(dp(7)); f.setStroke(dp(1),0xff91a5b4); }
        return f;
    }
    private GradientDrawable fondoPanel(){GradientDrawable f=new GradientDrawable();f.setColor(0xffdfe8f0);f.setCornerRadius(dp(16));f.setStroke(dp(3),0xff718596);return f;}
    private GradientDrawable fondoSolapa(boolean activa){GradientDrawable f=new GradientDrawable();f.setColor(activa?0xffd5e9f7:0xffeef3f7);f.setCornerRadius(dp(14));f.setStroke(dp(activa?3:2),activa?0xff496f88:0xff718596);return f;}
    private LinearLayout.LayoutParams peso(float peso,int alto,int margen){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,alto,peso);p.setMargins(margen,0,margen,0);return p;}
    /** En un LinearLayout vertical el peso reparte altura; el ancho debe conservarse completo. */
    private LinearLayout.LayoutParams expandirEnVertical(){return new LinearLayout.LayoutParams(-1,0,1);}
    private LinearLayout.LayoutParams fijo(int ancho,int alto,int margen){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ancho,alto);p.setMargins(margen,0,margen,0);return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private void ocultarBarras(){getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){Window w=getWindow();w.setDecorFitsSystemWindows(false);WindowInsetsController c=w.getDecorView().getWindowInsetsController();if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);}
}
