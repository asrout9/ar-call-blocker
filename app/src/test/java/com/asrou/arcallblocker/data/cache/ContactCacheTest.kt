package com.asrou.arcallblocker.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactCacheTest {

    private lateinit var cache: ContactCache

    @Before
    fun setUp() {
        cache = ContactCache()
    }

    @Test
    fun `initial state is UNINITIALIZED`() {
        assertEquals(ContactCache.State.UNINITIALIZED, cache.state)
        assertEquals(0, cache.size)
    }

    @Test
    fun `rebuild populates cache and sets state to READY`() {
        val numbers = setOf("+919876543210", "+14155552671", "09876543210")
        cache.rebuild(numbers)

        assertEquals(ContactCache.State.READY, cache.state)
        assertEquals(3, cache.size)
        assertTrue(cache.contains("+919876543210"))
        assertTrue(cache.contains("+14155552671"))
        assertTrue(cache.contains("09876543210"))
        assertFalse(cache.contains("+919999999999"))
    }

    @Test
    fun `rebuild completely replaces previous items`() {
        cache.rebuild(setOf("+919876543210"))
        assertEquals(1, cache.size)

        cache.rebuild(setOf("+14155552671"))
        assertEquals(1, cache.size)
        assertFalse(cache.contains("+919876543210"))
        assertTrue(cache.contains("+14155552671"))
    }

    @Test
    fun `clear resets cache to UNINITIALIZED`() {
        cache.rebuild(setOf("+919876543210"))
        cache.clear()

        assertEquals(ContactCache.State.UNINITIALIZED, cache.state)
        assertEquals(0, cache.size)
        assertFalse(cache.contains("+919876543210"))
    }
}
