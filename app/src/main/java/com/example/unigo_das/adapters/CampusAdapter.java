package com.example.unigo_das.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.example.unigo_das.R;
import com.example.unigo_das.item.Campus;

public class CampusAdapter extends RecyclerView.Adapter<CampusAdapter.CampusViewHolder> implements Filterable {

    private List<Campus> campusListFull;
    private List<Campus> campusList;
    private OnStarClickListener starClickListener;
    private OnItemClickListener itemListener;

    public interface OnStarClickListener {
        void onStarClick(Campus campus, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(Campus campus);
    }

    public CampusAdapter(List<Campus> campusList, OnStarClickListener starListener, OnItemClickListener itemListener) {
        this.campusListFull = new ArrayList<>(campusList);
        this.campusList = campusList;
        this.starClickListener = starListener;
        this.itemListener = itemListener;
    }

    @NonNull
    @Override
    public CampusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campus, parent, false);
        return new CampusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CampusViewHolder holder, int position) {
        final int currentPosition = position;
        final Campus campus = campusList.get(currentPosition);

        holder.tvCampusName.setText(campus.getName());
        holder.tvUniLocation.setText(campus.getDistrict());
        String uni = campus.getUniversity();

        holder.ivLogo.clearColorFilter();

        if ("UPV/EHU".equals(uni) || "EHU".equals(uni)) {
            holder.ivLogo.setImageResource(R.drawable.logo_ehu);
        } else if ("Deusto".equals(uni)) {
            holder.ivLogo.setImageResource(R.drawable.logo_deusto);
        } else if ("Mondragon".equals(uni)) {
            holder.ivLogo.setImageResource(R.drawable.logo_mondragon);
        } else {
            holder.ivLogo.setImageResource(R.drawable.ic_menu_school);
            holder.ivLogo.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_grey_dark));
        }

        if (campus.isStarred()) {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_on);
            holder.ivStar.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_red));
        } else {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_off);
            holder.ivStar.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_grey_light));
        }

        holder.ivStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                campus.setStarred(!campus.isStarred());
                notifyItemChanged(currentPosition);
                if (starClickListener != null) {
                    starClickListener.onStarClick(campus, currentPosition);
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemListener != null) {
                    itemListener.onItemClick(campus);
                }
            }
        });

        // --- EXPANDABLE MAP LOGIC ---

        // Set the photo that corresponds to the campus ID
        holder.ivCampusPhoto.setImageResource(getPhotoForCampus(campus.getId()));

        if (campus.isExpanded()) {
            holder.layoutTransport.setVisibility(View.VISIBLE);
            holder.ivExpand.setRotation(180f);
        } else {
            holder.layoutTransport.setVisibility(View.GONE);
            holder.ivExpand.setRotation(0f);
        }

        holder.ivExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                campus.setExpanded(!campus.isExpanded());
                notifyItemChanged(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return campusList.size();
    }

    public void updateFullList(List<Campus> newList) {
        this.campusList = newList;
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Campus> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(campusListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Campus item : campusListFull) {
                        if (item.getName().toLowerCase().contains(filterPattern) ||
                                item.getUniversity().toLowerCase().contains(filterPattern) ||
                                (item.getDescription() != null && item.getDescription().toLowerCase().contains(filterPattern))) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                campusList.clear();
                campusList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    // --- IMAGE DICTIONARY ---
    private int getPhotoForCampus(String campusCode) {
        switch (campusCode) {
            // ==========================================
            // UPV/EHU
            // ==========================================
            case "EHU_345":
                 return R.drawable.photo_sanmames_old;
            case "EHU_363":
                 return R.drawable.photo_sanmames_new;
            case "EHU_364":
                 return R.drawable.photo_sanmames_new;
            case "EHU_350":
                 return R.drawable.photo_nautical;
            case "EHU_320":
                 return R.drawable.photo_fine_arts;
            case "EHU_310":
                 return R.drawable.photo_science_technology;
            case "EHU_323":
                 return R.drawable.photo_social_communication;
            case "EHU_324":
                 return R.drawable.photo_law;
            case "EHU_321":
                 return R.drawable.photo_sarriko;
            case "EHU_351":
                 return R.drawable.photo_elcano;
            case "EHU_354":
                 return R.drawable.photo_education;
            case "EHU_327":
                 return R.drawable.photo_medicine_nursing;
            case "EHU_352":
                return R.drawable.photo_medicine_nursing;
            case "EHU_332":
                 return R.drawable.photo_galdakao;
            case "EHU_328":
                 return R.drawable.photo_basurto;
            case "EHU_329":
                 return R.drawable.photo_cruces;
            case "EHU_EXP":
                 return R.drawable.photo_experience;

                // ==========================================
                // MONDRAGON UNIBERTSITATEA
                // ==========================================
            case "MU_BBF_EMP":
                 return R.drawable.photo_mondragon_business_leinn;
            case "MU_BBF_LEINN":
                return R.drawable.photo_mondragon_business_leinn;
            case "MU_AS_POL":
                 return R.drawable.photo_as_fabrik;
            case "MU_AS_HUM":
                return R.drawable.photo_as_fabrik;

                // ==========================================
                // UNIVERSITY OF DEUSTO
                // ==========================================
            case "DEU_DBS":
                 return R.drawable.photo_deusto;
            case "DEU_DER":
                return R.drawable.photo_deusto;
            case "DEU_CSH":
                return R.drawable.photo_deusto;
            case "DEU_ING":
                return R.drawable.photo_deusto;
            case "DEU_EDU":
                return R.drawable.photo_deusto;
            case "DEU_SAL":
                return R.drawable.photo_deusto;
            case "DEU_CSC":
                return R.drawable.photo_deusto;

            default:

                return R.drawable.ic_menu_school;
        }
    }

    static class CampusViewHolder extends RecyclerView.ViewHolder {
        TextView tvCampusName, tvUniLocation;
        ImageView ivStar, ivLogo, ivExpand, ivCampusPhoto;
        View layoutTransport;

        CampusViewHolder(View itemView) {
            super(itemView);
            tvCampusName = itemView.findViewById(R.id.tvCampusTitle);
            tvUniLocation = itemView.findViewById(R.id.tvUniLocation);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivLogo = itemView.findViewById(R.id.ivCampusLogo);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            layoutTransport = itemView.findViewById(R.id.layoutTransport);

            ivCampusPhoto = itemView.findViewById(R.id.ivCampusPhoto);
        }
    }
}