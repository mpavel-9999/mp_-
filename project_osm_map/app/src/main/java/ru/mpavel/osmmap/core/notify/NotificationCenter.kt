package ru.mpavel.osmmap.core.notify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.data.Notification
import ru.mpavel.osmmap.data.NotificationRepository
import java.util.concurrent.atomic.AtomicInteger

/**
 * Уведомления in_app (ТЗ 4.7): один опрос GET /notifications?since= на всё приложение -
 * раз в 2 с, пока открыт экран сделки ([fast]), иначе раз в 5 с.
 * При входе старые уведомления не показываются: первый опрос только запоминает последний номер.
 */
class NotificationCenter(private val repo: NotificationRepository) {

    private val _events = MutableSharedFlow<Notification>(extraBufferCapacity = 64)

    /** Новые уведомления после входа. */
    val events: SharedFlow<Notification> = _events

    private val fastHolders = AtomicInteger(0)
    private var job: Job? = null

    /** Экран сделки открыт / закрыт: частый опрос, пока открыт хотя бы один. */
    fun fast(on: Boolean) {
        if (on) fastHolders.incrementAndGet() else fastHolders.updateAndGet { (it - 1).coerceAtLeast(0) }
    }

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            var lastId: Long? = null
            while (isActive) {
                try {
                    lastId = if (lastId == null) baseline() else poll(lastId)
                } catch (_: AppError) {
                    // нет связи - повторим на следующем шаге; ошибку покажет экран, которому нужны данные
                }
                delay(if (fastHolders.get() > 0) FAST_MS else SLOW_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun markRead(notificationId: Long) {
        try {
            repo.read(notificationId)
        } catch (_: AppError) {
            // не критично: уведомление уже показано
        }
    }

    /** Последний номер на момент входа; сервер отдаёт до [PAGE] за раз - листаем до конца. */
    private suspend fun baseline(): Long {
        var last = 0L
        while (true) {
            val page = repo.since(last)
            if (page.isEmpty()) return last
            last = page.maxOf { it.notificationId }
            if (page.size < PAGE) return last
        }
    }

    private suspend fun poll(lastId: Long): Long {
        val fresh = repo.since(lastId)
        fresh.forEach { _events.emit(it) }
        return fresh.maxOfOrNull { it.notificationId } ?: lastId
    }

    private companion object {
        const val FAST_MS = 2_000L
        const val SLOW_MS = 5_000L
        const val PAGE = 200
    }
}
