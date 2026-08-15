# Blue-ocean strategy — evidence-first Body Hacker protocol

## Strategic thesis

Do not enter the market as another workout catalogue, macro tracker, generic AI coach, or supplement recommender. Those categories reward breadth, creator inventory, medical credibility, or data scale that a new product does not yet possess.

The initial category is:

> **A proof-before-advice protocol system that turns a physically scanned supplement label, explicit user confirmation, and a chosen workout time into an auditable daily fitness timeline.**

The user is not buying more content. The user is buying reduced uncertainty and fewer missed steps.

## Beachhead user

Primary early adopter:

- lives in Taiwan or reads Traditional Chinese labels;
- strength trains three or more times per week;
- owns several supplements and already follows an informal schedule;
- trains at a non-standard time such as 16:00 or 22:00;
- has experienced duplicated ingredients, forgotten servings, uncertain label units, or conflicting online advice;
- is willing to confirm evidence rather than demand instant certainty.

This segment has a repeated, observable job-to-be-done and produces the correction/evaluation data needed to improve the product safely.

## Non-customers to convert

1. **Spreadsheet and screenshot users** — already maintain routines but lack confirmation, state, and reminders.
2. **Users who abandoned nutrition trackers** — find food databases too heavy for the narrow problem of protocol execution.
3. **Users who distrust AI advice** — will use AI only when the evidence and deterministic decision are visible.
4. **Coaches managing protocol adherence manually** — may review a client timeline without receiving raw label photos or becoming a supplement marketplace.

## Strategic canvas

Relative levels: 1 = low, 5 = high.

| Competitive factor | Workout library app | Macro tracker | Generic AI coach | Supplement database/scanner | Gym Come True target |
|---|---:|---:|---:|---:|---:|
| Exercise/media breadth | 5 | 2 | 3 | 1 | 2 |
| Food logging depth | 2 | 5 | 3 | 2 | 2 |
| Conversational breadth | 1 | 2 | 5 | 2 | 2 |
| Label evidence visibility | 1 | 2 | 1 | 3 | 5 |
| Human confirmation before analysis | 1 | 2 | 1 | 2 | 5 |
| Deterministic fail-closed safety | 1 | 2 | 1 | 3 | 5 |
| Workout-time protocol scheduling | 2 | 2 | 3 | 1 | 5 |
| Asset-level copyright provenance | 2 | 2 | 1 | 2 | 5 |
| Traditional Chinese/Taiwan label cases | 2 | 3 | 2 | 2 | 5 |
| Offline/local-first utility | 3 | 2 | 1 | 2 | 4 |
| Medical/recommendation claims | 2 | 3 | 4 | 4 | 1 |

The target curve intentionally sacrifices initial catalogue breadth and open-ended chat to raise trust, evidence, local execution, and rights discipline.

## Eliminate–Reduce–Raise–Create grid

### Eliminate

- hidden OCR-to-answer flow;
- client-side LLM provider keys;
- “AI knows the safe dose” messaging;
- hotlinked or source-unknown exercise media;
- forced account creation before the user sees the evidence workflow;
- notifications that imply the user must ingest a product now;
- vanity roadmaps optimized for the number of exercises or model providers.

### Reduce

- manual food logging for the first release;
- social feeds, challenges, followers, and coach marketplaces;
- long onboarding questionnaires;
- generic chatbot surface area;
- cloud retention of raw label images;
- privileged alarm behavior;
- dependence on any single proprietary exercise API.

### Raise

- correction visibility and uncertainty language;
- source, rule-version, and evidence traceability;
- bilingual label normalization quality;
- medication/unknown-unit fail-closed routing;
- local-first speed and privacy;
- protocol completion measurement;
- accessibility and readable schedule states;
- asset-level licensing and hash verification.

### Create

- a “label evidence diff” showing OCR, user edits, and final confirmed values;
- `SAFE_TO_EXPLAIN`, `NEEDS_CONFIRMATION`, `REQUIRES_PROFESSIONAL_REVIEW`, and `NOT_SUPPORTED` states;
- 16:00 and 22:00 workout-day protocol templates;
- rule-pack provenance and reviewer signatures;
- a protocol simulator that explains why an event moved or was blocked;
- local muscle heatmaps backed by first-party or explicitly licensed assets;
- privacy-preserving coach review based on structured protocol data instead of raw photos;
- a rights ledger that can become a developer-facing licensed fitness-data product later.

## Defensible assets

Code alone is replicable. The moat must accumulate in assets a foundation model cannot safely recreate from a prompt:

1. confirmed Traditional Chinese label/evidence correction cases with consent and provenance;
2. adversarial parser and safety-gate evaluation cases;
3. effective-dated Taiwan rule packs signed by qualified reviewers;
4. protocol adherence outcomes and reason codes, not just clicks;
5. a clean exercise/muscle ontology with translation decisions;
6. first-party or contract-backed media plus exact rights and hashes;
7. platform reliability evidence across devices, OS versions, permissions, timezones, and process states;
8. creator assets with reusable paid-media rights and truthful claim records.

## Business model sequence

### Stage 1 — Consumer validation

- Free: manual protocol, limited confirmed scans, local timeline.
- Paid individual: scan history, reusable products, advanced schedule templates, exports, device sync when safely implemented.
- Do not paywall a critical safety warning or correction screen.

### Stage 2 — Coach/team workflow

- client-approved structured protocol review;
- role-based access, comment/audit trail, template assignment;
- no diagnosis or automatic supplement prescription.

### Stage 3 — Licensed data/API

- normalized exercise ontology;
- bilingual label parsing/eval cases where rights and consent allow;
- first-party/licensed muscle maps and media;
- provenance API and rights-state webhook.

The developer-data service is a possible later business, not a justification to ingest questionable datasets now.

## Pricing experiments

Run willingness-to-pay tests after activation and seven-day retention, not before:

| Package | Hypothesis | Test |
|---|---|---|
| Free local protocol | evidence workflow itself creates trust | scan-to-confirm and first-protocol completion |
| NT$90–150/month | recurring value comes from reduced daily friction | retained cohort paywall after three completed protocols |
| NT$790–1,190/year | annual plan fits habitual gym users | annual/monthly choice after demonstrated value; no deceptive default |
| Coach pilot | coaches pay for review throughput and auditability | 5–10 paid design partners before multi-tenant build |

Prices are experiment ranges, not committed store prices. Store fees, taxes, refunds, localization, customer support, and medical/legal review must be included in unit economics.

## Acquisition strategy

### Native content loop

`real label confusion -> visible scan uncertainty -> correction -> blocked/accepted state -> personal timeline -> next-day review`

The product is shown as a five-second part of a real routine rather than a narrated feature ad.

### Creator selection

Prioritize audience-context fit over follower count:

- Taiwan office workers who train after work;
- students/athletes with morning or late-night routines;
- evidence-based fitness educators;
- meal-prep creators who already disclose sponsorship clearly.

Use a fixed compliant-production/licensing fee plus a bounded performance bonus. View-only guarantees can reward misleading reach and should not be the sole payment mechanism.

### Owned distribution

- searchable evidence pages explaining common label structures without giving personal dosing advice;
- public changelog of parser/rule limitations;
- anonymized “what the scanner got wrong” engineering posts;
- KMP engineering case studies for Android/iOS/Web;
- creator templates that remain useful without paid reach.

## 90-day validation plan

### Days 1–21 — evidence loop

- ship manual entry and one-device scan path to a small internal/test cohort;
- measure scan start, confirmed evidence, correction fields, abandonment reason, and time to first protocol;
- interview users who abandon at confirmation;
- do not add more exercise content to fix a broken scan/confirmation loop.

Gate: at least 40% of qualified users who start a usable scan reach confirmed evidence; otherwise narrow labels and improve capture/correction UX.

### Days 22–45 — protocol value

- test 16:00 and 22:00 templates;
- measure first checkpoint, full-day completion, manual schedule edits, reminder denial/disable, and safety-block acknowledgement;
- test an in-app timeline before privileged alarms.

Gate: at least 30% of users who create a protocol complete three protocol days in the first seven days, with no material increase in unsafe override attempts.

### Days 46–70 — positioning and content

- run the four compliant creative concepts from the launch pack;
- compare proof-before-advice messaging with generic feature messaging;
- segment metrics by creator, training-time cohort, and label language;
- preserve creator/asset rights evidence with every deliverable.

Gate: one content angle must produce retained protocol users at a sustainable acquisition cost; viral views alone do not pass.

### Days 71–90 — monetization and partner proof

- test the paid individual tier only with retained users;
- recruit 5–10 coaches for structured review interviews and paid pilot proposals;
- decide whether the next investment is consumer retention, coach workflow, or rights-cleared exercise media.

Gate: paid conversion plus retention must support customer support, store fees, compute, review, and rights costs. Otherwise remain local-first and narrow scope.

## Kill criteria

Pause or change direction when any of these persists after a focused repair cycle:

- most scans require corrections that users cannot confidently make;
- users interpret blocked states as an approval or dose instruction;
- protocol reminders raise unsafe adherence pressure;
- retained value depends on medical advice the product is not qualified to provide;
- licensed media cost cannot be supported by retention/revenue;
- creator reach cannot be connected to confirmed scans and retained protocols;
- platform restrictions make the core experience unreliable without policy-risky behavior.

A kill criterion protects capital and user safety. It is not a reason to weaken evidence gates.
