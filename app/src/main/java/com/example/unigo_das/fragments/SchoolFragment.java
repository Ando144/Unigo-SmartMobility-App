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
import com.example.unigo_das.adapters.CampusAdapter;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.item.Campus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SchoolFragment extends Fragment {

    private RecyclerView rvCampuses;
    private CampusAdapter adapter;
    private List<Campus> campusItems;
    // Keep an untouched copy of the DB data so we can filter without repeated SQLite queries
    private List<Campus> fullOriginalList;
    private SearchView searchView;

    private android.widget.Button btnFilterAll, btnFilterEhu, btnFilterDeusto, btnFilterMondragon;

    private DataBaseHelper dbHelper;

    private String currentUniversityFilter = "Todas";
    private String currentSearch = "";

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

        rvCampuses = view.findViewById(R.id.rvCampuses);
        searchView = view.findViewById(R.id.searchViewCampuses);

        dbHelper = new DataBaseHelper(requireContext());

        loadDataFromDb();

        rvCampuses.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CampusAdapter(campusItems, new CampusAdapter.OnStarClickListener() {
            @Override
            public void onStarClick(Campus campus, int position) {
                saveFavorite(campus);
                sortList(); // Sort again so the new favourite moves to the top automatically
            }
        }, new CampusAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Campus campus) {
                // When a campus is tapped, show the confirmation dialog.
                // If the user accepts, the action is delegated to MainActivity, which controls the FragmentManager
                // and can pass the data to the map without breaking the back stack.
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.route_to_campus)
                        .setIcon(R.drawable.ic_menu_school)
                        .setMessage(getString(R.string.open_map_to_route_question) + campus.getName() + "?")
                        .setPositiveButton(R.string.yes_draw_route, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((MainActivity) requireActivity())
                                        .showRouteOnMap(campus.getName(), campus.getLatitude(), campus.getLongitude());
                            }
                        })
                        .setNegativeButton(R.string.cancel2, null)
                        .show();
            }
        });
        rvCampuses.setAdapter(adapter);

        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterEhu = view.findViewById(R.id.btnFilterEhu);
        btnFilterDeusto = view.findViewById(R.id.btnFilterDeusto);
        btnFilterMondragon = view.findViewById(R.id.btnFilterMondragon);

        updateActiveButton(btnFilterAll);

        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentUniversityFilter = "Todas";
                applyCombinedFilters();
                updateActiveButton(btnFilterAll);
            }
        });

        btnFilterEhu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentUniversityFilter = "UPV/EHU";
                applyCombinedFilters();
                updateActiveButton(btnFilterEhu);
            }
        });

        btnFilterDeusto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentUniversityFilter = "Deusto";
                applyCombinedFilters();
                updateActiveButton(btnFilterDeusto);
            }
        });

        btnFilterMondragon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentUniversityFilter = "Mondragon";
                applyCombinedFilters();
                updateActiveButton(btnFilterMondragon);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearch = newText;
                applyCombinedFilters();
                return false;
            }
        });

        return view;
    }

    // Load all the campuses from SQLite at once and combine them with the current user's favourites.
    // This is done only once to save memory.
    private void loadDataFromDb() {
        fullOriginalList = dbHelper.getAllCampuses();

        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("user_id", 0);
        List<String> favs = dbHelper.getUserFavoriteIds(currentUserId);

        for (Campus c : fullOriginalList) {
            if (favs.contains(c.getId())) {
                c.setStarred(true);
            }
        }

        campusItems = new ArrayList<>(fullOriginalList);
        sortList();
    }

    // Main search method. Combines the typed text with the selected university button.
    // Always filter from 'fullOriginalList' and overwrite 'campusItems' with the result.
    private void applyCombinedFilters() {
        List<Campus> filteredCampuses = new ArrayList<>();

        for (Campus c : fullOriginalList) {
            boolean passesUniFilter = currentUniversityFilter.equals("Todas") || c.getUniversity().equals(currentUniversityFilter);

            boolean passesTextFilter = c.getName().toLowerCase().contains(currentSearch.toLowerCase()) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(currentSearch.toLowerCase())) ||
                    c.getUniversity().toLowerCase().contains(currentSearch.toLowerCase());

            // Only add it if it matches both the university button and the search text
            if (passesUniFilter && passesTextFilter) {
                filteredCampuses.add(c);
            }
        }

        campusItems.clear();
        campusItems.addAll(filteredCampuses);
        sortList();

        if (adapter != null) {
            adapter.updateFullList(campusItems);
        }
    }

    // Syncs the visual star in the RecyclerView with the real database.
    // The ID from SharedPreferences is used so that each account has its own separate list.
    private void saveFavorite(Campus campus) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("user_id", 0);

        if (campus.isStarred()) {
            dbHelper.addFavorite(currentUserId, campus.getId());
        } else {
            dbHelper.removeFavorite(currentUserId, campus.getId());
        }
    }

    // Favourite campuses go first.
    // If both have the same state (both starred, or both not), sort alphabetically.
    private void sortList() {
        java.util.Collections.sort(campusItems, new java.util.Comparator<Campus>() {
            @Override
            public int compare(Campus c1, Campus c2) {
                if (c1.isStarred() && !c2.isStarred()) return -1;
                if (!c1.isStarred() && c2.isStarred()) return 1;
                return c1.getName().compareTo(c2.getName());
            }
        });

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateActiveButton(android.widget.Button btnActive) {
        int greyColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bilbao_grey_dark);
        btnFilterAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(greyColor));
        btnFilterEhu.setBackgroundTintList(android.content.res.ColorStateList.valueOf(greyColor));
        btnFilterDeusto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(greyColor));
        btnFilterMondragon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(greyColor));

        int redColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bilbao_red);
        btnActive.setBackgroundTintList(android.content.res.ColorStateList.valueOf(redColor));
    }
}