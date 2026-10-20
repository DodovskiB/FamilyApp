package com.example.familyapp.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountStorageTest {
    @Test fun databaseNamesAreStableAndSeparate() {
        val first = AccountStorage.databaseNameFor("12345678-1234-1234-1234-123456789abc")
        assertEquals(first, AccountStorage.databaseNameFor("12345678-1234-1234-1234-123456789ABC"))
        assertNotEquals(first, AccountStorage.databaseNameFor("12345678-1234-1234-1234-123456789abd"))
        assertTrue(first.matches(Regex("familyapp_account_[a-f0-9]{64}\\.db")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathsAsAccountIds() {
        AccountStorage.databaseNameFor("../../familyapp.db")
    }
}
