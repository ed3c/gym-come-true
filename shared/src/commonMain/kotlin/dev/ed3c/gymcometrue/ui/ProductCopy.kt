package dev.ed3c.gymcometrue.ui

import dev.ed3c.gymcometrue.catalog.ActivationIntensity
import dev.ed3c.gymcometrue.catalog.BodyView
import dev.ed3c.gymcometrue.domain.SafetyDecision
import dev.ed3c.gymcometrue.explanation.AdmittedExplanationTemplates
import dev.ed3c.gymcometrue.explanation.ExplanationReasonKey
import dev.ed3c.gymcometrue.explanation.GatewayRejection

/**
 * One user-visible sentence pair. zh-Hant is primary and en is secondary, the order
 * `legal/DISCLAIMER.md` records.
 */
data class LocalizedText(val zhHant: String, val en: String) {
    init {
        require(zhHant.isNotBlank() && en.isNotBlank()) { "Both locales must be authored." }
    }

    fun forLocale(localeTag: String): String = if (localeTag.startsWith("zh")) zhHant else en

    val bothLocales: List<String> get() = listOf(zhHant, en)
}

/**
 * The single Kotlin source for every sentence a user can read in the shared UI (Issue #50).
 *
 * Two rules hold this file together:
 *
 * 1. The wording of [aiResponseNotice], [firstRunAcknowledgement] and [positioning] is copied from
 *    `legal/DISCLAIMER.md`, which the owner declared the SSOT on 2026-08-18. Changing it here
 *    without changing the disclaimer splits the source.
 * 2. Nothing here renders a safety verdict. The deterministic engine keeps its own types
 *    ([SafetyDecision] and friends are untouched); this object is the only place their names are
 *    turned into information language, and [internalEnumNamesNotUserFacing] is the list the tests
 *    use to prove those names never reach a screen.
 */
object ProductCopy {
    const val DISCLAIMER_SOURCE: String = "legal/DISCLAIMER.md"

    /** Mandatory on every AI response surface. Verbatim from the disclaimer SSOT. */
    val aiResponseNotice: LocalizedText = LocalizedText(
        zhHant = "AI 回應僅為一般資訊，並非醫療建議。本 App 不提供診斷、治療或劑量建議；" +
            "補充品、飲食與運動安排請諮詢合格醫療專業人員。相關決定與後果由使用者自行負責。",
        en = "AI responses are general information only and are not medical advice. This app does " +
            "not diagnose, treat, or recommend doses. Consult a qualified healthcare professional " +
            "about supplements, diet, and exercise. Decisions and their outcomes remain the user's own.",
    )

    /** Shown once before any AI feature is reachable. Verbatim from the disclaimer SSOT. */
    val firstRunAcknowledgement: LocalizedText = LocalizedText(
        zhHant = "本 App 為資訊與記錄工具，僅提供告知性內容，不構成醫療建議，" +
            "亦不對使用者依內容所做的決定承擔責任。使用 AI 功能代表你了解上述風險提示。",
        en = "This app is an information and logging tool. Content is provided for notice purposes " +
            "only, is not medical advice, and the platform does not accept responsibility for " +
            "decisions you make based on it. Using the AI features means you understand the risk " +
            "notice above.",
    )

    val firstRunAcknowledgeAction: LocalizedText = LocalizedText(
        zhHant = "我了解，繼續使用 AI 功能",
        en = "I understand — enable AI features",
    )

    val firstRunHeading: LocalizedText = LocalizedText(
        zhHant = "使用 AI 功能前，請先閱讀",
        en = "Read this before using AI features",
    )

    val positioning: LocalizedText = LocalizedText(
        zhHant = "資訊與記錄工具：呈現你自己登記的資料，以及對這些資料的誠實計算。",
        en = "An information and logging tool: your own logged data, and honest arithmetic over it.",
    )

    /** What the AI may and may not do, stated where the AI entry point lives. */
    val aiBoundary: LocalizedText = LocalizedText(
        zhHant = "AI 只說明你自己的紀錄與計算結果，不做診斷，也不給劑量建議。",
        en = "AI only explains your own logged data and arithmetic. It does not diagnose and does " +
            "not recommend doses.",
    )

    val aiSectionHeading: LocalizedText = LocalizedText(
        zhHant = "AI 說明",
        en = "AI explanation",
    )

    val aiResponseAbsent: LocalizedText = LocalizedText(
        zhHant = "目前沒有 AI 說明。要有紀錄可以說明時才會產生。",
        en = "No AI explanation yet. One is produced only when there is a record to explain.",
    )

    val loggedDataHeading: LocalizedText = LocalizedText(
        zhHant = "你的紀錄",
        en = "Your logged data",
    )

    /**
     * OCR assist copy. All three semantics the MVP promises — on device, confirmed by the user,
     * not retained — are stated in one sentence so a screen cannot show two of the three.
     */
    val ocrAssist: LocalizedText = LocalizedText(
        zhHant = "標籤辨識在裝置本機執行，辨識結果要由你確認之後才會成為紀錄；" +
            "照片不會上傳，也不會保留。",
        en = "Label recognition runs on your device. You confirm every candidate before it becomes " +
            "a record. Photos are not uploaded and are not retained.",
    )

    val ocrAssistHeading: LocalizedText = LocalizedText(
        zhHant = "標籤辨識",
        en = "Label assist",
    )

    val ocrAssistIdle: LocalizedText = LocalizedText(
        zhHant = "還沒有辨識過標籤。",
        en = "No label has been recognized yet.",
    )

    val scanAction: LocalizedText = LocalizedText(zhHant = "辨識標籤", en = "Scan label")

    val reminderAction: LocalizedText = LocalizedText(zhHant = "測試提醒", en = "Test reminder")

    val timelineHeading: LocalizedText = LocalizedText(
        zhHant = "今天的行程",
        en = "Today's timeline",
    )

    val timelineVariantNote: LocalizedText = LocalizedText(
        zhHant = "深夜的行程會保留隔日順序，不會把 00:15 排到 22:00 前面。",
        en = "Late-plan events preserve next-day ordering instead of placing 00:15 before 22:00.",
    )

    val timelineHeadingVariants: LocalizedText = LocalizedText(
        zhHant = "訓練時段",
        en = "Training slot",
    )

    val confirmationRequired: LocalizedText = LocalizedText(
        zhHant = "需要你確認",
        en = "Needs your confirmation",
    )

    val muscleHeading: LocalizedText = LocalizedText(
        zhHant = "本機肌群檢視",
        en = "Local muscle view",
    )

    val muscleNote: LocalizedText = LocalizedText(
        zhHant = "由第一方示意圖自己的區塊幾何即時繪製，沒有內含第三方解剖插圖。",
        en = "Drawn from the first-party schematic's own region geometry. No third-party anatomy " +
            "illustration is bundled.",
    )

    /**
     * Muscle-map copy (Issue #48 rendering).
     *
     * Every sentence here describes the user's own log. None of them says what to train next, how
     * hard to train it, or what any shading means for the body — the shading is an editorial
     * classification of the movement, and this is where that is said out loud.
     */
    val muscleInformationNote: LocalizedText = LocalizedText(
        zhHant = "這裡呈現你自己登記的動作涵蓋到哪些肌群。深淺只是動作分類的強弱，不是量測值，也不是訓練建議。",
        en = "This shows which muscles your own logged exercises cover. The shading is an editorial " +
            "movement classification, not a measurement and not a training recommendation.",
    )

    val muscleLogEmpty: LocalizedText = LocalizedText(
        zhHant = "這個時段還沒有登記動作。有紀錄之後才會標示肌群。",
        en = "No exercise is logged for this slot yet. Muscles are shaded once there is a record.",
    )

    val muscleLogUnresolved: LocalizedText = LocalizedText(
        zhHant = "有紀錄對不到目錄裡的動作，因此這次不標示任何肌群：",
        en = "A logged entry matches no catalog exercise, so nothing is shaded this time:",
    )

    val muscleUnrenderedNote: LocalizedText = LocalizedText(
        zhHant = "這張示意圖沒有下列肌群的區塊，只能以文字列出：",
        en = "The schematic has no region for these muscles, so they are listed as text only:",
    )

    val muscleViewFront: LocalizedText = LocalizedText(zhHant = "正面", en = "Front")

    val muscleViewBack: LocalizedText = LocalizedText(zhHant = "背面", en = "Back")

    /** Legend wording for the closed intensity scale; the same three classes the catalog uses. */
    val muscleIntensityLabels: Map<ActivationIntensity, LocalizedText> = mapOf(
        ActivationIntensity.PRIMARY to LocalizedText(zhHant = "主要", en = "Primary"),
        ActivationIntensity.SECONDARY to LocalizedText(zhHant = "協同", en = "Supporting"),
        ActivationIntensity.STABILIZER to LocalizedText(zhHant = "穩定", en = "Stabilizing"),
    )

    fun muscleViewLabel(view: BodyView): LocalizedText = when (view) {
        BodyView.FRONT -> muscleViewFront
        BodyView.BACK -> muscleViewBack
    }

    /**
     * Information language for each deterministic decision type. The engine still returns
     * [SafetyDecision]; only this table decides what a person reads, and none of these sentences
     * tells the user that anything is fine, permitted, or forbidden.
     */
    val templateCopy: Map<String, LocalizedText> = mapOf(
        AdmittedExplanationTemplates.DISCLAIMER_TEMPLATE to aiResponseNotice,
        "tpl.decision.log-only" to LocalizedText(
            zhHant = "這筆內容與你登記的資料相符，已存成紀錄。",
            en = "This matches your logged data and is kept as a record.",
        ),
        "tpl.decision.review-required" to LocalizedText(
            zhHant = "這筆紀錄還有未確認的細節。要做決定前，可以先諮詢合格醫療專業人員。",
            en = "Some details in this record are unconfirmed. Consider consulting a qualified " +
                "healthcare professional before you decide.",
        ),
        "tpl.decision.block-automation" to LocalizedText(
            zhHant = "本 App 不會依這筆紀錄自動排程。相關決定請自行判斷，或諮詢合格醫療專業人員。",
            en = "The app will not schedule anything automatically from this record. The decision " +
                "is yours; consider consulting a qualified healthcare professional.",
        ),
        "tpl.reason.no-confirmed-ingredient" to LocalizedText(
            zhHant = "還沒有從標籤確認到成分與含量。",
            en = "No ingredient-and-amount pair has been confirmed from the label yet.",
        ),
        "tpl.reason.adverse-symptom" to LocalizedText(
            zhHant = "你記錄了不適症狀。這類狀況適合由醫療專業人員當面評估。",
            en = "You logged an adverse symptom. A healthcare professional is the right person to " +
                "assess it.",
        ),
        "tpl.reason.pregnancy-or-breastfeeding" to LocalizedText(
            zhHant = "你記錄了懷孕或哺乳。相關的補充品安排請諮詢合格醫療專業人員。",
            en = "You logged pregnancy or breastfeeding. Consider consulting a qualified " +
                "healthcare professional about supplements.",
        ),
        "tpl.reason.planned-procedure" to LocalizedText(
            zhHant = "你記錄了預定的手術或處置。相關安排請先諮詢醫療專業人員。",
            en = "You logged a planned procedure. Consider consulting a healthcare professional " +
                "about it first.",
        ),
        "tpl.reason.medication-context" to LocalizedText(
            zhHant = "你記錄了近期用藥。本 App 不評估藥物與補充品的交互作用。",
            en = "You logged recent medication use. This app does not assess interactions between " +
                "medication and supplements.",
        ),
        "tpl.reason.unverified-evidence" to LocalizedText(
            zhHant = "標籤辨識與手動輸入的內容，你還沒有對照實體標籤確認。",
            en = "Recognized and typed label details have not been confirmed against the physical " +
                "label.",
        ),
        "tpl.reason.unresolved-unit" to LocalizedText(
            zhHant = "IU、體積或計數單位沒有通用的質量換算，所以沒有加進總量。",
            en = "IU, volume, and count units have no generic mass conversion, so they are left " +
                "out of the total.",
        ),
        "tpl.reason.no-reviewed-rule-pack" to LocalizedText(
            zhHant = "本 App 不比對任何臨床標準，只呈現你自己記錄的資料。",
            en = "This app compares nothing against clinical limits. It shows your own logged data.",
        ),
        "tpl.next-step.confirm-physical-label" to LocalizedText(
            zhHant = "下一步：對照實體標籤確認內容。",
            en = "Next: confirm the details against the physical label.",
        ),
        "tpl.next-step.seek-qualified-review" to LocalizedText(
            zhHant = "下一步：諮詢合格醫療專業人員。",
            en = "Next: consider consulting a qualified healthcare professional.",
        ),
        "tpl.next-step.record-symptom" to LocalizedText(
            zhHant = "下一步：把你注意到的症狀記錄下來。",
            en = "Next: record the symptom you noticed.",
        ),
    )

    /** Information rendering of a deterministic decision. The enum name never leaves the engine. */
    fun informationFor(decision: SafetyDecision): LocalizedText =
        templateCopy.getValue(AdmittedExplanationTemplates.decisionTemplates.getValue(decision))

    /**
     * Enum names that stay inside the deterministic engine and the gateway. They are legitimate
     * Kotlin identifiers and dangerous user-facing words, so the tests assert they appear in no
     * string this object publishes.
     */
    val internalEnumNamesNotUserFacing: Set<String> =
        SafetyDecision.entries.map { it.name }.toSet() +
            ExplanationReasonKey.entries.map { it.name }.toSet() +
            GatewayRejection.entries.map { it.name }.toSet()

    /** Every sentence the shared UI can render from this object. The language tests scan it. */
    val userFacing: List<LocalizedText> = listOf(
        aiResponseNotice,
        firstRunAcknowledgement,
        firstRunAcknowledgeAction,
        firstRunHeading,
        positioning,
        aiBoundary,
        aiSectionHeading,
        aiResponseAbsent,
        loggedDataHeading,
        ocrAssist,
        ocrAssistHeading,
        ocrAssistIdle,
        scanAction,
        reminderAction,
        timelineHeading,
        timelineVariantNote,
        timelineHeadingVariants,
        confirmationRequired,
        muscleHeading,
        muscleNote,
        muscleInformationNote,
        muscleLogEmpty,
        muscleLogUnresolved,
        muscleUnrenderedNote,
        muscleViewFront,
        muscleViewBack,
    ) + templateCopy.values + muscleIntensityLabels.values
}
