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
import com.example.unigo_das.item.Centro;

public class CentroAdapter extends RecyclerView.Adapter<CentroAdapter.CentroViewHolder> implements Filterable {

    private List<Centro> centroListFull;
    private List<Centro> centroList;
    private OnStarClickListener starClickListener;
    private OnItemClickListener itemListener; // 1. NUEVO: Variable para el clic general

    // Interfaz para avisar al Fragment cuando se pulsa una estrella
    public interface OnStarClickListener {
        void onStarClick(Centro centro, int position);
    }

    // 2. NUEVO: Interfaz para el clic en toda la tarjeta
    public interface OnItemClickListener {
        void onItemClick(Centro centro);
    }

    // 3. ACTUALIZADO: El constructor ahora recibe ambas interfaces
    public CentroAdapter(List<Centro> centroList, OnStarClickListener starListener, OnItemClickListener itemListener) {
        this.centroListFull = new ArrayList<>(centroList);
        this.centroList = centroList;
        this.starClickListener = starListener;
        this.itemListener = itemListener;
    }

    @NonNull
    @Override
    public CentroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_centro, parent, false);
        return new CentroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CentroViewHolder holder, int position) {
        // Hacemos la posición final para poder usarla dentro de las clases anónimas (estándar en Java clásico)
        final int currentPosition = position;
        final Centro centro = centroList.get(currentPosition);

        holder.tvNombre.setText(centro.getNombre());
        holder.tvUniUbi.setText(centro.getUniversidad() + " - " + centro.getUbicacion());

        String uni = centro.getUniversidad();

        // Limpiamos cualquier tinte
        holder.ivLogo.clearColorFilter();

        if ("EHU".equals(uni)) {
            holder.ivLogo.setImageResource(R.drawable.logo_ehu);
        } else if ("Deusto".equals(uni)) {
            holder.ivLogo.setImageResource(R.drawable.logo_deusto);
        } else if ("Mondragon".equals(uni)) {
            holder.ivLogo.setImageResource(R.drawable.logo_mondragon);
        } else {
            // Si no hay logo, usamos el birrete genérico
            holder.ivLogo.setImageResource(R.drawable.ic_menu_school);
            holder.ivLogo.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_grey_dark));
        }

        if (centro.isStarred()) {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_on);
            holder.ivStar.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_red));
        } else {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_off);
            holder.ivStar.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_grey_light));
        }

        // Clic de la estrella con sintaxis clásica
        holder.ivStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centro.setStarred(!centro.isStarred());
                notifyItemChanged(currentPosition);
                if (starClickListener != null) {
                    starClickListener.onStarClick(centro, currentPosition);
                }
            }
        });

        // 4. NUEVO: Evento de clic para TODA la tarjeta (Activa la ruta en el mapa)
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemListener != null) {
                    itemListener.onItemClick(centro);
                }
            }
        });

        // --- LÓGICA DEL DESPLEGABLE DE TRANSPORTE ---
        holder.tvTransporteInfo.setText(centro.getInfoTransporte());

        // Mostrar u ocultar según el estado y rotar flecha
        if (centro.isExpanded()) {
            holder.layoutTransporte.setVisibility(View.VISIBLE);
            holder.ivExpand.setRotation(180f); // La flecha apunta hacia arriba
        } else {
            holder.layoutTransporte.setVisibility(View.GONE);
            holder.ivExpand.setRotation(0f); // La flecha apunta hacia abajo
        }

        // Clic del icono de información con sintaxis clásica
        holder.ivExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centro.setExpanded(!centro.isExpanded());
                notifyItemChanged(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return centroList.size();
    }

    public void actualizarListaCompleta(List<Centro> nuevaLista) {
        this.centroList = nuevaLista;
        notifyDataSetChanged();
    }

    // Buscador
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Centro> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(centroListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Centro item : centroListFull) {
                        if (item.getNombre().toLowerCase().contains(filterPattern) ||
                                item.getUniversidad().toLowerCase().contains(filterPattern)) {
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
                centroList.clear();
                centroList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class CentroViewHolder extends RecyclerView.ViewHolder {
        // VARIABLES AÑADIDAS AQUÍ PARA QUE NO FALLE EL CONSTRUCTOR
        TextView tvNombre, tvUniUbi, tvTransporteInfo;
        ImageView ivStar, ivLogo, ivExpand;
        View layoutTransporte;

        CentroViewHolder(View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCentro);
            tvUniUbi = itemView.findViewById(R.id.tvUniUbi);
            tvTransporteInfo = itemView.findViewById(R.id.tvTransporteInfo);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivLogo = itemView.findViewById(R.id.ivLogoCentro);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            layoutTransporte = itemView.findViewById(R.id.layoutTransporte);
        }
    }
}