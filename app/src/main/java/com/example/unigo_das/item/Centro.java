package com.example.unigo_das.item;

public class Centro {
    private String id;
    private String nombre;
    private String universidad;
    private String descripcion;
    private String ubicacion; // RECUPERADO
    private boolean isStarred;
    private boolean isExpanded;
    private double latitud;
    private double longitud;

    public Centro(String id, String nombre, String universidad, String descripcion, String ubicacion, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.universidad = universidad;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.latitud = latitud;
        this.longitud = longitud;

        this.isStarred = false;
        this.isExpanded = false;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUniversidad() { return universidad; }
    public String getDescripcion() { return descripcion; }
    public String getUbicacion() { return ubicacion; } // NUEVO GETTER

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { this.isStarred = starred; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }

    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }

    @Override
    public String toString() {
        return nombre + " (" + universidad + ")";
    }
}