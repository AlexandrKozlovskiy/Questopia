package com.qsp.player.settings;

import android.content.SharedPreferences;
import android.graphics.Color;

public class Settings {
    public boolean soundEnabled;
    public String language;
    public float actionsHeightRatio;
    public int typeface;
    public int fontSize;
    public boolean useGameFont;
    public int backColor;
    public int textColor;
    public int linkColor;

    public static Settings from(SharedPreferences preferences) {
        Settings settings = new Settings();
        settings.soundEnabled = preferences.getBoolean("sound", true);
        settings.language = preferences.getString("lang", "ru");
        settings.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settings.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settings.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settings.useGameFont = preferences.getBoolean("useGameFont", false);
        settings.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settings.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settings.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
        return settings;
    }

    private static float parseActionsHeightRatio(String str) {
        switch (str) {
            case "1/5":
                return 0.2f;
            case "1/4":
                return 0.25f;
            case "1/3":
                return 0.33f;
            case "1/2":
                return 0.5f;
            case "2/3":
                return 0.67f;
            default:
                throw new RuntimeException("Unsupported value of actsHeight: " + str);
        }
    }
}
