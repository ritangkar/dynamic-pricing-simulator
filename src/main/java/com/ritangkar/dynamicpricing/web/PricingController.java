package com.ritangkar.dynamicpricing.web;

import com.ritangkar.dynamicpricing.model.PricingRecommendation;
import com.ritangkar.dynamicpricing.model.PricingRequest;
import com.ritangkar.dynamicpricing.model.WhatIfRequest;
import com.ritangkar.dynamicpricing.model.WhatIfScenario;
import com.ritangkar.dynamicpricing.service.PricingEngine;
import com.ritangkar.dynamicpricing.service.WhatIfService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PricingController {

    private final PricingEngine pricingEngine;
    private final WhatIfService whatIfService;

    public PricingController(PricingEngine pricingEngine, WhatIfService whatIfService) {
        this.pricingEngine = pricingEngine;
        this.whatIfService = whatIfService;
    }

    /** POST /api/pricing/recommend — a single price recommendation. */
    @PostMapping("/api/pricing/recommend")
    public PricingRecommendation recommend(@Valid @RequestBody PricingRequest request) {
        return pricingEngine.recommend(request);
    }

    /**
     * POST /api/pricing/what-if — a baseline plus any number of named
     * override scenarios, each compared against the same baseline.
     */
    @PostMapping("/api/pricing/what-if")
    public WhatIfScenario.Comparison whatIf(@Valid @RequestBody WhatIfRequest request) {
        return whatIfService.compare(request);
    }
}
