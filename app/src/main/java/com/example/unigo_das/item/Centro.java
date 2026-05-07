package com.example.unigo_das.item;

public class Centro {
    private String id;
    private String nombre;
    private String universidad;
    private String ubicacion;
    private boolean isStarred;

    // CAMPOS DE TRANSPORTE
    private String infoTransporte;
    private boolean isExpanded;

    // CAMPOS DE COORDENADAS PARA LA RUTA
    private double latitud;
    private double longitud;

    public Centro(String id, String nombre, String universidad, String ubicacion, String infoTransporte, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.universidad = universidad;
        this.ubicacion = ubicacion;
        this.infoTransporte = infoTransporte;
        this.latitud = latitud;
        this.longitud = longitud;

        this.isStarred = false;
        this.isExpanded = false; // Por defecto, la tarjeta está cerrada
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUniversidad() { return universidad; }
    public String getUbicacion() { return ubicacion; }
    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    public String getInfoTransporte() { return infoTransporte; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }

    // --- ¡ESTO ES LO QUE HACE QUE EL BUSCADOR DEL MAPA FUNCIONE! ---
    @Override
    public String toString() {
        return nombre + " (" + universidad + ")";
    }
}