package com.example.unigo_das.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import com.example.unigo_das.item.Campus;
import java.util.ArrayList;
import java.util.List;
import com.example.unigo_das.item.TransitStop;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Unigo.db";
    private static final int DATABASE_VERSION = 3; // Schema version 3

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

        createCampusTable(sqLiteDatabase);

        sqLiteDatabase.execSQL("CREATE TABLE Favoritos (" +
                "id_usuario INTEGER, " +
                "id_centro VARCHAR(255), " +
                "PRIMARY KEY (id_usuario, id_centro), " +
                "FOREIGN KEY(id_usuario) REFERENCES Usuarios(id_usuario) ON DELETE CASCADE, " +
                "FOREIGN KEY(id_centro) REFERENCES Centros(id_centro) ON DELETE CASCADE)");

        createStopTable(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // The schemas before version 3 are unknown: same behaviour as before (rebuild everything).
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Favoritos");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Paradas");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Centros");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Usuarios");
            onCreate(sqLiteDatabase);
            return;
        }
        // Fix F8: from version 3 on, only the reference tables are rebuilt. The app fills them
        // again at start-up (campuses on every start, stops when the table is empty), so the
        // users' favourites are no longer deleted on every schema change.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Paradas");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS Centros");
        createCampusTable(sqLiteDatabase);
        createStopTable(sqLiteDatabase);
    }

    private static void createCampusTable(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE Centros (" +
                "id_centro VARCHAR(255) PRIMARY KEY, " +
                "nombre VARCHAR(255), " +
                "universidad VARCHAR(255), " +
                "descripcion TEXT, " +
                "ubicacion VARCHAR(255), " +
                "latitud REAL, " +
                "longitud REAL)");
    }

    private static void createStopTable(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE Paradas (" +
                "id_parada VARCHAR(255) PRIMARY KEY, " +
                "nombre VARCHAR(255), " +
                "tipo_transporte VARCHAR(255), " +
                "latitud REAL, " +
                "longitud REAL)");
    }

    public boolean insertUser(String name, String email, String password) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues row = new ContentValues();
        row.put("nombre", name);
        row.put("email", email);
        row.put("password", password);
        long insertResult = database.insert("Usuarios", null, row);
        database.close();
        return insertResult != -1;
    }

    public int checkLogin(String email, String password) {
        SQLiteDatabase database = getReadableDatabase();
        String[] queryArgs = new String[] {email, password};
        Cursor c = database.rawQuery("SELECT id_usuario FROM Usuarios WHERE email=? AND password=?", queryArgs);
        int foundUserId = -1;
        if (c.moveToFirst()) foundUserId = c.getInt(0);
        c.close();
        database.close();
        return foundUserId;
    }

    // Updated so that it also updates an existing row (needed when the language changes)
    public boolean insertCampus(String campusId, String name, String university, String description, String districtName, double latitude, double longitude) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues row = new ContentValues();
        row.put("id_centro", campusId);
        row.put("nombre", name);
        row.put("universidad", university);
        row.put("descripcion", description);
        row.put("ubicacion", districtName);
        row.put("latitud", latitude);
        row.put("longitud", longitude);
        
        // IGNORE so it does not fail if the row exists, then UPDATE to refresh the text fields
        long insertResult = database.insertWithOnConflict("Centros", null, row, SQLiteDatabase.CONFLICT_IGNORE);
        if (insertResult == -1) {
            database.update("Centros", row, "id_centro=?", new String[]{campusId});
        }
        database.close();
        return true;
    }

    // Fix F9: inserts (or updates) all the campuses in one transaction, with the same
    // insert-or-update logic as insertCampus(), opening the database only once.
    public void insertCampusesBulk(List<Campus> campuses) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            for (Campus campus : campuses) {
                ContentValues row = new ContentValues();
                row.put("id_centro", campus.getId());
                row.put("nombre", campus.getName());
                row.put("universidad", campus.getUniversity());
                row.put("descripcion", campus.getDescription());
                row.put("ubicacion", campus.getDistrict());
                row.put("latitud", campus.getLatitude());
                row.put("longitud", campus.getLongitude());
                long insertResult = database.insertWithOnConflict("Centros", null, row, SQLiteDatabase.CONFLICT_IGNORE);
                if (insertResult == -1) {
                    database.update("Centros", row, "id_centro=?", new String[]{campus.getId()});
                }
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            database.close();
        }
    }

    public boolean isCampusTableEmpty() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM Centros", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        database.close();
        return count == 0;
    }

    public Campus getCampusById(String campusId) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM Centros WHERE id_centro = ?", new String[]{campusId});
        Campus campus = null;
        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow("id_centro"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            String uni = cursor.getString(cursor.getColumnIndexOrThrow("universidad"));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
            String district = cursor.getString(cursor.getColumnIndexOrThrow("ubicacion"));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud"));
            double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"));
            campus = new Campus(id, name, uni, desc, district, lat, lon);
        }
        cursor.close();
        database.close();
        return campus;
    }

    public List<Campus> getAllCampuses() {
        List<Campus> campusItems = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM Centros ORDER BY nombre ASC", null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id_centro"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String uni = cursor.getString(cursor.getColumnIndexOrThrow("universidad"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                String district = cursor.getString(cursor.getColumnIndexOrThrow("ubicacion"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"));
                campusItems.add(new Campus(id, name, uni, desc, district, lat, lon));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return campusItems;
    }

    public boolean addFavorite(int userId, String campusId) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues row = new ContentValues();
        row.put("id_usuario", userId);
        row.put("id_centro", campusId);
        long insertResult = database.insertWithOnConflict("Favoritos", null, row, SQLiteDatabase.CONFLICT_IGNORE);
        database.close();
        return insertResult != -1;
    }

    public void removeFavorite(int userId, String campusId) {
        SQLiteDatabase database = getWritableDatabase();
        String[] queryArgs = new String[]{String.valueOf(userId), campusId};
        database.delete("Favoritos", "id_usuario=? AND id_centro=?", queryArgs);
        database.close();
    }

    public List<String> getUserFavoriteIds(int userId) {
        List<String> favoriteIds = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        String[] queryArgs = new String[]{String.valueOf(userId)};
        Cursor cursor = database.rawQuery("SELECT id_centro FROM Favoritos WHERE id_usuario=?", queryArgs);
        if (cursor.moveToFirst()) {
            do {
                favoriteIds.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return favoriteIds;
    }

    public boolean isStopTableEmpty() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM Paradas", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        database.close();
        return count == 0;
    }

    public void insertStopsBulk(List<TransitStop> stopList) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            for (TransitStop p : stopList) {
                ContentValues row = new ContentValues();
                row.put("id_parada", p.getId());
                row.put("nombre", p.getName());
                row.put("tipo_transporte", p.getType());
                row.put("latitud", p.getLatitude());
                row.put("longitud", p.getLongitude());

                database.insertWithOnConflict("Paradas", null, row, SQLiteDatabase.CONFLICT_IGNORE);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            database.close();
        }
    }

    public List<TransitStop> getStopsByType(String type) {
        List<TransitStop> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query filtered by the tipo_transporte column
        Cursor cursor = db.rawQuery("SELECT * FROM Paradas WHERE tipo_transporte = ?", new String[]{type});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id_parada"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String t = cursor.getString(cursor.getColumnIndexOrThrow("tipo_transporte"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"));

                list.add(new TransitStop(id, name, t, lat, lon));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }
}