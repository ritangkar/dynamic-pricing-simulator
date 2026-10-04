# Portfolio Demo

**Demo level:** 1 — client-side, fully deterministic.

`lab/demos/dynamic-pricing-simulator.js` ports `PricingEngine` line-for-line
to client-side JavaScript — the same named coefficients, the same
constraint order — so moving a slider (demand change, inventory,
competitor price, seasonality) recomputes a real recommendation instantly,
with the full rationale and any constraints that fired, no backend call.

Pre-loaded scenarios include the baseline (17 units, +34% demand) and the
"inventory −30%" / "inventory +50%" what-if comparisons documented in
`docs/design-decisions.md`, so a recruiter can see the continuous
inventory-adjustment behavior directly rather than taking the design
decision's write-up on faith.
