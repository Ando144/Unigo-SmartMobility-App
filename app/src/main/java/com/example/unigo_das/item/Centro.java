package com.example.unigo_das.item;

public class Centro {
    private String id;
    private String nombre;
    private String universidad;
    private String ubicacion;
    private boolean isStarred;

    // NUEVOS CAMPOS
    private String infoTransporte;
    private boolean isExpanded;

    public Centro(String id, String nombre, String universidad, String ubicacion, String infoTransporte) {
        this.id = id;
        this.nombre = nombre;
        this.universidad = universidad;
        this.ubicacion = ubicacion;
        this.infoTransporte = infoTransporte;
        this.isStarred = false;
        this.isExpanded = false; // Por defecto, la tarjeta está cerrada
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUniversidad() { return universidad; }
    public String getUbicacion() { return ubicacion; }
    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    // NUEVOS GETTERS Y SETTERS
    public String getInfoTransporte() { return infoTransporte; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}