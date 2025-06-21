package com.example.readmegenerator.domain.service;

public class HeuristicDependencyFilter {

    public static boolean isRelevant(String name) {
        int score = 0;
        String lower = name.toLowerCase();

        if (name.contains("/") || name.contains("-")) score += 2;
        if (lower.contains("core")) score += 1;
        if (lower.contains("cli")) score += 1;
        if (lower.contains("kit")) score += 1;
        if (lower.contains("builder")) score += 1;
        if (lower.contains("framework")) score += 2;

        // Penalizuj testowe i zbędne zależności
        if (lower.contains("test") || lower.contains("jasmine") || lower.contains("karma")) score -= 2;
        if (name.length() <= 5) score -= 2;
        if (lower.equals("tslib") || lower.equals("zone.js") || lower.equals("postcss")) score -= 2;

        return score >= 1;
    }
}
