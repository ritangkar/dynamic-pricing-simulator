# Design Decisions

---

**Decision:** Every coefficient in `PricingEngine` (demand sensitivity
0.08, inventory sensitivity 0.4, price elasticity -1.6, competitive pull
0.15, target days-of-supply 14) is a named constant with a documented
rationale, not a number tuned to hit a specific target output.

**Why this needs saying plainly:** the master brief for this collection
includes an illustrative example (Running Shoe X: inventory 17, demand
+34%, competitor ₹7,499, current price ₹7,199 → recommended ₹7,399,
revenue +8.4%). This project's engine, given the same demand/inventory/
competitor inputs (with an assumed sales velocity and cost price, since
the brief's example doesn't specify them), recommends **₹7,680** with an
expected revenue change of **+31.55%** — not ₹7,399 / +8.4%. That's
deliberate: reverse-engineering the coefficients to reproduce someone
else's illustrative numbers would be curve-fitting to a made-up target,
not real pricing-logic engineering. The rules here are independently
reasoned, independently verified for internal consistency (monotonicity,
constraint enforcement), and reported honestly — including when that means
not matching a spec's example number.

**Trade-off:** These coefficients are illustrative business-rule
assumptions (a plausible elasticity, a plausible inventory-coverage
target), not fitted to real sales data — this is a demonstration of
pricing-engine *architecture*, not a calibrated production pricing model.
Labeled as such throughout.

---

**Decision:** Inventory adjustment is a continuous function of
days-of-supply (`(target − actual) × sensitivity`, capped), not discrete
bands.

**Why — the actual design history:** the first version used bands
("< 7 days: +6%, < 14 days: +3%, otherwise 0% or a discount band"). While
verifying the what-if scenario "inventory decreases by 30%" against a
baseline of 17 units (5.7 days of supply) down to 12 units (4.0 days of
supply), both values landed in the same "< 7 days" band — so the
recommended price came out **identical** for two meaningfully different
inventory levels. That's a real defect in a project whose whole point is
letting someone explore what-if scenarios: a discrete model made two
different questions produce the same answer. Switching to a continuous
function fixed it — the same what-if now shows ₹7,680 → ₹7,728, a real,
differentiated response.

**Trade-off:** A continuous function is slightly harder to reason about at
a glance than "if inventory is low, add 6%" — mitigated by
`PricingRecommendation.rationale` always stating the exact days-of-supply
and the resulting percentage, not just the rule name.

---

**Decision:** Inventory risk is bidirectional — both very low days-of-supply
(stockout risk) and very high days-of-supply (overstock/markdown risk)
are classified `HIGH`, with `MEDIUM`/`LOW` in between.

**Why:** A real merchandising team worries about both directions. A model
that only flagged low stock as risky would silently miss the overstock
case entirely — see the verified "Overstock scenario" in the test suite,
where 500 units against a weakening demand signal is correctly flagged
`HIGH` risk (severe overstock) even though nothing is remotely close to
running out.

---

**Decision:** Business constraints (max single move, margin floor, optional
competitor ceiling) are applied in that fixed order, after the raw signal
combination, every time — never skipped, never reordered per-request.

**Why:** A pricing engine whose safety rails could be silently bypassed by
an unusual combination of inputs isn't safe rails at all.
`constraintsApplied` in the response always lists exactly which ones fired,
so this is auditable per-request, not just asserted in documentation.
