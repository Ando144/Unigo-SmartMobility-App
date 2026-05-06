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

        rvCentros = view.findViewById(R.id.rvCentros);
        searchView = view.findViewById(R.id.searchViewCentros);
        prefs = requireActivity().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);

        cargarDatosMocK();

        rvCentros.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CentroAdapter(listaCentros, new CentroAdapter.OnStarClickListener() {
            @Override
            public void onStarClick(Centro centro, int position) {
                guardarFavorito(centro);
                ordenarLista();
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

        // Se añaden los datos integrando la información de Bilbobus, Bizkaibus y red de transporte de Euskadi

        // --- CENTROS EHU ---
        listaOriginalCompletita.add(new Centro("EHU_345", "Escuela de Ingeniería de Bilbao", "EHU", "Edif II", "Autobús: Bilbobus 28, 38, 62.\nTranvía: Parada San Mamés."));
        listaOriginalCompletita.add(new Centro("EHU_350", "Escuela de Ingeniería de Bilbao", "EHU", "Náutica", "Autobús: Bizkaibus A3411.\nBicicleta: Bidegorri cercano."));
        listaOriginalCompletita.add(new Centro("EHU_363", "Escuela de Ingeniería de Bilbao", "EHU", "Edif II", "Autobús: Bilbobus 28, 38.\nTranvía: Parada San Mamés."));
        listaOriginalCompletita.add(new Centro("EHU_364", "Escuela de Ingeniería de Bilbao", "EHU", "Edif II", "Autobús: Bilbobus 28, 38.\nTranvía: Parada San Mamés."));
        listaOriginalCompletita.add(new Centro("EHU_320", "Facultad de Bellas Artes", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_310", "Facultad de Ciencia y Tecnología", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_323", "Facultad de Ciencias Sociales y Comunicación", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_324", "Facultad de Derecho. Sección bizkaia", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_321", "Facultad de Economía y Empresa", "EHU", "Sarriko", "Metro: Sarriko.\nAutobús: Bilbobus 13, 18, 71."));
        listaOriginalCompletita.add(new Centro("EHU_351", "Facultad de Economía y Empresa. Sección Elkano", "EHU", "Bilbao", "Metro: Moyua.\nAutobús: Bilbobus varias líneas."));
        listaOriginalCompletita.add(new Centro("EHU_354", "Facultad de Educación de Bilbao", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_327", "Facultad de Medicina y Enfermería", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_352", "Facultad de Medicina y Enfermería", "EHU", "Leioa", "Autobús: Bizkaibus A2161, A2321.\nBicicleta: Parking Leioa."));
        listaOriginalCompletita.add(new Centro("EHU_332", "Unidad Docente Fac. Med. y Enfermería", "EHU", "Galdakao", "Autobús: Bizkaibus Línea Galdakao Ospitalea."));
        listaOriginalCompletita.add(new Centro("EHU_328", "Unidad Docente Fac. Med. y Enfermería", "EHU", "Basurto - Bilbao", "Tranvía: Ospitalea/Hospital.\nAutobús: Bilbobus 28, 58."));
        listaOriginalCompletita.add(new Centro("EHU_329", "Unidad Docente Fac. Med. y Enfermería", "EHU", "Cruces", "Metro: Gurutzeta/Cruces.\nAutobús: Bizkaibus."));
        listaOriginalCompletita.add(new Centro("EHU_EXP", "Aulas de la Experiencia", "EHU", "Bilbao", "Autobús: Casco Viejo.\nMetro: Zazpikaleak."));

        // --- CENTROS MONDRAGON UNIBERTSITATEA ---
        listaOriginalCompletita.add(new Centro("MU_BBF_EMP", "Bilbao Berrikuntza Faktoria (BBF): Empresariales", "Mondragon", "Uribitarte, 6", "Autobús: Bilbobus Línea 11.\nBicicleta: Estación Bilbaobizi a 50m."));
        listaOriginalCompletita.add(new Centro("MU_BBF_LEINN", "Bilbao Berrikuntza Faktoria (BBF): LEINN", "Mondragon", "Uribitarte, 6", "Autobús: Bilbobus Línea 11.\nBicicleta: Estación Bilbaobizi a 50m."));
        listaOriginalCompletita.add(new Centro("MU_AS_POL", "As Fabrik: Escuela Politécnica Superior", "Mondragon", "Zorrotzaurre", "Autobús: Bilbobus Línea A4.\nBicicleta: Bidegorri Zorrotzaurre."));
        listaOriginalCompletita.add(new Centro("MU_AS_HUM", "As Fabrik: Fac. Humanidades y Ciencias Edu.", "Mondragon", "Zorrotzaurre", "Autobús: Bilbobus Línea A4.\nBicicleta: Bidegorri Zorrotzaurre."));

        // --- CENTROS DEUSTO ---
        listaOriginalCompletita.add(new Centro("DEU_DBS", "Deustu Business School", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));
        listaOriginalCompletita.add(new Centro("DEU_DER", "Facultad de Derecho", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));
        listaOriginalCompletita.add(new Centro("DEU_CSH", "Facultad de Ciencias Sociales y Humanas", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));
        listaOriginalCompletita.add(new Centro("DEU_ING", "Facultad de Ingeniería", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));
        listaOriginalCompletita.add(new Centro("DEU_EDU", "Facultad de Educación y Deporte", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));
        listaOriginalCompletita.add(new Centro("DEU_SAL", "Facultad de Ciencias de la Salud", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));
        listaOriginalCompletita.add(new Centro("DEU_CSC", "Facultad de Ciencias Sociales y Comunicación", "Deusto", "Deusto", "Autobús: Bizkaibus A3411, Bilbobus 18, 71.\nBicicleta: Carril bici conectado."));

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