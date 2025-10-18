package com.visualguard.finnalproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IngredientDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ingredients.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "ingredients";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EFFECTS = "harmful_effects";

    public IngredientDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COLUMN_NAME + " TEXT PRIMARY KEY COLLATE NOCASE, "
                + COLUMN_EFFECTS + " TEXT)");

        // initial sample data
        insertIfNotExists(db, "Sugar", "May increase blood sugar");
        insertIfNotExists(db, "Trans fat", "Raises bad cholesterol");
        insertIfNotExists(db, "Alcohol", "Not safe for children or certain illnesses");
        insertIfNotExists(db, "Nicotine", "Addictive and harmful");
        insertIfNotExists(db, "Peanut", "May cause severe allergy");
        insertIfNotExists(db, "Orange", "Generally safe, contains vitamin C");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // simple migration: add column if missing (works for this small demo)
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_EFFECTS + " TEXT");
            } catch (Exception ignored) {}
            // optionally fill default values for existing rows (left as exercise)
        }
    }

    private void insertIfNotExists(SQLiteDatabase db, String name, String effects) {
        ContentValues v = new ContentValues();
        v.put(COLUMN_NAME, name);
        v.put(COLUMN_EFFECTS, effects);
        db.insertWithOnConflict(TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void insertOrUpdate(String name, String effects) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_NAME, name);
        v.put(COLUMN_EFFECTS, effects);
        db.insertWithOnConflict(TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<String> getAllIngredients() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{COLUMN_NAME}, null, null, null, null, null);
        try {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Map<String, String> findMatchesWithEffects(String text) {
        Map<String, String> found = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty()) return found;

        String lower = normalize(text);
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, new String[]{COLUMN_NAME, COLUMN_EFFECTS},
                null, null, null, null, null);
        try {
            while (c.moveToNext()) {
                String name = c.getString(0);
                String effects = c.getString(1);
                if (name == null) continue;
                String nname = normalize(name);
                if (nname.isEmpty()) continue;
                if (lower.contains(nname) || lower.matches(".*\\b" + java.util.regex.Pattern.quote(nname) + "\\b.*")) {
                    found.put(name, effects != null ? effects : "");
                }
            }
        } finally {
            c.close();
        }
        return found;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).trim();
        t = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        t = t.replaceAll("[^a-z0-9\\s]", " ");
        return t.replaceAll("\\s+", " ").trim();
    }
}

