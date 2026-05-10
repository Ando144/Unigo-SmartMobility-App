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
    private OnItemClickListener itemListener;

    public interface OnStarClickListener {
        void onStarClick(Centro centro, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(Centro centro);
    }

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
        final int currentPosition = position;
        final Centro centro = centroList.get(currentPosition);

        holder.tvNombre.setText(centro.getNombre());
        holder.tvUniUbi.setText(centro.getUbicacion());
        String uni = centro.getUniversidad();

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

        if (centro.isStarred()) {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_on);
            holder.ivStar.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_red));
        } else {
            holder.ivStar.setImageResource(android.R.drawable.btn_star_big_off);
            holder.ivStar.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.bilbao_grey_light));
        }

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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemListener != null) {
                    itemListener.onItemClick(centro);
                }
            }
        });

        // --- NUEVA LÓGICA DEL MAPA DESPLEGABLE ---

        // Inyectamos la imagen correspondiente al ID del centro
        holder.ivMapaCentro.setImageResource(obtenerMapaPorCentro(centro.getId()));

        if (centro.isExpanded()) {
            holder.layoutTransporte.setVisibility(View.VISIBLE);
            holder.ivExpand.setRotation(180f);
        } else {
            holder.layoutTransporte.setVisibility(View.GONE);
            holder.ivExpand.setRotation(0f);
        }

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
                                item.getUniversidad().toLowerCase().contains(filterPattern) ||
                                (item.getDescripcion() != null && item.getDescripcion().toLowerCase().contains(filterPattern))) {
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

    // --- DICCIONARIO DE IMÁGENES ---
    private int obtenerMapaPorCentro(String idCentro) {
        switch (idCentro) {
            // ==========================================
            // UPV/EHU
            // ==========================================
            case "EHU_345":
                 return R.drawable.foto_sanma_viejo;
            case "EHU_363":
                 return R.drawable.foto_sanma_nuevo;
            case "EHU_364":
                 return R.drawable.foto_sanma_nuevo;
            case "EHU_350":
                 return R.drawable.foto_nautica;
            case "EHU_320":
                 return R.drawable.foto_bellas_artes;
            case "EHU_310":
                 return R.drawable.foto_ciencia_y_tecnologia;
            case "EHU_323":
                 return R.drawable.foto_sociales_comunicacion;
            case "EHU_324":
                 return R.drawable.foto_derecho;
            case "EHU_321":
                 return R.drawable.foto_sarriko;
            case "EHU_351":
                 return R.drawable.foto_elcano;
            case "EHU_354":
                 return R.drawable.foto_educacion;
            case "EHU_327":
                 return R.drawable.foto_medicina_y_enfermeria;
            case "EHU_352":
                return R.drawable.foto_medicina_y_enfermeria;
            case "EHU_332":
                 return R.drawable.foto_galdakao;
            case "EHU_328":
                 return R.drawable.foto_basurto;
            case "EHU_329":
                 return R.drawable.foto_cruces;
            case "EHU_EXP":
                 return R.drawable.foto_experiencia;

                // ==========================================
                // MONDRAGON UNIBERTSITATEA
                // ==========================================
            case "MU_BBF_EMP":
                 return R.drawable.foto_mondragon_empresariales_leinn;
            case "MU_BBF_LEINN":
                return R.drawable.foto_mondragon_empresariales_leinn;
            case "MU_AS_POL":
                 return R.drawable.foto_as_fabrik;
            case "MU_AS_HUM":
                return R.drawable.foto_as_fabrik;

                // ==========================================
                // UNIVERSIDAD DE DEUSTO
                // ==========================================
            case "DEU_DBS":
                 return R.drawable.foto_deusto;
            case "DEU_DER":
                return R.drawable.foto_deusto;
            case "DEU_CSH":
                return R.drawable.foto_deusto;
            case "DEU_ING":
                return R.drawable.foto_deusto;
            case "DEU_EDU":
                return R.drawable.foto_deusto;
            case "DEU_SAL":
                return R.drawable.foto_deusto;
            case "DEU_CSC":
                return R.drawable.foto_deusto;

            default:
                // Devuelve una imagen gris por defecto si aún no tienes el plano de ese centro
                // IMPORTANTE: Asegúrate de tener una imagen llamada ic_menu_school (o cámbialo por otro nombre válido)
                return R.drawable.ic_menu_school;
        }
    }

    static class CentroViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvUniUbi;
        ImageView ivStar, ivLogo, ivExpand, ivMapaCentro; // Añadido ivMapaCentro
        View layoutTransporte;

        CentroViewHolder(View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCentro);
            tvUniUbi = itemView.findViewById(R.id.tvUniUbi);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivLogo = itemView.findViewById(R.id.ivLogoCentro);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            layoutTransporte = itemView.findViewById(R.id.layoutTransporte);

            // Enlazamos el nuevo ImageView y quitamos el TextView antiguo
            ivMapaCentro = itemView.findViewById(R.id.ivMapaCentro);
        }
    }
}