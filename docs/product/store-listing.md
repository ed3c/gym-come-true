# Store-listing wording — DRAFT (Issue #50)

**State:** `DRAFT`. This is proposed wording, not admitted copy. Store submission is Human Admit,
and the disclaimer SSOT records that final terms-of-service language needs legal review first.

**Source of truth:** [`legal/DISCLAIMER.md`](../../legal/DISCLAIMER.md). Every claim below is a
projection of that file plus [`docs/product/mvp-redesign.md`](mvp-redesign.md). If the two disagree,
the disclaimer wins and this draft is wrong.

**In-app SSOT:** the same sentences live in
`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/ui/ProductCopy.kt`. Listing copy and app copy
must not drift; a change here without a change there splits the source.

---

## Positioning line (zh-Hant primary, en secondary)

zh-Hant:
> 資訊與記錄工具：呈現你自己登記的資料，以及對這些資料的誠實計算。

en:
> An information and logging tool: your own logged data, and honest arithmetic over it.

## Short description

zh-Hant:
> 記錄補充品、飲食與訓練，看見你自己資料的加總與時間軸。AI 只說明你的紀錄，不做診斷，
> 也不給劑量建議。

en:
> Log supplements, meals, and training, and see the totals and timeline of your own data. AI only
> explains your records — it does not diagnose and does not recommend doses.

## Full description

zh-Hant:
> Gym Come True 是資訊與記錄工具。
>
> - 記錄：補充品、飲食、訓練與提醒，都由你自己輸入或確認。
> - 計算：單位換算（微克／毫克／公克）、每日加總、重複成分的重疊，全部由確定性程式完成，
>   結果只呈現為資訊。
> - 標籤辨識：在裝置本機執行，辨識結果要由你確認之後才會成為紀錄；照片不會上傳，也不會保留。
> - AI 說明：由第三方大型語言模型（OpenAI ChatGPT、Anthropic Claude）提供，僅為一般資訊，
>   每則回應都附上風險提示。
> - 肌群檢視與動作分類：第一方素材，沒有內含第三方解剖插圖。

en:
> Gym Come True is an information and logging tool.
>
> - Log: supplements, meals, training, and reminders — all entered or confirmed by you.
> - Compute: unit conversion (mcg/mg/g), daily totals, and duplicate-ingredient overlap, all done
>   by deterministic code and presented as information.
> - Label assist: recognition runs on your device. You confirm every candidate before it becomes a
>   record. Photos are not uploaded and are not retained.
> - AI explanation: powered by third-party large-language-model providers (OpenAI ChatGPT and
>   Anthropic Claude). Responses are general information and each one carries the risk notice.
> - Muscle view and exercise taxonomy: first-party material, with no third-party anatomy
>   illustration bundled.

## What this app does not do (must stay in the listing)

zh-Hant:
> 本 App 不提供診斷、治療或劑量建議，也不會對你的補充品組合做出判定。
> 補充品、飲食與運動安排請諮詢合格醫療專業人員。相關決定與後果由使用者自行負責。

en:
> This app does not diagnose, treat, or recommend doses, and it renders no judgement about your
> supplement combination. Consult a qualified healthcare professional about supplements, diet, and
> exercise. Decisions and their outcomes remain the user's own.

## AI notice shown in the app (verbatim from the SSOT)

zh-Hant:
> AI 回應僅為一般資訊，並非醫療建議。本 App 不提供診斷、治療或劑量建議；補充品、飲食與運動
> 安排請諮詢合格醫療專業人員。相關決定與後果由使用者自行負責。

en:
> AI responses are general information only and are not medical advice. This app does not
> diagnose, treat, or recommend doses. Consult a qualified healthcare professional about
> supplements, diet, and exercise. Decisions and their outcomes remain the user's own.

## Claims this listing may never make

Apple 1.4.x and Google Play health policies are reviewed independently of any disclaimer, so the
listing must not imply a medical function it does not have:

- no diagnosis, treatment, dosing, or interaction assessment;
- no "safe", "approved", or "cleared" claim about any product, dose, or combination;
- no clinical-review, certification, or regulator-endorsement claim (none exists — the reviewed
  rule-pack lane is retired);
- no reliability claim for reminders beyond measured device evidence
  (`HONEST_ALARM_SEMANTICS`);
- no accuracy claim for label recognition that is not backed by a recorded OCR evaluation.

## Gates that remain open

| Gate | State |
|---|---|
| legal review of listing + terms | `ABSENT` |
| store submission and review | `ABSENT` (Human Admit) |
| screenshots / marketing assets | `ABSENT` |
| localization review beyond zh-Hant/en | `ABSENT` |
