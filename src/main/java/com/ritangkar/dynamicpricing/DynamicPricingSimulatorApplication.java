package com.ritangkar.dynamicpricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dynamic Pricing Simulator.
 *
 * A deterministic, rule-based pricing engine: demand, inventory,
 * competitor pricing and seasonality signals combine into a single bounded
 * price recommendation, with business constraints (margin floor, maximum
 * single-move size, optional competitor ceiling) always enforced last.
 * Every coefficient is a documented, named constant — see
 * docs/design-decisions.md — not a fitted or hidden number.
 */
@SpringBootApplication
public class DynamicPricingSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicPricingSimulatorApplication.class, args);
    }
}
