package ru.mpavel.osmmap.feature.provider

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * "Пропустить" входящий запрос (В-Э5-3): только на этом устройстве, на подбор не влияет и в БД не хранится.
 * Очищается при выходе агента.
 */
class SkippedRequests(context: Context) {

    private val prefs = context.getSharedPreferences("provider_skipped", Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(read())
    val ids: StateFlow<Set<Long>> = _ids

    fun skip(dealId: Long) = save(_ids.value + dealId)

    fun clear() = save(emptySet())

    private fun read(): Set<Long> = prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    private fun save(ids: Set<Long>) {
        _ids.value = ids
        prefs.edit().putStringSet(KEY, ids.map { it.toString() }.toSet()).apply()
    }

    private companion object {
        const val KEY = "deal_ids"
    }
}
