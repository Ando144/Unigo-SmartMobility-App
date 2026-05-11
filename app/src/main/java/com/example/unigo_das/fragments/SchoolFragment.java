package com.example.unigo_das.fragments;

import android.content.Context;
import android.content.DialogInterface;
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
import java.util.List;

import com.example.unigo_das.R;
import com.example.unigo_das.activities.MainActivity;
import com.example.unigo_das.adapters.CentroAdapter;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.item.Centro;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SchoolFragment extends Fragment {

    private RecyclerView rvCentros;
    private CentroAdapter adapter;
    private List<Centro> listaCentros;
    // Guardamos una copia intacta de la BD para poder filtrar sin tener que hacer queries contínuas a SQLite
    private List<Centro> listaOriginalCompletita;
    private SearchView searchView;

    private android.widget.Button btnFiltroTodas, btnFiltroEHU, btnFiltroDeusto, btnFiltroMondragon;

    private DataBaseHelper dbHelper;

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

        dbHelper = new DataBaseHelper(requireContext());

        cargarDatosDesdeBD();

        rvCentros.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CentroAdapter(listaCentros, new CentroAdapter.OnStarClickListener() {
            @Override
            public void onStarClick(Centro centro, int position) {
                guardarFavorito(centro);
                ordenarLista(); // Re-ordenamos para que el nuevo favorito suba arriba automáticamente
            }
        }, new CentroAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Centro centro) {
                // Al hacer clic en un centro, lanzamos el diálogo de confirmación.
                // Si el usuario acepta, delegamos la acción al MainActivity, que es quien controla el FragmentManager
                // y puede inyectar los datos en el mapa sin romper la pila.
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.ruta_al_campus)
                        .setIcon(R.drawable.ic_menu_school)
                        .setMessage(getString(R.string.quieres_abrir_el_mapa_para_ver_c_mo_llegar_a) + centro.getNombre() + "?")
                        .setPositiveButton(R.string.s_trazar_ruta, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((MainActivity) requireActivity())
                                        .irRutaEnMapa(centro.getNombre(), centro.getLatitud(), centro.getLongitud());
                            }
                        })
                        .setNegativeButton(R.string.cancelar2, null)
                        .show();
            }
        });
        rvCentros.setAdapter(adapter);

        btnFiltroTodas = view.findViewById(R.id.btnFiltroTodas);
        btnFiltroEHU = view.findViewById(R.id.btnFiltroEHU);
        btnFiltroDeusto = view.findViewById(R.id.btnFiltroDeusto);
        btnFiltroMondragon = view.findViewById(R.id.btnFiltroMondragon);

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
                filtroUniversidadActual = "UPV/EHU";
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                busquedaActual = newText;
                aplicarFiltrosCombinados();
                return false;
            }
        });

        return view;
    }

    // Cargamos todos los centros de golpe desde SQLite y los cruzamos con los favoritos del usuario actual.
    // Hacemos esto una sola vez  para optimizar memoria.
    private void cargarDatosDesdeBD() {
        listaOriginalCompletita = dbHelper.obtenerTodosLosCentros();

        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        int idUsuarioActual = prefs.getInt("user_id", 0);
        List<String> favs = dbHelper.obtenerIdsFavoritosUsuario(idUsuarioActual);

        for (Centro c : listaOriginalCompletita) {
            if (favs.contains(c.getId())) {
                c.setStarred(true);
            }
        }

        listaCentros = new ArrayList<>(listaOriginalCompletita);
        ordenarLista();
    }

    // Metodo central del buscador. Combina el texto escrito con el botón de universidad pulsado.
    // Filtramos siempre leyendo de 'listaOriginalCompletita' y machacamos 'listaCentros' con el resultado.
    private void aplicarFiltrosCombinados() {
        List<Centro> listaFiltrada = new ArrayList<>();

        for (Centro c : listaOriginalCompletita) {
            boolean pasaFiltroUni = filtroUniversidadActual.equals("Todas") || c.getUniversidad().equals(filtroUniversidadActual);

            boolean pasaFiltroTexto = c.getNombre().toLowerCase().contains(busquedaActual.toLowerCase()) ||
                    (c.getDescripcion() != null && c.getDescripcion().toLowerCase().contains(busquedaActual.toLowerCase())) ||
                    c.getUniversidad().toLowerCase().contains(busquedaActual.toLowerCase());

            // Solo añadimos si cumple tanto el botón de universidad como lo escrito en el buscador
            if (pasaFiltroUni && pasaFiltroTexto) {
                listaFiltrada.add(c);
            }
        }

        listaCentros.clear();
        listaCentros.addAll(listaFiltrada);
        ordenarLista();

        if (adapter != null) {
            adapter.actualizarListaCompleta(listaCentros);
        }
    }

    // Sincroniza la acción visual de la estrellita en el RecyclerView con la base de datos real.
    // Usamos el ID de las SharedPreferences para asegurar que cada cuenta tenga su propia lista separada.
    private void guardarFavorito(Centro centro) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        int idUsuarioActual = prefs.getInt("user_id", 0);

        if (centro.isStarred()) {
            dbHelper.anadirFavorito(idUsuarioActual, centro.getId());
        } else {
            dbHelper.borrarFavorito(idUsuarioActual, centro.getId());
        }
    }

    //  Prioridad a los centros marcados como favoritos .
    // Si tienen el mismo estado (los dos con estrella, o los dos sin ella),  alfabéticamente.
    private void ordenarLista() {
        java.util.Collections.sort(listaCentros, new java.util.Comparator<Centro>() {
            @Override
            public int compare(Centro c1, Centro c2) {
                if (c1.isStarred() && !c2.isStarred()) return -1;
                if (!c1.isStarred() && c2.isStarred()) return 1;
                return c1.getNombre().compareTo(c2.getNombre());
            }
        });

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void actualizarBotonActivo(android.widget.Button btnActivo) {
        int colorGris = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bilbao_grey_dark);
        btnFiltroTodas.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));
        btnFiltroEHU.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));
        btnFiltroDeusto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));
        btnFiltroMondragon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorGris));

        int colorRojo = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bilbao_red);
        btnActivo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorRojo));
    }
}