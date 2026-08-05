package com.example.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Pure-JVM test (no Robolectric, no Firestore) — [TrialLocalStore] and
 * [ServerTrialStore] are injectable fakes, and the clock is a fake lambda.
 * [TrialManager.reconcile] and [TrialManager.initJob] return [kotlinx.coroutines.Job]
 * so tests `runBlocking { ... .join() }` instead of racing a dispatcher.
 */
class TrialManagerTest {

    private var fakeClock: Long = 0L

    private class InMemoryTrialStore : TrialLocalStore {
        var stored: Long? = null
        override suspend fun readStartMillis(): Long? = stored
        override suspend fun writeStartMillis(millis: Long) { stored = millis }
    }

    private fun createManager(
        localStore: TrialLocalStore = InMemoryTrialStore(),
        clock: Long = 1_000_000_000_000L,
        serverStore: ServerTrialStore = ServerTrialStore { _, _ -> null },
    ): TrialManager {
        fakeClock = clock
        return TrialManager(
            externalScope = CoroutineScope(Dispatchers.Unconfined),
            trialLocalStore = localStore,
            clockMillis = { fakeClock },
            serverTrialStore = serverStore,
        )
    }

    @Test
    fun `trial not active when not started`() = runBlocking {
        val tm = createManager()
        tm.initJob.join()
        assertFalse(tm.isTrialActive())
        assertFalse(tm.trialExpired())
        assertNull(tm.trialStartedAt.value)
    }

    @Test
    fun `trial active within window`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        assertTrue(tm.isTrialActive())
        assertEquals(BillingConfig.TRIAL_DAYS.toInt(), tm.trialDaysRemaining())
        assertFalse(tm.trialExpired())
    }

    @Test
    fun `trial active after 1 day`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        fakeClock += TimeUnit.DAYS.toMillis(1)
        assertTrue(tm.isTrialActive())
        assertEquals(2, tm.trialDaysRemaining())
        assertFalse(tm.trialExpired())
    }

    @Test
    fun `trial active after 2 days`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        fakeClock += TimeUnit.DAYS.toMillis(2)
        assertTrue(tm.isTrialActive())
        assertEquals(1, tm.trialDaysRemaining())
        assertFalse(tm.trialExpired())
    }

    @Test
    fun `trial expires on last day boundary`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        fakeClock += TimeUnit.DAYS.toMillis(3) - 1
        assertTrue(tm.isTrialActive())
        assertEquals(1, tm.trialDaysRemaining())
        assertFalse(tm.trialExpired())
    }

    @Test
    fun `trial expired after window`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        fakeClock += TimeUnit.DAYS.toMillis(3)
        assertFalse(tm.isTrialActive())
        assertEquals(0, tm.trialDaysRemaining())
        assertTrue(tm.trialExpired())
    }

    @Test
    fun `trial expired well after window`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        fakeClock += TimeUnit.DAYS.toMillis(10)
        assertFalse(tm.isTrialActive())
        assertEquals(0, tm.trialDaysRemaining())
        assertTrue(tm.trialExpired())
    }

    @Test
    fun `reconcileLocalOnly sets start on first call`() = runBlocking {
        val tm = createManager()
        tm.initJob.join()
        assertNull(tm.trialStartedAt.value)
        tm.reconcile("guest", isAnonymous = true).join()
        assertNotNull(tm.trialStartedAt.value)
        assertEquals(1_000_000_000_000L, tm.trialStartedAt.value)
    }

    @Test
    fun `reconcileLocalOnly does not reset existing start`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        val firstStart = tm.trialStartedAt.value
        assertNotNull(firstStart)

        fakeClock = 2_000_000_000_000L
        tm.reconcile("guest", isAnonymous = true).join()
        assertEquals(firstStart, tm.trialStartedAt.value)
    }

    @Test
    fun `reconcile with real user calls server store`() = runBlocking {
        var serverCalled = false
        val serverStore = ServerTrialStore { uid, local ->
            serverCalled = true
            assertEquals("real-uid", uid)
            assertNotNull(local)
            local
        }
        val tm = createManager(serverStore = serverStore)
        tm.reconcile("real-uid", isAnonymous = false).join()
        assertTrue(serverCalled)
    }

    @Test
    fun `reconcile with anonymous user uses local only`() = runBlocking {
        var serverCalled = false
        val serverStore = ServerTrialStore { _, _ ->
            serverCalled = true
            throw IllegalStateException("should not reach server")
        }
        val tm = createManager(serverStore = serverStore)
        tm.reconcile("anon-uid", isAnonymous = true).join()
        assertFalse(serverCalled)
        assertNotNull(tm.trialStartedAt.value)
    }

    @Test
    fun `reconcile with null uid uses local only`() = runBlocking {
        var serverCalled = false
        val serverStore = ServerTrialStore { _, _ ->
            serverCalled = true
            null
        }
        val tm = createManager(serverStore = serverStore)
        tm.reconcile(null, isAnonymous = false).join()
        assertFalse(serverCalled)
        assertNotNull(tm.trialStartedAt.value)
    }

    @Test
    fun `reconcile with blank uid uses local only`() = runBlocking {
        var serverCalled = false
        val serverStore = ServerTrialStore { _, _ ->
            serverCalled = true
            null
        }
        val tm = createManager(serverStore = serverStore)
        tm.reconcile("", isAnonymous = false).join()
        assertFalse(serverCalled)
    }

    @Test
    fun `server store value takes precedence over local`() = runBlocking {
        val serverStart = 9_000_000_000_000L
        val serverStore = ServerTrialStore { _, _ -> serverStart }
        val tm = createManager(serverStore = serverStore)
        tm.reconcile("real-uid", isAnonymous = false).join()
        assertEquals(serverStart, tm.trialStartedAt.value)
    }

    @Test
    fun `server store returning null falls back to local`() = runBlocking {
        val serverStore = ServerTrialStore { _, _ -> null }
        val tm = createManager(clock = 1_000_000_000_000L, serverStore = serverStore)
        tm.reconcile("real-uid", isAnonymous = false).join()
        assertNotNull(tm.trialStartedAt.value)
        assertEquals(1_000_000_000_000L, tm.trialStartedAt.value)
    }

    @Test
    fun `trial remains persistent across reconcile calls`() = runBlocking {
        val tm = createManager()
        tm.reconcile("guest", isAnonymous = true).join()
        val start = tm.trialStartedAt.value

        fakeClock += TimeUnit.HOURS.toMillis(6)
        tm.reconcile("guest", isAnonymous = true).join()
        assertEquals(start, tm.trialStartedAt.value)
        assertTrue(tm.isTrialActive())
    }

    @Test
    fun `init loads persisted value from local store`() = runBlocking {
        val store = InMemoryTrialStore()
        store.writeStartMillis(9_000_000_000_000L)
        val tm = createManager(localStore = store, clock = 9_000_000_000_000L)
        tm.initJob.join()
        assertNotNull(tm.trialStartedAt.value)
        assertEquals(9_000_000_000_000L, tm.trialStartedAt.value)
    }
}
