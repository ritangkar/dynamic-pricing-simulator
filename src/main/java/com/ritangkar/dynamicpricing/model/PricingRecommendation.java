package com.ritangkar.dynamicpricing.model;

import java.util.List;

public record PricingRecommendation(
        String productName,
        double currentPrice,
        double recommendedPrice,
        double priceChangePercent,
        double expectedRevenueChangePercent,
        double expectedUnitsChangePercent,
        double currentDaysOfSupply,
        double projectedDaysOfSupply,
        String inventoryRisk, // LOW, MEDIUM, HIGH
        List<String> rationale,
        List<String> constraintsApplied
) {
}
