package dev.ed3c.gymcometrue.ui

import dev.ed3c.gymcometrue.domain.SafetyDecision
import dev.ed3c.gymcometrue.explanation.AdmittedExplanationTemplates
import dev.ed3c.gymcometrue.explanation.DeterministicExplanationPlanner
import dev.ed3c.gymcometrue.explanation.ExplanationPlan
import dev.ed3c.gymcometrue.explanation.ExplanationReasonKey
import dev.ed3c.gymcometrue.explanation.MinimizedDecisionReceipt
import dev.ed3c.gymcometrue.privacy.ConsentEvent
import dev.ed3c.gymcometrue.privacy.ProcessingPurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TODAY = "2026-08-18"

private fun surfaceReceipt(
    decision: SafetyDecision,
    reasonKeys: List<ExplanationReasonKey>,
): MinimizedDecisionReceipt = MinimizedDecisionReceipt(
    receiptId = "receipt-surface",
    receiptSha256 = "a".repeat(64),
    productVariantKeySha256 = "b".repeat(64),
    evidenceSha256 = "c".repeat(64),
    rulePackId = "pack.absent",
    rulePackVersion = "v0",
    rulePackContentSha256 = "d".repeat(64),
    decision = decision,
    reasonKeys = reasonKeys,
)

/** The plan shape the gateway actually serves: the deterministic fallback for one receipt. */
private fun surfacePlan(
    decision: SafetyDecision,
    reasonKeys: List<ExplanationReasonKey>,
    localeTag: String,
): ExplanationPlan {
    val receipt = surfaceReceipt(decision, reasonKeys)
    return ExplanationPlan(
        receiptId = receipt.receiptId,
        receiptSha256 = receipt.receiptSha256,
        decisionRestated = decision,
        templateIds = DeterministicExplanationPlanner.requiredTemplates(receipt).sorted(),
        reasonKeysCovered = reasonKeys,
        localeTag = localeTag,
    )
}

class AiAcknowledgementGateTest {
    private val anyPlan = surfacePlan(SafetyDecision.LOG_ONLY, emptyList(), "zh-TW")

    @Test
    fun aiSurfacesAreUnreachableUntilTheNoticeIsAcknowledged() {
        val access = FirstRunAcknowledgement.resolve(emptyList(), TODAY)
        assertEquals(AiAccess.UNACKNOWLEDGED, access)
        assertNull(
            AiResponsePresenter.present(access, anyPlan),
            "An AI response must have no representation before the notice is acknowledged.",
        )
    }

    @Test
    fun acknowledgementOpensTheAiSurface() {
        val history = listOf(FirstRunAcknowledgement.event(sequence = 1, occurredAtIsoDate = TODAY))
        val access = FirstRunAcknowledgement.resolve(history, TODAY)
        assertEquals(AiAccess.ACKNOWLEDGED, access)
        assertNotNull(AiResponsePresenter.present(access, anyPlan))
    }

    @Test
    fun withdrawalClosesTheAiSurfaceAgain() {
        val history = listOf(
            FirstRunAcknowledgement.event(sequence = 1, occurredAtIsoDate = "2026-08-17"),
            FirstRunAcknowledgement.event(sequence = 2, occurredAtIsoDate = TODAY, granted = false),
        )
        val access = FirstRunAcknowledgement.resolve(history, TODAY)
        assertEquals(AiAccess.UNACKNOWLEDGED, access)
        assertNull(AiResponsePresenter.present(access, anyPlan))
    }

    @Test
    fun aChangedNoticeVersionRequiresAFreshAcknowledgement() {
        val stale = ConsentEvent(
            sequence = 1,
            purpose = ProcessingPurpose.SERVICE_DELIVERY,
            granted = true,
            occurredAtIsoDate = "2026-01-01",
            policyVersion = "disclaimer-2026-01-01",
            evidenceRef = FirstRunAcknowledgement.EVIDENCE_REF,
        )
        assertEquals(AiAccess.UNACKNOWLEDGED, FirstRunAcknowledgement.resolve(listOf(stale), TODAY))
    }

    @Test
    fun aBrokenAcknowledgementLogIsUnresolvedRatherThanAcknowledged() {
        // Sequence 1 is missing: a lost record must never read as "still acknowledged".
        val history = listOf(FirstRunAcknowledgement.event(sequence = 2, occurredAtIsoDate = TODAY))
        val access = FirstRunAcknowledgement.resolve(history, TODAY)
        assertEquals(AiAccess.HISTORY_UNRESOLVED, access)
        assertNull(AiResponsePresenter.present(access, anyPlan))
    }

    @Test
    fun everyAiAccessStateIsEmittedByProductionCode() {
        val emitted = setOf(
            FirstRunAcknowledgement.resolve(emptyList(), TODAY),
            FirstRunAcknowledgement.resolve(
                listOf(FirstRunAcknowledgement.event(1, TODAY)),
                TODAY,
            ),
            FirstRunAcknowledgement.resolve(
                listOf(FirstRunAcknowledgement.event(2, TODAY)),
                TODAY,
            ),
        )
        assertEquals(AiAccess.entries.toSet(), emitted)
    }

    @Test
    fun theAcknowledgementReusesTheAppendOnlyConsentLog() {
        val event = FirstRunAcknowledgement.event(sequence = 1, occurredAtIsoDate = TODAY)
        assertEquals(ProcessingPurpose.SERVICE_DELIVERY, event.purpose)
        assertEquals(FirstRunAcknowledgement.POLICY_VERSION, event.policyVersion)
        assertTrue(event.evidenceRef.startsWith(ProductCopy.DISCLAIMER_SOURCE))
    }
}

class AiResponseNoticeTest {
    @Test
    fun everyPresentedAiResponseCarriesTheMandatoryNotice() {
        var presented = 0
        SafetyDecision.entries.forEach { decision ->
            ExplanationReasonKey.entries.forEach { key ->
                AdmittedExplanationTemplates.admittedLocales.forEach { locale ->
                    val plan = surfacePlan(decision, listOf(key), locale)
                    val view = assertNotNull(
                        AiResponsePresenter.present(AiAccess.ACKNOWLEDGED, plan),
                        "$decision/$key/$locale produced no AI response view",
                    )
                    assertEquals(ProductCopy.aiResponseNotice.forLocale(locale), view.notice)
                    assertTrue(view.notice.isNotBlank())
                    assertFalse(
                        view.bodyLines.contains(view.notice),
                        "The notice is a separate component, not a body line.",
                    )
                    assertTrue(view.bodyLines.isNotEmpty())
                    presented++
                }
            }
        }
        assertEquals(SafetyDecision.entries.size * ExplanationReasonKey.entries.size * 2, presented)
    }

    @Test
    fun theNoticeIsTheDisclaimerSourceOfTruthWording() {
        val notice = ProductCopy.aiResponseNotice
        assertTrue(notice.zhHant.contains("並非醫療建議"))
        assertTrue(notice.zhHant.contains("不提供診斷、治療或劑量建議"))
        assertTrue(notice.en.contains("are not medical advice"))
        assertTrue(notice.en.contains("does not diagnose"))
    }

    @Test
    fun everyAdmittedTemplateHasReviewedInformationCopy() {
        val missing = AdmittedExplanationTemplates.all - ProductCopy.templateCopy.keys
        assertEquals(
            emptySet(),
            missing,
            "An admitted template with no reviewed wording would reach a screen as a raw id.",
        )
    }

    @Test
    fun aTemplateWithoutReviewedCopyFailsClosed() {
        val plan = surfacePlan(SafetyDecision.LOG_ONLY, emptyList(), "zh-TW")
        val invented = plan.copy(templateIds = plan.templateIds + "tpl.decision.approved")
        assertNull(
            AiResponsePresenter.present(AiAccess.ACKNOWLEDGED, invented),
            "An unknown template id must fail closed instead of rendering.",
        )
    }

    @Test
    fun anAbsentPlanRendersNoResponseAtAll() {
        assertNull(AiResponsePresenter.present(AiAccess.ACKNOWLEDGED, null))
    }
}
