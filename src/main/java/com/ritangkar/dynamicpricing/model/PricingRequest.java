package com.ritangkar.dynamicpricing.model;

import jakarta.validation.constraints.*;

/**
 * All inputs the pricing engine considers. Only productName, currentPrice,
 * costPrice, inventoryUnits, salesVelocityPerDay, demandChangePercent and
 * competitorPrice are required; everything else has a documented default.
 */
public record PricingRequest(
        @NotBlank String productName,
        @NotNull @Positive Double currentPrice,
        @NotNull @Positive Double costPrice,
        @NotNull @PositiveOrZero Integer inventoryUnits,
        @NotNull @Positive Double salesVelocityPerDay,
        @NotNull @DecimalMin("-100.0") Double demandChangePercent,
        @NotNull @Positive Double competitorPrice,
        Double seasonalityFactor,          // default 1.0
        Double minMarginPercent,           // default 15.0
        Double maxPriceMovePercent,        // default 10.0
        Double maxPriceVsCompetitorPercent // default null = no competitor ceiling
) {
    public PricingRequest {
        if (seasonalityFactor == null) seasonalityFactor = 1.0;
        if (minMarginPercent == null) minMarginPercent = 15.0;
        if (maxPriceMovePercent == null) maxPriceMovePercent = 10.0;
    }

    /** Returns a copy of this request with the given fields overridden — the mechanism behind what-if scenarios. */
    public PricingRequest with(Double currentPrice, Double costPrice, Integer inventoryUnits,
                                Double salesVelocityPerDay, Double demandChangePercent, Double competitorPrice,
                                Double seasonalityFactor) {
        return new PricingRequest(
                productName,
                currentPrice != null ? currentPrice : this.currentPrice,
                costPrice != null ? costPrice : this.costPrice,
                inventoryUnits != null ? inventoryUnits : this.inventoryUnits,
                salesVelocityPerDay != null ? salesVelocityPerDay : this.salesVelocityPerDay,
                demandChangePercent != null ? demandChangePercent : this.demandChangePercent,
                competitorPrice != null ? competitorPrice : this.competitorPrice,
                seasonalityFactor != null ? seasonalityFactor : this.seasonalityFactor,
                this.minMarginPercent, this.maxPriceMovePercent, this.maxPriceVsCompetitorPercent
        );
    }
}
