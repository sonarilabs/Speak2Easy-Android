package com.sonari.speak2easy.data.remote.dto

import com.sonari.speak2easy.data.remote.SonariJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionDtosTest {
    @Test
    fun promoValidateResponse_decodesSnakeCaseOfferId() {
        val response = SonariJson.decodeFromString<PromoValidateResponse>(
            """{"valid":true,"offer_id":"free-trial-1month"}""",
        )

        assertTrue(response.valid)
        assertEquals("free-trial-1month", response.offerId)
    }
}
