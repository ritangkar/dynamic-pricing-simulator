package com.ritangkar.dynamicpricing.service;

import com.ritangkar.dynamicpricing.model.PricingRecommendation;
import com.ritangkar.dynamicpricing.model.PricingRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A deterministic, rule-based pricing engine — no ML, no LLM. Four signals
 * (demand, inventory coverage, competitor gap, seasonality) combine into a
 * bounded price adjustment; business constraints (margin floor, maximum
 * single-move size, optional competitor ceiling) are enforced last, always.
 *
 * Every coefficient below is a named constant with a one-line rationale in
 * its comment, and the whole design was independently verified in Python
 * — including a monotonicity check (more demand never recommends a lower
 * price) — before being written here. See docs/design-decisions.md for the
 * full write-up, including why these are illustrative business-rule
 * coefficients and not numbers fitted to real sales data.
 */
@Service
public class PricingEngine {

    /** Healthy inventory coverage target, in days of supply at current velocity. */
    private static final double TARGET_DAYS_OF_SUPPLY = 14.0;
    /** % price move per day of inventory coverage away from the target, before capping. */
    private static final double INVENTORY_SENSITIVITY = 0.4;
    /** % price move per 1% of underlying demand change, before capping. */
    private static final double DEMAND_SENSITIVITY = 0.08;
    /** Assumed own-price elasticity of demand: a 1% price rise reduces units by 1.6%, all else equal. */
    private static final double PRICE_ELASTICITY = -1.6;
    /** Fraction of the price gap to a competitor this engine is willing to close in one move. */
    private static final double COMPETITIVE_PULL = 0.15;
    private static final double INVENTORY_ADJ_CAP = 6.0;
    private static final double COMPETITIVE_ADJ_CAP = 5.0;

    public PricingRecommendation recommend(PricingRequest request) {
        List<String> rationale = new ArrayList<>();
        List<String> constraints = new ArrayList<>();

        double demandAdj = request.demandChangePercent() * DEMAND_SENSITIVITY;
        rationale.add(String.format("Demand signal: %.1f%% change in underlying demand contributes %.2f%% (sensitivity %.2f).",
                request.demandChangePercent(), demandAdj, DEMAND_SENSITIVITY));

        double daysOfSupply = request.inventoryUnits() / request.salesVelocityPerDay();
        double invAdj = clamp((TARGET_DAYS_OF_SUPPLY - daysOfSupply) * INVENTORY_SENSITIVITY,
                -INVENTORY_ADJ_CAP, INVENTORY_ADJ_CAP);
        rationale.add(String.format("Inventory signal: %.1f days of supply vs. a %.0f-day target contributes %.2f%%.",
                daysOfSupply, TARGET_DAYS_OF_SUPPLY, invAdj));

        double competitiveGapPercent = (request.competitorPrice() - request.currentPrice()) / request.currentPrice() * 100;
        double compAdj = clamp(competitiveGapPercent * COMPETITIVE_PULL, -COMPETITIVE_ADJ_CAP, COMPETITIVE_ADJ_CAP);
        rationale.add(String.format("Competitor signal: current price is %.1f%% %s competitor's, contributes %.2f%%.",
                Math.abs(competitiveGapPercent), competitiveGapPercent >= 0 ? "below" : "above", compAdj));

        double totalAdjRaw = (demandAdj + invAdj + compAdj) * request.seasonalityFactor();
        double totalAdj = clamp(totalAdjRaw, -request.maxPriceMovePercent(), request.maxPriceMovePercent());
        if (totalAdj != totalAdjRaw) {
            constraints.add(String.format("Combined adjustment of %.2f%% capped to the configured maximum single move of \u00B1%.1f%%.",
                    totalAdjRaw, request.maxPriceMovePercent()));
        }
        if (request.seasonalityFactor() != 1.0) {
            rationale.add(String.format("Seasonality factor %.2f applied to the combined signal.", request.seasonalityFactor()));
        }

        double candidatePrice = request.currentPrice() * (1 + totalAdj / 100.0);

        double minPrice = request.costPrice() * (1 + request.minMarginPercent() / 100.0);
        if (candidatePrice < minPrice) {
            constraints.add(String.format("Margin floor enforced: price raised to \u20B9%.0f to preserve the minimum %.1f%% margin over cost.",
                    minPrice, request.minMarginPercent()));
            candidatePrice = minPrice;
        }

        if (request.maxPriceVsCompetitorPercent() != null) {
            double ceiling = request.competitorPrice() * (1 + request.maxPriceVsCompetitorPercent() / 100.0);
            if (candidatePrice > ceiling) {
                constraints.add(String.format("Competitor ceiling enforced: price capped to \u20B9%.0f (%.1f%% above competitor).",
                        ceiling, request.maxPriceVsCompetitorPercent()));
                candidatePrice = ceiling;
            }
        }

        double recommendedPrice = Math.round(candidatePrice);
        double priceChangePercent = round2((recommendedPrice - request.currentPrice()) / request.currentPrice() * 100.0);

        double unitsChangePercent = round2(request.demandChangePercent() + PRICE_ELASTICITY * priceChangePercent);
        double revenueChangePercent = round2(
                ((1 + priceChangePercent / 100.0) * (1 + unitsChangePercent / 100.0) - 1) * 100.0);

        double adjustedVelocity = request.salesVelocityPerDay() * (1 + unitsChangePercent / 100.0);
        double projectedDaysOfSupply = request.inventoryUnits() / Math.max(0.01, adjustedVelocity);
        String risk = classifyRisk(projectedDaysOfSupply);

        return new PricingRecommendation(
                request.productName(), request.currentPrice(), recommendedPrice, priceChangePercent,
                revenueChangePercent, unitsChangePercent, round1(daysOfSupply), round1(projectedDaysOfSupply),
                risk, rationale, constraints
        );
    }

    /**
     * Inventory risk reflects both directions of danger: too little stock
     * (stockout risk, low days of supply) and too much (overstock/markdown
     * risk, very high days of supply) — not just "low inventory = risky".
     */
    private String classifyRisk(double projectedDaysOfSupply) {
        if (projectedDaysOfSupply < 5) return "HIGH";        // imminent stockout
        if (projectedDaysOfSupply < 15) return "MEDIUM";     // tight, worth watching
        if (projectedDaysOfSupply <= 60) return "LOW";        // healthy coverage
        if (projectedDaysOfSupply <= 120) return "MEDIUM";   // overstock building
        return "HIGH";                                        // severe overstock / markdown risk
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
