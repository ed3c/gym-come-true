# Gym Come True — user notice and disclaimer (SSOT)

Owner decision 2026-08-18. This file is the single source for in-app, store-listing, and
AI-response wording. Product surfaces must render from or link to this text; deterministic
tests assert the AI-response notice is present on every LLM surface.

## Positioning

Gym Come True is an information and logging tool. It records what you choose to log and shows
honest arithmetic over your own data. It does not diagnose, treat, prescribe, or recommend
doses, and it renders no safety verdicts.

## AI medical-risk notice (mandatory on every AI response)

zh-Hant:
> AI 回應僅為一般資訊，並非醫療建議。本 App 不提供診斷、治療或劑量建議；補充品、飲食與運動
> 安排請諮詢合格醫療專業人員。相關決定與後果由使用者自行負責。

en:
> AI responses are general information only and are not medical advice. This app does not
> diagnose, treat, or recommend doses. Consult a qualified healthcare professional about
> supplements, diet, and exercise. Decisions and their outcomes remain the user's own.

AI features are powered by third-party large-language-model providers (OpenAI ChatGPT and
Anthropic Claude). Provider output may be inaccurate or incomplete.

## First-run acknowledgement (zh-Hant primary, en secondary)

zh-Hant:
> 本 App 為資訊與記錄工具，僅提供告知性內容，不構成醫療建議，亦不對使用者依內容所做的決定
> 承擔責任。使用 AI 功能代表你了解上述風險提示。

en:
> This app is an information and logging tool. Content is provided for notice purposes only,
> is not medical advice, and the platform does not accept responsibility for decisions you make
> based on it. Using the AI features means you understand the risk notice above.

## Scope note (recorded for the owner, not user-facing)

A disclaimer reduces exposure but cannot fully exclude statutory liability — e.g. Taiwan
consumer-protection rules limit blanket waivers in standard-form terms, and Apple/Google
health-app review policies apply independently. The functional design (no diagnosis, no dosing,
no verdicts) is the primary control; this document is the secondary one. Final terms-of-service
language should get legal review before store submission.
