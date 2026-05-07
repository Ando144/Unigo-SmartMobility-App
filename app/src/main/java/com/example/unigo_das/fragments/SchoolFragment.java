package com.example.unigo_das.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.unigo_das.R;
import com.example.unigo_das.adapters.CentroAdapter;
import com.example.unigo_das.item.Centro;

public class SchoolFragment extends Fragment {

    private RecyclerView rvCentros;
    private CentroAdapter adapter;
    private List<Centro> listaCentros;
    private List<Centro> listaOriginalCompletita; // Base de datos maestra para no perder info al filtrar
    private SearchView searchView;

    private android.widget.Button btnFiltroTodas, btnFiltroEHU, btnFiltroDeusto, btnFiltroMondragon;
    private SharedPreferences prefs;

    // Variables de estado para los filtros combinados
    private String filtroUniversidadActual = "Todas";
    private String busquedaActual = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_school, container, false);

        ViewCompat.setOnApplyWindowInsetsListener(view, new androidx.core.view.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {

                int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(0, topInset, 0, 0);

                return windowInsets;
            }
        });

        rvCentros = view.findViewById(R.id.rvCentros);
        searchView = view.findViewById(R.id.searchViewCentros);
        prefs = requireActivity().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);

        cargarDatosMocK();

        rvCentros.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- ACTUALIZADO: Inicializamos el adapter con ambos Listeners (Estrella y Tarjeta general) ---
        adapter = new CentroAdapter(listaCentros, new CentroAdapter.OnStarClickListener() {
            @Override
            public void onStarClick(Centro centro, int position) {
                guardarFavorito(centro);
                ordenarLista();
            }
        }, new CentroAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Centro centro) {
                // Llamamos a la Activity principal para que muestre el mapa y trace la ruta
                ((com.example.unigo_das.activities.MainActivity) requireActivity())
                        .irRutaEnMapa(centro.getNombre(), centro.getLatitud(), centro.getLongitud());
            }
        });
        rvCentros.setAdapter(adapter);

        // --- LÓGICA DE LOS BOTONES DE FILTRADO (CHIPS) ---
        btnFiltroTodas = view.findViewById(R.id.btnFiltroTodas);
        btnFiltroEHU = view.findViewById(R.id.btnFiltroEHU);
        btnFiltroDeusto = view.findViewById(R.id.btnFiltroDeusto);
        btnFiltroMondragon = view.findViewById(R.id.btnFiltroMondragon);

        // Por defecto, el botón "Todas" empieza en rojo
        actualizarBotonActivo(btnFiltroTodas);

        btnFiltroTodas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroUniversidadActual = "Todas";
                aplicarFiltrosCombinados();
                actualizarBotonActivo(btnFiltroTodas);
            }
        });

        btnFiltroEHU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroUniversidadActual = "EHU";
                aplicarFiltrosCombinados();
                actualizarBotonActivo(btnFiltroEHU);
            }
        });

        btnFiltroDeusto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroUniversidadActual = "Deusto";
                aplicarFiltrosCombinados();
                actualizarBotonActivo(btnFiltroDeusto);
            }
        });

        btnFiltroMondragon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroUniversidadActual = "Mondragon";
                aplicarFiltrosCombinados();
                actualizarBotonActivo(btnFiltroMondragon);
            }
        });

        // --- LÓGICA DEL BUSCADOR ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                busquedaActual = newText;
                aplicarFiltrosCombinados(); // Llamamos al filtro global en lugar del interno del Adapter
                return false;
            }
        });

        return view;
    }

    private void cargarDatosMocK() {
        listaOriginalCompletita = new ArrayList<>();

        // Se añaden los datos con COORDENADAS REALES añadidas al final de cada constructor (Latitud, Longitud)

        // --- CENTROS EHU ---
        listaOriginalCompletita.add(new Centro("EHU_345", getString(R.string.centro_ehu_ingenieria_bilbao), "EHU", "Edif II", getString(R.string.bus) + ": Bilbobus 28, 38, 62.\n" + getString(R.string.tram) + ": " + getString(R.string.stop) + " San Mamés.", 43.2638, -2.9511));
        listaOriginalCompletita.add(new Centro("EHU_350", getString(R.string.centro_ehu_ingenieria_bilbao), "EHU", "Náutica", getString(R.string.bus) + ": Bizkaibus A3411.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.3308, -3.0186));
        listaOriginalCompletita.add(new Centro("EHU_363", getString(R.string.centro_ehu_ingenieria_bilbao), "EHU", "Edif II", getString(R.string.bus) + ": Bilbobus 28, 38.\n" + getString(R.string.tram) + ": " + getString(R.string.stop) + " San Mamés.", 43.2638, -2.9511));
        listaOriginalCompletita.add(new Centro("EHU_364", getString(R.string.centro_ehu_ingenieria_bilbao), "EHU", "Edif II", getString(R.string.bus) + ": Bilbobus 28, 38.\n" + getString(R.string.tram) + ": " + getString(R.string.stop) + " San Mamés.", 43.2638, -2.9511));
        listaOriginalCompletita.add(new Centro("EHU_320", getString(R.string.centro_ehu_bellas_artes), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_310", getString(R.string.centro_ehu_ciencia_tecnologia), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_323", getString(R.string.centro_ehu_ciencias_sociales_comunicacion), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_324", getString(R.string.centro_ehu_derecho), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_321", getString(R.string.centro_ehu_economia_empresa), "EHU", "Sarriko", getString(R.string.metro) + ": Sarriko.\n" + getString(R.string.bus) + ": Bilbobus 13, 18, 71.", 43.2721, -2.9566));
        listaOriginalCompletita.add(new Centro("EHU_351", getString(R.string.centro_ehu_economia_empresa_elcano), "EHU", "Bilbao", getString(R.string.metro) + ": Moyua.\n" + getString(R.string.bus) + ": Bilbobus (Varias " + getString(R.string.line) + "s).", 43.2642, -2.9355));
        listaOriginalCompletita.add(new Centro("EHU_354", getString(R.string.centro_ehu_educacion_bilbao), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_327", getString(R.string.centro_ehu_medicina_enfermeria), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_352", getString(R.string.centro_ehu_medicina_enfermeria), "EHU", "Leioa", getString(R.string.bus) + ": Bizkaibus A2161, A2321.\n" + getString(R.string.bicycle) + ": Parking Leioa.", 43.3301, -2.9678));
        listaOriginalCompletita.add(new Centro("EHU_332", getString(R.string.centro_ehu_unidad_docente_medicina), "EHU", "Galdakao", getString(R.string.bus) + ": Bizkaibus " + getString(R.string.line) + " Galdakao Ospitalea.", 43.2289, -2.8360));
        listaOriginalCompletita.add(new Centro("EHU_328", getString(R.string.centro_ehu_unidad_docente_medicina), "EHU", "Basurto - Bilbao", getString(R.string.tram) + ": Ospitalea/Hospital.\n" + getString(R.string.bus) + ": Bilbobus 28, 58.", 43.2605, -2.9490));
        listaOriginalCompletita.add(new Centro("EHU_329", getString(R.string.centro_ehu_unidad_docente_medicina), "EHU", "Cruces", getString(R.string.metro) + ": Gurutzeta/Cruces.\n" + getString(R.string.bus) + ": Bizkaibus.", 43.2847, -2.9829));
        listaOriginalCompletita.add(new Centro("EHU_EXP", getString(R.string.centro_ehu_aulas_experiencia), "EHU", "Bilbao", getString(R.string.bus) + ": Casco Viejo.\n" + getString(R.string.metro) + ": Zazpikaleak.", 43.2576, -2.9238));

        // --- CENTROS MONDRAGON UNIBERTSITATEA ---
        listaOriginalCompletita.add(new Centro("MU_BBF_EMP", getString(R.string.centro_mu_bbf_empresariales), "Mondragon", "Uribitarte, 6", getString(R.string.bus) + ": Bilbobus " + getString(R.string.line) + " 11.\n" + getString(R.string.bicycle) + ": " + getString(R.string.bilbaobizi_50m) + ".", 43.2665, -2.9304));
        listaOriginalCompletita.add(new Centro("MU_BBF_LEINN", getString(R.string.centro_mu_bbf_leinn), "Mondragon", "Uribitarte, 6", getString(R.string.bus) + ": Bilbobus " + getString(R.string.line) + " 11.\n" + getString(R.string.bicycle) + ": " + getString(R.string.bilbaobizi_50m) + ".", 43.2665, -2.9304));
        listaOriginalCompletita.add(new Centro("MU_AS_POL", getString(R.string.centro_mu_as_politecnica), "Mondragon", "Zorrotzaurre", getString(R.string.bus) + ": Bilbobus " + getString(R.string.line) + " A4.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path) + " Zorrotzaurre.", 43.2764, -2.9642));
        listaOriginalCompletita.add(new Centro("MU_AS_HUM", getString(R.string.centro_mu_as_humanidades), "Mondragon", "Zorrotzaurre", getString(R.string.bus) + ": Bilbobus " + getString(R.string.line) + " A4.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path) + " Zorrotzaurre.", 43.2764, -2.9642));

        // --- CENTROS DEUSTO ---
        listaOriginalCompletita.add(new Centro("DEU_DBS", getString(R.string.centro_deusto_business), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));
        listaOriginalCompletita.add(new Centro("DEU_DER", getString(R.string.centro_deusto_derecho), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));
        listaOriginalCompletita.add(new Centro("DEU_CSH", getString(R.string.centro_deusto_sociales_humanas), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));
        listaOriginalCompletita.add(new Centro("DEU_ING", getString(R.string.centro_deusto_ingenieria), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));
        listaOriginalCompletita.add(new Centro("DEU_EDU", getString(R.string.centro_deusto_educacion_deporte), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));
        listaOriginalCompletita.add(new Centro("DEU_SAL", getString(R.string.centro_deusto_salud), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));
        listaOriginalCompletita.add(new Centro("DEU_CSC", getString(R.string.centro_deusto_sociales_comunicacion), "Deusto", "Deusto", getString(R.string.bus) + ": Bizkaibus A3411, Bilbobus 18, 71.\n" + getString(R.string.bicycle) + ": " + getString(R.string.cycle_path_joined) + ".", 43.2713, -2.9379));

        // Comprobamos en la lista original si hay favoritos guardados
        Set<String> favs = prefs.getStringSet("centros_favoritos", new HashSet<>());
        for (Centro c : listaOriginalCompletita) {
            if (favs.contains(c.getId())) {
                c.setStarred(true);
            }
        }

        // Cargamos la lista visible
        listaCentros = new ArrayList<>(listaOriginalCompletita);

        ordenarLista();
    }

    private void aplicarFiltrosCombinados() {
        List<Centro> listaFiltrada = new ArrayList<>();

        for (Centro c : listaOriginalCompletita) {
            // Filtro por Universidad (Si es "Todas" pasa siempre)
            boolean pasaFiltroUni = filtroUniversidadActual.equals("Todas") || c.getUniversidad().equals(filtroUniversidadActual);

            // Filtro por Texto de Búsqueda
            boolean pasaFiltroTexto = c.getNombre().toLowerCase().contains(busquedaActual.toLowerCase()) ||
                    c.getUbicacion().toLowerCase().contains(busquedaActual.toLowerCase());

            if (pasaFiltroUni && pasaFiltroTexto) {
                listaFiltrada.add(c);
            }
        }

        // Actualizamos la lista visible respetando las coincidencias
        listaCentros.clear();
        listaCentros.addAll(listaFiltrada);
        ordenarLista();

        if (adapter != null) {
            adapter.actualizarListaCompleta(listaCentros);
        }
    }

    private void guardarFavorito(Centro centro) {
        Set<String> favs = new HashSet<>(prefs.getStringSet("centros_favoritos", new HashSet<>()));
        if (centro.isStarred()) {
            favs.add(centro.getId());
        } else {
            favs.remove(centro.getId());
        }
        prefs.edit().putStringSet("centros_favoritos", favs).apply();
    }

    private void ordenarLista() {
        java.util.Collections.sort(listaCentros, new java.util.Comparator<Centro>() {
            @Override
            public int compare(Centro c1, Centro c2) {
                // Si c1 tiene estrella y c2 no, c1 va primero (-1)
                if (c1.isStarred() && !c2.isStarred()) return -1;
                // Si c2 tiene estrella y c1 no, c2 va primero (1)
                if (!c1.isStarred() && c2.isStarred()) return 1;
                // Si los dos son iguales, mantenemos el orden alfabético por nombre
                return c1.getNombre().compareTo(c2.getNombre());
            }
        });

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void actualizarBotonActivo(android.widget.Button btnActivo) {
        //Ponemos todos los botones en gris
        int colorGris = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bilbao_grey_dark);
        btnFiltroTodas.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));
        btnFiltroEHU.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));
        btnFiltroDeusto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));
        btnFiltroMondragon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));

        // Ponemos SOLO el botón pulsado en rojo
        int colorRojo = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bilbao_red);
        btnActivo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorRojo));
    }
}