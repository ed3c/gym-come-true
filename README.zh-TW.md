# Gym Come True

[English](README.md)

> **狀態：可稽核的跨平台 foundation，尚未達到 App Store / Google Play 上架條件。**  
> 專案使用 Kotlin Multiplatform（KMP）與 Compose Multiplatform，支援 Android、iOS 與 Web。它不是醫療器材，不提供診斷、治療、用藥建議或自動補充品劑量推薦。

## 核心定位

Gym Come True 不是另一個泛用 AI 健身聊天機器人，而是：

> **面向台灣與繁體中文市場的 evidence-backed fitness protocol executor。**

它把四個原本分散的工作合在同一條可追溯流程：

1. 在裝置端辨識補充品標籤；
2. 讓使用者逐欄確認，找出跨產品重複成分；
3. 只做 deterministic arithmetic，不把總量冒充成安全或建議劑量；
4. 將已確認的個人計畫編譯成 16:00 或 22:00 訓練日 timetable。

## 已完成的垂直切片

- **Android**
  - system camera；
  - bundled Google ML Kit Chinese + Latin OCR；
  - barcode scanning；
  - temporary image deletion；
  - inexact local reminder。
- **iOS**
  - Shared Compose UI host；
  - PhotosPicker；
  - Apple Vision OCR/barcode；
  - local notification reminder。
- **Web**
  - Kotlin/Wasm；
  - Kotlin/JS fallback；
  - compatibility distribution。
- **Shared domain**
  - Traditional Chinese / English label parser；
  - `mcg / mg / g` conversion；
  - `IU / ml / 顆 / 錠 / unknown` fail closed；
  - daily intake aggregation；
  - duplicate ingredient detection；
  - medication、pregnancy、procedure、adverse symptom safety gate；
  - A/B schedule 與跨午夜排序；
  - LLM explanation-only contract。
- **Exercise data / media**
  - default-deny source/media registry；
  - first-party exercise seed 與 schematic provenance；
  - 不包含 scraped 或第三方 hotlinked media。

## 最重要的安全界線

- OCR 與 barcode 是 evidence，不是 truth。
- 使用者確認不等於臨床或官方來源驗證。
- `IU` 不能用通用公式轉成 `mg`。
- 每日總量只表示「已確認紀錄的算術加總」，不表示安全、有效或建議。
- 近期服藥、懷孕/哺乳、預定手術或異常症狀會阻擋 automation。
- 台灣 rule pack 尚未經合格專業審查，因此不能做 personalized dose / interaction 結論。
- LLM 不能補完缺失欄位、推薦劑量、判斷藥物安全或壓過 warning。
- provider key 不得放在 Android、iOS 或 Web client。
- local notification / inexact alarm 不是保證喚醒的 system alarm。
- AlarmKit 的系統 UI 有 stop control，因此不得宣稱「完成伏地挺身前無法關閉」。

## 藍海策略

不要與大型 workout logger 比「動作數量」或「AI 回答範圍」。初期 beachhead：

- 使用多種包裝補充品的成年重訓者；
- 需要繁體中文 label capture；
- 在下午與深夜訓練日之間切換；
- 需要把資料交給教練、營養師或藥師，而不是只留一堆截圖。

護城河不是基座模型，而是：

- canonical ingredient / exercise identity；
- OCR correction pairs；
- evidence lineage；
- clinically reviewed regional rule packs；
- rights-cleared media contracts and hashes；
- deterministic safety tests；
- protocol execution history；
- platform delivery reliability evidence。

完整策略見 [docs/product-strategy.md](docs/product-strategy.md)，行銷素材見 [docs/marketing-plan.md](docs/marketing-plan.md)。

## 建置

```bash
python3 scripts/validate_repository.py
./gradlew :shared:jvmTest
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
./gradlew :webApp:composeCompatibilityBrowserDistribution
```

iOS：

```bash
cd iosApp
xcodegen generate --spec project.yml
xcodebuild \
  -project GymComeTrue.xcodeproj \
  -scheme GymComeTrue \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

工具鏈固定為 JDK 21、Kotlin 2.4.10、AGP 9.1.0、Gradle 9.5.0、Compose Multiplatform 1.11.1。

## 文件索引

- [Architecture](docs/architecture.md)
- [Health safety](docs/health-safety.md)
- [Copyright / data governance](docs/copyright-and-data-governance.md)
- [Platform capability matrix](docs/platform-capability-matrix.md)
- [Store compliance](docs/store-compliance.md)
- [Implementation status](docs/implementation-status.md)
- [Roadmap](docs/roadmap.md)
