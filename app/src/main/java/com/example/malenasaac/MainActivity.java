package com.example.malenasaac;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.core.content.FileProvider;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Comunicador visual con composición y reproducción de frases. */
public class MainActivity extends Activity {
    /** Cantidad aproximada de pictos visibles a la vez en la solapa FRASES (el resto va con scroll). */
    private static final int PICTOS_VISIBLES_FRASE = 4;
    private static final int MAX_FRASES_PDF = 7;
    private static final int MAX_PICTOS_JUEGO = 7;
    /** Casillas visibles a la vez en la tarjeta de Pictos; el resto se alcanza deslizando. */
    private static final int SLOTS_VISIBLES_JUEGO = 4;
    private static final int SEGUNDOS_MAX_ESCUCHA = 20;
    private static final int COLUMNAS_MOSAICO_JUEGO = 5;
    /** Tamaño máximo de decodificación para miniaturas de lista (70 dp). */
    private static final int TAM_MINIATURA_LISTA = 256;
    /** Tamaño máximo de decodificación para reproducción de frases. */
    private static final int TAM_PICTO_REPRODUCCION = 512;
    private static final int ELEGIR_RESPALDO = 41;
    private static final int PERMISO_DESCARGAS = 42;
    private static final int PERMISO_MICRO = 43;
    private static final int ELEGIR_FOTO = 44;
    private static final int TOMAR_FOTO = 45;
    private static final int PERMISO_CAMARA = 46;
    private static final int PERMISO_MICRO_NOMBRE = 47;
    private static final int RECORTAR_FOTO = 48;
    private static final int PERMISO_MICRO_AUDIO = 49;
    private static final int PERMISO_EXPORT_AUDIOS = 50;
    private static final int PERMISO_EXPORT_PICTOS = 51;
    private static final int SEGUNDOS_MAX_NOMBRE = 5;
    private static final int SEGUNDOS_MAX_GRABACION = 8;
    private static final long MS_MODO_EDICION = 5000L;
    private static final long MS_ACCESO_AUDIO = 5000L;
    private static final int FILTRO_AUDIO_TODOS = 0;
    private static final int FILTRO_AUDIO_CON = 1;
    private static final int FILTRO_AUDIO_SIN = 2;
    private static final String DIR = "pictos";
    private static final String PREFS_USOS_JUEGO = "usos_pictos_juego";
    private static final int PERMISO_MICRO_PICTOS = 52;
    private static final int SEGUNDOS_MAX_ESCUCHA_PICTOS = 15;
    private static final int[] COLORES_PALABRAS = {0xffe3f4e8, 0xffe1f0fa, 0xfffff5c9, 0xffffe3ee};
    private static final int[] COLORES_FRASES = {0xffeee8fb, 0xffffeadb, 0xffdef3f1, 0xffe7edf9};
    /** Estilos visuales de teclas del teclado predictivo. */
    private static final int TECLA_NORMAL = 0;
    private static final int TECLA_PULSADA = 1;
    private static final int TECLA_ULTIMA = 2;
    private static final int SOLAPA_PALABRAS = 0;
    private static final int SOLAPA_FRASES = 1;
    private static final int SOLAPA_JUEGO = 2;
    /** Palabras y Frases siguen funcionando internamente; esto solo controla si se ven. */
    private static final boolean MOSTRAR_PALABRAS_Y_FRASES = false;
    private LinearLayout contenido, lista, juegoPanel;
    private ScrollView scrollLista;
    private Button tabPalabras, tabFrases, tabJuego;
    private ImageButton tabEditar, salir;
    private LinearLayout tecladoJuegoContenedor;
    private GridLayout mosaicoJuego;
    private LinearLayout tabEleccionJuego, slotsEleccionJuego;
    private HorizontalScrollView scrollSlotsJuego;
    private Button botonEnviarJuego, botonDisyuncionJuego;
    private ImageButton botonReciclajeJuego;
    private final List<String> pictos = new ArrayList<>();
    private final List<PhraseRecord> frases = new ArrayList<>();
    /** Frases marcadas para PDF; el orden de selección define la secuencia 1..N. */
    private final List<PhraseRecord> frasesSeleccionadas = new ArrayList<>();
    private final List<PhraseRecord.Item> borrador = new ArrayList<>();
    private int solapaActual = SOLAPA_JUEGO;
    private int frasesDescartadasAlCargar;
    /** Prefijo elegido en el teclado predictivo de pictogramas. */
    private String filtroTeclado = "";
    /** Prefijo del teclado predictivo en la solapa Juego. */
    private String filtroTecladoJuego = "";
    /** Catálogo de pictos buscables, precalculado al iniciar. */
    private final List<String> catalogoBuscable = new ArrayList<>();
    /** Nombres visibles cacheados por archivo. */
    private final Map<String, String> nombresCache = new HashMap<>();
    /** Miniaturas decodificadas para evitar releer assets en cada tecla. */
    private LruCache<String, Bitmap> cacheMiniaturas;
    private LinearLayout botonMicroCaja;
    private ImageButton botonMicro;
    private TextView textoCountdownMicro;
    private SpeechRecognizer reconocedorVoz;
    private Intent intentEscucha;
    private boolean escuchandoVoz;
    private boolean bloqueoUiAplicado;
    private AnimationDrawable animacionMicro;
    private final List<PhraseRecord.Item> seleccionJuego = new ArrayList<>();
    private ToneGenerator sonidoJuego;
    private MediaPlayer sonidoDisyuncion;
    /** Evita cerrar/sonar dos veces si el corte dispara varios callbacks. */
    private boolean avisoCorteEscuchaEmitido;
    private int volumenNotificacionPrevio = -1;
    private int volumenSistemaPrevio = -1;
    /** Último texto parcial del segmento actual. */
    private String textoParcialEscucha;
    /** Texto reunido durante toda la ventana de 20 s (varios segmentos del motor). */
    private String textoAcumuladoEscucha = "";
    private boolean corteIntencionalEscucha;
    private boolean procesamientoEscuchaHecho;
    private Runnable fallbackProcesarEscucha;
    private final Handler handlerEscucha = new Handler(Looper.getMainLooper());
    private Runnable countdownEscucha;
    private int segundosRestantesEscucha;
    private View overlayConstruyendo;
    private View overlayDisyuncion;
    private boolean eleccionDisyuncionHecha;
    private boolean animandoEleccionJuego;
    private FrameLayout overlayVistaPrevia;
    private final List<CoincidenciaPicto> colaVozJuego = new ArrayList<>();
    private int totalColaVozJuego;
    private ImageButton botonMicroJuego;
    private SpeechRecognizer reconocedorPictos;
    private boolean escuchandoPictos;
    private String textoParcialPictos;
    private AnimationDrawable animacionMicroPictos;
    private final Runnable cortarEscuchaPictosRunnable = this::detenerEscuchaPictos;
    private final Runnable fallbackEscuchaPictosRunnable = () -> finalizarEscuchaPictos(null);
    /** Archivo de catálogo → archivo real en la carpeta local (pictos propios). */
    private final Map<String, File> pictosLocales = new LinkedHashMap<>();
    private boolean modoEdicion;
    private LinearLayout panelEdicion;
    private LinearLayout listaPictosEdicion;
    private ScrollView scrollEdicion;
    private LinearLayout filaControlesEdicion;
    private LinearLayout cajaPendienteEdicion;
    private LinearLayout cajaAccionesFotoEdicion;
    private LinearLayout cajaRecorteEdicion;
    private RecorteFotoVista vistaRecorteEdicion;
    private ImageView imagenPendienteEdicion;
    private ImageView imagenPreviewEdicion;
    private EditText nombrePendienteEdicion;
    private Button botonOkPendienteEdicion;
    private ImageButton botonCamaraEdicion;
    private LinearLayout botonMicroEdicionCaja;
    private ImageButton botonMicroEdicion;
    private TextView textoCountdownNombre;
    private Bitmap bitmapPendienteEdicion;
    private Uri uriFotoCamara;
    private Uri uriFotoRecorte;
    private SpeechRecognizer reconocedorNombre;
    private boolean escuchandoNombre;
    private AnimationDrawable animacionMicroNombre;
    private Runnable countdownNombre;
    private int segundosRestantesNombre;
    private final Runnable entrarModoEdicionRunnable = this::entrarModoEdicion;
    /** Gestión de audios por picto (acceso con "!" 5 s dentro de modo edición). */
    private boolean modoAudioPictos;
    private LinearLayout panelAudioPictos;
    private LinearLayout listaAudioPictos;
    private Button botonAccesoAudio;
    private int filtroAudioPictos = FILTRO_AUDIO_TODOS;
    private MediaPlayer playerAudioPicto;
    private MediaRecorder grabadorAudio;
    private String pictoPendienteGrabar;
    private String pictoGrabando;
    private AlertDialog dialogoGrabacion;
    private Runnable countdownGrabacion;
    private int segundosRestantesGrabacion;
    private final Runnable entrarGestionAudioRunnable = this::entrarGestionAudio;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); cargarDatos(); crearVista(); mostrarJuego(); ocultarBarras();
        if (frasesDescartadasAlCargar > 0) {
            if (MOSTRAR_PALABRAS_Y_FRASES)
                Toast.makeText(this, "Se quitaron " + frasesDescartadasAlCargar + " frases con pictogramas que ya no existen.", Toast.LENGTH_LONG).show();
            guardarRespaldo();
        }
        if (MOSTRAR_PALABRAS_Y_FRASES && esInstalacionNueva() && frases.isEmpty()) contenido.post(this::ofrecerRestauracion);
    }
    @Override protected void onResume() { super.onResume(); ocultarBarras(); }
    @Override protected void onPause() {
        cancelarCountdownEscucha();
        cancelarFallbackProcesarEscucha();
        restaurarBeepsReconocedor();
        if (escuchandoVoz && reconocedorVoz != null) {
            corteIntencionalEscucha = true;
            escuchandoVoz = false;
            actualizarIconoMicro(false);
            reconocedorVoz.cancel();
        }
        if (escuchandoNombre) detenerEscuchaNombre();
        cancelarEscuchaPictos();
        cancelarCountdownNombre();
        detenerGrabacionAudio(false);
        detenerReproduccionAudioPicto();
        handlerEscucha.removeCallbacks(entrarModoEdicionRunnable);
        handlerEscucha.removeCallbacks(entrarGestionAudioRunnable);
        super.onPause();
    }
    @Override protected void onDestroy() {
        cancelarCountdownEscucha();
        cancelarFallbackProcesarEscucha();
        restaurarBeepsReconocedor();
        ocultarOverlayConstruyendo();
        cerrarDisyuncion(false);
        detenerGrabacionAudio(false);
        detenerReproduccionAudioPicto();
        if (dialogoGrabacion != null) { dialogoGrabacion.dismiss(); dialogoGrabacion = null; }
        if (reconocedorVoz != null) { reconocedorVoz.destroy(); reconocedorVoz = null; }
        if (reconocedorNombre != null) { reconocedorNombre.destroy(); reconocedorNombre = null; }
        cancelarEscuchaPictos();
        if (reconocedorPictos != null) { reconocedorPictos.destroy(); reconocedorPictos = null; }
        if (sonidoJuego != null) { sonidoJuego.release(); sonidoJuego = null; }
        if (sonidoDisyuncion != null) { sonidoDisyuncion.release(); sonidoDisyuncion = null; }
        handlerEscucha.removeCallbacks(entrarModoEdicionRunnable);
        handlerEscucha.removeCallbacks(entrarGestionAudioRunnable);
        super.onDestroy();
    }
    @Override public void onWindowFocusChanged(boolean foco) { super.onWindowFocusChanged(foco); if (foco) ocultarBarras(); }

    private void cargarDatos() {
        int cacheBytes = (int) (Runtime.getRuntime().maxMemory() / 8);
        cacheMiniaturas = new LruCache<String, Bitmap>(cacheBytes) {
            @Override protected int sizeOf(String clave, Bitmap valor) { return valor.getByteCount(); }
        };
        cargarCatalogoPictos();
        List<PhraseRecord> cargadas = PhraseStore.cargar(this);
        List<PhraseRecord> validas = validarFrases(cargadas);
        frasesDescartadasAlCargar = cargadas.size() - validas.size();
        frases.addAll(validas);
        if (frasesDescartadasAlCargar > 0) PhraseStore.guardar(this, frases);
        restaurarSeleccionPdf();
    }

    /** Assets de base + carpeta local, con sufijo (n) si el nombre ya existe. */
    private void cargarCatalogoPictos() {
        pictos.clear();
        catalogoBuscable.clear();
        nombresCache.clear();
        pictosLocales.clear();
        if (cacheMiniaturas != null) cacheMiniaturas.evictAll();

        List<String> deAssets = new ArrayList<>();
        try {
            String[] nombres = getAssets().list(DIR);
            if (nombres != null) deAssets.addAll(Arrays.asList(nombres));
        } catch (IOException ignored) { }

        pictosLocales.putAll(PictosLocales.unificarConAssets(deAssets, PictosLocales.listar(this)));
        pictos.addAll(deAssets);
        pictos.addAll(pictosLocales.keySet());
        Collections.sort(pictos, String.CASE_INSENSITIVE_ORDER);
        for (String archivo : pictos) {
            if (!esCaptura(archivo)) catalogoBuscable.add(archivo);
            nombresCache.put(archivo, calcularNombre(archivo));
        }
        Collections.sort(catalogoBuscable, String.CASE_INSENSITIVE_ORDER);
        precargarMiniaturas();
    }

    /** Recupera las frases marcadas para PDF emparejando por contenido. */
    private void restaurarSeleccionPdf() {
        frasesSeleccionadas.clear();
        List<PhraseRecord> guardadas = PhraseStore.cargarSeleccionPdf(this);
        for (PhraseRecord sel : guardadas) {
            if (frasesSeleccionadas.size() >= MAX_FRASES_PDF) break;
            for (PhraseRecord f : frases) {
                if (frasesSeleccionadas.contains(f)) continue;
                if (mismosItems(f.items, sel.items)) {
                    frasesSeleccionadas.add(f);
                    break;
                }
            }
        }
    }

    private void persistirSeleccionPdf() {
        PhraseStore.guardarSeleccionPdf(this, frasesSeleccionadas);
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
        tabPalabras = tab("Palabras", v -> { if (!escuchandoVoz) mostrarPalabras(); });
        tabFrases = tab("Frases", v -> { if (!escuchandoVoz) mostrarFrases(); });
        tabJuego = tab("Pictos", v -> { if (!escuchandoVoz) mostrarJuego(); });
        tabs.addView(tabPalabras, peso(1,-2,dp(2))); tabs.addView(tabFrases, peso(1,-2,dp(2)));
        tabs.addView(tabJuego, peso(1,-2,dp(2)));
        if (!MOSTRAR_PALABRAS_Y_FRASES) {
            tabPalabras.setVisibility(View.GONE);
            tabFrases.setVisibility(View.GONE);
        }
        tabEditar = tabIcono(android.R.drawable.ic_menu_edit, "Editar", null);
        configurarPulsacionLargaEditar();
        salir = tabIcono(android.R.drawable.ic_menu_close_clear_cancel, "Salir de la aplicación", v -> finishAffinity());
        tabs.addView(tabEditar, fijo(dp(48), dp(48), 0));
        tabs.addView(salir, fijo(dp(48), dp(48), 0));
        contenido.addView(tabs);
        scrollLista = new ScrollView(this); lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); lista.setPadding(dp(10),dp(4),dp(10),dp(4)); scrollLista.addView(lista); contenido.addView(scrollLista, expandirEnVertical());
        juegoPanel = new LinearLayout(this); juegoPanel.setOrientation(LinearLayout.VERTICAL); juegoPanel.setVisibility(View.GONE);
        tecladoJuegoContenedor = new LinearLayout(this);
        tecladoJuegoContenedor.setOrientation(LinearLayout.VERTICAL);
        juegoPanel.addView(tecladoJuegoContenedor, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scrollMosaico = new ScrollView(this);
        scrollMosaico.setVerticalScrollBarEnabled(true);
        scrollMosaico.setFillViewport(true);
        mosaicoJuego = new GridLayout(this); mosaicoJuego.setPadding(dp(6), dp(4), dp(6), dp(4));
        scrollMosaico.addView(mosaicoJuego, new FrameLayout.LayoutParams(-1, -2));
        juegoPanel.addView(scrollMosaico, expandirEnVertical());
        tabEleccionJuego = new LinearLayout(this); tabEleccionJuego.setOrientation(LinearLayout.VERTICAL);
        tabEleccionJuego.setPadding(dp(8), dp(10), dp(8), dp(8)); tabEleccionJuego.setBackground(fondoPanel());
        slotsEleccionJuego = new LinearLayout(this); slotsEleccionJuego.setOrientation(LinearLayout.HORIZONTAL);
        slotsEleccionJuego.setGravity(Gravity.CENTER_VERTICAL);
        scrollSlotsJuego = new HorizontalScrollView(this);
        scrollSlotsJuego.setHorizontalScrollBarEnabled(true);
        scrollSlotsJuego.setScrollbarFadingEnabled(false);
        scrollSlotsJuego.addView(slotsEleccionJuego, new FrameLayout.LayoutParams(-2, -2));
        tabEleccionJuego.addView(scrollSlotsJuego, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout controlesJuego = new LinearLayout(this); controlesJuego.setOrientation(LinearLayout.HORIZONTAL);
        controlesJuego.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams controlesParams = new LinearLayout.LayoutParams(-1, -2);
        controlesParams.topMargin = dp(6);
        int tamBoton = dp(40);
        botonDisyuncionJuego = tecla("Disyunción", TECLA_NORMAL, v -> ejecutarDisyuncionJuego());
        botonDisyuncionJuego.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        botonDisyuncionJuego.setContentDescription("Elegir entre los dos pictogramas");
        botonDisyuncionJuego.setVisibility(View.GONE);
        LinearLayout.LayoutParams disParams = new LinearLayout.LayoutParams(dp(92), tamBoton);
        disParams.rightMargin = dp(4);
        controlesJuego.addView(botonDisyuncionJuego, disParams);
        botonMicroJuego = new ImageButton(this);
        botonMicroJuego.setBackground(fondoTecla(TECLA_NORMAL));
        botonMicroJuego.setScaleType(ImageView.ScaleType.FIT_CENTER);
        botonMicroJuego.setPadding(dp(4), dp(4), dp(4), dp(4));
        botonMicroJuego.setOnClickListener(v -> alternarMicroPictos());
        actualizarIconoMicroPictos(false);
        LinearLayout.LayoutParams microParams = new LinearLayout.LayoutParams(tamBoton, tamBoton);
        microParams.rightMargin = dp(4);
        controlesJuego.addView(botonMicroJuego, microParams);
        botonEnviarJuego = tecla("", TECLA_NORMAL, v -> enviarJuego());
        botonEnviarJuego.setText(etiquetaPlay());
        botonEnviarJuego.setContentDescription("Reproducir frase");
        controlesJuego.addView(botonEnviarJuego, peso(1, tamBoton, dp(4)));
        botonReciclajeJuego = new ImageButton(this);
        botonReciclajeJuego.setImageResource(android.R.drawable.ic_menu_delete);
        botonReciclajeJuego.setContentDescription("Quitar último pictograma");
        botonReciclajeJuego.setBackground(fondoTecla(TECLA_NORMAL));
        botonReciclajeJuego.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        botonReciclajeJuego.setPadding(dp(6), dp(6), dp(6), dp(6));
        botonReciclajeJuego.setOnClickListener(v -> quitarUltimoJuego());
        controlesJuego.addView(botonReciclajeJuego, fijo(tamBoton, tamBoton, 0));
        tabEleccionJuego.addView(controlesJuego, controlesParams);
        tabEleccionJuego.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (solapaActual == SOLAPA_JUEGO && r - l != or - ol) actualizarTabEleccionJuego();
        });
        juegoPanel.addView(tabEleccionJuego, new LinearLayout.LayoutParams(-1, -2));
        juegoPanel.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (solapaActual == SOLAPA_JUEGO && r - l != or - ol) {
                List<String> visibles = pictosVisiblesJuego();
                if (!visibles.isEmpty()) aplicarMosaicoJuego(visibles);
            }
        });
        contenido.addView(juegoPanel, expandirEnVertical());
        panelEdicion = crearPanelEdicion();
        panelEdicion.setVisibility(View.GONE);
        raiz.addView(panelEdicion, new FrameLayout.LayoutParams(-1, -1));
        panelAudioPictos = crearPanelAudioPictos();
        panelAudioPictos.setVisibility(View.GONE);
        raiz.addView(panelAudioPictos, new FrameLayout.LayoutParams(-1, -1));
        setContentView(raiz);
    }
    private Button tab(String texto, View.OnClickListener accion) {
        Button b = new Button(this);
        b.setText(texto.toUpperCase(Locale.ROOT));
        b.setTextSize(13);
        b.setTextColor(0xff263238);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(2), dp(6), dp(2), dp(6));
        b.setOnClickListener(accion);
        return b;
    }

    private ImageButton tabIcono(int icono, String descripcion, View.OnClickListener accion) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(icono);
        b.setContentDescription(descripcion);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        if (accion != null) b.setOnClickListener(accion);
        return b;
    }

    private void mostrarPalabras() {
        solapaActual = SOLAPA_PALABRAS; actualizarTabs();
        scrollLista.setVisibility(View.VISIBLE); juegoPanel.setVisibility(View.GONE);
        lista.removeAllViews();
        int indice=0;
        // Las tarjetas ya elegidas siempre se muestran primero, incluso si la misma se eligió más de una vez.
        for(PhraseRecord.Item item:borrador) lista.addView(filaPalabra(item, indice++));
        agregarTecladoYCandidatos();
        scrollLista.scrollTo(0, 0);
        bloqueoUiAplicado = false;
        actualizarModoEscucha();
    }

    /** Actualiza solo teclado y candidatos, sin reconstruir las tarjetas ya elegidas. */
    private void actualizarTrasTecla() {
        if (escuchandoVoz) return;
        int indiceTeclado = borrador.size();
        while (lista.getChildCount() > indiceTeclado) lista.removeViewAt(indiceTeclado);
        agregarTecladoYCandidatos();
    }

    private void agregarTecladoYCandidatos() {
        lista.addView(botonPlayFrase());
        lista.addView(tecladoPredictivo(catalogoBuscable));
        if (!filtroTeclado.isEmpty()) {
            int candidato = 0;
            for (String archivo : coincidenciasTeclado()) lista.addView(filaCandidato(archivo, candidato++));
        }
    }

    /** Micrófono (con countdown) + PLAY más chico. */
    private View botonPlayFrase() {
        int altoBoton = dp(40);
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams filaParams = new LinearLayout.LayoutParams(-1, altoBoton);
        filaParams.setMargins(0, dp(3), 0, dp(3));
        fila.setLayoutParams(filaParams);

        botonMicroCaja = new LinearLayout(this);
        botonMicroCaja.setOrientation(LinearLayout.HORIZONTAL);
        botonMicroCaja.setGravity(Gravity.CENTER);
        botonMicroCaja.setBackground(fondoTecla(TECLA_NORMAL));
        botonMicroCaja.setPadding(dp(6), 0, dp(8), 0);
        botonMicroCaja.setOnClickListener(v -> alternarMicro());

        botonMicro = new ImageButton(this);
        botonMicro.setBackgroundColor(Color.TRANSPARENT);
        botonMicro.setScaleType(ImageView.ScaleType.FIT_CENTER);
        botonMicro.setPadding(dp(4), dp(6), dp(4), dp(6));
        botonMicro.setClickable(false);
        botonMicro.setFocusable(false);
        botonMicroCaja.addView(botonMicro, fijo(dp(32), altoBoton, 0));

        textoCountdownMicro = new TextView(this);
        textoCountdownMicro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textoCountdownMicro.setTypeface(Typeface.DEFAULT_BOLD);
        textoCountdownMicro.setTextColor(0xff263238);
        textoCountdownMicro.setGravity(Gravity.CENTER);
        textoCountdownMicro.setMinWidth(dp(28));
        textoCountdownMicro.setVisibility(View.GONE);
        botonMicroCaja.addView(textoCountdownMicro, new LinearLayout.LayoutParams(-2, -1));

        actualizarIconoMicro(escuchandoVoz);
        // Ancho fijo amplio para el countdown; PLAY queda más chico.
        fila.addView(botonMicroCaja, fijo(dp(108), altoBoton, dp(4)));

        Button play = tecla("", TECLA_NORMAL, v -> { if (!escuchandoVoz) enviar(); });
        play.setEnabled(true);
        play.setAlpha(1f);
        play.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        play.setText(etiquetaPlay());
        fila.addView(play, peso(1, altoBoton, 0));
        return fila;
    }

    private CharSequence etiquetaPlay() {
        int ladoIcono = dp(14);
        Drawable iconoPlay = getResources().getDrawable(android.R.drawable.ic_media_play, getTheme()).mutate();
        iconoPlay.setTint(0xff263238);
        iconoPlay.setBounds(0, 0, ladoIcono, ladoIcono);
        SpannableStringBuilder etiqueta = new SpannableStringBuilder("PLAY ");
        int inicioIcono = etiqueta.length();
        etiqueta.append('\uFFFC');
        etiqueta.setSpan(new ImageSpan(iconoPlay, ImageSpan.ALIGN_CENTER), inicioIcono, etiqueta.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return etiqueta;
    }

    private void alternarMicro() {
        if (escuchandoVoz) { detenerEscuchaYEnviar(); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconocimiento de voz no disponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISO_MICRO);
            return;
        }
        iniciarEscucha();
    }

    private void asegurarReconocedor() {
        if (reconocedorVoz != null) return;
        reconocedorVoz = SpeechRecognizer.createSpeechRecognizer(this);
        reconocedorVoz.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) {
                runOnUiThread(() -> {
                    if (!escuchandoVoz || botonMicro == null) return;
                    float pulso = Math.max(0f, rmsdB) / 12f;
                    float escala = 1f + pulso * 0.18f;
                    botonMicro.setScaleX(escala);
                    botonMicro.setScaleY(escala);
                });
            }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() {
                // No cortar acá: el silencio del motor no debe enviar la oración antes de los 20 s.
            }
            @Override public void onPartialResults(Bundle partialResults) {
                String parcial = primerResultado(partialResults);
                if (parcial != null && !parcial.isEmpty()) textoParcialEscucha = parcial;
            }
            @Override public void onEvent(int eventType, Bundle params) { }
            @Override public void onError(int error) {
                runOnUiThread(() -> onErrorEscucha(error));
            }
            @Override public void onResults(Bundle results) {
                String texto = primerResultado(results);
                if ((texto == null || texto.isEmpty()) && textoParcialEscucha != null) {
                    texto = textoParcialEscucha;
                }
                textoParcialEscucha = null;
                String segmento = texto;
                runOnUiThread(() -> onResultadosEscucha(segmento));
            }
        });
    }

    private static String primerResultado(Bundle bundle) {
        if (bundle == null) return null;
        ArrayList<String> coincidencias = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (coincidencias == null || coincidencias.isEmpty()) return null;
        return coincidencias.get(0);
    }

    private void iniciarEscucha() {
        asegurarReconocedor();
        intentEscucha = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentEscucha.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intentEscucha.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR");
        intentEscucha.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intentEscucha.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        // Pedir al motor que no corte por silencio dentro de la ventana de 20 s (puede ignorarlo).
        intentEscucha.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000);
        intentEscucha.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000);
        intentEscucha.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000);
        avisoCorteEscuchaEmitido = false;
        corteIntencionalEscucha = false;
        procesamientoEscuchaHecho = false;
        textoParcialEscucha = null;
        textoAcumuladoEscucha = "";
        cancelarFallbackProcesarEscucha();
        silenciarBeepsReconocedor();
        escuchandoVoz = true;
        actualizarIconoMicro(true);
        iniciarCountdownEscucha();
        reconocedorVoz.startListening(intentEscucha);
    }

    private void onResultadosEscucha(String segmento) {
        acumularSegmentoEscucha(segmento);
        if (corteIntencionalEscucha || avisoCorteEscuchaEmitido) {
            finalizarProcesamientoEscucha();
            return;
        }
        // Sigue la ventana de 20 s: no construir aún; volver a escuchar.
        if (escuchandoVoz && segundosRestantesEscucha > 0) {
            reiniciarEscuchaContinua();
        }
    }

    private void onErrorEscucha(int error) {
        if (corteIntencionalEscucha || avisoCorteEscuchaEmitido) {
            finalizarProcesamientoEscucha();
            return;
        }
        // Silencio / timeout del motor a mitad de los 20 s → seguir grabando.
        if (escuchandoVoz && segundosRestantesEscucha > 0) {
            reiniciarEscuchaContinua();
            return;
        }
        finalizarProcesamientoEscucha();
    }

    private void reiniciarEscuchaContinua() {
        if (reconocedorVoz == null || intentEscucha == null) return;
        if (!escuchandoVoz || corteIntencionalEscucha || avisoCorteEscuchaEmitido) return;
        try {
            reconocedorVoz.startListening(intentEscucha);
        } catch (RuntimeException ignored) {
            handlerEscucha.postDelayed(() -> {
                if (!escuchandoVoz || corteIntencionalEscucha || avisoCorteEscuchaEmitido) return;
                try {
                    reconocedorVoz.startListening(intentEscucha);
                } catch (RuntimeException ignored2) { }
            }, 250);
        }
    }

    private void acumularSegmentoEscucha(String segmento) {
        if (segmento == null) return;
        segmento = segmento.trim();
        if (segmento.isEmpty()) return;
        if (textoAcumuladoEscucha.isEmpty()) {
            textoAcumuladoEscucha = segmento;
            return;
        }
        String acc = textoAcumuladoEscucha;
        if (segmento.startsWith(acc) || segmento.contains(acc)) {
            textoAcumuladoEscucha = segmento;
        } else if (!acc.contains(segmento)) {
            textoAcumuladoEscucha = acc + " " + segmento;
        }
    }

    /** Usuario toca el mic o se agotan los 20 s: corta y recién ahí construye. */
    private void detenerEscuchaYEnviar() {
        if (reconocedorVoz == null || !escuchandoVoz) return;
        corteIntencionalEscucha = true;
        cancelarCountdownEscucha();
        marcarFinEscuchaVisual();
        try {
            reconocedorVoz.stopListening();
        } catch (RuntimeException ignored) { }
        // Por si el motor no devuelve onResults tras stopListening.
        cancelarFallbackProcesarEscucha();
        fallbackProcesarEscucha = this::finalizarProcesamientoEscucha;
        handlerEscucha.postDelayed(fallbackProcesarEscucha, 1200);
    }

    private void finalizarProcesamientoEscucha() {
        if (procesamientoEscuchaHecho) return;
        procesamientoEscuchaHecho = true;
        cancelarFallbackProcesarEscucha();
        if (!avisoCorteEscuchaEmitido) marcarFinEscuchaVisual();
        String texto = textoAcumuladoEscucha == null ? "" : textoAcumuladoEscucha.trim();
        if (texto.isEmpty() && textoParcialEscucha != null) texto = textoParcialEscucha.trim();
        textoParcialEscucha = null;
        textoAcumuladoEscucha = "";
        if (!texto.isEmpty()) {
            procesarTextoEscuchadoConOverlay(texto);
        } else {
            borrador.clear();
            filtroTeclado = "";
            mostrarPalabras();
        }
    }

    private void cancelarFallbackProcesarEscucha() {
        if (fallbackProcesarEscucha != null) {
            handlerEscucha.removeCallbacks(fallbackProcesarEscucha);
            fallbackProcesarEscucha = null;
        }
    }

    private void iniciarCountdownEscucha() {
        cancelarCountdownEscucha();
        segundosRestantesEscucha = SEGUNDOS_MAX_ESCUCHA;
        actualizarTextoCountdown();
        countdownEscucha = new Runnable() {
            @Override public void run() {
                if (!escuchandoVoz) return;
                segundosRestantesEscucha--;
                if (segundosRestantesEscucha <= 0) {
                    actualizarTextoCountdown();
                    detenerEscuchaYEnviar();
                    return;
                }
                actualizarTextoCountdown();
                handlerEscucha.postDelayed(this, 1000);
            }
        };
        handlerEscucha.postDelayed(countdownEscucha, 1000);
    }

    private void cancelarCountdownEscucha() {
        if (countdownEscucha != null) {
            handlerEscucha.removeCallbacks(countdownEscucha);
            countdownEscucha = null;
        }
    }

    private void actualizarTextoCountdown() {
        if (textoCountdownMicro == null) return;
        if (escuchandoVoz) {
            textoCountdownMicro.setVisibility(View.VISIBLE);
            textoCountdownMicro.setText(String.valueOf(Math.max(0, segundosRestantesEscucha)));
        } else {
            textoCountdownMicro.setVisibility(View.GONE);
        }
    }

    /** Cierra UI de escucha (sin sonidos). */
    private void marcarFinEscuchaVisual() {
        cancelarCountdownEscucha();
        restaurarBeepsReconocedor();
        if (avisoCorteEscuchaEmitido) {
            escuchandoVoz = false;
            return;
        }
        avisoCorteEscuchaEmitido = true;
        escuchandoVoz = false;
        actualizarIconoMicro(false);
    }

    /** Evita el beep del sistema al iniciar/cortar el reconocimiento. */
    private void silenciarBeepsReconocedor() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        try {
            if (volumenNotificacionPrevio < 0) {
                volumenNotificacionPrevio = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            }
            if (volumenSistemaPrevio < 0) {
                volumenSistemaPrevio = am.getStreamVolume(AudioManager.STREAM_SYSTEM);
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
            }
        } catch (RuntimeException ignored) { }
    }

    private void restaurarBeepsReconocedor() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) {
            volumenNotificacionPrevio = -1;
            volumenSistemaPrevio = -1;
            return;
        }
        try {
            if (volumenNotificacionPrevio >= 0) {
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volumenNotificacionPrevio, 0);
                volumenNotificacionPrevio = -1;
            }
            if (volumenSistemaPrevio >= 0) {
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, volumenSistemaPrevio, 0);
                volumenSistemaPrevio = -1;
            }
        } catch (RuntimeException ignored) {
            volumenNotificacionPrevio = -1;
            volumenSistemaPrevio = -1;
        }
    }

    private void procesarTextoEscuchadoConOverlay(String texto) {
        mostrarOverlayConstruyendo();
        // post: deja pintar el overlay antes de armar pictos.
        handlerEscucha.post(() -> {
            try {
                aplicarTextoEscuchado(texto);
            } finally {
                ocultarOverlayConstruyendo();
            }
        });
    }

    private void mostrarOverlayConstruyendo() {
        if (overlayConstruyendo != null || contenido == null) return;
        FrameLayout raiz = (FrameLayout) contenido.getParent();
        if (raiz == null) return;
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        TextView mensaje = new TextView(this);
        mensaje.setText("Construyendo oración");
        mensaje.setTextColor(Color.WHITE);
        mensaje.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        mensaje.setTypeface(Typeface.DEFAULT_BOLD);
        mensaje.setGravity(Gravity.CENTER);
        mensaje.setPadding(dp(24), dp(16), dp(24), dp(16));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xee37474f);
        fondo.setCornerRadius(dp(14));
        mensaje.setBackground(fondo);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        overlay.addView(mensaje, lp);
        raiz.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        overlayConstruyendo = overlay;
    }

    private void ocultarOverlayConstruyendo() {
        if (overlayConstruyendo == null) return;
        ViewParent padre = overlayConstruyendo.getParent();
        if (padre instanceof ViewGroup) ((ViewGroup) padre).removeView(overlayConstruyendo);
        overlayConstruyendo = null;
    }

    private void actualizarIconoMicro(boolean escuchando) {
        if (botonMicro != null) {
            detenerAnimacionMicro();
            botonMicro.setScaleX(1f);
            botonMicro.setScaleY(1f);
            if (escuchando) {
                Drawable anim = getResources().getDrawable(R.drawable.anim_escucha, getTheme());
                botonMicro.setImageDrawable(anim);
                if (anim instanceof AnimationDrawable) {
                    animacionMicro = (AnimationDrawable) anim;
                    animacionMicro.start();
                }
            } else {
                Drawable icono = getResources().getDrawable(android.R.drawable.ic_btn_speak_now, getTheme()).mutate();
                icono.setTint(0xff263238);
                botonMicro.setImageDrawable(icono);
            }
        }
        if (botonMicroCaja != null) {
            botonMicroCaja.setBackground(fondoTecla(escuchando ? TECLA_PULSADA : TECLA_NORMAL));
            botonMicroCaja.setContentDescription(escuchando ? "Enviar audio" : "Dictar con voz");
        }
        actualizarTextoCountdown();
        actualizarModoEscucha();
    }

    /** Inhibe el resto de la interfaz mientras el micrófono escucha. */
    private void actualizarModoEscucha() {
        boolean bloqueado = escuchandoVoz;
        if (bloqueado) {
            tabPalabras.setEnabled(false);
            tabFrases.setEnabled(false);
            tabJuego.setEnabled(false);
            tabEditar.setEnabled(false);
            tabPalabras.setAlpha(0.35f);
            tabFrases.setAlpha(0.35f);
            tabJuego.setAlpha(0.35f);
            tabEditar.setAlpha(0.35f);
            salir.setEnabled(false);
            salir.setAlpha(0.35f);
        } else {
            actualizarTabs();
            salir.setEnabled(true);
            salir.setAlpha(1f);
        }
        if (scrollLista != null) scrollLista.setEnabled(!bloqueado);
        if (solapaActual != SOLAPA_PALABRAS || lista == null) return;
        if (bloqueado) {
            aplicarBloqueoLista(true);
            bloqueoUiAplicado = true;
        } else if (bloqueoUiAplicado) {
            bloqueoUiAplicado = false;
            restaurarInteraccionPalabras();
        }
    }

    private void aplicarBloqueoLista(boolean bloqueado) {
        int indicePlay = borrador.size();
        for (int i = 0; i < lista.getChildCount(); i++) {
            View hijo = lista.getChildAt(i);
            if (i < indicePlay) {
                inhibirVista(hijo, bloqueado);
            } else if (i == indicePlay && hijo instanceof ViewGroup) {
                ViewGroup fila = (ViewGroup) hijo;
                for (int j = 0; j < fila.getChildCount(); j++) {
                    View hijoFila = fila.getChildAt(j);
                    if (hijoFila == botonMicro || hijoFila == botonMicroCaja) continue;
                    inhibirVista(hijoFila, bloqueado);
                }
            } else {
                inhibirVista(hijo, bloqueado);
            }
        }
    }

    private void inhibirVista(View vista, boolean bloqueado) {
        vista.setEnabled(!bloqueado);
        vista.setAlpha(bloqueado ? 0.35f : 1f);
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) inhibirVista(grupo.getChildAt(i), bloqueado);
        }
    }

    private void restaurarInteraccionPalabras() {
        int indiceTeclado = borrador.size();
        for (int i = 0; i < indiceTeclado && i < lista.getChildCount(); i++) {
            View hijo = lista.getChildAt(i);
            hijo.setEnabled(true);
            hijo.setAlpha(1f);
            if (hijo instanceof ViewGroup) habilitarVista((ViewGroup) hijo);
        }
        while (lista.getChildCount() > indiceTeclado) lista.removeViewAt(indiceTeclado);
        agregarTecladoYCandidatos();
    }

    private void habilitarVista(ViewGroup grupo) {
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View hijo = grupo.getChildAt(i);
            hijo.setEnabled(true);
            hijo.setAlpha(1f);
            if (hijo instanceof ViewGroup) habilitarVista((ViewGroup) hijo);
        }
    }

    private void detenerAnimacionMicro() {
        if (animacionMicro != null) {
            animacionMicro.stop();
            animacionMicro = null;
        }
    }

    private void aplicarTextoEscuchado(String texto) {
        borrador.clear();
        List<CoincidenciaPicto> encontrados = AlgoritmoPictosFrase.secuenciaConAutomaticos(
                texto, catalogoBuscable, this::nombre);
        for (CoincidenciaPicto coincidencia : encontrados) {
            if (coincidencia.automatico) {
                String palabra = coincidencia.texto == null ? "" : coincidencia.texto.trim();
                if (palabra.isEmpty()) continue;
                if (!borrador.isEmpty()) {
                    PhraseRecord.Item ultimo = borrador.get(borrador.size() - 1);
                    if (ultimo.automatico) {
                        String juntado = (ultimo.etiqueta == null ? "" : ultimo.etiqueta) + " " + palabra;
                        borrador.set(borrador.size() - 1, PhraseRecord.Item.automatico(juntado.trim()));
                        continue;
                    }
                }
                borrador.add(PhraseRecord.Item.automatico(palabra));
                continue;
            }
            borrador.add(new PhraseRecord.Item(varianteMasUsada(coincidencia.archivo), coincidencia.negado));
        }
        filtroTeclado = "";
        mostrarPalabras();
    }
    private void mostrarFrases() {
        if (escuchandoVoz) return;
        solapaActual = SOLAPA_FRASES; actualizarTabs();
        scrollLista.setVisibility(View.VISIBLE); juegoPanel.setVisibility(View.GONE);
        lista.removeAllViews();
        limpiarSeleccionInvalida();
        if (frases.isEmpty()) {
            TextView v = new TextView(this);
            v.setText("Todavía no hay frases guardadas.");
            v.setTextSize(18);
            v.setGravity(Gravity.CENTER);
            v.setPadding(0, dp(35), 0, 0);
            lista.addView(v);
            return;
        }
        List<PhraseRecord> resto = new ArrayList<>();
        for (PhraseRecord f : frases) {
            if (!frasesSeleccionadas.contains(f)) resto.add(f);
        }
        int indice = 0;
        for (PhraseRecord f : frasesSeleccionadas) lista.addView(filaFrase(f, indice++, true));
        if (!frasesSeleccionadas.isEmpty()) lista.addView(filaBotonCrearPdf());
        for (PhraseRecord f : resto) lista.addView(filaFrase(f, indice++, false));
    }

    private void limpiarSeleccionInvalida() {
        int antes = frasesSeleccionadas.size();
        for (int i = frasesSeleccionadas.size() - 1; i >= 0; i--) {
            if (!frases.contains(frasesSeleccionadas.get(i))) frasesSeleccionadas.remove(i);
        }
        while (frasesSeleccionadas.size() > MAX_FRASES_PDF) {
            frasesSeleccionadas.remove(frasesSeleccionadas.size() - 1);
        }
        if (frasesSeleccionadas.size() != antes) persistirSeleccionPdf();
    }

    private View filaBotonCrearPdf() {
        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(Gravity.CENTER);
        fila.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(2), 0, dp(8));
        fila.setLayoutParams(fp);

        Button boton = new Button(this);
        boton.setText("Crear PDF");
        boton.setAllCaps(false);
        boton.setTextSize(14);
        boton.setPadding(dp(18), dp(6), dp(18), dp(6));
        boton.setMinHeight(0);
        boton.setMinimumHeight(0);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xff496f88);
        fondo.setCornerRadius(dp(12));
        boton.setBackground(fondo);
        boton.setTextColor(Color.WHITE);
        boton.setOnClickListener(v -> crearPdfFrasesSeleccionadas());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        fila.addView(boton, bp);
        return fila;
    }

    private void crearPdfFrasesSeleccionadas() {
        if (frasesSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Seleccioná al menos una frase.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FrasePdfExporter.exportar(this, new ArrayList<>(frasesSeleccionadas));
            Toast.makeText(this, "PDF guardado en Descargas / Malena Comunicador", Toast.LENGTH_LONG).show();
            Intent ver = new Intent(Intent.ACTION_VIEW);
            ver.setDataAndType(uri, "application/pdf");
            ver.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(ver);
            } catch (ActivityNotFoundException ignored) {
                Intent compartir = new Intent(Intent.ACTION_SEND);
                compartir.setType("application/pdf");
                compartir.putExtra(Intent.EXTRA_STREAM, uri);
                compartir.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(compartir, "Compartir PDF"));
            }
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo crear el PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void alternarSeleccionFrase(PhraseRecord frase, boolean seleccionar) {
        if (seleccionar) {
            if (frasesSeleccionadas.contains(frase)) return;
            if (frasesSeleccionadas.size() >= MAX_FRASES_PDF) {
                Toast.makeText(this, "Podés seleccionar hasta " + MAX_FRASES_PDF + " frases.", Toast.LENGTH_SHORT).show();
                mostrarFrases();
                return;
            }
            frasesSeleccionadas.add(frase);
        } else {
            frasesSeleccionadas.remove(frase);
        }
        persistirSeleccionPdf();
        mostrarFrases();
        scrollLista.post(() -> scrollLista.scrollTo(0, 0));
    }
    private void mostrarJuego() {
        if (escuchandoVoz) return;
        solapaActual = SOLAPA_JUEGO; actualizarTabs();
        scrollLista.setVisibility(View.GONE); juegoPanel.setVisibility(View.VISIBLE);
        while (seleccionJuego.size() > MAX_PICTOS_JUEGO) seleccionJuego.remove(seleccionJuego.size() - 1);
        construirVistaJuego();
    }

    private void actualizarTabs() {
        boolean enPalabras = solapaActual == SOLAPA_PALABRAS;
        boolean enFrases = solapaActual == SOLAPA_FRASES;
        boolean enJuego = solapaActual == SOLAPA_JUEGO;
        tabPalabras.setEnabled(!enPalabras); tabFrases.setEnabled(!enFrases); tabJuego.setEnabled(!enJuego);
        tabPalabras.setAlpha(enPalabras ? 1 : .72f); tabFrases.setAlpha(enFrases ? 1 : .72f); tabJuego.setAlpha(enJuego ? 1 : .72f);
        tabPalabras.setBackground(fondoSolapa(enPalabras)); tabFrases.setBackground(fondoSolapa(enFrases));
        tabJuego.setBackground(fondoSolapa(enJuego));
        tabEditar.setEnabled(true);
        tabEditar.setAlpha(.72f);
        tabEditar.setBackground(fondoSolapa(false));
        salir.setVisibility(View.VISIBLE);
        salir.setBackground(fondoSolapa(false));
        salir.setAlpha(1f);
    }

    private void configurarPulsacionLargaEditar() {
        tabEditar.setOnTouchListener((v, event) -> {
            if (escuchandoVoz || modoEdicion) return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    handlerEscucha.postDelayed(entrarModoEdicionRunnable, MS_MODO_EDICION);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handlerEscucha.removeCallbacks(entrarModoEdicionRunnable);
                    return true;
                default:
                    return true;
            }
        });
    }

    private LinearLayout crearPanelEdicion() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xfff7f8fa);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout cabecera = new LinearLayout(this);
        cabecera.setOrientation(LinearLayout.HORIZONTAL);
        cabecera.setGravity(Gravity.CENTER_VERTICAL);

        Button guardar = new Button(this);
        guardar.setText("Guardar y Salir");
        guardar.setTextSize(16);
        guardar.setAllCaps(false);
        guardar.setTextColor(0xff263238);
        guardar.setBackground(fondoSolapa(true));
        guardar.setOnClickListener(v -> guardarYSalirEdicion());
        cabecera.addView(guardar, new LinearLayout.LayoutParams(0, dp(52), 1));

        ImageButton exportarPictos = new ImageButton(this);
        exportarPictos.setImageResource(android.R.drawable.ic_menu_save);
        exportarPictos.setContentDescription("Exportar pictos propios a carpeta pública");
        exportarPictos.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        exportarPictos.setBackground(fondoSolapa(false));
        exportarPictos.setPadding(dp(8), dp(8), dp(8), dp(8));
        exportarPictos.setOnClickListener(v -> pedirExportarPictos());
        LinearLayout.LayoutParams exportPictosParams = fijo(dp(52), dp(52), 0);
        exportPictosParams.leftMargin = dp(6);
        cabecera.addView(exportarPictos, exportPictosParams);

        botonAccesoAudio = new Button(this);
        botonAccesoAudio.setText("!");
        botonAccesoAudio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        botonAccesoAudio.setTypeface(Typeface.DEFAULT_BOLD);
        botonAccesoAudio.setAllCaps(false);
        botonAccesoAudio.setTextColor(0xff718596);
        botonAccesoAudio.setContentDescription("Audios de pictogramas");
        botonAccesoAudio.setBackground(fondoSolapa(false));
        botonAccesoAudio.setOnClickListener(null);
        configurarPulsacionLargaAudio();
        LinearLayout.LayoutParams accesoParams = fijo(dp(44), dp(52), 0);
        accesoParams.leftMargin = dp(6);
        cabecera.addView(botonAccesoAudio, accesoParams);
        panel.addView(cabecera, new LinearLayout.LayoutParams(-1, -2));

        filaControlesEdicion = new LinearLayout(this);
        filaControlesEdicion.setOrientation(LinearLayout.HORIZONTAL);
        filaControlesEdicion.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams filaParams = new LinearLayout.LayoutParams(-1, -2);
        filaParams.topMargin = dp(8);
        filaControlesEdicion.setLayoutParams(filaParams);

        botonCamaraEdicion = new ImageButton(this);
        botonCamaraEdicion.setImageResource(android.R.drawable.ic_menu_camera);
        botonCamaraEdicion.setContentDescription("Tomar o buscar foto");
        botonCamaraEdicion.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        botonCamaraEdicion.setBackground(fondoTecla(TECLA_NORMAL));
        botonCamaraEdicion.setOnClickListener(v -> elegirOrigenFoto());
        filaControlesEdicion.addView(botonCamaraEdicion, fijo(dp(80), dp(80), dp(8)));

        imagenPendienteEdicion = new ImageView(this);
        imagenPendienteEdicion.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imagenPendienteEdicion.setAdjustViewBounds(true);
        imagenPendienteEdicion.setBackground(bordePicto());
        imagenPendienteEdicion.setPadding(dp(3), dp(3), dp(3), dp(3));
        imagenPendienteEdicion.setVisibility(View.GONE);
        filaControlesEdicion.addView(imagenPendienteEdicion, fijo(dp(80), dp(80), dp(8)));

        botonMicroEdicionCaja = new LinearLayout(this);
        botonMicroEdicionCaja.setOrientation(LinearLayout.HORIZONTAL);
        botonMicroEdicionCaja.setGravity(Gravity.CENTER);
        botonMicroEdicionCaja.setBackground(fondoTecla(TECLA_NORMAL));
        botonMicroEdicionCaja.setPadding(dp(6), 0, dp(8), 0);
        botonMicroEdicionCaja.setOnClickListener(v -> pedirEscuchaNombre());

        botonMicroEdicion = new ImageButton(this);
        botonMicroEdicion.setBackgroundColor(Color.TRANSPARENT);
        botonMicroEdicion.setScaleType(ImageView.ScaleType.FIT_CENTER);
        botonMicroEdicion.setPadding(dp(4), dp(6), dp(4), dp(6));
        botonMicroEdicion.setClickable(false);
        botonMicroEdicion.setFocusable(false);
        botonMicroEdicionCaja.addView(botonMicroEdicion, fijo(dp(32), dp(80), 0));

        textoCountdownNombre = new TextView(this);
        textoCountdownNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textoCountdownNombre.setTypeface(Typeface.DEFAULT_BOLD);
        textoCountdownNombre.setTextColor(0xff263238);
        textoCountdownNombre.setGravity(Gravity.CENTER);
        textoCountdownNombre.setMinWidth(dp(28));
        textoCountdownNombre.setVisibility(View.GONE);
        botonMicroEdicionCaja.addView(textoCountdownNombre, new LinearLayout.LayoutParams(-2, -1));
        filaControlesEdicion.addView(botonMicroEdicionCaja, fijo(dp(108), dp(80), dp(8)));
        panel.addView(filaControlesEdicion);
        habilitarMicroEdicion(false);
        actualizarIconoMicroEdicion(false);

        cajaPendienteEdicion = new LinearLayout(this);
        cajaPendienteEdicion.setOrientation(LinearLayout.VERTICAL);
        cajaPendienteEdicion.setPadding(dp(8), dp(8), dp(8), dp(8));
        cajaPendienteEdicion.setBackground(fondoPanel());
        cajaPendienteEdicion.setVisibility(View.GONE);
        LinearLayout.LayoutParams pendParams = new LinearLayout.LayoutParams(-1, -2);
        pendParams.topMargin = dp(8);
        cajaPendienteEdicion.setLayoutParams(pendParams);

        imagenPreviewEdicion = new ImageView(this);
        imagenPreviewEdicion.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imagenPreviewEdicion.setAdjustViewBounds(true);
        imagenPreviewEdicion.setBackground(bordePicto());
        imagenPreviewEdicion.setPadding(dp(4), dp(4), dp(4), dp(4));
        cajaPendienteEdicion.addView(imagenPreviewEdicion, new LinearLayout.LayoutParams(-1, dp(180)));

        cajaAccionesFotoEdicion = new LinearLayout(this);
        cajaAccionesFotoEdicion.setOrientation(LinearLayout.HORIZONTAL);
        cajaAccionesFotoEdicion.setGravity(Gravity.CENTER);
        Button recortar = tecla("Recortar", TECLA_NORMAL, v -> pedirRecorteFoto());
        Button aceptar = tecla("Aceptar foto", TECLA_NORMAL, v -> aceptarFotoPendiente());
        Button cancelar = tecla("Cancelar", TECLA_NORMAL, v -> cancelarProcesoFoto());
        cajaAccionesFotoEdicion.addView(recortar, peso(1, dp(40), dp(3)));
        cajaAccionesFotoEdicion.addView(aceptar, peso(1, dp(40), dp(3)));
        cajaAccionesFotoEdicion.addView(cancelar, peso(1, dp(40), dp(3)));
        cajaPendienteEdicion.addView(cajaAccionesFotoEdicion);

        nombrePendienteEdicion = new EditText(this);
        nombrePendienteEdicion.setHint("Nombre del pictograma");
        nombrePendienteEdicion.setSingleLine(true);
        nombrePendienteEdicion.setImeOptions(EditorInfo.IME_ACTION_DONE);
        nombrePendienteEdicion.setTextSize(18);
        nombrePendienteEdicion.setVisibility(View.GONE);
        LinearLayout.LayoutParams nomParams = new LinearLayout.LayoutParams(-1, -2);
        nomParams.topMargin = dp(8);
        cajaPendienteEdicion.addView(nombrePendienteEdicion, nomParams);

        botonOkPendienteEdicion = tecla("OK", TECLA_NORMAL, v -> confirmarPictoPendiente());
        botonOkPendienteEdicion.setVisibility(View.GONE);
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(-1, dp(44));
        okParams.topMargin = dp(8);
        cajaPendienteEdicion.addView(botonOkPendienteEdicion, okParams);
        panel.addView(cajaPendienteEdicion);

        cajaRecorteEdicion = new LinearLayout(this);
        cajaRecorteEdicion.setOrientation(LinearLayout.VERTICAL);
        cajaRecorteEdicion.setVisibility(View.GONE);
        LinearLayout.LayoutParams recParams = new LinearLayout.LayoutParams(-1, 0, 1);
        recParams.topMargin = dp(8);
        cajaRecorteEdicion.setLayoutParams(recParams);
        vistaRecorteEdicion = new RecorteFotoVista(this);
        cajaRecorteEdicion.addView(vistaRecorteEdicion, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout accionesRecorte = new LinearLayout(this);
        accionesRecorte.setOrientation(LinearLayout.HORIZONTAL);
        accionesRecorte.setGravity(Gravity.CENTER);
        Button listoRecorte = tecla("Listo", TECLA_NORMAL, v -> aplicarRecorteInterno());
        Button cancelarRecorte = tecla("Cancelar", TECLA_NORMAL, v -> cancelarRecorteInterno());
        accionesRecorte.addView(listoRecorte, peso(1, dp(40), dp(4)));
        accionesRecorte.addView(cancelarRecorte, peso(1, dp(40), dp(4)));
        LinearLayout.LayoutParams accRec = new LinearLayout.LayoutParams(-1, -2);
        accRec.topMargin = dp(6);
        cajaRecorteEdicion.addView(accionesRecorte, accRec);
        panel.addView(cajaRecorteEdicion);

        scrollEdicion = new ScrollView(this);
        listaPictosEdicion = new LinearLayout(this);
        listaPictosEdicion.setOrientation(LinearLayout.VERTICAL);
        scrollEdicion.addView(listaPictosEdicion);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(10);
        panel.addView(scrollEdicion, scrollParams);
        return panel;
    }

    private void entrarModoEdicion() {
        if (escuchandoVoz || modoEdicion) return;
        modoEdicion = true;
        cancelarEscuchaPictos();
        colaVozJuego.clear();
        tabEditar.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        PictosLocales.carpeta(this);
        AudiosPictos.carpeta(this);
        ocultarTeclado();
        cancelarPendienteEdicion();
        mostrarListadoEdicion(true);
        mostrarControlesEdicion(true);
        habilitarMicroEdicion(false);
        refrescarListaEdicion();
        contenido.setVisibility(View.GONE);
        if (panelAudioPictos != null) panelAudioPictos.setVisibility(View.GONE);
        modoAudioPictos = false;
        panelEdicion.setVisibility(View.VISIBLE);
    }

    private void guardarYSalirEdicion() {
        if (!modoEdicion) return;
        if (modoAudioPictos) salirGestionAudio(false);
        detenerEscuchaNombre();
        cancelarPendienteEdicion();
        ocultarTeclado();
        cargarCatalogoPictos();
        List<PhraseRecord> validas = validarFrases(new ArrayList<>(frases));
        if (validas.size() != frases.size()) {
            frases.clear();
            frases.addAll(validas);
            PhraseStore.guardar(this, frases);
            limpiarSeleccionInvalida();
            persistirSeleccionPdf();
        }
        modoEdicion = false;
        panelEdicion.setVisibility(View.GONE);
        if (panelAudioPictos != null) panelAudioPictos.setVisibility(View.GONE);
        contenido.setVisibility(View.VISIBLE);
        if (solapaActual == SOLAPA_FRASES) mostrarFrases();
        else if (solapaActual == SOLAPA_JUEGO) mostrarJuego();
        else mostrarPalabras();
    }

    private void configurarPulsacionLargaAudio() {
        if (botonAccesoAudio == null) return;
        botonAccesoAudio.setOnTouchListener((v, event) -> {
            if (!modoEdicion || modoAudioPictos || escuchandoNombre) return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    handlerEscucha.postDelayed(entrarGestionAudioRunnable, MS_ACCESO_AUDIO);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handlerEscucha.removeCallbacks(entrarGestionAudioRunnable);
                    return true;
                default:
                    return true;
            }
        });
    }

    private LinearLayout crearPanelAudioPictos() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xfff7f8fa);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout cabecera = new LinearLayout(this);
        cabecera.setOrientation(LinearLayout.HORIZONTAL);
        cabecera.setGravity(Gravity.CENTER_VERTICAL);

        Button volver = new Button(this);
        volver.setText("Volver");
        volver.setTextSize(16);
        volver.setAllCaps(false);
        volver.setTextColor(0xff263238);
        volver.setBackground(fondoSolapa(true));
        volver.setOnClickListener(v -> salirGestionAudio(true));
        cabecera.addView(volver, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView titulo = new TextView(this);
        titulo.setText("Audios");
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xff263238);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(dp(8), 0, dp(8), 0);
        cabecera.addView(titulo, new LinearLayout.LayoutParams(0, -2, 1));

        ImageButton exportar = new ImageButton(this);
        exportar.setImageResource(android.R.drawable.ic_menu_save);
        exportar.setContentDescription("Exportar audios a carpeta pública");
        exportar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        exportar.setBackground(fondoSolapa(false));
        exportar.setPadding(dp(8), dp(8), dp(8), dp(8));
        exportar.setOnClickListener(v -> pedirExportarAudios());
        LinearLayout.LayoutParams exportParams = fijo(dp(48), dp(48), 0);
        exportParams.leftMargin = dp(4);
        cabecera.addView(exportar, exportParams);
        panel.addView(cabecera, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout filtros = new LinearLayout(this);
        filtros.setOrientation(LinearLayout.HORIZONTAL);
        filtros.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams filtrosParams = new LinearLayout.LayoutParams(-1, -2);
        filtrosParams.topMargin = dp(8);
        filtrosParams.bottomMargin = dp(4);
        filtros.setLayoutParams(filtrosParams);

        Button fTodos = botonFiltroAudio("Todos", FILTRO_AUDIO_TODOS);
        Button fCon = botonFiltroAudio("Con audio", FILTRO_AUDIO_CON);
        Button fSin = botonFiltroAudio("Sin audio", FILTRO_AUDIO_SIN);
        filtros.addView(fTodos, peso(1, dp(40), dp(3)));
        filtros.addView(fCon, peso(1, dp(40), dp(3)));
        filtros.addView(fSin, peso(1, dp(40), dp(3)));
        panel.addView(filtros);
        panel.setTag(filtros);

        ScrollView scroll = new ScrollView(this);
        listaAudioPictos = new LinearLayout(this);
        listaAudioPictos.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listaAudioPictos);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(6);
        panel.addView(scroll, scrollParams);
        return panel;
    }

    private Button botonFiltroAudio(String texto, int filtro) {
        Button boton = new Button(this);
        boton.setText(texto);
        boton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        boton.setAllCaps(false);
        boton.setTextColor(0xff263238);
        boton.setBackground(fondoTecla(TECLA_NORMAL));
        boton.setTag("filtro_audio_" + filtro);
        boton.setOnClickListener(v -> aplicarFiltroAudio(filtro));
        return boton;
    }

    private void entrarGestionAudio() {
        if (!modoEdicion || modoAudioPictos) return;
        detenerEscuchaNombre();
        cancelarPendienteEdicion();
        ocultarTeclado();
        modoAudioPictos = true;
        if (botonAccesoAudio != null) {
            botonAccesoAudio.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        AudiosPictos.carpeta(this);
        filtroAudioPictos = FILTRO_AUDIO_TODOS;
        panelEdicion.setVisibility(View.GONE);
        panelAudioPictos.setVisibility(View.VISIBLE);
        actualizarBotonesFiltroAudio();
        refrescarListaAudioPictos();
    }

    private void salirGestionAudio(boolean volverAEdicion) {
        detenerGrabacionAudio(false);
        detenerReproduccionAudioPicto();
        if (dialogoGrabacion != null) {
            dialogoGrabacion.dismiss();
            dialogoGrabacion = null;
        }
        modoAudioPictos = false;
        if (panelAudioPictos != null) panelAudioPictos.setVisibility(View.GONE);
        if (volverAEdicion && modoEdicion) {
            mostrarListadoEdicion(true);
            mostrarControlesEdicion(true);
            refrescarListaEdicion();
            panelEdicion.setVisibility(View.VISIBLE);
        }
    }

    private void aplicarFiltroAudio(int filtro) {
        if (filtroAudioPictos == filtro) return;
        filtroAudioPictos = filtro;
        actualizarBotonesFiltroAudio();
        refrescarListaAudioPictos();
    }

    private void actualizarBotonesFiltroAudio() {
        if (panelAudioPictos == null) return;
        Object tag = panelAudioPictos.getTag();
        if (!(tag instanceof ViewGroup)) return;
        ViewGroup filtros = (ViewGroup) tag;
        for (int i = 0; i < filtros.getChildCount(); i++) {
            View hijo = filtros.getChildAt(i);
            if (!(hijo instanceof Button)) continue;
            Object t = hijo.getTag();
            boolean activo = ("filtro_audio_" + filtroAudioPictos).equals(t);
            ((Button) hijo).setBackground(fondoTecla(activo ? TECLA_PULSADA : TECLA_NORMAL));
            ((Button) hijo).setTextColor(activo ? Color.WHITE : 0xff263238);
        }
    }

    private void refrescarListaAudioPictos() {
        if (listaAudioPictos == null) return;
        listaAudioPictos.removeAllViews();
        List<String> visibles = pictosFiltradosAudio();
        if (visibles.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText(filtroAudioPictos == FILTRO_AUDIO_CON
                    ? "Ningún pictograma tiene audio todavía."
                    : filtroAudioPictos == FILTRO_AUDIO_SIN
                    ? "Todos los pictogramas ya tienen audio."
                    : "No hay pictogramas.");
            vacio.setTextSize(16);
            vacio.setGravity(Gravity.CENTER);
            vacio.setPadding(0, dp(24), 0, dp(8));
            listaAudioPictos.addView(vacio);
            return;
        }
        int indice = 0;
        for (String archivo : visibles) {
            listaAudioPictos.addView(tarjetaAudioPicto(archivo, indice++));
        }
    }

    private List<String> pictosFiltradosAudio() {
        List<String> r = new ArrayList<>();
        for (String archivo : pictos) {
            if (!AudiosPictos.esImagenCatalogo(archivo)) continue;
            boolean tiene = AudiosPictos.existe(this, archivo);
            if (filtroAudioPictos == FILTRO_AUDIO_CON && !tiene) continue;
            if (filtroAudioPictos == FILTRO_AUDIO_SIN && tiene) continue;
            r.add(archivo);
        }
        Collections.sort(r, String.CASE_INSENSITIVE_ORDER);
        return r;
    }

    private View tarjetaAudioPicto(String archivo, int indice) {
        LinearLayout fila = nuevaFila();
        fila.setBackground(fondoPalabra(indice, false));
        fila.setGravity(Gravity.CENTER_VERTICAL);

        ImageView imagen = new ImageView(this);
        imagen.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imagen.setAdjustViewBounds(true);
        imagen.setBackground(bordePicto());
        imagen.setPadding(dp(3), dp(3), dp(3), dp(3));
        Bitmap mini = decodificarPicto(archivo, TAM_MINIATURA_LISTA);
        if (mini != null) imagen.setImageBitmap(mini);
        fila.addView(imagen, fijo(dp(64), dp(64), dp(4)));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.setGravity(Gravity.CENTER_VERTICAL);
        TextView nombreArchivo = new TextView(this);
        nombreArchivo.setText(AudiosPictos.baseDePicto(archivo));
        nombreArchivo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nombreArchivo.setTypeface(Typeface.DEFAULT_BOLD);
        nombreArchivo.setTextColor(0xff263238);
        nombreArchivo.setMaxLines(2);
        textos.addView(nombreArchivo, new LinearLayout.LayoutParams(-1, -2));

        boolean tieneAudio = AudiosPictos.existe(this, archivo);
        TextView estado = new TextView(this);
        estado.setText(tieneAudio ? "Con audio" : "Sin audio");
        estado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        estado.setTextColor(tieneAudio ? 0xff2E7D32 : 0xff90A4AE);
        textos.addView(estado, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams textoParams = new LinearLayout.LayoutParams(0, -2, 1);
        textoParams.leftMargin = dp(4);
        textoParams.rightMargin = dp(4);
        fila.addView(textos, textoParams);

        ImageButton play = new ImageButton(this);
        Drawable iconoPlay = getResources().getDrawable(android.R.drawable.ic_media_play, getTheme()).mutate();
        iconoPlay.setTint(tieneAudio ? 0xff263238 : 0xffB0BEC5);
        play.setImageDrawable(iconoPlay);
        play.setContentDescription("Reproducir audio");
        play.setBackground(fondoTecla(TECLA_NORMAL));
        play.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        play.setEnabled(tieneAudio);
        play.setAlpha(tieneAudio ? 1f : 0.45f);
        play.setOnClickListener(v -> reproducirAudioPicto(archivo));
        fila.addView(play, fijo(dp(48), dp(48), dp(2)));

        ImageButton micro = new ImageButton(this);
        Drawable iconoMicro = getResources().getDrawable(android.R.drawable.ic_btn_speak_now, getTheme()).mutate();
        iconoMicro.setTint(0xff263238);
        micro.setImageDrawable(iconoMicro);
        micro.setContentDescription("Grabar audio");
        micro.setBackground(fondoTecla(TECLA_NORMAL));
        micro.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        micro.setOnClickListener(v -> pedirGrabacionAudio(archivo));
        fila.addView(micro, fijo(dp(48), dp(48), dp(2)));

        return fila;
    }

    private void reproducirAudioPicto(String archivoPicto) {
        File audio = AudiosPictos.archivoPara(this, archivoPicto);
        if (!audio.isFile()) {
            Toast.makeText(this, "Todavía no hay audio para este pictograma.", Toast.LENGTH_SHORT).show();
            return;
        }
        detenerReproduccionAudioPicto();
        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(audio.getAbsolutePath());
            player.setOnCompletionListener(mp -> {
                mp.release();
                if (playerAudioPicto == mp) playerAudioPicto = null;
            });
            player.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                if (playerAudioPicto == mp) playerAudioPicto = null;
                Toast.makeText(this, "No se pudo reproducir el audio.", Toast.LENGTH_SHORT).show();
                return true;
            });
            player.prepare();
            player.start();
            playerAudioPicto = player;
        } catch (IOException | RuntimeException e) {
            Toast.makeText(this, "No se pudo reproducir el audio.", Toast.LENGTH_SHORT).show();
        }
    }

    private void detenerReproduccionAudioPicto() {
        if (playerAudioPicto != null) {
            try { playerAudioPicto.stop(); } catch (RuntimeException ignored) { }
            try { playerAudioPicto.release(); } catch (RuntimeException ignored) { }
            playerAudioPicto = null;
        }
    }

    private void pedirGrabacionAudio(String archivoPicto) {
        if (archivoPicto == null || archivoPicto.isEmpty()) return;
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pictoPendienteGrabar = archivoPicto;
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISO_MICRO_AUDIO);
            return;
        }
        iniciarGrabacionAudio(archivoPicto);
    }

    private void iniciarGrabacionAudio(String archivoPicto) {
        detenerReproduccionAudioPicto();
        detenerGrabacionAudio(false);
        AudiosPictos.borrarTemp(this);
        File temp = AudiosPictos.tempGrabacion(this);
        try {
            MediaRecorder recorder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(this);
            } else {
                recorder = new MediaRecorder();
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(temp.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            grabadorAudio = recorder;
            pictoGrabando = archivoPicto;
            mostrarDialogoGrabacion(archivoPicto);
        } catch (IOException | RuntimeException e) {
            liberarGrabadorAudio();
            AudiosPictos.borrarTemp(this);
            Toast.makeText(this, "No se pudo iniciar la grabación.", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoGrabacion(String archivoPicto) {
        if (dialogoGrabacion != null) {
            dialogoGrabacion.dismiss();
            dialogoGrabacion = null;
        }
        segundosRestantesGrabacion = SEGUNDOS_MAX_GRABACION;
        final TextView mensaje = new TextView(this);
        mensaje.setText("Decí \"" + AudiosPictos.baseDePicto(archivoPicto) + "\"\n"
                + "Quedan " + segundosRestantesGrabacion + " s");
        mensaje.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mensaje.setPadding(dp(24), dp(16), dp(24), dp(8));
        mensaje.setGravity(Gravity.CENTER);

        dialogoGrabacion = new AlertDialog.Builder(this)
                .setTitle("Grabando audio")
                .setView(mensaje)
                .setCancelable(false)
                .setPositiveButton("Detener", (d, w) -> detenerGrabacionAudio(true))
                .setNegativeButton("Cancelar", (d, w) -> detenerGrabacionAudio(false))
                .create();
        dialogoGrabacion.setOnDismissListener(d -> {
            if (dialogoGrabacion == d) dialogoGrabacion = null;
        });
        dialogoGrabacion.show();

        cancelarCountdownGrabacion();
        countdownGrabacion = new Runnable() {
            @Override public void run() {
                if (grabadorAudio == null || pictoGrabando == null) return;
                segundosRestantesGrabacion--;
                if (segundosRestantesGrabacion <= 0) {
                    detenerGrabacionAudio(true);
                    return;
                }
                if (mensaje.getParent() != null) {
                    mensaje.setText("Decí \"" + AudiosPictos.baseDePicto(archivoPicto) + "\"\n"
                            + "Quedan " + segundosRestantesGrabacion + " s");
                }
                handlerEscucha.postDelayed(this, 1000);
            }
        };
        handlerEscucha.postDelayed(countdownGrabacion, 1000);
    }

    private void cancelarCountdownGrabacion() {
        if (countdownGrabacion != null) {
            handlerEscucha.removeCallbacks(countdownGrabacion);
            countdownGrabacion = null;
        }
    }

    private void detenerGrabacionAudio(boolean ofrecerGuardar) {
        cancelarCountdownGrabacion();
        String picto = pictoGrabando;
        boolean habiaGrabador = grabadorAudio != null;
        if (habiaGrabador) {
            try { grabadorAudio.stop(); } catch (RuntimeException ignored) { }
            liberarGrabadorAudio();
        }
        pictoGrabando = null;
        if (dialogoGrabacion != null) {
            AlertDialog d = dialogoGrabacion;
            dialogoGrabacion = null;
            d.dismiss();
        }
        if (!ofrecerGuardar || picto == null) {
            AudiosPictos.borrarTemp(this);
            return;
        }
        File temp = AudiosPictos.tempGrabacion(this);
        if (!temp.isFile() || temp.length() == 0) {
            AudiosPictos.borrarTemp(this);
            Toast.makeText(this, "No se grabó nada.", Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarConfirmacionAudio(picto);
    }

    private void liberarGrabadorAudio() {
        if (grabadorAudio != null) {
            try { grabadorAudio.reset(); } catch (RuntimeException ignored) { }
            try { grabadorAudio.release(); } catch (RuntimeException ignored) { }
            grabadorAudio = null;
        }
    }

    private void mostrarConfirmacionAudio(String archivoPicto) {
        new AlertDialog.Builder(this)
                .setTitle("¿Guardar audio?")
                .setMessage("Se guardará como " + AudiosPictos.nombreMp3(archivoPicto)
                        + (AudiosPictos.existe(this, archivoPicto) ? "\n(reemplaza el audio anterior)" : ""))
                .setPositiveButton("Guardar", (d, w) -> {
                    if (AudiosPictos.confirmarTemp(this, archivoPicto)) {
                        Toast.makeText(this, "Audio guardado.", Toast.LENGTH_SHORT).show();
                        refrescarListaAudioPictos();
                    } else {
                        AudiosPictos.borrarTemp(this);
                        Toast.makeText(this, "No se pudo guardar el audio.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Reintentar", (d, w) -> {
                    AudiosPictos.borrarTemp(this);
                    iniciarGrabacionAudio(archivoPicto);
                })
                .setNegativeButton("Descartar", (d, w) -> AudiosPictos.borrarTemp(this))
                .setOnCancelListener(d -> AudiosPictos.borrarTemp(this))
                .show();
    }

    private void pedirExportarAudios() {
        int cantidad = AudiosPictos.listar(this).size();
        if (cantidad == 0) {
            Toast.makeText(this, "No hay audios para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Exportar audios")
                .setMessage("Se copiarán " + cantidad + " archivo(s) a:\n"
                        + AudiosPictos.rutaPublicaVisible()
                        + "\n\nSi ya existen, se reemplazan.")
                .setPositiveButton("Exportar", (d, w) -> exportarAudiosPublicos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exportarAudiosPublicos() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISO_EXPORT_AUDIOS);
            return;
        }
        Toast.makeText(this, "Exportando audios…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ExportadorPublico.Resultado r = AudiosPictos.exportarPublico(this);
                runOnUiThread(() -> {
                    if (r.exportados == 0) {
                        Toast.makeText(this, "No hay audios para exportar.", Toast.LENGTH_SHORT).show();
                    } else if (r.fallidos == 0) {
                        Toast.makeText(this, "Exportados " + r.exportados + " a " + r.rutaVisible, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Exportados " + r.exportados + ", fallaron " + r.fallidos
                                + ".\nCarpeta: " + r.rutaVisible, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudieron exportar los audios.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void pedirExportarPictos() {
        int cantidad = PictosLocales.listar(this).size();
        if (cantidad == 0) {
            Toast.makeText(this, "No hay pictos propios para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Exportar pictos")
                .setMessage("Se copiarán " + cantidad + " imagen(es) a:\n"
                        + PictosLocales.rutaPublicaVisible()
                        + "\n\nSi ya existen, se reemplazan.")
                .setPositiveButton("Exportar", (d, w) -> exportarPictosPublicos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exportarPictosPublicos() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISO_EXPORT_PICTOS);
            return;
        }
        Toast.makeText(this, "Exportando pictos…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ExportadorPublico.Resultado r = PictosLocales.exportarPublico(this);
                runOnUiThread(() -> {
                    if (r.exportados == 0) {
                        Toast.makeText(this, "No hay pictos propios para exportar.", Toast.LENGTH_SHORT).show();
                    } else if (r.fallidos == 0) {
                        Toast.makeText(this, "Exportados " + r.exportados + " a " + r.rutaVisible, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Exportados " + r.exportados + ", fallaron " + r.fallidos
                                + ".\nCarpeta: " + r.rutaVisible, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudieron exportar los pictos.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void elegirOrigenFoto() {
        if (escuchandoNombre) detenerEscuchaNombre();
        mostrarListadoEdicion(false);
        new AlertDialog.Builder(this)
                .setTitle("Nueva foto")
                .setItems(new CharSequence[]{"Tomar foto", "Buscar archivo"}, (d, which) -> {
                    if (which == 0) pedirCamara();
                    else buscarArchivoFoto();
                })
                .setNegativeButton("Cancelar", (d, w) -> {
                    if (bitmapPendienteEdicion == null) mostrarListadoEdicion(true);
                })
                .setOnCancelListener(d -> {
                    if (bitmapPendienteEdicion == null) mostrarListadoEdicion(true);
                })
                .show();
    }

    private void pedirCamara() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISO_CAMARA);
            return;
        }
        lanzarCamara();
    }

    private void lanzarCamara() {
        try {
            File temp = new File(getCacheDir(), "captura_temp.jpg");
            uriFotoCamara = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", temp);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoCamara);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            for (ResolveInfo info : getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
                grantUriPermission(info.activityInfo.packageName, uriFotoCamara,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "No hay cámara disponible.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(intent, TOMAR_FOTO);
        } catch (RuntimeException error) {
            Toast.makeText(this, "No se pudo abrir la cámara.", Toast.LENGTH_SHORT).show();
        }
    }

    private void buscarArchivoFoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, ELEGIR_FOTO);
    }

    private void onFotoParaEdicion(Uri uri) {
        if (uri == null) return;
        new Thread(() -> {
            Bitmap bitmap = PictosLocales.decodificarUri(this, uri, 1024);
            runOnUiThread(() -> mostrarFotoPendiente(bitmap));
        }).start();
    }

    private void mostrarFotoPendiente(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "No se pudo leer esa foto.", Toast.LENGTH_SHORT).show();
            if (bitmapPendienteEdicion == null) mostrarListadoEdicion(true);
            return;
        }
        if (bitmapPendienteEdicion != null && bitmapPendienteEdicion != bitmap) {
            bitmapPendienteEdicion.recycle();
        }
        bitmapPendienteEdicion = bitmap;
        mostrarListadoEdicion(false);
        mostrarControlesEdicion(true);
        botonCamaraEdicion.setVisibility(View.VISIBLE);
        imagenPendienteEdicion.setVisibility(View.GONE);
        imagenPendienteEdicion.setImageBitmap(bitmap);
        if (imagenPreviewEdicion != null) {
            imagenPreviewEdicion.setImageBitmap(bitmap);
            imagenPreviewEdicion.setVisibility(View.VISIBLE);
        }
        nombrePendienteEdicion.setText("");
        nombrePendienteEdicion.setVisibility(View.GONE);
        botonOkPendienteEdicion.setVisibility(View.GONE);
        cajaAccionesFotoEdicion.setVisibility(View.VISIBLE);
        cajaPendienteEdicion.setVisibility(View.VISIBLE);
        cajaRecorteEdicion.setVisibility(View.GONE);
        habilitarMicroEdicion(false);
    }

    private void aceptarFotoPendiente() {
        if (bitmapPendienteEdicion == null) return;
        botonCamaraEdicion.setVisibility(View.GONE);
        imagenPendienteEdicion.setImageBitmap(bitmapPendienteEdicion);
        imagenPendienteEdicion.setVisibility(View.VISIBLE);
        if (imagenPreviewEdicion != null) imagenPreviewEdicion.setVisibility(View.GONE);
        cajaAccionesFotoEdicion.setVisibility(View.GONE);
        cajaPendienteEdicion.setVisibility(View.VISIBLE);
        nombrePendienteEdicion.setVisibility(View.VISIBLE);
        botonOkPendienteEdicion.setVisibility(View.VISIBLE);
        habilitarMicroEdicion(true);
        nombrePendienteEdicion.requestFocus();
        mostrarTeclado(nombrePendienteEdicion);
    }

    private void cancelarProcesoFoto() {
        detenerEscuchaNombre();
        cancelarPendienteEdicion();
        mostrarControlesEdicion(true);
        mostrarListadoEdicion(true);
        habilitarMicroEdicion(false);
    }

    private void mostrarListadoEdicion(boolean visible) {
        if (scrollEdicion != null) scrollEdicion.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (cajaRecorteEdicion != null && visible) cajaRecorteEdicion.setVisibility(View.GONE);
    }

    private void mostrarControlesEdicion(boolean visible) {
        if (filaControlesEdicion != null) filaControlesEdicion.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void pedirRecorteFoto() {
        if (bitmapPendienteEdicion == null) return;
        mostrarControlesEdicion(false);
        if (lanzarEditorExterno()) return;
        iniciarRecorteInterno();
    }

    private boolean lanzarEditorExterno() {
        try {
            File origen = new File(getCacheDir(), "edicion_origen.jpg");
            try (FileOutputStream out = new FileOutputStream(origen)) {
                bitmapPendienteEdicion.compress(Bitmap.CompressFormat.JPEG, 92, out);
            }
            File destino = new File(getCacheDir(), "edicion_recorte.jpg");
            uriFotoRecorte = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", destino);
            Uri uriOrigen = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", origen);
            Intent crop = new Intent("com.android.camera.action.CROP");
            crop.setDataAndType(uriOrigen, "image/*");
            crop.putExtra("crop", "true");
            crop.putExtra("scale", true);
            crop.putExtra("return-data", false);
            crop.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoRecorte);
            crop.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            crop.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            concederUriCamara(crop, uriOrigen);
            concederUriCamara(crop, uriFotoRecorte);
            if (crop.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(crop, RECORTAR_FOTO);
                return true;
            }
            Intent edit = new Intent(Intent.ACTION_EDIT);
            edit.setDataAndType(uriOrigen, "image/*");
            edit.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoRecorte);
            edit.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            concederUriCamara(edit, uriOrigen);
            concederUriCamara(edit, uriFotoRecorte);
            if (edit.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(Intent.createChooser(edit, "Recortar foto"), RECORTAR_FOTO);
                return true;
            }
        } catch (IOException | RuntimeException ignored) { }
        return false;
    }

    private void concederUriCamara(Intent intent, Uri uri) {
        if (uri == null) return;
        for (ResolveInfo info : getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            grantUriPermission(info.activityInfo.packageName, uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private void iniciarRecorteInterno() {
        mostrarControlesEdicion(false);
        cajaPendienteEdicion.setVisibility(View.GONE);
        cajaRecorteEdicion.setVisibility(View.VISIBLE);
        vistaRecorteEdicion.setBitmap(bitmapPendienteEdicion);
    }

    private void aplicarRecorteInterno() {
        Bitmap recortado = vistaRecorteEdicion.recortar();
        cajaRecorteEdicion.setVisibility(View.GONE);
        mostrarControlesEdicion(true);
        if (recortado != null) mostrarFotoPendiente(recortado);
        else if (bitmapPendienteEdicion != null) mostrarFotoPendiente(bitmapPendienteEdicion);
    }

    private void cancelarRecorteInterno() {
        cajaRecorteEdicion.setVisibility(View.GONE);
        mostrarControlesEdicion(true);
        if (bitmapPendienteEdicion != null) mostrarFotoPendiente(bitmapPendienteEdicion);
    }

    private void habilitarMicroEdicion(boolean habilitado) {
        if (botonMicroEdicionCaja == null) return;
        botonMicroEdicionCaja.setEnabled(habilitado);
        botonMicroEdicionCaja.setAlpha(habilitado ? 1f : 0.35f);
        botonMicroEdicionCaja.setClickable(habilitado);
    }

    private void pedirEscuchaNombre() {
        if (!botonMicroEdicionCaja.isEnabled() || bitmapPendienteEdicion == null) return;
        if (escuchandoNombre) {
            detenerEscuchaNombreYUsar();
            return;
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISO_MICRO_NOMBRE);
            return;
        }
        iniciarEscuchaNombre();
    }

    private void iniciarEscuchaNombre() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconocimiento de voz no disponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        detenerEscuchaNombre();
        reconocedorNombre = SpeechRecognizer.createSpeechRecognizer(this);
        reconocedorNombre.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) {
                runOnUiThread(() -> {
                    if (!escuchandoNombre || botonMicroEdicion == null) return;
                    float pulso = Math.max(0f, rmsdB) / 12f;
                    float escala = 1f + pulso * 0.18f;
                    botonMicroEdicion.setScaleX(escala);
                    botonMicroEdicion.setScaleY(escala);
                });
            }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onPartialResults(Bundle partialResults) {
                String parcial = primerResultado(partialResults);
                if (parcial != null && !parcial.trim().isEmpty()) {
                    runOnUiThread(() -> nombrePendienteEdicion.setText(parcial.trim()));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) { }
            @Override public void onError(int error) {
                runOnUiThread(() -> {
                    if (escuchandoNombre && segundosRestantesNombre > 0) {
                        reiniciarEscuchaNombre();
                        return;
                    }
                    finalizarEscuchaNombre();
                });
            }
            @Override public void onResults(Bundle results) {
                String texto = primerResultado(results);
                runOnUiThread(() -> {
                    if (texto != null && !texto.trim().isEmpty()) nombrePendienteEdicion.setText(texto.trim());
                    if (escuchandoNombre && segundosRestantesNombre > 0) {
                        reiniciarEscuchaNombre();
                        return;
                    }
                    finalizarEscuchaNombre();
                });
            }
        });
        escuchandoNombre = true;
        silenciarBeepsReconocedor();
        actualizarIconoMicroEdicion(true);
        iniciarCountdownNombre();
        try {
            reconocedorNombre.startListening(intentEscuchaNombre());
        } catch (RuntimeException ignored) {
            finalizarEscuchaNombre();
        }
    }

    private Intent intentEscuchaNombre() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        return intent;
    }

    private void reiniciarEscuchaNombre() {
        if (reconocedorNombre == null || !escuchandoNombre) return;
        try {
            reconocedorNombre.startListening(intentEscuchaNombre());
        } catch (RuntimeException ignored) {
            handlerEscucha.postDelayed(() -> {
                if (reconocedorNombre == null || !escuchandoNombre) return;
                try { reconocedorNombre.startListening(intentEscuchaNombre()); } catch (RuntimeException ignored2) { }
            }, 250);
        }
    }

    private void iniciarCountdownNombre() {
        cancelarCountdownNombre();
        segundosRestantesNombre = SEGUNDOS_MAX_NOMBRE;
        actualizarTextoCountdownNombre();
        countdownNombre = new Runnable() {
            @Override public void run() {
                if (!escuchandoNombre) return;
                segundosRestantesNombre--;
                if (segundosRestantesNombre <= 0) {
                    actualizarTextoCountdownNombre();
                    detenerEscuchaNombreYUsar();
                    return;
                }
                actualizarTextoCountdownNombre();
                handlerEscucha.postDelayed(this, 1000);
            }
        };
        handlerEscucha.postDelayed(countdownNombre, 1000);
    }

    private void cancelarCountdownNombre() {
        if (countdownNombre != null) {
            handlerEscucha.removeCallbacks(countdownNombre);
            countdownNombre = null;
        }
    }

    private void actualizarTextoCountdownNombre() {
        if (textoCountdownNombre == null) return;
        if (escuchandoNombre) {
            textoCountdownNombre.setVisibility(View.VISIBLE);
            textoCountdownNombre.setText(String.valueOf(Math.max(0, segundosRestantesNombre)));
        } else {
            textoCountdownNombre.setVisibility(View.GONE);
        }
    }

    private void actualizarIconoMicroEdicion(boolean escuchando) {
        if (botonMicroEdicion == null) return;
        if (animacionMicroNombre != null) {
            animacionMicroNombre.stop();
            animacionMicroNombre = null;
        }
        botonMicroEdicion.setScaleX(1f);
        botonMicroEdicion.setScaleY(1f);
        if (escuchando) {
            Drawable anim = getResources().getDrawable(R.drawable.anim_escucha, getTheme());
            botonMicroEdicion.setImageDrawable(anim);
            if (anim instanceof AnimationDrawable) {
                animacionMicroNombre = (AnimationDrawable) anim;
                animacionMicroNombre.start();
            }
        } else {
            Drawable icono = getResources().getDrawable(android.R.drawable.ic_btn_speak_now, getTheme()).mutate();
            icono.setTint(0xff263238);
            botonMicroEdicion.setImageDrawable(icono);
        }
        if (botonMicroEdicionCaja != null) {
            botonMicroEdicionCaja.setBackground(fondoTecla(escuchando ? TECLA_PULSADA : TECLA_NORMAL));
        }
        actualizarTextoCountdownNombre();
    }

    private void detenerEscuchaNombreYUsar() {
        if (!escuchandoNombre) return;
        escuchandoNombre = false;
        cancelarCountdownNombre();
        if (reconocedorNombre != null) {
            try { reconocedorNombre.stopListening(); } catch (RuntimeException ignored) { }
        }
        handlerEscucha.postDelayed(this::finalizarEscuchaNombre, 400);
    }

    private void finalizarEscuchaNombre() {
        escuchandoNombre = false;
        cancelarCountdownNombre();
        restaurarBeepsReconocedor();
        if (reconocedorNombre != null) {
            try { reconocedorNombre.cancel(); } catch (RuntimeException ignored) { }
            reconocedorNombre.destroy();
            reconocedorNombre = null;
        }
        actualizarIconoMicroEdicion(false);
        mostrarCampoNombrePendiente(nombrePendienteEdicion.getText().toString());
    }

    private void detenerEscuchaNombre() {
        escuchandoNombre = false;
        cancelarCountdownNombre();
        restaurarBeepsReconocedor();
        if (reconocedorNombre != null) {
            try { reconocedorNombre.cancel(); } catch (RuntimeException ignored) { }
            reconocedorNombre.destroy();
            reconocedorNombre = null;
        }
        actualizarIconoMicroEdicion(false);
    }

    private void mostrarCampoNombrePendiente(String texto) {
        nombrePendienteEdicion.setVisibility(View.VISIBLE);
        botonOkPendienteEdicion.setVisibility(View.VISIBLE);
        if (texto != null && !texto.trim().isEmpty()) nombrePendienteEdicion.setText(texto.trim());
        nombrePendienteEdicion.requestFocus();
        nombrePendienteEdicion.setSelection(nombrePendienteEdicion.getText().length());
        mostrarTeclado(nombrePendienteEdicion);
    }

    private void confirmarPictoPendiente() {
        if (bitmapPendienteEdicion == null) return;
        String escrito = nombrePendienteEdicion.getText() == null ? "" : nombrePendienteEdicion.getText().toString();
        String base = PictosLocales.sanitizarBase(escrito);
        if (base.isEmpty()) {
            Toast.makeText(this, "Escribí o decí un nombre.", Toast.LENGTH_SHORT).show();
            mostrarCampoNombrePendiente(escrito);
            return;
        }
        List<String> ocupados = PictosLocales.nombresArchivo(PictosLocales.listar(this));
        String archivo = PictosLocales.siguienteNombrePng(ocupados, base);
        Bitmap original = bitmapPendienteEdicion;
        botonOkPendienteEdicion.setEnabled(false);
        new Thread(() -> {
            try {
                Bitmap procesado = PictosLocales.procesarComoPicto(original);
                PictosLocales.guardarPng(this, procesado, archivo);
                if (procesado != null && procesado != original) procesado.recycle();
                runOnUiThread(() -> {
                    botonOkPendienteEdicion.setEnabled(true);
                    cancelarPendienteEdicion();
                    mostrarControlesEdicion(true);
                    mostrarListadoEdicion(true);
                    habilitarMicroEdicion(false);
                    refrescarListaEdicion();
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    botonOkPendienteEdicion.setEnabled(true);
                    Toast.makeText(this, "No se pudo guardar la foto.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void cancelarPendienteEdicion() {
        detenerEscuchaNombre();
        if (imagenPendienteEdicion != null) {
            imagenPendienteEdicion.setImageDrawable(null);
            imagenPendienteEdicion.setVisibility(View.GONE);
        }
        if (imagenPreviewEdicion != null) {
            imagenPreviewEdicion.setImageDrawable(null);
            imagenPreviewEdicion.setVisibility(View.GONE);
        }
        if (botonCamaraEdicion != null) botonCamaraEdicion.setVisibility(View.VISIBLE);
        if (nombrePendienteEdicion != null) nombrePendienteEdicion.setText("");
        if (cajaPendienteEdicion != null) cajaPendienteEdicion.setVisibility(View.GONE);
        if (cajaAccionesFotoEdicion != null) cajaAccionesFotoEdicion.setVisibility(View.GONE);
        if (cajaRecorteEdicion != null) cajaRecorteEdicion.setVisibility(View.GONE);
        if (nombrePendienteEdicion != null) nombrePendienteEdicion.setVisibility(View.GONE);
        if (botonOkPendienteEdicion != null) botonOkPendienteEdicion.setVisibility(View.GONE);
        if (bitmapPendienteEdicion != null) {
            bitmapPendienteEdicion.recycle();
            bitmapPendienteEdicion = null;
        }
    }

    private void refrescarListaEdicion() {
        if (listaPictosEdicion == null) return;
        listaPictosEdicion.removeAllViews();
        List<File> archivos = PictosLocales.listar(this);
        if (archivos.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Todavía no hay fotos propias.");
            vacio.setTextSize(16);
            vacio.setGravity(Gravity.CENTER);
            vacio.setPadding(0, dp(24), 0, dp(8));
            listaPictosEdicion.addView(vacio);
            return;
        }
        int indice = 0;
        for (File archivo : archivos) listaPictosEdicion.addView(filaPictoLocal(archivo, indice++));
    }

    private View filaPictoLocal(File archivo, int indice) {
        LinearLayout fila = nuevaFila();
        fila.setBackground(fondoPalabra(indice, false));

        ImageView imagen = new ImageView(this);
        imagen.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imagen.setAdjustViewBounds(true);
        imagen.setBackground(bordePicto());
        imagen.setPadding(dp(3), dp(3), dp(3), dp(3));
        Bitmap mini = PictosLocales.decodificarArchivo(archivo, TAM_MINIATURA_LISTA);
        if (mini != null) imagen.setImageBitmap(mini);
        fila.addView(imagen, fijo(dp(70), dp(70), dp(4)));

        EditText nombre = new EditText(this);
        nombre.setText(PictosLocales.baseSinSufijo(archivo.getName()));
        nombre.setTextSize(18);
        nombre.setTypeface(Typeface.DEFAULT_BOLD);
        nombre.setBackgroundColor(Color.TRANSPARENT);
        nombre.setSingleLine(true);
        nombre.setImeOptions(EditorInfo.IME_ACTION_DONE);
        nombre.setFocusable(false);
        nombre.setFocusableInTouchMode(false);
        nombre.setCursorVisible(false);
        fila.addView(nombre, peso(1, -1, dp(4)));

        ImageButton editarNombre = tabIcono(android.R.drawable.ic_menu_edit, "Editar nombre", v -> {
            nombre.setFocusable(true);
            nombre.setFocusableInTouchMode(true);
            nombre.setCursorVisible(true);
            nombre.requestFocus();
            nombre.setSelection(nombre.getText().length());
            mostrarTeclado(nombre);
        });
        fila.addView(editarNombre, fijo(dp(40), dp(40), 0));

        nombre.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                renombrarPictoLocal(archivo, nombre.getText().toString());
                return true;
            }
            return false;
        });
        nombre.setOnFocusChangeListener((v, tieneFoco) -> {
            if (!tieneFoco && modoEdicion) renombrarPictoLocal(archivo, nombre.getText().toString());
        });

        ImageButton borrar = papelera("Eliminar foto");
        borrar.setOnClickListener(v -> confirmarBorrarLocal(archivo));
        fila.addView(borrar, fijo(dp(44), dp(44), 0));
        return fila;
    }

    private void renombrarPictoLocal(File archivo, String nuevoTexto) {
        if (archivo == null || !archivo.exists()) return;
        String base = PictosLocales.sanitizarBase(nuevoTexto);
        if (base.isEmpty() || base.equals(PictosLocales.baseSinSufijo(archivo.getName()))) {
            ocultarTeclado();
            return;
        }
        List<String> ocupados = PictosLocales.nombresArchivo(PictosLocales.listar(this));
        ocupados.remove(archivo.getName());
        String destino = PictosLocales.siguienteNombrePng(ocupados, base);
        PictosLocales.renombrar(archivo, destino);
        ocultarTeclado();
        refrescarListaEdicion();
    }

    private void confirmarBorrarLocal(File archivo) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar foto")
                .setMessage("¿Seguro que querés borrar esta foto?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) -> {
                    if (archivo != null) archivo.delete();
                    refrescarListaEdicion();
                }).show();
    }

    private void mostrarTeclado(View campo) {
        campo.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(campo, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void ocultarTeclado() {
        View foco = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && foco != null) imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
    }

    @Override public void onBackPressed() {
        if (escuchandoPictos) {
            cancelarEscuchaPictos();
            return;
        }
        if (overlayVistaPrevia != null) {
            colaVozJuego.clear();
            cerrarVistaPreviaJuego();
            return;
        }
        if (modoEdicion) {
            guardarYSalirEdicion();
            return;
        }
        if (overlayDisyuncion != null) {
            cerrarDisyuncion(true);
            return;
        }
        super.onBackPressed();
    }

    private void construirVistaJuego() {
        tabEleccionJuego.post(this::actualizarTabEleccionJuego);
        actualizarTecladoJuego();
        refrescarMosaicoJuego();
    }

    private void actualizarTrasTeclaJuego() {
        if (escuchandoVoz) return;
        actualizarTecladoJuego();
        refrescarMosaicoJuego();
    }

    private void actualizarTecladoJuego() {
        tecladoJuegoContenedor.removeAllViews();
        tecladoJuegoContenedor.addView(tecladoPredictivoJuego(catalogoBuscable));
    }

    private void refrescarMosaicoJuego() {
        List<String> visibles = pictosVisiblesJuego();
        if (visibles.isEmpty()) {
            mosaicoJuego.removeAllViews();
            TextView aviso = new TextView(this);
            if (filtroTecladoJuego.isEmpty())
                aviso.setText("Todavía no hay pictogramas usados en frases.");
            else
                aviso.setText("Sin coincidencias.");
            aviso.setTextSize(18);
            aviso.setGravity(Gravity.CENTER);
            aviso.setPadding(dp(16), dp(40), dp(16), dp(16));
            mosaicoJuego.addView(aviso, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0)));
            return;
        }
        Runnable aplicar = () -> aplicarMosaicoJuego(visibles);
        if (juegoPanel.getWidth() > 0) aplicar.run();
        else juegoPanel.post(aplicar);
    }

    /** Sin filtro: pictos usados en Frases. Con teclas: coincidencias del catálogo. */
    private List<String> pictosVisiblesJuego() {
        if (filtroTecladoJuego.isEmpty()) return pictosUsadosEnFrases();
        List<String> r = new ArrayList<>();
        for (String archivo : catalogoBuscable)
            if (nombre(archivo).startsWith(filtroTecladoJuego)) r.add(archivo);
        return r;
    }

    private int anchoUtilMosaicoJuego() {
        int ancho = mosaicoJuego.getWidth();
        if (ancho <= 0 && juegoPanel.getWidth() > 0) ancho = juegoPanel.getWidth();
        if (ancho <= 0) ancho = getResources().getDisplayMetrics().widthPixels;
        return Math.max(0, ancho - mosaicoJuego.getPaddingLeft() - mosaicoJuego.getPaddingRight() - dp(8));
    }

    private void aplicarMosaicoJuego(List<String> usados) {
        mosaicoJuego.removeAllViews();
        int columnas = COLUMNAS_MOSAICO_JUEGO;
        int margen = dp(2);
        int anchoUtil = anchoUtilMosaicoJuego();
        int ladoCelda = Math.max(dp(48), (anchoUtil - columnas * margen * 2) / columnas);
        mosaicoJuego.setColumnCount(columnas);
        int total = usados.size();
        int restoUltimaFila = total % columnas;
        int offsetUltimaFila = restoUltimaFila == 0 ? 0
                : Math.round((columnas - restoUltimaFila) / 2f);
        int indiceUltimaFila = (total - 1) / columnas;
        for (int i = 0; i < total; i++) {
            String archivo = usados.get(i);
            int fila = i / columnas;
            int columnaEnFila = i % columnas;
            int columna = (fila == indiceUltimaFila && restoUltimaFila != 0)
                    ? offsetUltimaFila + columnaEnFila : columnaEnFila;
            View celda = celdaMosaicoJuego(archivo);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(fila), GridLayout.spec(columna));
            params.width = ladoCelda;
            params.height = ladoCelda;
            params.setMargins(margen, margen, margen, margen);
            mosaicoJuego.addView(celda, params);
        }
    }

    private List<String> pictosUsadosEnFrases() {
        Set<String> unicos = new LinkedHashSet<>();
        for (PhraseRecord frase : frases)
            for (PhraseRecord.Item item : frase.items)
                if (!item.automatico && item.archivo != null && !item.archivo.isEmpty())
                    unicos.add(item.archivo);
        List<String> orden = new ArrayList<>(unicos);
        Collections.sort(orden, (a, b) -> {
            int comparacion = Integer.compare(usosEnFrases(b), usosEnFrases(a));
            return comparacion != 0 ? comparacion : a.compareToIgnoreCase(b);
        });
        return orden;
    }

    private View celdaMosaicoJuego(String archivo) {
        FrameLayout celda = new FrameLayout(this);
        celda.setBackground(fondo());
        ImageView img = imagen(archivo);
        celda.addView(img, new FrameLayout.LayoutParams(-1, -1));
        celda.setContentDescription("Elegir " + nombre(archivo));
        celda.setOnClickListener(v -> mostrarVistaPreviaJuego(archivo, v));
        return celda;
    }

    private int anchoUtilTabSlots() {
        int ancho = scrollSlotsJuego.getWidth();
        if (ancho <= 0 && tabEleccionJuego.getWidth() > 0) {
            ancho = tabEleccionJuego.getWidth()
                    - tabEleccionJuego.getPaddingLeft() - tabEleccionJuego.getPaddingRight();
        }
        if (ancho <= 0 && juegoPanel.getWidth() > 0) ancho = juegoPanel.getWidth();
        if (ancho <= 0) ancho = getResources().getDisplayMetrics().widthPixels;
        return Math.max(0, ancho - dp(12));
    }

    private int ladoSlotTab() {
        return Math.max(dp(28), anchoUtilTabSlots() / SLOTS_VISIBLES_JUEGO);
    }

    private void actualizarTabEleccionJuego() {
        Runnable aplicar = this::aplicarTabEleccionJuego;
        if (tabEleccionJuego.getWidth() > 0) aplicar.run();
        else tabEleccionJuego.post(aplicar);
    }

    private void aplicarTabEleccionJuego() {
        slotsEleccionJuego.removeAllViews();
        int lado = ladoSlotTab();
        ViewGroup.LayoutParams filaParams = slotsEleccionJuego.getLayoutParams();
        filaParams.height = lado;
        slotsEleccionJuego.setLayoutParams(filaParams);
        int casillas = Math.min(MAX_PICTOS_JUEGO, Math.max(SLOTS_VISIBLES_JUEGO, seleccionJuego.size() + 1));
        for (int i = 0; i < casillas; i++) {
            View slot = i < seleccionJuego.size()
                    ? slotPictoJuego(seleccionJuego.get(i), i + 1)
                    : slotVacioJuego(i + 1, lado);
            slotsEleccionJuego.addView(slot, new LinearLayout.LayoutParams(lado, lado));
        }
        scrollSlotsJuego.post(() -> scrollSlotsJuego.smoothScrollTo(slotsEleccionJuego.getWidth(), 0));
        boolean haySeleccion = !seleccionJuego.isEmpty();
        botonReciclajeJuego.setEnabled(haySeleccion);
        botonReciclajeJuego.setAlpha(haySeleccion ? 1f : .35f);
        botonEnviarJuego.setEnabled(haySeleccion);
        botonEnviarJuego.setAlpha(haySeleccion ? 1f : .35f);
        botonDisyuncionJuego.setVisibility(seleccionJuego.size() == 2 ? View.VISIBLE : View.GONE);
    }

    private View slotVacioJuego(int numero, int lado) {
        FrameLayout vacio = new FrameLayout(this);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0x33ffffff);
        fondo.setCornerRadius(dp(10));
        fondo.setStroke(dp(2), 0x668195a5);
        vacio.setBackground(fondo);
        TextView texto = new TextView(this);
        texto.setText(String.valueOf(numero));
        texto.setTextColor(0x7790a4ae);
        texto.setTypeface(Typeface.DEFAULT_BOLD);
        texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, lado * 0.45f);
        texto.setGravity(Gravity.CENTER);
        texto.setIncludeFontPadding(false);
        vacio.addView(texto, new FrameLayout.LayoutParams(-1, -1));
        return vacio;
    }

    private View slotPictoJuego(PhraseRecord.Item item, int numero) {
        FrameLayout caja = new FrameLayout(this);
        caja.setClipChildren(true);
        ImageView img = imagen(item.archivo);
        caja.addView(img, new FrameLayout.LayoutParams(-1, -1));
        TextView posicion = new TextView(this);
        posicion.setText(String.valueOf(numero));
        posicion.setTextColor(0x9990a4ae);
        posicion.setTypeface(Typeface.DEFAULT_BOLD);
        posicion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        pp.setMargins(dp(6), dp(3), 0, 0);
        caja.addView(posicion, pp);
        View raya = new View(this);
        raya.setBackgroundColor(0xccb00020);
        raya.setVisibility(item.negado ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, dp(4), Gravity.CENTER);
        pr.setMargins(dp(2), 0, dp(2), 0);
        caja.addView(raya, pr);
        caja.setContentDescription((item.negado ? "Afirmar " : "Negar ") + nombre(item.archivo));
        caja.setOnClickListener(v -> {
            item.negado = !item.negado;
            raya.setVisibility(item.negado ? View.VISIBLE : View.GONE);
            caja.setContentDescription((item.negado ? "Afirmar " : "Negar ") + nombre(item.archivo));
        });
        return caja;
    }

    private void mostrarVistaPreviaJuego(String archivo, View origen) {
        abrirVistaPreviaJuego(Collections.singletonList(archivo), 0, false, origen, null);
    }

    /**
     * Amplía el picto al 50% del ancho para confirmarlo (OK) o descartarlo (NO) antes de enviarlo a la tarjeta.
     * Con más de una opción (variantes del mismo nombre) muestra − / + para cambiar de imagen.
     * Sin origen, el picto aparece creciendo desde el centro.
     */
    private void abrirVistaPreviaJuego(List<String> opciones, int indiceInicial, boolean negado,
                                       View origen, String progreso) {
        if (animandoEleccionJuego || overlayVistaPrevia != null || opciones.isEmpty()) return;
        if (seleccionJuego.size() >= MAX_PICTOS_JUEGO) {
            Toast.makeText(this, "Podés seleccionar hasta " + MAX_PICTOS_JUEGO + " pictogramas.", Toast.LENGTH_SHORT).show();
            colaVozJuego.clear();
            return;
        }
        FrameLayout raiz = (FrameLayout) contenido.getParent();
        int anchoPantalla = raiz.getWidth() > 0 ? raiz.getWidth() : getResources().getDisplayMetrics().widthPixels;
        int lado = anchoPantalla / 2;
        final int[] indice = { Math.max(0, Math.min(indiceInicial, opciones.size() - 1)) };

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setElevation(dp(10));
        overlay.setOnClickListener(v -> cerrarVistaPreviaJuego());

        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setGravity(Gravity.CENTER_HORIZONTAL);
        caja.setClickable(true);

        if (progreso != null) {
            TextView textoProgreso = new TextView(this);
            textoProgreso.setText(progreso);
            textoProgreso.setTextColor(Color.WHITE);
            textoProgreso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            textoProgreso.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
            pp.bottomMargin = dp(10);
            caja.addView(textoProgreso, pp);
        }

        LinearLayout filaImagen = new LinearLayout(this);
        filaImagen.setOrientation(LinearLayout.HORIZONTAL);
        filaImagen.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout marco = new FrameLayout(this);
        ImageView img = imagenGrande(opciones.get(indice[0]));
        marco.addView(img, new FrameLayout.LayoutParams(-1, -1));
        View raya = new View(this);
        raya.setBackgroundColor(0xccb00020);
        raya.setVisibility(negado ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, dp(8), Gravity.CENTER);
        pr.setMargins(dp(4), 0, dp(4), 0);
        marco.addView(raya, pr);

        TextView textoVariante = new TextView(this);
        textoVariante.setTextColor(Color.WHITE);
        textoVariante.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        Runnable mostrarOpcion = () -> {
            String actual = opciones.get(indice[0]);
            Bitmap bitmap = decodificarPicto(actual, TAM_PICTO_REPRODUCCION);
            if (bitmap != null) img.setImageBitmap(bitmap);
            img.setContentDescription(nombre(actual));
            textoVariante.setText((indice[0] + 1) + " de " + opciones.size());
        };
        boolean hayVariantes = opciones.size() > 1;
        if (hayVariantes) {
            Button menos = botonVariante("\u2212");
            menos.setContentDescription("Imagen anterior");
            menos.setOnClickListener(v -> {
                indice[0] = (indice[0] - 1 + opciones.size()) % opciones.size();
                mostrarOpcion.run();
            });
            filaImagen.addView(menos, fijo(dp(44), dp(44), dp(8)));
        }
        filaImagen.addView(marco, new LinearLayout.LayoutParams(lado, lado));
        if (hayVariantes) {
            Button mas = botonVariante("+");
            mas.setContentDescription("Imagen siguiente");
            mas.setOnClickListener(v -> {
                indice[0] = (indice[0] + 1) % opciones.size();
                mostrarOpcion.run();
            });
            filaImagen.addView(mas, fijo(dp(44), dp(44), dp(8)));
        }
        caja.addView(filaImagen, new LinearLayout.LayoutParams(-2, -2));
        if (hayVariantes) {
            mostrarOpcion.run();
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
            vp.topMargin = dp(6);
            caja.addView(textoVariante, vp);
        }

        LinearLayout botones = new LinearLayout(this);
        botones.setOrientation(LinearLayout.HORIZONTAL);
        botones.setGravity(Gravity.CENTER);
        Button ok = botonVistaPrevia("\u2714 OK", 0xff2e7d32);
        ok.setContentDescription("Aceptar");
        ok.setOnClickListener(v -> {
            if (overlayVistaPrevia != overlay) return;
            overlayVistaPrevia = null;
            animarEleccionJuego(opciones.get(indice[0]), negado, marco);
            raiz.removeView(overlay);
        });
        Button no = botonVistaPrevia("\u2716 NO", 0xffc62828);
        no.setContentDescription("Descartar");
        no.setOnClickListener(v -> cerrarVistaPreviaJuego());
        int anchoBoton = Math.max(dp(96), lado / 2 - dp(8));
        botones.addView(ok, fijo(anchoBoton, dp(56), dp(6)));
        botones.addView(no, fijo(anchoBoton, dp(56), dp(6)));
        LinearLayout.LayoutParams botonesParams = new LinearLayout.LayoutParams(-2, -2);
        botonesParams.topMargin = dp(16);
        caja.addView(botones, botonesParams);

        overlay.addView(caja, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        overlayVistaPrevia = overlay;
        raiz.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(180).start();
        marco.post(() -> {
            if (origen != null) {
                int[] locOrigen = new int[2], locMarco = new int[2];
                origen.getLocationOnScreen(locOrigen);
                marco.getLocationOnScreen(locMarco);
                float escala = Math.max(1, Math.min(origen.getWidth(), origen.getHeight())) / (float) lado;
                marco.setTranslationX((locOrigen[0] + origen.getWidth() / 2f) - (locMarco[0] + lado / 2f));
                marco.setTranslationY((locOrigen[1] + origen.getHeight() / 2f) - (locMarco[1] + lado / 2f));
                marco.setScaleX(escala);
                marco.setScaleY(escala);
            } else {
                marco.setScaleX(0.3f);
                marco.setScaleY(0.3f);
            }
            marco.animate().translationX(0).translationY(0).scaleX(1f).scaleY(1f)
                    .setDuration(240).setInterpolator(new DecelerateInterpolator()).start();
        });
    }

    private void cerrarVistaPreviaJuego() {
        FrameLayout overlay = overlayVistaPrevia;
        if (overlay == null) return;
        overlayVistaPrevia = null;
        overlay.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            ViewParent padre = overlay.getParent();
            if (padre instanceof ViewGroup) ((ViewGroup) padre).removeView(overlay);
            continuarColaVozJuego();
        }).start();
    }

    private Button botonVariante(String texto) {
        Button b = new Button(this);
        b.setText(texto);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(0xff263238);
        b.setPadding(0, 0, 0, 0);
        b.setGravity(Gravity.CENTER);
        GradientDrawable f = new GradientDrawable();
        f.setShape(GradientDrawable.OVAL);
        f.setColor(Color.WHITE);
        f.setStroke(dp(2), 0xff718596);
        b.setBackground(f);
        return b;
    }

    /** Muestra el siguiente picto reconocido por voz, si quedan y no hay otra vista abierta. */
    private void continuarColaVozJuego() {
        if (colaVozJuego.isEmpty() || overlayVistaPrevia != null || animandoEleccionJuego) return;
        if (solapaActual != SOLAPA_JUEGO || modoEdicion) { colaVozJuego.clear(); return; }
        if (seleccionJuego.size() >= MAX_PICTOS_JUEGO) {
            Toast.makeText(this, "La tarjeta está completa (" + MAX_PICTOS_JUEGO + " pictogramas).", Toast.LENGTH_SHORT).show();
            colaVozJuego.clear();
            return;
        }
        CoincidenciaPicto siguiente = colaVozJuego.remove(0);
        int numero = totalColaVozJuego - colaVozJuego.size();
        String progreso = totalColaVozJuego > 1 ? numero + " de " + totalColaVozJuego : null;
        List<String> opciones = variantesDe(siguiente.archivo);
        if (opciones.isEmpty()) opciones = Collections.singletonList(siguiente.archivo);
        int indice = Math.max(0, opciones.indexOf(varianteMasUsada(siguiente.archivo)));
        abrirVistaPreviaJuego(opciones, indice, siguiente.negado, null, progreso);
    }

    private Button botonVistaPrevia(String texto, int color) {
        Button b = new Button(this);
        b.setText(texto);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(color);
        GradientDrawable f = new GradientDrawable();
        f.setColor(Color.WHITE);
        f.setCornerRadius(dp(12));
        f.setStroke(dp(3), color);
        b.setBackground(f);
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private void animarEleccionJuego(String archivo, boolean negado, View origen) {
        if (animandoEleccionJuego) return;
        if (seleccionJuego.size() >= MAX_PICTOS_JUEGO) {
            Toast.makeText(this, "Podés seleccionar hasta " + MAX_PICTOS_JUEGO + " pictogramas.", Toast.LENGTH_SHORT).show();
            colaVozJuego.clear();
            return;
        }
        int indiceDestino = seleccionJuego.size();
        View destino = slotsEleccionJuego.getChildAt(indiceDestino);
        if (destino == null) { colaVozJuego.clear(); return; }
        animandoEleccionJuego = true;
        int scrollNecesario = destino.getRight() - scrollSlotsJuego.getWidth();
        if (scrollNecesario > scrollSlotsJuego.getScrollX()) scrollSlotsJuego.scrollTo(scrollNecesario, 0);
        int lado = destino.getWidth() > 0 ? destino.getWidth() : ladoSlotTab();
        int anchoOrigen = origen.getWidth();
        int altoOrigen = origen.getHeight();
        int[] locOrigen = new int[2], locDestino = new int[2], locRaiz = new int[2];
        origen.getLocationOnScreen(locOrigen);
        destino.getLocationOnScreen(locDestino);
        FrameLayout raiz = (FrameLayout) contenido.getParent();
        raiz.getLocationOnScreen(locRaiz);
        if (!filtroTecladoJuego.isEmpty()) liberarFiltroJuego();
        FrameLayout copia = new FrameLayout(this);
        copia.setElevation(dp(12));
        ImageView img = imagen(archivo);
        copia.addView(img, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(lado, lado);
        lp.leftMargin = locOrigen[0] - locRaiz[0] + (anchoOrigen - lado) / 2;
        lp.topMargin = locOrigen[1] - locRaiz[1] + (altoOrigen - lado) / 2;
        raiz.addView(copia, lp);
        float escalaInicial = Math.max(1, Math.min(anchoOrigen, altoOrigen)) / (float) lado;
        copia.setScaleX(escalaInicial);
        copia.setScaleY(escalaInicial);
        sonidoEleccionJuego();
        float dx = (locDestino[0] + destino.getWidth() / 2f) - (locOrigen[0] + anchoOrigen / 2f);
        float dy = (locDestino[1] + destino.getHeight() / 2f) - (locOrigen[1] + altoOrigen / 2f);
        copia.animate().translationX(dx).translationY(dy).scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    raiz.removeView(copia);
                    seleccionJuego.add(new PhraseRecord.Item(archivo, negado));
                    registrarUsoJuego(archivo);
                    actualizarTabEleccionJuego();
                    animandoEleccionJuego = false;
                    tabEleccionJuego.post(this::continuarColaVozJuego);
                }).start();
    }

    private void alternarMicroPictos() {
        if (escuchandoPictos) { detenerEscuchaPictos(); return; }
        if (overlayVistaPrevia != null || animandoEleccionJuego || escuchandoVoz) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconocimiento de voz no disponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISO_MICRO_PICTOS);
            return;
        }
        iniciarEscuchaPictos();
    }

    private void iniciarEscuchaPictos() {
        if (reconocedorPictos == null) {
            reconocedorPictos = SpeechRecognizer.createSpeechRecognizer(this);
            reconocedorPictos.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) {
                    runOnUiThread(() -> {
                        if (!escuchandoPictos || botonMicroJuego == null) return;
                        float escala = 1f + Math.max(0f, rmsdB) / 12f * 0.18f;
                        botonMicroJuego.setScaleX(escala);
                        botonMicroJuego.setScaleY(escala);
                    });
                }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() { }
                @Override public void onPartialResults(Bundle partialResults) {
                    String parcial = primerResultado(partialResults);
                    if (parcial != null && !parcial.trim().isEmpty()) textoParcialPictos = parcial;
                }
                @Override public void onEvent(int eventType, Bundle params) { }
                @Override public void onError(int error) {
                    runOnUiThread(() -> finalizarEscuchaPictos(null));
                }
                @Override public void onResults(Bundle results) {
                    String texto = primerResultado(results);
                    runOnUiThread(() -> finalizarEscuchaPictos(texto));
                }
            });
        }
        colaVozJuego.clear();
        textoParcialPictos = null;
        escuchandoPictos = true;
        silenciarBeepsReconocedor();
        actualizarIconoMicroPictos(true);
        handlerEscucha.postDelayed(cortarEscuchaPictosRunnable, SEGUNDOS_MAX_ESCUCHA_PICTOS * 1000L);
        try {
            reconocedorPictos.startListening(intentEscuchaNombre());
        } catch (RuntimeException e) {
            finalizarEscuchaPictos(null);
        }
    }

    /** Corta la escucha; el resultado llega por onResults o, si el motor no responde, por el fallback. */
    private void detenerEscuchaPictos() {
        if (!escuchandoPictos) return;
        handlerEscucha.removeCallbacks(cortarEscuchaPictosRunnable);
        try { reconocedorPictos.stopListening(); } catch (RuntimeException ignored) { }
        handlerEscucha.removeCallbacks(fallbackEscuchaPictosRunnable);
        handlerEscucha.postDelayed(fallbackEscuchaPictosRunnable, 1500);
    }

    private void cancelarEscuchaPictos() {
        if (!escuchandoPictos) return;
        escuchandoPictos = false;
        handlerEscucha.removeCallbacks(cortarEscuchaPictosRunnable);
        handlerEscucha.removeCallbacks(fallbackEscuchaPictosRunnable);
        try { reconocedorPictos.cancel(); } catch (RuntimeException ignored) { }
        restaurarBeepsReconocedor();
        actualizarIconoMicroPictos(false);
        textoParcialPictos = null;
    }

    private void finalizarEscuchaPictos(String texto) {
        if (!escuchandoPictos) return;
        escuchandoPictos = false;
        handlerEscucha.removeCallbacks(cortarEscuchaPictosRunnable);
        handlerEscucha.removeCallbacks(fallbackEscuchaPictosRunnable);
        restaurarBeepsReconocedor();
        actualizarIconoMicroPictos(false);
        if (texto == null || texto.trim().isEmpty()) texto = textoParcialPictos;
        textoParcialPictos = null;
        if (texto == null || texto.trim().isEmpty()) {
            Toast.makeText(this, "No se escuchó nada.", Toast.LENGTH_SHORT).show();
            return;
        }
        colaVozJuego.clear();
        for (CoincidenciaPicto c : AlgoritmoPictosFrase.coincidencias(texto, catalogoBuscable, this::nombre)) {
            if (!c.automatico && c.archivo != null && !c.archivo.isEmpty()) colaVozJuego.add(c);
        }
        if (colaVozJuego.isEmpty()) {
            Toast.makeText(this, "No encontré pictos para: \"" + texto.trim() + "\"", Toast.LENGTH_LONG).show();
            return;
        }
        int libres = MAX_PICTOS_JUEGO - seleccionJuego.size();
        if (libres <= 0) {
            Toast.makeText(this, "La tarjeta está completa (" + MAX_PICTOS_JUEGO + " pictogramas).", Toast.LENGTH_SHORT).show();
            colaVozJuego.clear();
            return;
        }
        while (colaVozJuego.size() > libres) colaVozJuego.remove(colaVozJuego.size() - 1);
        totalColaVozJuego = colaVozJuego.size();
        continuarColaVozJuego();
    }

    private void actualizarIconoMicroPictos(boolean escuchando) {
        if (botonMicroJuego == null) return;
        if (animacionMicroPictos != null) {
            animacionMicroPictos.stop();
            animacionMicroPictos = null;
        }
        botonMicroJuego.setScaleX(1f);
        botonMicroJuego.setScaleY(1f);
        if (escuchando) {
            Drawable anim = getResources().getDrawable(R.drawable.anim_escucha, getTheme());
            botonMicroJuego.setImageDrawable(anim);
            if (anim instanceof AnimationDrawable) {
                animacionMicroPictos = (AnimationDrawable) anim;
                animacionMicroPictos.start();
            }
        } else {
            Drawable icono = getResources().getDrawable(android.R.drawable.ic_btn_speak_now, getTheme()).mutate();
            icono.setTint(0xff263238);
            botonMicroJuego.setImageDrawable(icono);
        }
        botonMicroJuego.setBackground(fondoTecla(escuchando ? TECLA_PULSADA : TECLA_NORMAL));
        botonMicroJuego.setContentDescription(escuchando ? "Terminar de escuchar" : "Decir pictos con la voz");
    }

    private void sonidoEleccionJuego() {
        try {
            if (sonidoJuego == null) sonidoJuego = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
            sonidoJuego.startTone(ToneGenerator.TONE_PROP_ACK, 100);
        } catch (RuntimeException ignored) { }
    }

    private void quitarUltimoJuego() {
        if (animandoEleccionJuego || seleccionJuego.isEmpty()) return;
        seleccionJuego.remove(seleccionJuego.size() - 1);
        actualizarTabEleccionJuego();
    }

    private void enviarJuego() {
        if (animandoEleccionJuego) return;
        List<PhraseRecord.Item> items = copiar(seleccionJuego);
        if (items.isEmpty()) {
            Toast.makeText(this, "Seleccioná al menos un pictograma.", Toast.LENGTH_SHORT).show();
            return;
        }
        ejecutar(items, this::mostrarJuego);
    }

    private void ejecutarDisyuncionJuego() {
        if (animandoEleccionJuego || seleccionJuego.size() != 2) return;
        ejecutarDisyuncion(new PhraseRecord(copiar(seleccionJuego)));
    }

    private View filaPalabra(PhraseRecord.Item item, int indice) {
        if (item.automatico) return filaPalabraAutomatica(item, indice);
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
        List<String> variantes = variantesDe(item.archivo);
        if (variantes.size() > 1) {
            TextView cambiar = botonMasVariante();
            cambiar.setOnClickListener(v -> {
                item.archivo = siguienteVariante(item.archivo);
                Bitmap bmp = obtenerMiniatura(item.archivo);
                if (bmp != null) imagen.setImageBitmap(bmp);
                imagen.setContentDescription(nombre(item.archivo));
                icono.setContentDescription((item.negado ? "Afirmar " : "Negar ") + nombre(item.archivo));
            });
            fila.addView(cambiar, fijo(dp(40), dp(40), 0));
        }
        configurarArrastrePalabra(fila, item, icono, nombre);
        ImageButton papelera=papelera("Quitar una selección"); papelera.setOnClickListener(v->quitar(item)); fila.addView(papelera,fijo(dp(54),dp(54),0)); return fila;
    }

    /** Palabra sin picto real: jerarquía visual inferior (prueba del tren). */
    private View filaPalabraAutomatica(PhraseRecord.Item item, int indice) {
        LinearLayout fila = nuevaFila();
        fila.setAlpha(0.55f);
        GradientDrawable fondo = fondoPalabra(indice, true);
        fondo.setStroke(dp(1), 0x33263238);
        fila.setBackground(fondo);
        TextView etiqueta = new TextView(this);
        etiqueta.setText(item.etiqueta == null ? "" : item.etiqueta);
        etiqueta.setTextSize(16);
        etiqueta.setTextColor(0xff607d8b);
        etiqueta.setTypeface(Typeface.DEFAULT);
        etiqueta.setGravity(Gravity.CENTER_VERTICAL);
        fila.addView(etiqueta, peso(1, -1, dp(8)));
        configurarArrastrePalabra(fila, item, etiqueta);
        ImageButton papelera = papelera("Quitar palabra sin picto");
        papelera.setOnClickListener(v -> quitar(item));
        fila.addView(papelera, fijo(dp(44), dp(44), 0));
        return fila;
    }

    /** Asa ☰ + long-press para reordenar pictos del borrador (como en Frases). */
    private void configurarArrastrePalabra(LinearLayout fila, PhraseRecord.Item item, View... zonas) {
        fila.setTag(item);
        if (escuchandoVoz || borrador.size() < 2) return;

        TextView asa = new TextView(this);
        asa.setText("☰");
        asa.setTextSize(18);
        asa.setTextColor(0xff263238);
        asa.setGravity(Gravity.CENTER);
        asa.setContentDescription("Arrastrar para cambiar el orden");
        View.OnLongClickListener iniciar = v -> iniciarDragPalabra(fila, item);
        asa.setOnLongClickListener(iniciar);
        fila.setOnLongClickListener(iniciar);
        fila.setOnDragListener((v, e) -> alArrastrarPalabra(v, e));
        for (View zona : zonas) {
            if (zona != null) zona.setOnLongClickListener(iniciar);
        }
        fila.addView(asa, fijo(dp(36), dp(48), 0));
    }

    private boolean iniciarDragPalabra(View fila, PhraseRecord.Item item) {
        if (escuchandoVoz || item == null || !borrador.contains(item) || borrador.size() < 2) return false;
        ClipData datos = ClipData.newPlainText("palabra_borrador", "mover");
        View.DragShadowBuilder sombra = new View.DragShadowBuilder(fila);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fila.startDragAndDrop(datos, sombra, item, 0);
        } else {
            fila.startDrag(datos, sombra, item, 0);
        }
        return true;
    }

    private boolean alArrastrarPalabra(View destinoVista, DragEvent evento) {
        Object estado = evento.getLocalState();
        if (!(estado instanceof PhraseRecord.Item)) return false;
        PhraseRecord.Item origen = (PhraseRecord.Item) estado;
        Object tag = destinoVista.getTag();
        if (!(tag instanceof PhraseRecord.Item)) return false;
        PhraseRecord.Item destino = (PhraseRecord.Item) tag;
        if (!borrador.contains(origen) || !borrador.contains(destino)) return false;
        float alphaNormal = destino.automatico ? 0.55f : 1f;

        switch (evento.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                destinoVista.setAlpha(destino.automatico ? 0.3f : 0.55f);
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                destinoVista.setAlpha(alphaNormal);
                return true;
            case DragEvent.ACTION_DROP:
                destinoVista.setAlpha(alphaNormal);
                reordenarPalabraBorrador(origen, destino);
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                destinoVista.setAlpha(alphaNormal);
                return true;
            default:
                return false;
        }
    }

    private void reordenarPalabraBorrador(PhraseRecord.Item origen, PhraseRecord.Item destino) {
        if (origen == destino || escuchandoVoz) return;
        int desde = borrador.indexOf(origen);
        int hasta = borrador.indexOf(destino);
        if (desde < 0 || hasta < 0 || desde == hasta) return;
        borrador.remove(desde);
        if (desde < hasta) hasta--;
        borrador.add(hasta, origen);
        int scrollY = scrollLista == null ? 0 : scrollLista.getScrollY();
        mostrarPalabras();
        if (scrollLista != null) {
            scrollLista.post(() -> scrollLista.scrollTo(0, scrollY));
        }
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
    private View filaFrase(PhraseRecord frase, int indice, boolean seleccionada) {
        FrameLayout marco = new FrameLayout(this);
        marco.setBackground(fondoFrase(indice, seleccionada));
        marco.setTag(frase);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.setMargins(0, dp(3), 0, dp(3));
        marco.setLayoutParams(mp);
        marco.setPadding(dp(4), dp(2), dp(2), dp(2));

        HorizontalScrollView h = new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        h.setFillViewport(true);
        // Deja hueco mínimo a los costados; los controles van superpuestos.
        boolean dosPictos = cantidadReales(frase.items) == 2;
        h.setPadding(dp(26), dp(2), dosPictos ? dp(148) : dp(68), dp(2));
        LinearLayout iconos = new LinearLayout(this);
        iconos.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(iconos, new FrameLayout.LayoutParams(-2, -1));
        h.post(() -> dibujarIconosDeFrase(iconos, h, frase));
        // Fila más alta para que ~4 pictos grandes ocupen el contenedor.
        marco.addView(h, new FrameLayout.LayoutParams(-1, dp(112)));

        CheckBox check = new CheckBox(this);
        check.setChecked(seleccionada);
        check.setContentDescription("Seleccionar frase para PDF");
        check.setPadding(0, 0, 0, 0);
        check.setScaleX(0.82f);
        check.setScaleY(0.82f);
        check.setOnClickListener(v -> alternarSeleccionFrase(frase, check.isChecked()));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP | Gravity.START);
        cp.leftMargin = dp(-2);
        cp.topMargin = dp(-2);
        marco.addView(check, cp);

        LinearLayout acciones = new LinearLayout(this);
        acciones.setOrientation(LinearLayout.VERTICAL);
        acciones.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageButton play = new ImageButton(this);
        play.setImageResource(android.R.drawable.ic_media_play);
        play.setContentDescription("Reproducir frase");
        play.setBackgroundColor(Color.TRANSPARENT);
        play.setPadding(dp(2), dp(2), dp(2), dp(2));
        play.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        play.setOnClickListener(v -> ejecutarFrase(frase));
        acciones.addView(play, new LinearLayout.LayoutParams(dp(34), dp(34)));

        ImageButton borrar = papelera("Eliminar frase");
        borrar.setPadding(dp(2), dp(2), dp(2), dp(2));
        borrar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        borrar.setOnClickListener(v -> confirmar(frase));
        acciones.addView(borrar, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout derecha = new LinearLayout(this);
        derecha.setOrientation(LinearLayout.HORIZONTAL);
        derecha.setGravity(Gravity.CENTER_VERTICAL);
        if (dosPictos) {
            Button disyuncion = tecla("Disyunción", TECLA_NORMAL, v -> {
                if (!escuchandoVoz && !modoEdicion) ejecutarDisyuncion(frase);
            });
            disyuncion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            disyuncion.setContentDescription("Elegir entre los dos pictogramas");
            LinearLayout.LayoutParams dpDis = new LinearLayout.LayoutParams(dp(92), dp(40));
            dpDis.rightMargin = dp(4);
            derecha.addView(disyuncion, dpDis);
        }
        derecha.addView(acciones);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        marco.addView(derecha, ap);

        if (seleccionada) {
            TextView asa = new TextView(this);
            asa.setText("☰");
            asa.setTextSize(16);
            asa.setTextColor(0xff382060);
            asa.setGravity(Gravity.CENTER);
            asa.setContentDescription("Arrastrar para cambiar el orden");
            asa.setPadding(0, 0, 0, 0);
            View.OnLongClickListener iniciar = v -> iniciarDragFrase(marco, frase);
            asa.setOnLongClickListener(iniciar);
            marco.setOnLongClickListener(iniciar);
            marco.setOnDragListener((v, e) -> alArrastrarFrase(v, e));
            h.setOnLongClickListener(v -> iniciarDragFrase(marco, frase));
            FrameLayout.LayoutParams asp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.BOTTOM | Gravity.START);
            asp.leftMargin = dp(2);
            asp.bottomMargin = dp(0);
            marco.addView(asa, asp);
        }
        return marco;
    }

    private boolean iniciarDragFrase(View fila, PhraseRecord frase) {
        if (!frasesSeleccionadas.contains(frase) || frasesSeleccionadas.size() < 2) return false;
        ClipData datos = ClipData.newPlainText("frase_pdf", "mover");
        View.DragShadowBuilder sombra = new View.DragShadowBuilder(fila);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fila.startDragAndDrop(datos, sombra, frase, 0);
        } else {
            fila.startDrag(datos, sombra, frase, 0);
        }
        return true;
    }

    private boolean alArrastrarFrase(View destinoVista, DragEvent evento) {
        Object estado = evento.getLocalState();
        if (!(estado instanceof PhraseRecord)) return false;
        PhraseRecord origen = (PhraseRecord) estado;
        Object tag = destinoVista.getTag();
        if (!(tag instanceof PhraseRecord)) return false;
        PhraseRecord destino = (PhraseRecord) tag;
        if (!frasesSeleccionadas.contains(origen) || !frasesSeleccionadas.contains(destino)) return false;

        switch (evento.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                destinoVista.setAlpha(0.55f);
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                destinoVista.setAlpha(1f);
                return true;
            case DragEvent.ACTION_DROP:
                destinoVista.setAlpha(1f);
                reordenarFraseSeleccionada(origen, destino);
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                destinoVista.setAlpha(1f);
                return true;
            default:
                return false;
        }
    }

    private void reordenarFraseSeleccionada(PhraseRecord origen, PhraseRecord destino) {
        if (origen == destino) return;
        int desde = frasesSeleccionadas.indexOf(origen);
        int hasta = frasesSeleccionadas.indexOf(destino);
        if (desde < 0 || hasta < 0 || desde == hasta) return;
        frasesSeleccionadas.remove(desde);
        if (desde < hasta) hasta--;
        frasesSeleccionadas.add(hasta, origen);
        persistirSeleccionPdf();
        mostrarFrases();
    }
    private void dibujarIconosDeFrase(LinearLayout destino, View espacio, PhraseRecord frase) {
        destino.removeAllViews();
        int margen = Math.max(1, Math.round(getResources().getDisplayMetrics().density * .5f));
        List<PhraseRecord.Item> reales = new ArrayList<>();
        for (PhraseRecord.Item item : frase.items) {
            if (!item.automatico) reales.add(item);
        }
        int anchoUtil = Math.max(0, espacio.getWidth() - espacio.getPaddingLeft() - espacio.getPaddingRight());
        int altoUtil = Math.max(0, espacio.getHeight() - espacio.getPaddingTop() - espacio.getPaddingBottom());
        int ladoPorAlto = Math.max(dp(48), altoUtil - margen * 2);
        int ladoPorAncho = anchoUtil > 0
                ? Math.max(dp(48), (anchoUtil - PICTOS_VISIBLES_FRASE * margen * 2) / PICTOS_VISIBLES_FRASE)
                : ladoPorAlto;
        int lado = Math.min(ladoPorAlto, ladoPorAncho);
        for (PhraseRecord.Item item : reales) {
            destino.addView(picto(item, false, lado, margen));
        }
    }
    private LinearLayout nuevaFila() { LinearLayout f=new LinearLayout(this); f.setGravity(Gravity.CENTER_VERTICAL); f.setPadding(dp(8),dp(6),dp(8),dp(6)); f.setBackground(fondo()); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(3),0,dp(3));f.setLayoutParams(p);return f; }
    private void agregar(String archivo) {
        if (escuchandoVoz) return;
        borrador.add(new PhraseRecord.Item(archivo, false));
        filtroTeclado = "";
        mostrarPalabras();
        scrollLista.scrollTo(0, 0);
    }
    private void quitar(PhraseRecord.Item item) { if (escuchandoVoz) return; borrador.remove(item); filtroTeclado=""; mostrarPalabras(); }
    private View picto(PhraseRecord.Item item, boolean editable, int lado) { return picto(item, editable, lado, dp(3), false); }
    private View picto(PhraseRecord.Item item, boolean editable, int lado, int margen) { return picto(item, editable, lado, margen, false); }
    private View picto(PhraseRecord.Item item, boolean editable, int lado, int margen, boolean altaResolucion) {
        if (item.automatico) return pictoAutomatico(item, lado, margen);
        FrameLayout caja=new FrameLayout(this); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(lado,lado);p.setMargins(margen,margen,margen,margen);caja.setLayoutParams(p);
        ImageView img = altaResolucion ? imagenGrande(item.archivo) : imagen(item.archivo);
        img.setPadding(dp(4),dp(4),dp(4),dp(4));caja.addView(img,new FrameLayout.LayoutParams(-1,-1));
        View raya=new View(this);raya.setBackgroundColor(0xccb00020);raya.setVisibility(item.negado?View.VISIBLE:View.GONE);FrameLayout.LayoutParams pr=new FrameLayout.LayoutParams(-1,dp(4),Gravity.CENTER);pr.setMargins(dp(5),0,dp(5),0);caja.addView(raya,pr);
        if(editable)caja.setOnClickListener(v->{item.negado=!item.negado;raya.setVisibility(item.negado?View.VISIBLE:View.GONE);}); return caja;
    }

    /** Picto de texto para palabras sin match: más chico, más tenue; se ensancha si junta varias. */
    private View pictoAutomatico(PhraseRecord.Item item, int lado, int margen) {
        int alto = Math.max(dp(72), Math.round(lado * 0.58f));
        String etiqueta = item.etiqueta == null ? "" : item.etiqueta.trim();
        int ancho = anchoPictoAutomatico(etiqueta, alto);
        FrameLayout caja = new FrameLayout(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ancho, alto);
        p.setMargins(margen, margen + dp(18), margen, margen + dp(18));
        p.gravity = Gravity.CENTER_VERTICAL;
        caja.setLayoutParams(p);
        caja.setAlpha(0.48f);
        caja.setTag(Boolean.TRUE);

        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xffeceff1);
        fondo.setCornerRadius(dp(10));
        fondo.setStroke(dp(1), 0x66263238);
        caja.setBackground(fondo);

        TextView texto = new TextView(this);
        texto.setText(etiqueta);
        texto.setTextColor(0xff546e7a);
        texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, alto * 0.18f);
        texto.setGravity(Gravity.CENTER);
        texto.setMaxLines(2);
        texto.setPadding(dp(8), dp(6), dp(8), dp(6));
        caja.addView(texto, new FrameLayout.LayoutParams(-1, -1));
        return caja;
    }

    private int anchoPictoAutomatico(String etiqueta, int alto) {
        if (etiqueta == null || etiqueta.isEmpty()) return alto;
        TextPaint medida = new TextPaint();
        medida.setTextSize(alto * 0.18f);
        medida.setTypeface(Typeface.DEFAULT);
        float textoAncho = medida.measureText(etiqueta) + dp(20);
        int ancho = Math.max(alto, Math.round(textoAncho));
        return Math.min(ancho, dp(360));
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
        File local = pictosLocales.get(archivo);
        if (local != null) {
            Bitmap desdeArchivo = PictosLocales.decodificarArchivo(local, tamMaximo);
            if (desdeArchivo != null) return desdeArchivo;
        }
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
        if (escuchandoVoz) return;
        List<PhraseRecord.Item> secuencia = copiar(borrador);
        if (cantidadReales(secuencia) == 0) {
            Toast.makeText(this, "Seleccioná al menos un pictograma.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Guarda reales + automáticos; en la lista de Frases solo se exhiben los reales.
        marcarUltimaEjecucion(secuencia);
        PhraseStore.guardar(this, frases);
        guardarRespaldo();
        ejecutar(secuencia, () -> {
            // Ya quedó guardada en Frases: Palabras vuelve vacía.
            borrador.clear();
            filtroTeclado = "";
            mostrarPalabras();
        });
    }
    private void ejecutarFrase(PhraseRecord frase) {
        // Queda en Frases como última ejecutada y se carga en Palabras (con automáticos) para editar/reejecutar.
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
            if (x.automatico != y.automatico) return false;
            if (x.automatico) {
                if (!Objects.equals(x.etiqueta, y.etiqueta)) return false;
            } else if (!x.archivo.equals(y.archivo) || x.negado != y.negado) {
                return false;
            }
        }
        return true;
    }
    private void ejecutar(List<PhraseRecord.Item> items, Runnable fin) {
        contenido.setVisibility(View.GONE);
        FrameLayout raiz = (FrameLayout) contenido.getParent();
        raiz.setClipChildren(false);
        raiz.setClipToPadding(false);
        LinearLayout tren = new LinearLayout(this);
        tren.setGravity(Gravity.CENTER_VERTICAL);
        tren.setClipChildren(false);
        tren.setClipToPadding(false);
        int margen = dp(3);
        int ladoReal = dp(185);
        int altoAuto = Math.max(dp(72), Math.round(ladoReal * 0.58f));
        int anchoTren = 0;
        for (PhraseRecord.Item i : items) {
            View casilla = picto(i, false, ladoReal, margen, true);
            tren.addView(casilla);
            int anchoCasilla = i.automatico
                    ? anchoPictoAutomatico(i.etiqueta, altoAuto)
                    : ladoReal;
            anchoTren += anchoCasilla + margen * 2;
        }
        raiz.addView(tren, new FrameLayout.LayoutParams(Math.max(anchoTren, ladoReal), dp(210), Gravity.CENTER_VERTICAL));
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
            float cercania=Math.max(0f,1f-Math.abs(centroPicto-centroPantalla)/rango);
            boolean esAutomatico = Boolean.TRUE.equals(picto.getTag());
            float extra = esAutomatico ? 0.06f : 0.15f;
            float escala=1f+extra*cercania;
            picto.setScaleX(escala); picto.setScaleY(escala);
            // La profundidad sigue el crecimiento: el más cercano al centro tapa a sus vecinos.
            float zBase = esAutomatico ? dp(1) * 0.5f : dp(1);
            float zExtra = esAutomatico ? dp(6) : dp(20);
            picto.setTranslationZ(zBase + zExtra * cercania);
        }
    }

    private float getTouchSlop(){return ViewConfiguration.get(this).getScaledTouchSlop();}

    private void ejecutarDisyuncion(PhraseRecord frase) {
        List<PhraseRecord.Item> reales = soloReales(frase.items);
        if (reales.size() != 2 || overlayDisyuncion != null) return;
        FrameLayout raiz = (FrameLayout) contenido.getParent();
        if (raiz == null) return;
        raiz.setClipChildren(false);
        raiz.setClipToPadding(false);
        eleccionDisyuncionHecha = false;
        contenido.setVisibility(View.GONE);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xff263238);
        overlay.setClipChildren(false);
        overlay.setClipToPadding(false);
        overlayDisyuncion = overlay;

        View fondo = new View(this);
        fondo.setBackgroundColor(Color.TRANSPARENT);
        overlay.addView(fondo, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(Gravity.CENTER);
        fila.setClipChildren(false);
        fila.setClipToPadding(false);
        int lado = dp(158);
        int separacion = dp(36);
        PhraseRecord.Item itemA = reales.get(0);
        PhraseRecord.Item itemB = reales.get(1);
        View pictoA = picto(itemA, false, lado, 0, true);
        View pictoB = picto(itemB, false, lado, 0, true);
        pictoA.setClickable(false);
        pictoB.setClickable(false);
        fila.addView(pictoA);
        View hueco = new View(this);
        fila.addView(hueco, new LinearLayout.LayoutParams(separacion, 1));
        fila.addView(pictoB);
        overlay.addView(fila, new FrameLayout.LayoutParams(-1, -1));

        Button ok = new Button(this);
        ok.setText("OK");
        ok.setTextSize(18);
        ok.setAllCaps(false);
        ok.setTextColor(0xff263238);
        ok.setBackground(fondoSolapa(true));
        ok.setVisibility(View.GONE);
        ok.setEnabled(false);
        ok.setOnClickListener(v -> cerrarDisyuncion(true));
        FrameLayout.LayoutParams okParams = new FrameLayout.LayoutParams(-1, dp(52), Gravity.BOTTOM);
        okParams.setMargins(dp(16), 0, dp(16), dp(18));
        overlay.addView(ok, okParams);

        prepararEntradaDisyuncion(pictoA);
        prepararEntradaDisyuncion(pictoB);
        raiz.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        animarEntradaDisyuncion(pictoA, 0);
        animarEntradaDisyuncion(pictoB, 90);
        handlerEscucha.postDelayed(() -> {
            if (overlayDisyuncion != overlay || eleccionDisyuncionHecha) return;
            pictoA.setClickable(true);
            pictoB.setClickable(true);
            pictoA.setOnClickListener(v -> elegirPictoDisyuncion(pictoA, pictoB, itemA, itemB, fondo, ok, overlay));
            pictoB.setOnClickListener(v -> elegirPictoDisyuncion(pictoB, pictoA, itemB, itemA, fondo, ok, overlay));
        }, 620);
    }

    private void prepararEntradaDisyuncion(View picto) {
        picto.setScaleX(0.12f);
        picto.setScaleY(0.12f);
        picto.setAlpha(0f);
        picto.setTranslationY(dp(28));
    }

    private void animarEntradaDisyuncion(View picto, long delay) {
        picto.animate()
                .scaleX(1f).scaleY(1f).alpha(1f).translationY(0)
                .setStartDelay(delay)
                .setDuration(520)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void elegirPictoDisyuncion(View elegido, View otro, PhraseRecord.Item itemElegido,
            PhraseRecord.Item itemOtro, View fondo, Button ok, View overlay) {
        if (eleccionDisyuncionHecha || overlayDisyuncion != overlay) return;
        eleccionDisyuncionHecha = true;
        elegido.setClickable(false);
        otro.setClickable(false);

        float ancho = getResources().getDisplayMetrics().widthPixels;
        int[] locElegido = new int[2];
        int[] locOtro = new int[2];
        elegido.getLocationOnScreen(locElegido);
        otro.getLocationOnScreen(locOtro);
        float dx = ancho / 2f - (locElegido[0] + elegido.getWidth() / 2f);
        float saleX = (locOtro[0] + otro.getWidth() / 2f) < ancho / 2f ? -ancho : ancho;
        elegido.setPivotX(elegido.getWidth() / 2f);
        elegido.setPivotY(elegido.getHeight() / 2f);

        elegido.animate()
                .translationX(elegido.getTranslationX() + dx)
                .scaleX(1.35f).scaleY(1.35f)
                .setDuration(480)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        otro.animate()
                .translationX(otro.getTranslationX() + saleX * 0.55f)
                .alpha(0f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        int colorFondo;
        int tipoSonido;
        if (itemElegido.negado) {
            colorFondo = 0xE6B71C1C;
            tipoSonido = 1;
        } else if (itemOtro.negado) {
            colorFondo = 0xE62E7D32;
            tipoSonido = 2;
        } else {
            colorFondo = 0xE6F9A825;
            tipoSonido = 3;
        }
        ObjectAnimator.ofArgb(fondo, "backgroundColor", 0x00000000, colorFondo)
                .setDuration(420)
                .start();
        sonarDisyuncion(tipoSonido);
        handlerEscucha.postDelayed(() -> {
            if (overlayDisyuncion != overlay) return;
            ok.setVisibility(View.VISIBLE);
            ok.setEnabled(true);
        }, 500);
    }

    private void sonarDisyuncion(int tipo) {
        String base = tipo == 1 ? "mal" : tipo == 2 ? "bien" : "ok";
        if (reproducirSonidoAsset("sonidos/" + base + ".mp3")
                || reproducirSonidoAsset("sonidos/" + base + ".ogg")
                || reproducirSonidoAsset("sonidos/" + base + ".wav")) {
            return;
        }
        try {
            if (sonidoJuego == null) sonidoJuego = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
            if (tipo == 1) {
                sonidoJuego.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 420);
                handlerEscucha.postDelayed(() -> {
                    if (sonidoJuego != null) sonidoJuego.startTone(ToneGenerator.TONE_SUP_ERROR, 280);
                }, 240);
            } else if (tipo == 2) {
                sonidoJuego.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 320);
                handlerEscucha.postDelayed(() -> {
                    if (sonidoJuego != null) sonidoJuego.startTone(ToneGenerator.TONE_PROP_ACK, 200);
                }, 200);
            } else {
                sonidoJuego.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
            }
        } catch (RuntimeException ignored) { }
    }

    private boolean reproducirSonidoAsset(String ruta) {
        try {
            android.content.res.AssetFileDescriptor afd = getAssets().openFd(ruta);
            if (sonidoDisyuncion != null) {
                sonidoDisyuncion.release();
                sonidoDisyuncion = null;
            }
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setOnCompletionListener(mp -> {
                mp.release();
                if (sonidoDisyuncion == mp) sonidoDisyuncion = null;
            });
            player.prepare();
            player.start();
            sonidoDisyuncion = player;
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void cerrarDisyuncion(boolean volver) {
        if (overlayDisyuncion == null) {
            if (volver) volverTrasDisyuncion();
            return;
        }
        if (sonidoDisyuncion != null) {
            sonidoDisyuncion.release();
            sonidoDisyuncion = null;
        }
        View overlay = overlayDisyuncion;
        overlayDisyuncion = null;
        eleccionDisyuncionHecha = false;
        ViewParent padre = overlay.getParent();
        if (padre instanceof ViewGroup) ((ViewGroup) padre).removeView(overlay);
        contenido.setVisibility(View.VISIBLE);
        if (volver) volverTrasDisyuncion();
    }

    private void volverTrasDisyuncion() {
        if (solapaActual == SOLAPA_JUEGO) mostrarJuego();
        else mostrarFrases();
    }

    private void confirmar(PhraseRecord frase) {
        new AlertDialog.Builder(this).setTitle("Eliminar frase").setMessage("¿Seguro que querés borrar esta frase?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) -> {
                    frases.remove(frase);
                    frasesSeleccionadas.remove(frase);
                    PhraseStore.guardar(this, frases);
                    persistirSeleccionPdf();
                    guardarRespaldo();
                    mostrarFrases();
                }).show();
    }

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
        if (resultado != RESULT_OK) {
            if ((codigo == TOMAR_FOTO || codigo == ELEGIR_FOTO) && bitmapPendienteEdicion == null) {
                mostrarListadoEdicion(true);
            }
            if (codigo == RECORTAR_FOTO) {
                mostrarControlesEdicion(true);
                if (bitmapPendienteEdicion != null) mostrarFotoPendiente(bitmapPendienteEdicion);
            }
            return;
        }
        if (codigo == TOMAR_FOTO) {
            onFotoParaEdicion(uriFotoCamara);
            return;
        }
        if (codigo == ELEGIR_FOTO && datos != null && datos.getData() != null) {
            onFotoParaEdicion(datos.getData());
            return;
        }
        if (codigo == RECORTAR_FOTO) {
            Uri recorte = uriFotoRecorte;
            if ((recorte == null || !archivoUriExiste(recorte)) && datos != null && datos.getData() != null) {
                recorte = datos.getData();
            }
            if (recorte != null) onFotoParaEdicion(recorte);
            else if (bitmapPendienteEdicion != null) mostrarFotoPendiente(bitmapPendienteEdicion);
            mostrarControlesEdicion(true);
            return;
        }
        if (codigo != ELEGIR_RESPALDO || datos == null || datos.getData() == null) return;
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

    private boolean archivoUriExiste(Uri uri) {
        if (uri == null) return false;
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            return stream != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void confirmarRestauracion(List<PhraseRecord> validas, int descartadas, Uri uri) {
        String mensaje = "Se encontraron " + validas.size() + " frases válidas." + (descartadas == 0 ? "" : " Se descartarán " + descartadas + " frases con pictogramas eliminados.");
        new AlertDialog.Builder(this).setTitle("Restaurar frases").setMessage(mensaje).setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar", (d, w) -> {
                    frases.clear();
                    frasesSeleccionadas.clear();
                    frases.addAll(validas);
                    PhraseStore.guardar(this, frases);
                    persistirSeleccionPdf();
                    BackupStore.usarDestino(this, uri);
                    guardarRespaldo();
                    mostrarFrases();
                }).show();
    }

    private List<PhraseRecord> validarFrases(List<PhraseRecord> origen) {
        Set<String> existentes = new HashSet<>(pictos);
        List<PhraseRecord> validas = new ArrayList<>();
        for (PhraseRecord frase : origen) {
            boolean tieneReal = false;
            boolean valida = true;
            for (PhraseRecord.Item item : frase.items) {
                if (item.automatico) continue;
                tieneReal = true;
                if (!existentes.contains(item.archivo)) {
                    valida = false;
                    break;
                }
            }
            if (valida && tieneReal) validas.add(frase);
        }
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

    @Override public void onRequestPermissionsResult(int codigo, String[] permisos, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permisos, resultados);
        if (resultados.length == 0) return;
        if (codigo == PERMISO_DESCARGAS && resultados[0] == PackageManager.PERMISSION_GRANTED) guardarRespaldo();
        if (codigo == PERMISO_EXPORT_AUDIOS) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) exportarAudiosPublicos();
            else Toast.makeText(this, "Se necesita permiso de almacenamiento.", Toast.LENGTH_SHORT).show();
        }
        if (codigo == PERMISO_MICRO_PICTOS) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) iniciarEscuchaPictos();
            else Toast.makeText(this, "Se necesita permiso de micrófono.", Toast.LENGTH_SHORT).show();
        }
        if (codigo == PERMISO_EXPORT_PICTOS) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) exportarPictosPublicos();
            else Toast.makeText(this, "Se necesita permiso de almacenamiento.", Toast.LENGTH_SHORT).show();
        }
        if (codigo == PERMISO_MICRO) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) iniciarEscucha();
            else Toast.makeText(this, "Se necesita permiso de micrófono.", Toast.LENGTH_SHORT).show();
        }
        if (codigo == PERMISO_CAMARA) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) lanzarCamara();
            else Toast.makeText(this, "Se necesita permiso de cámara.", Toast.LENGTH_SHORT).show();
        }
        if (codigo == PERMISO_MICRO_NOMBRE) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) iniciarEscuchaNombre();
            else Toast.makeText(this, "Se necesita permiso de micrófono.", Toast.LENGTH_SHORT).show();
        }
        if (codigo == PERMISO_MICRO_AUDIO) {
            if (resultados[0] == PackageManager.PERMISSION_GRANTED) {
                String picto = pictoPendienteGrabar;
                pictoPendienteGrabar = null;
                if (picto != null) iniciarGrabacionAudio(picto);
            } else {
                pictoPendienteGrabar = null;
                Toast.makeText(this, "Se necesita permiso de micrófono.", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
                if (!item.automatico && item.archivo.equals(archivo)) cantidad++;
        return cantidad;
    }

    /** Pictos que se ven con el mismo nombre (dolor.png y dolor (2).png). */
    private List<String> variantesDe(String archivo) {
        if (archivo == null || archivo.isEmpty()) return Collections.emptyList();
        String clave = nombre(archivo);
        List<String> variantes = new ArrayList<>();
        for (String candidato : catalogoBuscable) {
            if (clave.equals(nombre(candidato))) variantes.add(candidato);
        }
        Collections.sort(variantes, (a, b) -> {
            int porNumero = Integer.compare(numeroVariante(a), numeroVariante(b));
            return porNumero != 0 ? porNumero : a.compareToIgnoreCase(b);
        });
        return variantes;
    }

    private int usosJuego(String archivo) {
        return getSharedPreferences(PREFS_USOS_JUEGO, MODE_PRIVATE).getInt(archivo, 0);
    }

    private void registrarUsoJuego(String archivo) {
        SharedPreferences prefs = getSharedPreferences(PREFS_USOS_JUEGO, MODE_PRIVATE);
        prefs.edit().putInt(archivo, prefs.getInt(archivo, 0) + 1).apply();
    }

    /** Entre variantes del mismo nombre, la más usada (frases + tarjeta de Pictos); si empatan, la sin (2)/(3). */
    private String varianteMasUsada(String archivo) {
        List<String> variantes = variantesDe(archivo);
        if (variantes.isEmpty()) return archivo;
        String mejor = variantes.get(0);
        int maxUsos = usosEnFrases(mejor) + usosJuego(mejor);
        for (int i = 1; i < variantes.size(); i++) {
            String candidato = variantes.get(i);
            int usos = usosEnFrases(candidato) + usosJuego(candidato);
            if (usos > maxUsos) {
                maxUsos = usos;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private String siguienteVariante(String archivo) {
        List<String> variantes = variantesDe(archivo);
        if (variantes.size() <= 1) return archivo;
        int indice = variantes.indexOf(archivo);
        if (indice < 0) return variantes.get(0);
        return variantes.get((indice + 1) % variantes.size());
    }

    /** dolor.png → 1; dolor (2).png → 2. */
    private static int numeroVariante(String archivo) {
        int cierre = archivo.lastIndexOf(')');
        int apertura = archivo.lastIndexOf('(');
        if (apertura < 0 || cierre <= apertura) return 1;
        try {
            return Integer.parseInt(archivo.substring(apertura + 1, cierre));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private TextView botonMasVariante() {
        TextView boton = new TextView(this);
        boton.setText("+");
        boton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        boton.setTypeface(Typeface.DEFAULT_BOLD);
        boton.setGravity(Gravity.CENTER);
        boton.setTextColor(0xff263238);
        boton.setContentDescription("Cambiar imagen del pictograma");
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xffffffff);
        fondo.setCornerRadius(dp(8));
        fondo.setStroke(dp(1), 0xff718596);
        boton.setBackground(fondo);
        return boton;
    }

    private int usoAjustado(String archivo) {
        int cantidad = usosEnFrases(archivo);
        for (PhraseRecord.Item item : borrador)
            if (!item.automatico && item.archivo.equals(archivo)) cantidad--;
        return Math.max(0, cantidad);
    }

    /** Teclado QWERTY que muestra pulsadas las letras que forman el filtro actual. */
    private View tecladoPredictivo(List<String> disponibles) {
        return construirTecladoPredictivo(disponibles, filtroTeclado, false);
    }

    private View tecladoPredictivoJuego(List<String> disponibles) {
        return construirTecladoPredictivo(disponibles, filtroTecladoJuego, true);
    }

    private View construirTecladoPredictivo(List<String> disponibles, String filtro, boolean paraJuego) {
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(6), dp(8), dp(6)); panel.setBackground(fondoTeclado());
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.setMargins(0, dp(5), 0, dp(5)); panel.setLayoutParams(panelParams);

        LinearLayout controles = new LinearLayout(this); controles.setGravity(Gravity.CENTER);
        Button liberar = tecla("Soltar todas", TECLA_NORMAL,
                v -> { if (paraJuego) liberarFiltroJuego(); else liberarFiltro(); });
        boolean soltarActivo = !filtro.isEmpty();
        liberar.setEnabled(soltarActivo); liberar.setAlpha(soltarActivo ? 1f : .35f);
        controles.addView(liberar, peso(1, dp(28), dp(1)));
        panel.addView(controles);

        Set<Character> iniciales = caracteresIniciales(disponibles);
        Set<Character> siguientes = siguientesCaracteres(disponibles, filtro);
        agregarFilaTeclado(panel, "QWERTYUIOP", siguientes, iniciales, true, filtro, paraJuego);
        agregarFilaTeclado(panel, "ASDFGHJKLÑ", siguientes, iniciales, true, filtro, paraJuego);
        agregarFilaTeclado(panel, "ZXCVBNM ", siguientes, iniciales, true, filtro, paraJuego);
        return panel;
    }

    private void liberarFiltro() { if (escuchandoVoz) return; filtroTeclado=""; actualizarTrasTecla(); }

    private void liberarFiltroJuego() {
        if (escuchandoVoz) return;
        filtroTecladoJuego = "";
        actualizarTrasTeclaJuego();
    }

    /** Espacio se muestra como "_" para que la tecla sea visible. */
    private String etiquetaTecla(char letra) { return letra == ' ' ? "_" : String.valueOf(letra); }

    private void borrarUltimaTecla() {
        if (escuchandoVoz) return;
        if (filtroTeclado.isEmpty()) return;
        filtroTeclado = filtroTeclado.substring(0, filtroTeclado.length() - 1);
        actualizarTrasTecla();
    }

    private void borrarUltimaTeclaJuego() {
        if (escuchandoVoz) return;
        if (filtroTecladoJuego.isEmpty()) return;
        filtroTecladoJuego = filtroTecladoJuego.substring(0, filtroTecladoJuego.length() - 1);
        actualizarTrasTeclaJuego();
    }

    private void agregarFilaTeclado(LinearLayout panel, String letras, Set<Character> siguientes, Set<Character> iniciales, boolean habilitado, String filtro, boolean paraJuego) {
        LinearLayout fila = new LinearLayout(this); fila.setGravity(Gravity.CENTER);
        char ultima = filtro.isEmpty() ? 0 : filtro.charAt(filtro.length() - 1);
        for (int i=0; i<letras.length(); i++) {
            char letra = letras.charAt(i);
            String etiqueta = etiquetaTecla(letra);
            if (!habilitado) {
                Button inactiva = tecla(etiqueta, TECLA_NORMAL, null);
                inactiva.setEnabled(false); inactiva.setAlpha(.35f);
                fila.addView(inactiva, peso(1, dp(31), dp(1)));
                continue;
            }
            int seleccionadas = cantidadDeLetra(letra, filtro);
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
                    Button deshacer = tecla(etiqueta, TECLA_ULTIMA,
                            v -> { if (paraJuego) borrarUltimaTeclaJuego(); else borrarUltimaTecla(); });
                    deshacer.setContentDescription("Borrar última letra");
                    fila.addView(deshacer, peso(1, dp(31), dp(1)));
                } else {
                    Button marcada = tecla(etiqueta, TECLA_PULSADA, null);
                    marcada.setEnabled(false); fila.addView(marcada, peso(1, dp(31), dp(1)));
                }
            }
            if (disponible) {
                Button opcion = tecla(etiqueta, TECLA_NORMAL, v -> {
                    if (paraJuego) {
                        filtroTecladoJuego += letra;
                        actualizarTrasTeclaJuego();
                    } else {
                        filtroTeclado += letra;
                        actualizarTrasTecla();
                    }
                });
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

    private int cantidadDeLetra(char letra, String filtro) {
        int cantidad = 0;
        for (int i=0; i<filtro.length(); i++) if (filtro.charAt(i) == letra) cantidad++;
        return cantidad;
    }

    private Set<Character> siguientesCaracteres(List<String> disponibles, String filtro) {
        Set<Character> resultado = new HashSet<>();
        for (String archivo : disponibles) {
            String texto = nombre(archivo);
            if (texto.startsWith(filtro) && texto.length() > filtro.length()) resultado.add(texto.charAt(filtro.length()));
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
                    if (!item.automatico && item.archivo.equals(candidato)) cantidad++;
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
                    PhraseRecord.Item prev = frase.items.get(i - 1);
                    PhraseRecord.Item actual = frase.items.get(i);
                    if (prev.automatico || actual.automatico) continue;
                    if (prev.archivo.equals(anterior) && actual.archivo.equals(candidato)) cantidad++;
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

    private static List<PhraseRecord.Item> copiar(List<PhraseRecord.Item> origen) {
        List<PhraseRecord.Item> r = new ArrayList<>();
        for (PhraseRecord.Item i : origen) {
            r.add(new PhraseRecord.Item(i.archivo, i.negado, i.automatico, i.etiqueta));
        }
        return r;
    }

    private static List<PhraseRecord.Item> soloReales(List<PhraseRecord.Item> origen) {
        List<PhraseRecord.Item> r = new ArrayList<>();
        for (PhraseRecord.Item i : origen) {
            if (!i.automatico) r.add(new PhraseRecord.Item(i.archivo, i.negado));
        }
        return r;
    }

    private static int cantidadReales(List<PhraseRecord.Item> items) {
        int n = 0;
        for (PhraseRecord.Item i : items) if (!i.automatico) n++;
        return n;
    }

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
