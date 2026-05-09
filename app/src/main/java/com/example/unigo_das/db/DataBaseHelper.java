package com.example.unigo_das.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import com.example.unigo_das.item.Centro;
import java.util.ArrayList;
import java.util.List;
import com.example.unigo_das.item.Parada;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Unigo.db";
    private static final int DATABASE_VERSION = 3; // SUBIMOS A VERSIÓN 3

    public DataBaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE Usuarios (" +
                "id_usuario INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "nombre VARCHAR(255), " +
                "email VARCHAR(255) UNIQUE, " +
                "password VARCHAR(255))");

        sqLiteDatabase.execSQL("CREATE TABLE Centros (" +
                "id_centro VARCHAR(255) PRIMARY KEY, " +
                "nombre VARCHAR(255), " +
                "universidad VARCHAR(255), " +
                "descripcion TEXT, " +
                "ubicacion VARCHAR(255), " +
                "latitud REAL, " +
                "longitud REAL)");

        sqLiteDatabase.execSQL("CREATE TABLE Favoritos (" +
                "id_usuario INTEGER, " +
                "id_centro VARCHAR(255), " +
                "PRIMARY KEY (id_usuario, id_centro), " +
                "FOREIGN KEY(id_usuario) REFERENCES Usuarios(id_usuario) ON DELETE CASCADE, " +
                "FOREIGN KEY(id_centro) REFERENCES Centros(id_centro) ON DELETE CASCADE)");

        sqLiteDatabase.execSQL("CREATE TABLE Paradas (" +
                "id_parada VARCHAR(255) PRIMARY KEY, " +
                "nombre VARCHAR(255), " +
                "tipo_transporte VARCHAR(255), " +
                "latitud REAL, " +
                "longitud REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Favoritos");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Paradas");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Centros");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Usuarios");
        onCreate(sqLiteDatabase);
    }

    public boolean insertarUsuario(String nombre, String email, String password) {
        SQLiteDatabase bd = getWritableDatabase();
        ContentValues nuevo = new ContentValues();
        nuevo.put("nombre", nombre);
        nuevo.put("email", email);
        nuevo.put("password", password);
        long resultado = bd.insert("Usuarios", null, nuevo);
        bd.close();
        return resultado != -1;
    }

    public int comprobarLogin(String email, String password) {
        SQLiteDatabase bd = getReadableDatabase();
        String[] argumentos = new String[] {email, password};
        Cursor c = bd.rawQuery("SELECT id_usuario FROM Usuarios WHERE email=? AND password=?", argumentos);
        int idUsuario = -1;
        if (c.moveToFirst()) idUsuario = c.getInt(0);
        c.close();
        bd.close();
        return idUsuario;
    }

    // AÑADIDO PARÁMETRO UBICACION - Modificado para actualizar si ya existe (para cambio de idioma)
    public boolean insertarCentro(String id_centro, String nombre, String universidad, String descripcion, String ubicacion, double latitud, double longitud) {
        SQLiteDatabase bd = getWritableDatabase();
        ContentValues nuevo = new ContentValues();
        nuevo.put("id_centro", id_centro);
        nuevo.put("nombre", nombre);
        nuevo.put("universidad", universidad);
        nuevo.put("descripcion", descripcion);
        nuevo.put("ubicacion", ubicacion);
        nuevo.put("latitud", latitud);
        nuevo.put("longitud", longitud);
        
        // Usamos IGNORE para no fallar si existe, y luego UPDATE para refrescar los campos de texto
        long resultado = bd.insertWithOnConflict("Centros", null, nuevo, SQLiteDatabase.CONFLICT_IGNORE);
        if (resultado == -1) {
            bd.update("Centros", nuevo, "id_centro=?", new String[]{id_centro});
        }
        bd.close();
        return true;
    }

    public boolean centrosEstaVacia() {
        SQLiteDatabase bd = getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT COUNT(*) FROM Centros", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        bd.close();
        return count == 0;
    }

    public Centro obtenerCentroPorId(String id_centro) {
        SQLiteDatabase bd = getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT * FROM Centros WHERE id_centro = ?", new String[]{id_centro});
        Centro centro = null;
        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow("id_centro"));
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            String uni = cursor.getString(cursor.getColumnIndexOrThrow("universidad"));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
            String ubi = cursor.getString(cursor.getColumnIndexOrThrow("ubicacion"));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud"));
            double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"));
            centro = new Centro(id, nombre, uni, desc, ubi, lat, lon);
        }
        cursor.close();
        bd.close();
        return centro;
    }

    public List<Centro> obtenerTodosLosCentros() {
        List<Centro> listaCentros = new ArrayList<>();
        SQLiteDatabase bd = getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT * FROM Centros ORDER BY nombre ASC", null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id_centro"));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String uni = cursor.getString(cursor.getColumnIndexOrThrow("universidad"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                String ubi = cursor.getString(cursor.getColumnIndexOrThrow("ubicacion"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"));
                listaCentros.add(new Centro(id, nombre, uni, desc, ubi, lat, lon));
            } while (cursor.moveToNext());
        }
        cursor.close();
        bd.close();
        return listaCentros;
    }

    public boolean anadirFavorito(int id_usuario, String id_centro) {
        SQLiteDatabase bd = getWritableDatabase();
        ContentValues nuevo = new ContentValues();
        nuevo.put("id_usuario", id_usuario);
        nuevo.put("id_centro", id_centro);
        long resultado = bd.insertWithOnConflict("Favoritos", null, nuevo, SQLiteDatabase.CONFLICT_IGNORE);
        bd.close();
        return resultado != -1;
    }

    public void borrarFavorito(int id_usuario, String id_centro) {
        SQLiteDatabase bd = getWritableDatabase();
        String[] argumentos = new String[]{String.valueOf(id_usuario), id_centro};
        bd.delete("Favoritos", "id_usuario=? AND id_centro=?", argumentos);
        bd.close();
    }

    public List<String> obtenerIdsFavoritosUsuario(int id_usuario) {
        List<String> listaFavoritos = new ArrayList<>();
        SQLiteDatabase bd = getReadableDatabase();
        String[] argumentos = new String[]{String.valueOf(id_usuario)};
        Cursor cursor = bd.rawQuery("SELECT id_centro FROM Favoritos WHERE id_usuario=?", argumentos);
        if (cursor.moveToFirst()) {
            do {
                listaFavoritos.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        bd.close();
        return listaFavoritos;
    }

    public boolean paradasEstaVacia() {
        SQLiteDatabase bd = getReadableDatabase();
        Cursor cursor = bd.rawQuery("SELECT COUNT(*) FROM Paradas", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        bd.close();
        return count == 0;
    }

    public void insertarParadasMasivas(List<Parada> listaParadas) {
        SQLiteDatabase bd = getWritableDatabase();
        bd.beginTransaction();
        try {
            for (Parada p : listaParadas) {
                ContentValues nuevo = new ContentValues();
                nuevo.put("id_parada", p.getId());
                nuevo.put("nombre", p.getNombre());
                nuevo.put("tipo_transporte", p.getTipo());
                nuevo.put("latitud", p.getLatitud());
                nuevo.put("longitud", p.getLongitud());

                bd.insertWithOnConflict("Paradas", null, nuevo, SQLiteDatabase.CONFLICT_IGNORE);
            }
            bd.setTransactionSuccessful();
        } finally {
            bd.endTransaction();
            bd.close();
        }
    }
}