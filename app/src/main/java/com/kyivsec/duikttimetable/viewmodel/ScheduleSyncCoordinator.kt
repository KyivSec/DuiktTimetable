package com.kyivsec.duikttimetable.viewmodel

import com.kyivsec.duikttimetable.data.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Owns the single refresh allowed to publish results to the current schedule screen. */
internal class ScheduleSyncCoordinator(private val scope: CoroutineScope) {
    private var job: Job? = null
    private var generation = 0L

    fun cancel() {
        generation++
        job?.cancel()
        job = null
    }

    fun start(
        onStart: () -> Unit,
        work: suspend () -> SyncResult,
        onResult: suspend (SyncResult) -> Unit,
        onFinish: () -> Unit,
    ) {
        cancel()
        val request = generation
        onStart()
        job = scope.launch {
            try {
                val result = work()
                currentCoroutineContext().ensureActive()
                if (request == generation) onResult(result)
            } finally {
                if (request == generation) onFinish()
            }
        }
    }
}
