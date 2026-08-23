package com.choice.app.widget

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex

/**
 * Guards a widget flip action against rapid repeated taps (research.md §6): a second tap arriving
 * while a first tap's select+record is still in flight is dropped rather than queued or
 * double-recorded. Keyed by `GlanceId` for `SingleCoinWidget`, or `(GlanceId, coinId)` for
 * `QuickCoinsWidget` so different coins within the same instance can flip concurrently while the
 * same coin cannot double-fire.
 *
 * This is process-lifetime, in-memory-only state (data-model.md): a stuck "busy" flag can never
 * persist across process death, since there is nothing to persist.
 */
object WidgetFlipGuard {

    private val mutexes = ConcurrentHashMap<Any, Mutex>()

    /**
     * Runs [block] only if [key]'s mutex is immediately acquirable; otherwise returns without
     * running it (the tap is dropped).
     */
    suspend fun tryFlip(key: Any, block: suspend () -> Unit) {
        val mutex = mutexes.getOrPut(key) { Mutex() }
        if (mutex.tryLock()) {
            try {
                block()
            } finally {
                mutex.unlock()
            }
        }
    }
}
