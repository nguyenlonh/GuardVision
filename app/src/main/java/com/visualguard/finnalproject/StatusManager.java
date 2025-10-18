package com.visualguard.finnalproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * StatusManager - uses Open-Meteo free APIs to get place name and weather.
 * Requirements:
 *  - INTERNET permission
 *  - (optional but recommended) ACCESS_FINE_LOCATION for accurate location
 *
 * Notes:
 *  - Open-Meteo geocoding: https://geocoding-api.open-meteo.com/v1/reverse
 *  - Open-Meteo forecast: https://api.open-meteo.com/v1/forecast
 */
public class StatusManager {

    private final Context ctx;
    private TextToSpeech tts;
    private final ExecutorService bg;
    private final Handler mainHandler;
    private final FusedLocationProviderClient fusedLocationClient;

    public StatusManager(Context context) {
        this.ctx = context.getApplicationContext();
        this.bg = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);
        initTts();
    }

    private void initTts() {
        tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setSpeechRate(1.05f);
            }
        });
    }

    public void shutdown() {
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
        } catch (Exception ignored) {}
        bg.shutdownNow();
    }

    /**
     * Public: speak a single short sentence with time, place, weather, temp, humidity, battery, and advice.
     */
    @SuppressLint("MissingPermission")
    public void speakStatus() {
        // 1) time
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);

        // 2) battery % - GIỮ NGUYÊN PHẦN NÀY
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = ctx.registerReceiver(null, ifilter);
        int level = -1, scale = 100;
        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        }
        final int batteryPct = (level >= 0 && scale > 0) ? Math.round((level / (float) scale) * 100f) : -1;

        final int fHour = hour;
        final int fMinute = minute;

        // do network/IO on background thread
        bg.execute(() -> {
            try {
                // 1) try device location if permission granted
                double[] coords = null;
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    final Object lock = new Object();
                    final double[] out = new double[]{Double.NaN, Double.NaN};
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if (location != null) {
                                    out[0] = location.getLatitude();
                                    out[1] = location.getLongitude();
                                }
                                synchronized (lock) { lock.notify(); }
                            })
                            .addOnFailureListener(e -> synchronizedNotify(lock));
                    // wait briefly for callback
                    try { synchronized (lock) { lock.wait(1500); } } catch (Exception ignored) {}
                    if (!Double.isNaN(out[0])) coords = new double[]{out[0], out[1]};
                }

                if (coords == null) {
                    // no location permission or failed -> we can't reliably call Open-Meteo with lat/lon
                    postSpeakNoWeather(fHour, fMinute, batteryPct);
                    return;
                }

                double lat = coords[0];
                double lon = coords[1];

                // 2) reverse geocode with Open-Meteo geocoding API to get place name
                String place = reverseGeocode(lat, lon); // may be null

                // 3) call Open-Meteo forecast with proper params
                String forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current_weather=true"
                        + "&hourly=relativehumidity_2m"
                        + "&daily=weathercode,temperature_2m_max,temperature_2m_min"
                        + "&timezone=auto";

                URL u = new URL(forecastUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                in.close();

                JSONObject root = new JSONObject(sb.toString());

                // parse current weather
                if (!root.has("current_weather")) {
                    postSpeakNoWeather(fHour, fMinute, batteryPct);
                    return;
                }
                JSONObject current = root.getJSONObject("current_weather");
                double temp = current.optDouble("temperature", Double.NaN);
                String currentTime = current.optString("time", null);
                int weatherCode = current.optInt("weathercode", -1);
                String shortDesc = mapWeatherCodeToPhrase(weatherCode);

                // find humidity from hourly arrays by matching time
                int humidity = -1;
                if (root.has("hourly")) {
                    JSONObject hourly = root.getJSONObject("hourly");
                    JSONArray times = hourly.optJSONArray("time");
                    JSONArray humidArr = hourly.optJSONArray("relativehumidity_2m");
                    if (times != null && humidArr != null && currentTime != null) {
                        for (int i = 0; i < times.length(); i++) {
                            if (currentTime.equals(times.optString(i))) {
                                double h = humidArr.optDouble(i, Double.NaN);
                                if (!Double.isNaN(h)) humidity = (int) Math.round(h);
                                break;
                            }
                        }
                    }
                }

                // daily summary: weathercode index 0 is today, and min/max temps
                String dailySummary = "";
                if (root.has("daily")) {
                    JSONObject daily = root.getJSONObject("daily");
                    JSONArray dCodes = daily.optJSONArray("weathercode");
                    JSONArray tMax = daily.optJSONArray("temperature_2m_max");
                    JSONArray tMin = daily.optJSONArray("temperature_2m_min");
                    if (dCodes != null && dCodes.length() > 0) {
                        int todayCode = dCodes.optInt(0, -1);
                        String dailyPhrase = mapWeatherCodeToPhrase(todayCode);
                        String temps = "";
                        if (tMax != null && tMin != null) {
                            double max = tMax.optDouble(0, Double.NaN);
                            double min = tMin.optDouble(0, Double.NaN);
                            if (!Double.isNaN(max) && !Double.isNaN(min)) {
                                temps = " High " + Math.round(max) + "°, low " + Math.round(min) + "°.";
                            }
                        }
                        dailySummary = dailyPhrase + temps;
                    }
                }

                // decide advice
                boolean good = isGoodWeatherByCode(weatherCode);

                // CẬP NHẬT PHẦN NÀY: Xây dựng câu thông báo mới với vị trí, thời tiết VÀ PIN
                String timePart = fHour + " hours " + fMinute + " minutes";
                String humidityPart = (humidity >= 0) ? (", humidity " + humidity + " percent") : "";
                String tempPart = !Double.isNaN(temp) ? (Math.round(temp) + " degrees") : "temperature unknown";
                String batteryPart = (batteryPct >= 0) ? (" Battery is " + batteryPct + " percent.") : "";

                String sentence;
                if (place != null) {
                    // Nếu có thông tin vị trí
                    sentence = "It is " + timePart + ". Your current location is " + place + ". The weather here is " + shortDesc
                            + ", " + tempPart + humidityPart + "." + batteryPart + " Today: " + dailySummary;
                } else {
                    // Nếu không có thông tin vị trí
                    sentence = "It is " + timePart + ". The weather is " + shortDesc
                            + ", " + tempPart + humidityPart + "." + batteryPart + " Today: " + dailySummary;
                }
                sentence += good ? " Have a nice day." : " Avoid going outside.";

                final String toSpeak = sentence;
                mainHandler.post(() -> {
                    if (tts != null) tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "status-speak");
                });

            } catch (Exception e) {
                e.printStackTrace();
                postSpeakNoWeather(fHour, fMinute, batteryPct);
            }
        });
    }

    // helper to safely post no-weather message - CẬP NHẬT PHƯƠNG THỨC NÀY
    private void postSpeakNoWeather(int hour, int minute, int batteryPct) {
        String timePart = hour + " hours " + minute + " minutes";
        String batteryPart = (batteryPct >= 0) ? (" Battery is " + batteryPct + " percent.") : "";
        String sentence = "It is " + timePart + ". Weather not available." + batteryPart;
        final String toSpeak = sentence;
        mainHandler.post(() -> {
            if (tts != null) tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "status-speak-fail");
        });
    }

    // reverse geocode using Open-Meteo geocoding API
    private String reverseGeocode(double lat, double lon) {
        try {
            String urlStr = "https://geocoding-api.open-meteo.com/v1/reverse?latitude="
                    + lat + "&longitude=" + lon + "&limit=1";
            URL u = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();
            JSONObject j = new JSONObject(sb.toString());
            if (j.has("results")) {
                JSONArray arr = j.getJSONArray("results");
                if (arr.length() > 0) {
                    JSONObject r = arr.getJSONObject(0);
                    String name = r.optString("name", null);
                    String country = r.optString("country", null);
                    if (name != null && country != null) return name + ", " + country;
                    if (name != null) return name;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // heuristic: good vs bad weather
    private boolean isGoodWeatherByCode(int code) {
        if (code == 0) return true;
        if (code == 1 || code == 2) return true;
        if (code == 3) return true;
        if (code == 45 || code == 48) return false;
        if (code >= 51 && code <= 55) return false;
        if (code >= 61 && code <= 65) return false;
        if (code >= 80 && code <= 82) return false;
        if (code >= 95) return false;
        return true;
    }

    private String mapWeatherCodeToPhrase(int code) {
        switch (code) {
            case 0: return "clear skies";
            case 1: case 2: return "partly cloudy";
            case 3: return "overcast";
            case 45: case 48: return "foggy";
            case 51: case 53: case 55: return "light drizzle";
            case 61: case 63: case 65: return "rain";
            case 71: case 73: case 75: return "snow";
            case 80: case 81: case 82: return "rain showers";
            case 95: case 96: case 99: return "thunderstorms";
            default: return "moderate conditions";
        }
    }

    // helper to avoid repeating synchronized code
    private static void synchronizedNotify(Object lock) {
        try { synchronized (lock) { lock.notify(); } } catch (Exception ignored) {}
    }
}
