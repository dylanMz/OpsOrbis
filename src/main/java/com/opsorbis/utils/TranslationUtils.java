package com.opsorbis.utils;

import com.hypixel.hytale.server.core.Message;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaire pour convertir des chaînes avec codes couleurs (&a ou [#RRGGBB])
 * en objets Message Hytale composés de plusieurs spans.
 */
public class TranslationUtils {

    // Regex pour détecter les codes couleurs : &+caractère OU [#RRGGBB]
    private static final Pattern COLOR_PATTERN = Pattern.compile("(&[0-9a-fk-or])|(\\[#([A-Fa-f0-9]{6})\\])");

    /**
     * Parse une chaîne et retourne un objet Message avec les couleurs appliquées.
     */
    public static Message parse(String input) {
        if (input == null || input.isEmpty()) return Message.raw("");

        Matcher matcher = COLOR_PATTERN.matcher(input);
        Message finalMessage = null;
        
        int lastEnd = 0;
        Color currentColor = Color.WHITE;

        while (matcher.find()) {
            // Ajouter le texte avant le code couleur
            String before = input.substring(lastEnd, matcher.start());
            if (!before.isEmpty()) {
                Message span = Message.raw(before).color(currentColor);
                finalMessage = (finalMessage == null) ? span : Message.join(finalMessage, span);
            }

            // Déterminer la nouvelle couleur
            if (matcher.group(1) != null) { // Code &x
                currentColor = getColorFromLegacy(matcher.group(1).charAt(1));
            } else if (matcher.group(2) != null) { // Code [#RRGGBB]
                try {
                    currentColor = Color.decode("#" + matcher.group(3));
                } catch (NumberFormatException e) {
                    currentColor = Color.WHITE;
                }
            }

            lastEnd = matcher.end();
        }

        // Ajouter le reste du texte
        String remaining = input.substring(lastEnd);
        if (!remaining.isEmpty() || finalMessage == null) {
            Message span = Message.raw(remaining).color(currentColor);
            finalMessage = (finalMessage == null) ? span : Message.join(finalMessage, span);
        }

        return finalMessage;
    }

    private static Color getColorFromLegacy(char code) {
        switch (code) {
            case '0': return Color.BLACK;
            case '1': return new Color(0, 0, 170);
            case '2': return new Color(0, 170, 0);
            case '3': return new Color(0, 170, 170);
            case '4': return new Color(170, 0, 0);
            case '5': return new Color(170, 0, 170);
            case '6': return new Color(255, 170, 0);
            case '7': return Color.LIGHT_GRAY;
            case '8': return Color.DARK_GRAY;
            case '9': return new Color(85, 85, 255);
            case 'a': return Color.GREEN;
            case 'b': return Color.CYAN;
            case 'c': return Color.RED;
            case 'd': return Color.MAGENTA;
            case 'e': return Color.YELLOW;
            case 'f': return Color.WHITE;
            default: return Color.WHITE;
        }
    }
}
