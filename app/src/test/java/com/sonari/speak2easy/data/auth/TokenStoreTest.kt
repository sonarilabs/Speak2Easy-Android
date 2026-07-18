package com.sonari.speak2easy.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenStoreTest {
    @Test
    fun normalizeAccessToken_stripsBearerPrefixAndWhitespace() {
        assertEquals(
            "header.payload.signature",
            normalizeAccessToken("  Bearer header.payload.signature  "),
        )
    }

    @Test
    fun normalizeAccessToken_acceptsAlreadyNormalizedToken() {
        assertEquals(
            "header.payload.signature",
            normalizeAccessToken("header.payload.signature"),
        )
    }

    @Test
    fun normalizeAccessToken_rejectsMalformedValues() {
        assertNull(normalizeAccessToken(null))
        assertNull(normalizeAccessToken(""))
        assertNull(normalizeAccessToken("Bearer"))
        assertNull(normalizeAccessToken("Bearer not-a-jwt"))
        assertNull(normalizeAccessToken("one.two"))
        assertNull(normalizeAccessToken("one.two.three.four"))
        assertNull(normalizeAccessToken("one..three"))
    }
}
