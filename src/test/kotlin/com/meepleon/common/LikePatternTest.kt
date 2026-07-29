package com.meepleon.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LikePatternTest {

    @Test
    fun `wraps plain text as a contains pattern`() {
        assertEquals("%catan%", toContainsLikePattern("catan"))
    }

    @Test
    fun `escapes percent and underscore so they are treated literally`() {
        assertEquals("%50\\%%", toContainsLikePattern("50%"))
        assertEquals("%a\\_b%", toContainsLikePattern("a_b"))
    }

    @Test
    fun `escapes the escape character itself first`() {
        assertEquals("%a\\\\b%", toContainsLikePattern("a\\b"))
    }
}
