package com.sonari.speak2easy.data.remote.dto

import com.sonari.speak2easy.data.remote.SonariJson
import com.sonari.speak2easy.ui.progress.FeedbackReportReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeDtosTest {
    @Test
    fun attemptResponse_decodesPersonalizedFeedbackAndDecimalScore() {
        val response = SonariJson.decodeFromString<PracticeAttemptResponse>(
            """
            {"success":true,"analysis":{"transcription":"い","confidence":0.9,"is_correct":false,
             "match_score":40.5,"feedback_message":"You're very close! Your 'ri' sounded like 'i'.",
             "diagnostic_reason":"generic_mismatch",
             "candidates":[{"text":"い","confidence":0.9,"score":80}],
             "feedback":{"message_en":"You're very close! Your 'ri' sounded like 'i'.","reason":"generic_mismatch",
               "version":2,"confidence_level":"high","headline":"You're very close!",
               "did_well":"Your vowel sound was right.",
               "issue":{"category":"consonant","expected_mora":"り","heard_mora":"い","mora_index":1,
                 "mora_count":1,"position":"start","text":"Your 'ri' sounded like 'i'."},
               "next_step":"Japanese r is a quick, light tap."}}}
            """.trimIndent(),
        )

        assertEquals("You're very close! Your 'ri' sounded like 'i'.", response.feedback)
        assertEquals(40.5, response.analysis.matchScore!!, 0.0)
        val feedback = response.analysis.feedback!!
        assertEquals("high", feedback.confidenceLevel)
        assertEquals("Your vowel sound was right.", feedback.didWell)
        assertEquals("り", feedback.issue?.expectedMora)
        assertEquals(1, feedback.issue?.moraIndex)
        assertEquals("Japanese r is a quick, light tap.", feedback.nextStep)
    }

    @Test
    fun sessionAttempts_decodeRatingsAndSessionFeedback() {
        val response = SonariJson.decodeFromString<SessionAttemptsResponse>(
            """
            {"attempts":[
              {"attempt_id":"a1","content_id":"c1","content_text":"り","romanization":"ri","is_correct":false,
               "match_score":40,"whisper_confidence":null,"feedback":"Your 'ri' sounded like 'i'.",
               "feedback_details":{"version":2,"headline":"You're very close!","confidence_level":"high"},
               "confidence_level":"high",
               "rating":{"is_helpful":false,"reason":"wrong_sound","comment":"I said ri"},
               "attempted_at":"2026-09-29T04:41:26.479Z"},
              {"attempt_id":"a2","content_id":"c2","is_correct":true,"feedback":"","feedback_details":null,"rating":null}
             ],
             "skipped":[],
             "session_feedback":{"comment":"Felt accurate","updated_at":"2026-09-29T04:41:51.696Z"}}
            """.trimIndent(),
        )

        assertEquals(FeedbackRating(isHelpful = false, reason = "wrong_sound", comment = "I said ri"), response.attempts[0].rating)
        assertEquals("You're very close!", response.attempts[0].feedbackDetails?.headline)
        assertEquals("Your 'ri' sounded like 'i'.", response.attempts[0].feedbackText)
        assertNull(response.attempts[1].rating)
        assertNull(response.attempts[1].feedbackText)
        assertEquals("Felt accurate", response.sessionFeedback?.comment)
    }

    @Test
    fun sessionAttempts_decodeWithoutNewFields() {
        val response = SonariJson.decodeFromString<SessionAttemptsResponse>(
            """{"attempts":[{"attempt_id":"a1","feedback":"Try again."}],"skipped":[]}""",
        )

        assertNull(response.attempts[0].rating)
        assertNull(response.sessionFeedback)
    }

    @Test
    fun ratingRequest_encodesSnakeCaseAndOmitsNulls() {
        val body = SonariJson.encodeToString(AttemptRatingRequest.serializer(), AttemptRatingRequest(isHelpful = true, platform = "android", appVersion = "1.0.11"))

        assertEquals("""{"is_helpful":true,"platform":"android","app_version":"1.0.11"}""", body)
    }

    @Test
    fun ratingResponse_decodes() {
        val response = SonariJson.decodeFromString<AttemptRatingResponse>(
            """{"success":true,"rating":{"is_helpful":false,"reason":"too_vague","comment":null}}""",
        )

        assertFalse(response.rating.isHelpful)
        assertEquals("too_vague", response.rating.reason)
    }

    @Test
    fun reportReasons_matchBackendValues() {
        assertEquals(
            listOf(
                "didnt_match_pronunciation", "wrong_sound", "pronunciation_was_correct",
                "correction_incorrect", "too_vague", "other",
            ),
            FeedbackReportReason.entries.map { it.apiValue },
        )
    }
}
