# MVP redesign — owner decision 2026-08-18

**Authority:** repository owner (Human Admit), recorded by the tech-lead session on 2026-08-18.
**Supersedes:** the regulated rule-pack product direction for the MVP. Contracts already merged
stay in the tree; this document changes what the shipped product claims and which issue lanes
continue.

## Product repositioning

Gym Come True MVP is an **information and logging tool**, not a safety-assessment or medical
product:

1. The app records what the user chooses to log (supplements, meals, workouts, reminders).
2. Deterministic code still does honest arithmetic (unit normalization mcg/mg/g, daily totals,
   duplicate-product overlap, protocol/meal-plan compilation) — presented as **information**,
   never as a safety verdict, clearance, or dose recommendation.
3. LLM features use **OpenAI (ChatGPT) and Anthropic (Claude)** as providers. Every AI response
   is general information and MUST carry the medical-risk notice (below). The AI never
   diagnoses, prescribes, or overrides the user's own decisions.
4. The platform and app **inform; they do not decide**. Legal positioning is
   disclaimer-and-notice based (see `legal/DISCLAIMER.md`). Note recorded for the owner: a
   disclaimer reduces exposure but cannot fully exclude statutory liability (e.g. Taiwan
   consumer-protection law limits blanket waivers; Apple 1.4.x / Google Play health policies
   still apply at store review). The functional repositioning in this document — no diagnosis,
   no dosing, no safety verdicts — is the primary risk control; the disclaimer is the secondary
   one.

## Retired lanes (owner: do not pursue)

| Retired | Was | Why retired |
|---|---|---|
| #24 consented TW label corpus | consent/deletion pipeline for real user label images | MVP does on-device OCR with user confirmation and does not retain a corpus |
| #26 reviewed TW rule pack | MOHW/TFDA-sourced clinical rule admission | app no longer renders safety verdicts, so no clinical rule pack is required |
| #34 licensed media pipeline | commercial exercise media admission | MVP ships first-party/synthetic assets only |
| #41–#43 creator-market loop | interviews, creator contracts, paid campaigns | owner decision: not part of MVP validation |

The corresponding Kotlin contracts and validators merged at `505b66f` remain in the tree as
dormant, tested code (they guard nothing in the MVP product surface and cost nothing). Removing
them is allowed later if they get in the way.

## Owner-admitted external gates (2026-08-18)

The owner admitted the previously `ABSENT` external gates for: #25 (OCR evaluation), #27–#29
(iOS device/entitlement/store evidence), #30–#31 (Android device/OEM evidence), #36–#37
(LLM provider integration and adversarial evals), #38–#40 (entitlement/privacy/store release).
Those issues close as human-admitted; follow-up work continues under the MVP issues below.

## MVP feature set

1. **Log & compile** — supplement/meal/workout logging, A/B protocol and daily meal timetable
   (already implemented, stays).
2. **On-device label OCR assist** — camera/photo → on-device text/barcode candidates → user
   confirms; images stay local and temporary; nothing uploaded, no corpus retained.
3. **AI explain (ChatGPT + Claude)** — server-side or user-key provider boundary; deterministic
   receipts and logged data may be summarized/explained; mandatory risk notice on every
   response; provider kill switch; no secrets in the repo or clients.
4. **Reminders** — inexact/local notifications with honest semantics (unchanged).
5. **Muscle map & catalog** — first-party taxonomy, bilingual top-50, schematic muscle view.
6. **Disclaimer & notice flow** — first-run acknowledgement + persistent AI-response notice +
   store-listing wording, single source in `legal/DISCLAIMER.md`.

## Mandatory AI medical-risk notice (single source of truth)

zh-Hant:
> AI 回應僅為一般資訊，並非醫療建議。本 App 不提供診斷、治療或劑量建議；補充品、飲食與運動
> 安排請諮詢合格醫療專業人員。相關決定與後果由使用者自行負責。

en:
> AI responses are general information only and are not medical advice. This app does not
> diagnose, treat, or recommend doses. Consult a qualified healthcare professional about
> supplements, diet, and exercise. Decisions and their outcomes remain the user's own.

Every LLM-rendered response surface must include this notice (or a platform-appropriate
abbreviation linking to it). Its presence is enforced by deterministic tests.
