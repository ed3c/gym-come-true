# Gym Come True — Taiwan launch creative pack

Audience: Taiwanese gym users who already own multiple supplements and struggle with label interpretation, timing, and adherence.  
Product wedge: **先證據、再分析；先確認、再排程。**

## Messaging hierarchy

1. **See the evidence** — show the recognized label text and uncertainty.
2. **Confirm before analysis** — the user corrects OCR rather than accepting a hidden guess.
3. **Fail closed** — unknown units, medication context, or unsupported rules stop the flow.
4. **Turn intent into time** — convert a confirmed protocol into a 16:00 or 22:00 workout-day timeline.
5. **Keep raw images local** — promote data minimization only where the platform implementation and privacy disclosure prove it.

Do not lead with “AI fitness coach.” Lead with a concrete moment: a crowded supplement shelf, a Traditional Chinese label, and uncertainty about what was actually written.

## App Store / Play short copy

### Name candidate

**Gym Come True — Body Protocol**

### Subtitle

**掃標籤、確認證據、排好今天的訓練與飲食時刻表**

### Short description

把補充品標籤、健身安排與飲食提醒整理成一條可確認、可追溯的每日 protocol。未知資訊會停止，而不是被 AI 猜測補完。

### Long-description opening

你可能不是缺少更多建議，而是缺少一個能回答三個問題的系統：

- 瓶身上到底寫了什麼？
- 哪些資訊仍不確定，不能直接分析？
- 今天 16:00 或 22:00 訓練時，整天要怎麼安排才不會忘記？

Gym Come True 先在裝置端擷取標籤文字與條碼候選，讓你逐項確認，再由可追溯規則產生狀態與時刻表。語言模型只能解釋已確認的結果，不能自行發明成分或劑量。

### Required disclaimer block

本產品用於個人紀錄、標籤整理與一般教育資訊，不提供診斷、治療、處方、藥物交互作用清除或個人化醫療建議。遇到用藥、疾病、手術、懷孕、過敏、未成年人、未知單位或不確定標籤時，系統應停止並請使用者尋求合格專業人員協助。

## Landing-page hero

**Eyebrow**  
PROOF BEFORE ADVICE

**Headline**  
你的補充品計畫，不該建立在 OCR 猜測上。

**Body**  
掃描實體標籤、看見系統讀到的文字、親自確認，再把今天的訓練、飲食與提醒排成一條可執行的時間線。遇到不確定資訊，Gym Come True 會停止，而不是補出一個看似合理的答案。

**Primary CTA**  
建立今天的 Protocol

**Secondary CTA**  
查看證據流程

**Trust strip**  
On-device evidence · Human confirmation · Deterministic safety gates · No client-side LLM keys

## UGC script A — 「它到底看到了什麼？」

Duration: 18–24 seconds  
Scene: real desk, supplement bottle, phone; no clinical coat or fake laboratory.

| Time | Visual | Spoken / on-screen text |
|---:|---|---|
| 0–2s | Camera pushes into a dense Chinese label | 「你真的知道 AI 剛剛讀到什麼嗎？」 |
| 2–6s | Scan; recognized lines appear with uncertain fields | 「這個 App 不會先給答案。它先把讀到的字攤開。」 |
| 6–10s | User corrects one unit/serving field | 「看錯一個單位，就不能假裝沒事。」 |
| 10–15s | Safety gate shows `需要確認` rather than green approval | 「不確定，就停。不是讓模型猜。」 |
| 15–21s | Confirmed items enter a 16:00 timeline | 「確認後，才排進今天的健身時刻表。」 |
| 21–24s | Product mark | 「Gym Come True。先證據，再分析。」 |

Caption:  
`掃描不是答案，只是證據的起點。#健身紀錄 #補充品標籤 #ProofBeforeAdvice`

Primary KPI: completed confirmations per qualified install.  
Stop rule: retire after two revisions if fewer than 25% of users who start a scan complete confirmation.

## UGC script B — 16:00 training day

Duration: 20–28 seconds  
Format: native “上班族健身日常”, not a feature walkthrough.

| Time | Visual | Text |
|---:|---|---|
| 0–3s | 12:00 lunch, timeline briefly visible | 「16:00 要練，但整天最容易亂掉的是前面。」 |
| 3–7s | User checks food and water items | 「今天吃了什麼，先記錄，不讓 App 自己猜。」 |
| 7–12s | 15:30 checkpoint | 「訓練前提醒：看自己的 protocol，不是照別人的劑量。」 |
| 12–18s | Gym set; no transformation claim | 「完成訓練，回來勾掉恢復步驟。」 |
| 18–24s | Daily evidence/protocol completion view | 「我的目標不是完美，是今天沒有跳步。」 |
| 24–28s | CTA | 「你是 16:00 還是 22:00 派？」 |

Caption question: `留言 16 或 22，我們會用完成率決定下一個模板。`

Primary KPI: seven-day protocol completion, segmented by training-time template.

## UGC script C — 22:00 training day

Duration: 20–25 seconds  
Risk to avoid: implying stimulants or a supplement are required for late training.

| Time | Visual | Text |
|---:|---|---|
| 0–3s | Late commute and clock | 「晚上十點才練，最怕不是懶，是把整天排錯。」 |
| 3–8s | Timeline shows meal, training, recovery, sleep boundary | 「我先排吃飯、訓練和睡眠邊界。」 |
| 8–13s | A supplement item displays `待確認` | 「標籤不確定，就不進 protocol。」 |
| 13–19s | Workout and post-workout check | 「做得到的步驟才留下。」 |
| 19–25s | Morning review | 「隔天不是看體重奇蹟，是看哪一步一直失敗。」 |

Primary KPI: retained protocols without reminder-disable or notification-uninstall signals.

## Creator brief

### Creator fit

- Taiwanese university/office-worker lifestyle, strength training, healthy meal preparation, or evidence-based fitness education.
- Comfortable showing real usage and uncertainty.
- No history of undisclosed medical, body-transformation, or supplement-income claims.

### Mandatory creative constraints

- Product appears inside the creator’s normal routine in the first three seconds.
- Show one uncertainty or correction; a flawless scan looks staged and undermines the product wedge.
- Do not say “安全劑量”, “不會交互作用”, “保證有效”, “醫師級”, “100% 準確”, “一定準時”, or “做不到就不能關掉”.
- Do not present the creator’s personal protocol as a universal schedule.
- Sponsored content must follow the platform’s disclosure tools and contract.
- Raw assets must be delivered with usage term, territory, platforms, edit rights, paid-media rights, creator/music releases, and expiry date.

### View-based experiment contract

Paying only for view counts can reward low-quality or misleading reach. Use a two-part structure:

1. fixed creation/licensing fee tied to compliant deliverables and rights;
2. bounded performance bonus tied to qualified metrics such as confirmed scans or retained protocols, with fraud review and a total cap.

Never require a creator to manufacture medical certainty or hide sponsorship to meet a view guarantee.

## Screenshot storyboard

1. **Today** — 16:00/22:00 protocol timeline with `confirmed`, `needs review`, and `skipped` states.
2. **Scan evidence** — recognized lines, barcode candidate, evidence hash, and correction controls.
3. **Safety gate** — a clear blocked example for an unknown/IU/medication-context case.
4. **Muscle map** — local SVG highlight with source/license attribution; no unlicensed exercise photo.
5. **Privacy** — diagram showing raw image local processing and the redacted LLM boundary.
6. **History** — adherence trend and reason codes, not a before/after body claim.

## Lifecycle messages

### Onboarding

- `先掃描，不先下結論。`
- `請確認每個成分、單位與每份用量。`
- `遇到用藥或未知資料，系統會停止分析。`

### Reminder

- `Protocol checkpoint：請依自己的已確認計畫操作。`
- `這是一則提醒，不代表現在必須服用任何產品。`
- `狀態改變了嗎？先更新，再繼續。`

### Error / blocked state

- `我們無法可靠換算這個單位。不要用猜測補完。`
- `標籤與條碼結果不一致，請重新確認實體包裝。`
- `目前規則未涵蓋你的用藥／健康情境，請詢問合格專業人員。`

## Measurement plan

| Layer | Metric | Why it matters |
|---|---|---|
| Acquisition | qualified landing sessions | filters irrelevant viral reach |
| Activation | scan started -> evidence confirmed | proves the core evidence loop |
| Safety | blocked cases acknowledged without override | confirms fail-closed UX is understood |
| Value | first protocol created and one checkpoint completed | measures action, not curiosity |
| Retention | protocols completed on 3 of first 7 days | tests repeat utility |
| Trust | correction rate, deletion/privacy-page views, support themes | reveals OCR and claim gaps |
| Revenue | trial-to-paid by retained protocol cohort | avoids optimizing the paywall before value |

North-star candidate: `weekly completed safety-gated protocols per retained user`.

## Asset ledger requirements for every creative

- creator and account identifiers;
- original file hash;
- creation and publication dates;
- sponsor disclosure evidence;
- music/stock/font/model releases;
- organic and paid-media rights separately;
- permitted edits, crops, subtitles, translations, territories, channels, and term;
- takedown/expiry state;
- performance data source and fraud notes.

A downloaded TikTok/Instagram post is not a reusable marketing asset unless the contract and all embedded rights allow it.
