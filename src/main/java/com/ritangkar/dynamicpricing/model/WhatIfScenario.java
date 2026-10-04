package com.ritangkar.dynamicpricing.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * One named override on top of a baseline PricingRequest — e.g. "Inventory
 * -30%" with {@code inventoryUnits} set to the already-reduced value. Only
 * non-null fields override the baseline; everything else is inherited.
 */
public record WhatIfScenario(
        @NotBlank String label,
        Double currentPrice,
        Double costPrice,
        Integer inventoryUnits,
        Double salesVelocityPerDay,
        Double demandChangePercent,
        Double competitorPrice,
        Double seasonalityFactor
) {

    public record Comparison(String baselineLabel, PricingRecommendation baseline,
                              List<Entry> scenarios) {
    }

    public record Entry(String label, PricingRecommendation recommendation) {
    }
}
