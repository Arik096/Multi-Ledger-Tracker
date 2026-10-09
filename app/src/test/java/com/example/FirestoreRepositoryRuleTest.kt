package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.model.FirestoreLedgerBook
import com.example.data.firestore.model.FirestoreTransactionRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticated_write_fails() = runBlocking {
        auth.signOut()
        val repo = FirestoreRepository(firestore, auth)
        try {
            repo.saveBook("unauth_user", FirestoreLedgerBook(id = "b1", userId = "unauth_user", name = "Test"))
            fail("Expected exception when unauthenticated")
        } catch (expected: Exception) {
            assertTrue(expected is IllegalStateException || expected.message?.contains("authenticated") == true)
        }
    }

    @Test
    fun authenticated_user_can_save_and_read_data() = runBlocking {
        val aliceUid = signInTestUser("alice@test.com")
        val repo = FirestoreRepository(firestore, auth)

        // 1. Save user profile
        repo.saveUserProfile(aliceUid, "alice@test.com", "Alice Tester")

        // 2. Save a book
        val book = FirestoreLedgerBook(
            id = "book_101",
            userId = aliceUid,
            name = "Personal Finances",
            currencySymbol = "৳",
            currencyCode = "BDT"
        )
        repo.saveBook(aliceUid, book)

        // 3. Read book back
        val books = repo.getBooksDirect(aliceUid)
        assertEquals(1, books.size)
        assertEquals("Personal Finances", books[0].name)

        // 4. Save a transaction
        val tx = FirestoreTransactionRecord(
            id = "tx_101",
            userId = aliceUid,
            bookId = "book_101",
            type = "IN",
            amount = 5000.0,
            category = "Salary",
            timestamp = System.currentTimeMillis(),
            memo = "First paycheck"
        )
        repo.saveTransaction(aliceUid, tx)

        // 5. Read transaction back
        val transactions = repo.getTransactionsDirect(aliceUid)
        assertEquals(1, transactions.size)
        assertEquals(5000.0, transactions[0].amount, 0.001)
    }

    @Test
    fun cross_user_access_is_prevented() = runBlocking {
        // Alice creates data
        val aliceUid = signInTestUser("alice2@test.com")
        val aliceRepo = FirestoreRepository(firestore, auth)
        val book = FirestoreLedgerBook(
            id = "alice_secret_book",
            userId = aliceUid,
            name = "Alice Confidential"
        )
        aliceRepo.saveBook(aliceUid, book)

        // Bob signs in and tries to read or write to Alice's collection
        val bobUid = signInTestUser("bob@test.com")
        val bobRepo = FirestoreRepository(firestore, auth)

        // Bob trying to read Alice's books directly via Firestore
        try {
            val books = bobRepo.getBooksDirect(aliceUid)
            // Either returns empty (handled error) or fails
            assertTrue(books.isEmpty())
        } catch (expected: Exception) {
            // Expected security rules failure
        }
    }
}
