# Dynamic Pricing Simulator

A deterministic, rule-based commerce pricing engine. Demand, inventory
coverage, competitor price and seasonality combine into a single bounded
recommendation — with every coefficient named, every constraint auditable,
and interactive what-if scenario comparison built on the same engine, not
a separate code path.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

Send a pricing scenario (current price, cost, inventory, sales velocity,
demand signal, competitor price); get back a recommended price, expected
revenue impact, inventory risk classification, and a full rationale —
or send a baseline plus named "what-if" overrides and compare them side by side.

## 2. Problem

Pricing recommendations are easy to fake as a single opaque number. The
harder, more valuable thing is a recommendation that shows its work: which
signal pushed the price up, which pushed it down, which business rule
capped the result, and what happens if one input changes.

## 3. Solution

Four named signals (demand, inventory, competitor, seasonality) combine
additively, then three business constraints (max move, margin floor,
optional competitor ceiling) are enforced in a fixed order — every step
recorded in the response's `rationale` and `constraintsApplied` lists.

## 4. Key Features

- Deterministic rule-based engine — no ML, no LLM, fully reproducible
- **Continuous** inventory-coverage adjustment (not discrete bands — see
  design decisions for why that distinction mattered)
- Bidirectional inventory risk (stockout risk *and* overstock risk)
- Margin floor, maximum single-move cap, and an optional competitor-price
  ceiling, all auditable per response
- What-if comparison endpoint: one baseline, any number of named override
  scenarios, same engine underneath

## 5. Architecture

```
PricingRequest
  ├─ demand signal (demandChangePercent × sensitivity)
  ├─ inventory signal (days-of-supply vs. target, continuous)
  └─ competitor signal (price gap × pull factor)
        │
        ▼  combine + seasonality → cap → margin floor → competitor ceiling
        ▼
PricingRecommendation (price, %change, revenue/units impact, risk, rationale)
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram.

## 6. Technical Approach

Every coefficient (`DEMAND_SENSITIVITY`, `INVENTORY_SENSITIVITY`,
`PRICE_ELASTICITY`, `COMPETITIVE_PULL`, `TARGET_DAYS_OF_SUPPLY`) is a named
constant in `PricingEngine` with a one-line rationale in its comment —
nothing is a hidden magic number.

## 7. Design Decisions

See [`docs/design-decisions.md`](docs/design-decisions.md) — including the
real defect an early discrete-band inventory model had (two different
what-if inputs producing an identical recommendation) and why it was
replaced with a continuous function, plus an explicit, honest note that
this engine does **not** reproduce the master brief's illustrative example
numbers, and why reproducing them would have been the wrong goal.

## 8. Sample Input

```json
POST /api/pricing/recommend
{
  "productName": "Running Shoe X",
  "currentPrice": 7199,
  "costPrice": 4500,
  "inventoryUnits": 17,
  "salesVelocityPerDay": 3,
  "demandChangePercent": 34,
  "competitorPrice": 7499
}
```

## 9. Sample Output

Real output for the input above:

```json
{
  "productName": "Running Shoe X",
  "currentPrice": 7199.0,
  "recommendedPrice": 7680.0,
  "priceChangePercent": 6.68,
  "expectedRevenueChangePercent": 31.55,
  "expectedUnitsChangePercent": 23.31,
  "currentDaysOfSupply": 5.7,
  "projectedDaysOfSupply": 4.6,
  "inventoryRisk": "HIGH",
  "rationale": [
    "Demand signal: 34.0% change in underlying demand contributes 2.72% (sensitivity 0.08).",
    "Inventory signal: 5.7 days of supply vs. a 14-day target contributes 3.33%.",
    "Competitor signal: current price is 4.2% below competitor's, contributes 0.63%."
  ],
  "constraintsApplied": []
}
```

## 10. How to Run

```bash
git clone <your-repo-url>
cd dynamic-pricing-simulator
mvn spring-boot:run
```

API available at `http://localhost:8088`.

## 11. How to Test

```bash
mvn test
```

Every test's expected value was independently computed with a Python
reference implementation before the test was written, including a
monotonicity check (recommended price never decreases as demand rises)
and explicit tests for both constraint types (margin floor, competitor ceiling).

## 12. API Documentation

| Method | Path | Description |
|---|---|---|
| POST | `/api/pricing/recommend` | One `PricingRequest` → one `PricingRecommendation` |
| POST | `/api/pricing/what-if` | A baseline + named override scenarios → a side-by-side comparison |

## 13. Portfolio Demo

**Demo level:** 1 — client-side, deterministic (the engine is simple
arithmetic, so it ports exactly). See [`docs/demo.md`](docs/demo.md).

## 14. Limitations

- Coefficients are illustrative business-rule assumptions, not fitted to
  real sales/elasticity data (stated plainly — see Section 7)
- Single-product scenarios only — no cross-product cannibalization or
  bundle effects
- Revenue projection assumes a constant elasticity, which real demand
  curves rarely are at extremes

## 15. Future Enhancements

- Per-category elasticity and sensitivity presets instead of one global set
- A time-series mode: simulate a sequence of pricing decisions over a
  season rather than one snapshot
- Expose the competitor ceiling and margin floor as visualized bounds in
  the what-if comparison output

## 16. License

MIT — see [`LICENSE`](LICENSE).
