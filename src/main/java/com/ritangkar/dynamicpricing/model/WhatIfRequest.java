package com.ritangkar.dynamicpricing.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record WhatIfRequest(
        @NotNull @Valid PricingRequest baseline,
        @NotEmpty @Valid List<WhatIfScenario> scenarios
) {
}
