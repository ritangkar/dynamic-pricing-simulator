package com.ritangkar.dynamicpricing.service;

import com.ritangkar.dynamicpricing.model.PricingRecommendation;
import com.ritangkar.dynamicpricing.model.PricingRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Every expected value below was independently computed with a Python
 * reference implementation of the same rule set before this test was
 * written — see docs/design-decisions.md.
 */
class PricingEngineTest {

    private final PricingEngine engine = new PricingEngine();

    private PricingRequest request(double currentPrice, double costPrice, int inventoryUnits,
                                    double salesVelocityPerDay, double demandChangePercent, double competitorPrice) {
        return new PricingRequest("Test Product", currentPrice, costPrice, inventoryUnits, salesVelocityPerDay,
                demandChangePercent, competitorPrice, null, null, null, null);
    }

    @Test
    void baselineScenarioMatchesTheIndependentlyVerifiedReference() {
        PricingRecommendation r = engine.recommend(request(7199, 4500, 17, 3, 34, 7499));

        assertThat(r.recommendedPrice()).isCloseTo(7680, within(1.0));
        assertThat(r.priceChangePercent()).isCloseTo(6.68, within(0.05));
        assertThat(r.expectedRevenueChangePercent()).isCloseTo(31.55, within(0.1));
        assertThat(r.currentDaysOfSupply()).isCloseTo(5.7, within(0.05));
        assertThat(r.inventoryRisk()).isEqualTo("HIGH");
    }

    @Test
    void whatIfLowerInventoryPushesPriceHigherThanBaseline() {
        PricingRecommendation baseline = engine.recommend(request(7199, 4500, 17, 3, 34, 7499));
        PricingRecommendation lowerInventory = engine.recommend(request(7199, 4500, 12, 3, 34, 7499));

        assertThat(lowerInventory.recommendedPrice()).isGreaterThan(baseline.recommendedPrice());
        assertThat(lowerInventory.recommendedPrice()).isCloseTo(7728, within(1.0));
    }

    @Test
    void whatIfHigherInventoryPushesPriceLowerThanBaseline() {
        PricingRecommendation baseline = engine.recommend(request(7199, 4500, 17, 3, 34, 7499));
        PricingRecommendation higherInventory = engine.recommend(request(7199, 4500, 26, 3, 34, 7499));

        assertThat(higherInventory.recommendedPrice()).isLessThan(baseline.recommendedPrice());
        assertThat(higherInventory.inventoryRisk()).isEqualTo("MEDIUM");
    }

    @Test
    void overstockScenarioRecommendsADiscountAndFlagsHighRisk() {
        PricingRecommendation r = engine.recommend(request(3000, 1800, 500, 3, -15, 3100));

        assertThat(r.recommendedPrice()).isLessThan(3000);
        assertThat(r.recommendedPrice()).isCloseTo(2799, within(1.0));
        assertThat(r.inventoryRisk()).isEqualTo("HIGH"); // severe overstock, not stockout
        assertThat(r.currentDaysOfSupply()).isGreaterThan(120);
    }

    @Test
    void marginFloorPreventsPricingBelowMinimumMargin() {
        PricingRecommendation r = engine.recommend(request(2000, 1850, 1000, 5, -40, 1900));

        double minAllowed = 1850 * 1.15; // default 15% minimum margin
        assertThat(r.recommendedPrice()).isGreaterThanOrEqualTo(minAllowed - 1.0);
        assertThat(r.constraintsApplied()).anyMatch(c -> c.contains("Margin floor"));
    }

    @Test
    void recommendedPriceIsMonotonicInDemandChange() {
        double previous = Double.NEGATIVE_INFINITY;
        for (int demand = -50; demand <= 50; demand += 10) {
            PricingRecommendation r = engine.recommend(request(5000, 3000, 100, 5, demand, 5200));
            assertThat(r.recommendedPrice()).isGreaterThanOrEqualTo(previous);
            previous = r.recommendedPrice();
        }
    }

    @Test
    void maxPriceMoveCapIsNeverExceeded() {
        PricingRequest extreme = new PricingRequest("Extreme", 5000.0, 1000.0, 1, 100.0, 500.0, 20000.0,
                1.0, 15.0, 8.0, null);
        PricingRecommendation r = engine.recommend(extreme);

        assertThat(Math.abs(r.priceChangePercent())).isLessThanOrEqualTo(8.01);
    }

    @Test
    void optionalCompetitorCeilingIsRespectedWhenSet() {
        PricingRequest withCeiling = new PricingRequest("Test", 7199.0, 4500.0, 5, 1.0, 60.0, 7499.0,
                1.0, 15.0, 15.0, 2.0); // never price more than 2% above competitor
        PricingRecommendation r = engine.recommend(withCeiling);

        double ceiling = 7499 * 1.02;
        assertThat(r.recommendedPrice()).isLessThanOrEqualTo(ceiling + 1.0);
    }
}
