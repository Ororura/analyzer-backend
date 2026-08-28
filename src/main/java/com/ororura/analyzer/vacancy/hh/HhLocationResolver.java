package com.ororura.analyzer.vacancy.hh;

import java.util.Locale;
import java.util.Map;

final class HhLocationResolver {

    private static final Map<String, String> AREA_IDS = Map.ofEntries(
            Map.entry("москва", "1"),
            Map.entry("санкт-петербург", "2"),
            Map.entry("спб", "2"),
            Map.entry("екатеринбург", "3"),
            Map.entry("новосибирск", "4"),
            Map.entry("казань", "88"),
            Map.entry("нижний новгород", "66"),
            Map.entry("самара", "78"),
            Map.entry("ростов-на-дону", "76"),
            Map.entry("краснодар", "53"));

    private HhLocationResolver() {
    }

    static ResolvedQuery resolve(String text, String location) {
        if (location == null) return new ResolvedQuery(text, null, null);
        String area = location.chars().allMatch(Character::isDigit)
                ? location
                : AREA_IDS.get(location.toLowerCase(Locale.ROOT));
        return area == null
                ? new ResolvedQuery(text + " " + location, null, location)
                : new ResolvedQuery(text, area, null);
    }

    static boolean matchesFallback(String actualLocation, String fallbackLocation) {
        return fallbackLocation == null
                || actualLocation != null
                        && actualLocation.toLowerCase(Locale.ROOT).contains(fallbackLocation.toLowerCase(Locale.ROOT));
    }

    record ResolvedQuery(String text, String area, String fallbackLocation) {
    }
}
