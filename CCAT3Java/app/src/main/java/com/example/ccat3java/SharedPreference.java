package com.example.ccat3java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.google.gson.Gson;

public class SharedPreference {

    public static final String PREFS_NAME = "CCAT Feeding Time";
    public static final String TIMES = "Feeding Times";

    public SharedPreference() {
        super();
    }

    // This four methods are used for maintaining favorites.
    public void saveTimes(Context context, List<Date> favorites) {
        SharedPreferences settings;
        Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonFavorites = gson.toJson(favorites);

        editor.putString(TIMES, jsonFavorites);

        editor.commit();
    }

    public void addTimes(Context context, Date product) {
        List<Date> favorites = getTimes(context);
        if (favorites == null)
            favorites = new ArrayList<Date>();
        favorites.add(product);
        saveTimes(context, favorites);
    }

    public void removeTimes(Context context, Date product) {
        ArrayList<Date> favorites = getTimes(context);
        if (favorites != null) {
            favorites.remove(product);
            saveTimes(context, favorites);
        }
    }

    public void removeTimes(Context context, int index) {
        ArrayList<Date> favorites = getTimes(context);
        if (favorites != null) {
            favorites.remove(index);
            saveTimes(context, favorites);
        }
    }

    public ArrayList<Date> getTimes(Context context) {
        SharedPreferences settings;
        List<Date> favorites;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);

        if (settings.contains(TIMES)) {
            String jsonFavorites = settings.getString(TIMES, null);
            Gson gson = new Gson();
            Date[] favoriteItems = gson.fromJson(jsonFavorites,
                    Date[].class);

            favorites = Arrays.asList(favoriteItems);
            favorites = new ArrayList<Date>(favorites);
        } else
            return null;

        return (ArrayList<Date>) favorites;
    }
}