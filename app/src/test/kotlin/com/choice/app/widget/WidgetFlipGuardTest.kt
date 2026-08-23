package com.choice.app.widget

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [WidgetFlipGuard]'s single-flight guarding (T011, research.md §6): two concurrent
 * [WidgetFlipGuard.tryFlip] calls on the same key must result in exactly one block running to
 * completion — the tap arriving while the first is still in flight is dropped, not queued.
 */
class WidgetFlipGuardTest {

    @Test
    fun concurrentTryFlip_sameKey_onlyOneRunsToCompletion() = runTest {
        val key = Any()
        var completedCount = 0
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val firstJob = launch {
            WidgetFlipGuard.tryFlip(key) {
                firstStarted.complete(Unit)
                releaseFirst.await()
                completedCount++
            }
        }

        firstStarted.await()
        // A second tap arrives while the first tap's block is still in flight.
        WidgetFlipGuard.tryFlip(key) {
            completedCount++
        }

        releaseFirst.complete(Unit)
        firstJob.join()

        assertEquals(1, completedCount)
    }

    @Test
    fun tryFlip_sequentialCallsOnSameKey_bothRun() = runTest {
        val key = Any()
        var completedCount = 0

        WidgetFlipGuard.tryFlip(key) { completedCount++ }
        WidgetFlipGuard.tryFlip(key) { completedCount++ }

        assertEquals(2, completedCount)
    }

    @Test
    fun tryFlip_differentKeys_bothRunEvenWhileOneIsInFlight() = runTest {
        val keyA = Any()
        val keyB = Any()
        var completedCount = 0
        val aStarted = CompletableDeferred<Unit>()
        val releaseA = CompletableDeferred<Unit>()

        val jobA = launch {
            WidgetFlipGuard.tryFlip(keyA) {
                aStarted.complete(Unit)
                releaseA.await()
                completedCount++
            }
        }

        aStarted.await()
        WidgetFlipGuard.tryFlip(keyB) { completedCount++ }
        releaseA.complete(Unit)
        jobA.join()

        assertEquals(2, completedCount)
    }
}
