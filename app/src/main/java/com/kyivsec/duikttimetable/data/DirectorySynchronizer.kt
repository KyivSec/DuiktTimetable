package com.kyivsec.duikttimetable.data

import androidx.room.withTransaction
import com.kyivsec.duikttimetable.data.local.DirectorySyncEntity
import com.kyivsec.duikttimetable.data.local.TimetableDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant

internal class DirectorySynchronizer(
    private val database: TimetableDatabase,
    private val clock: Clock,
) {
    private val dao = database.timetableDao()

    suspend fun <T> sync(
        branch: String,
        force: Boolean,
        hasExistingRows: suspend () -> Boolean,
        fetch: suspend () -> List<T>,
        replace: suspend (List<T>, Long) -> Unit,
    ): SyncResult = database.directorySyncMutex.withLock {
        var hasCache = false
        try {
            val fetchedAt = dao.directoryFetchedAt(branch)
            hasCache = fetchedAt != null || hasExistingRows()
            if (!force && fetchedAt != null &&
                fetchedAt >= clock.instant().minus(Duration.ofDays(7)).toEpochMilli()
            ) return@withLock SyncResult.Success(Instant.ofEpochMilli(fetchedAt), 0)
            val values = fetch()
            val completed = clock.instant()
            database.withTransaction {
                replace(values, completed.toEpochMilli())
                dao.insertDirectorySync(DirectorySyncEntity(branch, completed.toEpochMilli()))
            }
            SyncResult.Success(completed, values.size)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            SyncResult.Failure(error.toDataError(), hasCache)
        }
    }
}
