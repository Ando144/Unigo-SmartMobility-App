package com.example.unigo_das.item;

public class Parada {
    private String id;
    private String nombre;
    private String tipo;
    private double latitud;
    private double longitud;

    public Parada(String id, String nombre, String tipo, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}