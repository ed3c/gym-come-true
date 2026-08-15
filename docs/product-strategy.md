# Blue-ocean product strategy

## Strategic thesis

Do not compete as “another AI workout generator.” That category is easy to copy, hard to trust, and crowded with generic plans. Gym Come True should own a narrower job:

> Turn a user's real labels, real schedule, real training time, and licensed exercise evidence into an executable daily protocol with a visible proof trail.

The product is a **protocol execution and evidence ledger**, not an AI health oracle.

## Beachhead

### Primary user

A high-intent Taiwan-based fitness user who:

- trains at materially different times, such as 16:00 on some days and 22:00 on others;
- already uses several foods or supplements and currently relies on screenshots, notes, chat messages, and memory;
- wants reminders and a clear timeline, but does not want to upload every label photo or health detail to the cloud;
- needs to know what is confirmed, what is merely OCR, and what requires professional review;
- values Traditional Chinese and local product-label support.

### Job to be done

“When my training time, meal timing, product label, or medication context changes, help me rebuild today's plan without losing the evidence behind each step or pretending uncertainty is medical advice.”

### Trigger moments

- buying or replacing a supplement;
- switching between afternoon and late-night training;
- preparing for a trip or time-zone change;
- receiving medication or procedure instructions;
- trying to remember whether two products duplicate an ingredient;
- following a coach-created plan with many timed steps;
- recovering after a missed session or poor sleep.

## Blue-ocean wedge

The first market wedge combines four capabilities that are usually sold separately:

1. **On-device label evidence** — scan, confirm, hash, and retain structured facts rather than a pile of photos.
2. **A/B protocol compiler** — re-project meals, workout, recovery, and reminders when the training time changes, including cross-midnight events.
3. **Safety receipts** — deterministic `LOG_ONLY`, `REVIEW_REQUIRED`, or `BLOCK_AUTOMATION` reasons that an LLM cannot override.
4. **Copyright-clean exercise layer** — local schematic muscle view now; independently licensed metadata/media later, with a manifest users and partners can audit.

This combination is harder to imitate than an LLM prompt because it depends on verified source operations, platform adapters, rule review, and rights evidence.

## Eliminate–reduce–raise–create grid

| Eliminate | Reduce | Raise | Create |
|---|---|---|---|
| Generic chatbot as the product authority | Number of first-release features | Evidence visibility | Scan → confirm → safety-receipt workflow |
| Scraped GIFs and hidden CDN dependencies | Cloud upload by default | Local processing and consent | A/B cross-midnight protocol compiler |
| Social feed and vanity engagement | Manual schedule duplication | Traditional Chinese label support | Rights ledger for every shipped exercise asset |
| “100% reliable” alarm claims | Dependence on one model provider | Permission and failure transparency | Creator-ready protocol challenge templates |
| Automatic supplement dose advice | Unreviewed health data collection | Offline usefulness | Future B2B compliant catalog/API SKU |

## Competitive frame

Score products during research rather than assuming the result. The proposed strategic canvas uses these axes:

```text
workout logging
plan personalization
label evidence capture
regional label support
cross-midnight scheduling
on-device privacy
health-rule traceability
copyright provenance
licensed media quality
coach collaboration
LLM explainability controls
```

The expected pattern is:

- traditional workout trackers score high on logging and exercise breadth;
- supplement trackers score high on reminders but low on training integration;
- generic AI fitness apps score high on conversational personalization but low on evidence and source traceability;
- catalog APIs score high on exercise breadth/media but do not solve individual protocol execution;
- Gym Come True should score highest on evidence, schedule adaptation, privacy, and provenance before attempting maximum catalog breadth.

This is a hypothesis to validate through product teardowns and user testing, not a market fact.

## Three tiers of noncustomers

### Tier 1 — users leaving spreadsheets and screenshots

They already manage a plan manually but do not trust a generic app. Convert them with import, explicit uncertainty, and a daily screenshot/export view.

### Tier 2 — coaches who avoid supplement apps

They need a plan-delivery tool but do not want the product to appear to prescribe. Convert them with role boundaries, immutable plan versions, client confirmations, and review flags. Professional scope and jurisdiction must be respected.

### Tier 3 — fitness developers blocked by media rights

They need a clean catalog and muscle visualization but cannot defend scraped GIFs. After the consumer product proves the catalog operations, offer a separate B2B export/API with exact rights scope and hashes. Do not subsidize B2B by exposing consumer health data.

## Product ladder

### Free — Evidence Ledger

- local label scan candidate;
- manual confirmation;
- one active A/B protocol;
- basic timeline and inexact reminders;
- original schematic muscle view;
- exportable evidence summary;
- no cloud model required.

### Pro — Protocol OS

- multiple protocol versions and templates;
- time-zone/travel re-projection;
- encrypted sync and multi-device history;
- reviewed catalog and richer visualizations;
- coach/client plan handoff;
- private explanation gateway;
- advanced adherence and recovery review;
- store subscription, with pricing tested rather than assumed.

### Professional — Coach Workspace

- client invitation and scoped visibility;
- plan authorship/version receipts;
- template library and organization policy;
- no implied clinical authority;
- export/audit and role-based access;
- jurisdiction-specific terms and data processing agreement.

### Developer — Rights-Clean Catalog

- metadata export or API;
- per-record provenance;
- licensed media manifest and revocation feed;
- local muscle-map package;
- contract-defined caching and redistribution;
- separate billing and support SLA.

The Professional and Developer products are future options, not part of the initial store release.

## Defensible assets

| Asset | Why a model cannot simply absorb it | Build method |
|---|---|---|
| Taiwan label evidence corpus | Current product variants, consent, image/field corrections, provenance | opt-in capture, human confirmation, product-version identity |
| Reviewed regional rule pack | Source dates, reviewer liability, conflict rules, rollback | source anchoring, qualified review, tests, versioned receipts |
| Copyright-clean exercise catalog | Exact contracts, authoring, hashes, takedown operations | independent writing, commissioned/licensed media, asset ledger |
| Protocol outcome graph | User-owned plan versions, adherence, training-time shifts, recovery context | privacy-preserving event model and explicit consent |
| Native capability adapters | Platform permission/reliability behavior changes over time | Android/iOS harness tests and store-release evidence |
| Creator content library | Rights-cleared raw footage, hooks, audience/cost history | view-based contracts, reusable usage rights, experiment ledger |

Do not treat raw personal health data as a moat. Trust, source operations, and consented corrections are the moat.

## Growth loops

### Personal protocol loop

```text
scan/confirm -> build today's plan -> execute -> record friction
-> improve template -> easier next day -> retained user
```

### Coach loop

```text
coach shares plan -> client confirms evidence -> coach sees exceptions
-> safer update -> reusable template -> more clients invited
```

### Catalog loop

```text
user cannot find exercise/product -> provenance request
-> reviewed addition -> better coverage -> fewer failed plans
-> demand signal for next licensed asset batch
```

### Creator loop

```text
creator demonstrates a real protocol moment -> viewers ask for template
-> install/import template -> completion screenshot -> new creator story
```

Templates must contain routines and timing, not medical claims or hidden endorsements.

## Monetization experiments

Pricing is an experiment, not a fact. Begin with three store-safe cells and compare annual conversion, refund rate, 30/90-day retention, and support burden:

| Cell | Monthly test | Annual test | Intended signal |
|---|---:|---:|---|
| Accessible | NT$149 | NT$990 | willingness to pay for schedule and sync |
| Core | NT$199 | NT$1,290 | value of evidence history and reviewed catalog |
| Premium | NT$249 | NT$1,490 | coach handoff and private explanation value |

Do not launch a free trial until the onboarding can demonstrate value before the trial expires. Test both a limited free tier and a time-limited Pro trial. Annual plans reduce billing friction but must not hide renewal terms.

### Unit-economics model

```text
contribution margin per paid user =
  net store/web revenue
  - model and backend variable cost
  - media/CDN cost
  - creator acquisition cost amortized over paid retention
  - support/refund/fraud reserve

creator cohort payback =
  creator fee + production rights + management cost
  -------------------------------------------------
  incremental net contribution from exposed cohort
```

Use aggregate time-series lift and store campaign links where available. Do not claim exact person-level attribution when platform privacy prevents it.

## Product metrics

### North-star metric

**Verified protocol days per retained user**: a day where the user confirms the relevant evidence and completes or explicitly reschedules the protocol.

This is better than raw scans, notifications sent, or chat messages because it measures the intended job.

### Input metrics

- first protocol created within 10 minutes;
- scan-to-confirm completion rate;
- percentage of OCR candidates corrected;
- reminder permission acceptance after value demonstration;
- day-2, day-7, day-30 verified protocol retention;
- A/B plan switch success and cross-midnight error rate;
- safety-block acknowledgment rate;
- percentage of catalog views served from admitted assets;
- model explanation rejection rate;
- creator cohort payback and content reuse rate.

### Guardrail metrics

- false reassurance reports;
- unconfirmed evidence used in a plan;
- unresolved medication-context bypasses;
- raw-image retention defects;
- forbidden media/hotlink findings;
- notification/alarm reliability complaints;
- store privacy disclosure mismatch;
- refund and chargeback rate;
- accessibility task-completion gap.

## Validation plan

### Problem interviews

Conduct 30 structured interviews across:

- afternoon and late-night lifters;
- users with 0–2, 3–5, and 6+ supplement products;
- spreadsheet/Notes users;
- coaches and clients;
- users who stopped a fitness app because of trust or reminder failure.

Ask for the last actual day, labels, screenshots, changes, and mistakes. Avoid asking whether the idea sounds useful.

### Concierge test

Before full automation:

1. Let users photograph labels locally and manually confirm fields.
2. Generate the A/B timeline deterministically.
3. Provide a human-reviewed evidence summary.
4. Observe whether they use it for seven days.
5. Record every correction, skipped event, and trust question.

Do not manually provide individualized medical advice during the concierge test.

### Kill criteria

Pause or change the wedge when any of these persist after two iterations:

- fewer than 30% of qualified interviewees complete a seven-day protocol test;
- scan confirmation takes longer than manual entry for common labels;
- most users want only a generic workout tracker;
- safety review friction prevents ordinary non-clinical logging;
- licensed catalog cost cannot fit contribution margin;
- creator content produces installs but no verified protocol retention;
- store review or regional regulation makes the intended claim set untenable.

A failed wedge is evidence, not a reason to broaden into an undifferentiated fitness super-app.

## Strategic sequencing

```text
1. Prove scan-confirm-timeline retention with original/no media.
2. Add a small rights-clean catalog around the top 50 requested exercises.
3. Add reviewed Taiwan product identity and rule-pack operations.
4. Add encrypted sync and explanation gateway.
5. Add native health-store reads only for user-visible jobs.
6. Test coach handoff.
7. Offer B2B catalog only after rights operations are repeatable.
```

The sequence keeps the strongest differentiator—proof and execution—while limiting early legal, clinical, and media cost.
