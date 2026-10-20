package com.nota.notes.data

import org.junit.Assert.*
import org.junit.Test

// P3-B2: اختبارات منطق الحذف النقي (JVM بلا جهاز).
class AccountDeletionTest {

    @Test
    fun `no request means no pending and no due`() {
        assertEquals(0, AccountDeletion.remainingDays(0L))
        assertFalse(AccountDeletion.isDue(0L))
        assertFalse(AccountDeletion.isPending(1000L))
    }

    @Test
    fun `fresh request gives 14 days`() {
        val now = 1_000_000_000L
        assertEquals(14, AccountDeletion.remainingDays(now, now))
        assertTrue(AccountDeletion.isPending(now, now))
        assertFalse(AccountDeletion.isDue(now, now))
    }

    @Test
    fun `due after 14 days`() {
        val req = 1_000_000_000L
        val after = req + AccountDeletion.GRACE_MS
        assertTrue(AccountDeletion.isDue(req, after))
        assertFalse(AccountDeletion.isPending(req, after))
        assertEquals(0, AccountDeletion.remainingDays(req, after))
    }

    @Test
    fun `one day left near the end`() {
        val req = 1_000_000_000L
        val almost = req + AccountDeletion.GRACE_MS - 1000L
        assertEquals(1, AccountDeletion.remainingDays(req, almost))
        assertTrue(AccountDeletion.isPending(req, almost))
    }

    @Test
    fun `confirm phrase must match exactly`() {
        assertTrue(AccountDeletion.isConfirmValid("احذف حسابي"))
        assertTrue(AccountDeletion.isConfirmValid("  احذف حسابي  "))
        assertFalse(AccountDeletion.isConfirmValid("احذف حسابى")) // ى بدل ي
        assertFalse(AccountDeletion.isConfirmValid("احذف"))
        assertFalse(AccountDeletion.isConfirmValid(""))
    }
}
