package com.ritangkar.dynamicpricing.service;

import com.ritangkar.dynamicpricing.model.PricingRequest;
import com.ritangkar.dynamicpricing.model.WhatIfRequest;
import com.ritangkar.dynamicpricing.model.WhatIfScenario;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs the same deterministic {@link PricingEngine} against a baseline
 * request and any number of named overrides — "what happens if inventory
 * decreases by 30%?" is just a scenario with {@code inventoryUnits} set to
 * the already-reduced number. No separate what-if code path exists; this
 * is the same engine, called more than once.
 */
@Service
public class WhatIfService {

    private final PricingEngine pricingEngine;

    public WhatIfService(PricingEngine pricingEngine) {
        this.pricingEngine = pricingEngine;
    }

    public WhatIfScenario.Comparison compare(WhatIfRequest request) {
        var baselineRecommendation = pricingEngine.recommend(request.baseline());

        List<WhatIfScenario.Entry> entries = request.scenarios().stream()
                .map(scenario -> {
                    PricingRequest merged = request.baseline().with(
                            scenario.currentPrice(), scenario.costPrice(), scenario.inventoryUnits(),
                            scenario.salesVelocityPerDay(), scenario.demandChangePercent(), scenario.competitorPrice(),
                            scenario.seasonalityFactor());
                    return new WhatIfScenario.Entry(scenario.label(), pricingEngine.recommend(merged));
                })
                .toList();

        return new WhatIfScenario.Comparison("Baseline", baselineRecommendation, entries);
    }
}
