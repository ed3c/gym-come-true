package dev.ed3c.gymcometrue.ui

import dev.ed3c.gymcometrue.domain.DailyProtocolCompiler
import dev.ed3c.gymcometrue.domain.SafetyDecision
import dev.ed3c.gymcometrue.domain.TrainingVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The MVP is an information and logging tool (`docs/product/mvp-redesign.md`), so no screen may
 * tell a person that something is fine, cleared, or forbidden. The deterministic engine keeps its
 * verdict-shaped types; this test proves their vocabulary stops at the presentation boundary.
 */
class UserFacingLanguageTest {
    /**
     * Latin words are matched on word boundaries so that a checkpoint called a "safety check" is
     * not confused with a claim that something is "safe".
     */
    private val bannedLatinWords = listOf(
        "safe",
        "unsafe",
        "approved",
        "approval",
        "block",
        "blocked",
        "blocks",
        "cleared",
        "clearance",
        "verdict",
        "forbidden",
        "prohibited",
    )

    private val bannedHanTerms = listOf("安全", "核准", "批准", "禁止", "阻擋", "通過審核")

    /** Every sentence the shared UI can put on a screen, from every source it draws them from. */
    private fun userFacingStrings(): List<String> {
        val copy = ProductCopy.userFacing.flatMap { it.bothLocales }
        val decisions = SafetyDecision.entries.flatMap { ProductCopy.informationFor(it).bothLocales }
        val timeline = TrainingVariant.entries
            .flatMap { DailyProtocolCompiler.compile(it) }
            .flatMap { listOf(it.title, it.note) }
        return copy + decisions + timeline
    }

    private fun verdictVocabularyIn(text: String): List<String> {
        val latin = bannedLatinWords.filter { word ->
            Regex("\\b" + word + "\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }
        return latin + bannedHanTerms.filter { text.contains(it) }
    }

    @Test
    fun noUserFacingStringRendersASafetyVerdict() {
        val offenders = userFacingStrings()
            .mapNotNull { text ->
                val hits = verdictVocabularyIn(text)
                if (hits.isEmpty()) null else "$hits in \"$text\""
            }
        assertEquals(emptyList(), offenders)
    }

    @Test
    fun theCheckWouldFailOnVerdictWording() {
        // Negative control: the scan is only worth its green when it can go red.
        assertEquals(listOf("safe"), verdictVocabularyIn("This stack is safe for you."))
        assertEquals(listOf("安全"), verdictVocabularyIn("這個組合對你是安全的。"))
        assertEquals(emptyList(), verdictVocabularyIn("Evening safety check"))
    }

    @Test
    fun internalEnumNamesNeverReachAScreen() {
        val strings = userFacingStrings()
        val leaked = ProductCopy.internalEnumNamesNotUserFacing
            .filter { name -> strings.any { it.contains(name) } }
            .sorted()
        assertEquals(emptyList(), leaked)
    }

    @Test
    fun theAllowlistCoversTheDeterministicDecisionNames() {
        // The allowlist exists so these identifiers stay legal in the engine and illegal on screen.
        SafetyDecision.entries.forEach {
            assertTrue(it.name in ProductCopy.internalEnumNamesNotUserFacing)
        }
    }

    @Test
    fun everyDecisionIsRenderedAsInformationAboutTheUsersOwnLog() {
        assertTrue(
            ProductCopy.informationFor(SafetyDecision.LOG_ONLY).en
                .contains("matches your logged data"),
        )
        SafetyDecision.entries.forEach { decision ->
            val text = ProductCopy.informationFor(decision)
            assertTrue(text.zhHant.isNotBlank() && text.en.isNotBlank())
        }
    }

    @Test
    fun ocrAssistCopyStatesOnDeviceUserConfirmAndNoRetention() {
        val zh = ProductCopy.ocrAssist.zhHant
        assertTrue(zh.contains("裝置本機"), "on-device is not stated")
        assertTrue(zh.contains("由你確認"), "user confirmation is not stated")
        assertTrue(zh.contains("不會保留"), "non-retention is not stated")
        assertTrue(zh.contains("不會上傳"), "no upload is not stated")

        val en = ProductCopy.ocrAssist.en
        assertTrue(en.contains("on your device"), "on-device is not stated")
        assertTrue(en.contains("You confirm"), "user confirmation is not stated")
        assertTrue(en.contains("are not retained"), "non-retention is not stated")
        assertTrue(en.contains("not uploaded"), "no upload is not stated")
    }

    @Test
    fun everyUserFacingStringIsAuthoredInBothLocales() {
        ProductCopy.userFacing.forEach { text ->
            assertTrue(text.zhHant.isNotBlank() && text.en.isNotBlank())
            assertTrue(text.forLocale("zh-TW") == text.zhHant)
            assertTrue(text.forLocale("en") == text.en)
        }
    }
}
