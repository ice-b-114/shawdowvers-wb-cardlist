package com.svwb.cardlist.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.svwb.cardlist.data.Card
import com.svwb.cardlist.data.CardData
import com.svwb.cardlist.data.CardRepository
import com.svwb.cardlist.data.Lang
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class FormatFilter { ALL, ROTATION, UNLIMITED_ONLY }

data class Filters(
    val sets: Set<Int> = emptySet(),
    val classes: Set<Int> = emptySet(),
    val costs: Set<Int> = emptySet(),
    val types: Set<Int> = emptySet(),
    val rarities: Set<Int> = emptySet(),
    val format: FormatFilter = FormatFilter.ALL,
) {
    val activeCount: Int
        get() = listOf(sets, classes, costs, types, rarities).count { it.isNotEmpty() } +
            if (format != FormatFilter.ALL) 1 else 0

    fun matches(c: Card): Boolean {
        if (sets.isNotEmpty() && c.setId !in sets) return false
        if (classes.isNotEmpty() && c.clazz !in classes) return false
        if (costs.isNotEmpty() && costs.none { if (it >= 10) c.cost >= 10 else c.cost == it }) return false
        if (types.isNotEmpty() && c.typeGroup !in types) return false
        if (rarities.isNotEmpty() && c.rarity !in rarities) return false
        return when (format) {
            FormatFilter.ALL -> true
            FormatFilter.ROTATION -> c.inRotation
            FormatFilter.UNLIMITED_ONLY -> !c.inRotation
        }
    }
}

class CardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CardRepository(app)
    private val prefs = app.getSharedPreferences("settings", 0)

    var lang by mutableStateOf(Lang.from(prefs.getString("lang", null)))
        private set
    var data by mutableStateOf<CardData?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var message by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var filters by mutableStateOf(Filters())
    var detailStack by mutableStateOf<List<Long>>(emptyList())
        private set

    val strings: Strings get() = Strings(lang)

    private var job: Job? = null

    init {
        load(lang)
    }

    fun visibleCards(): List<Card> {
        val d = data ?: return emptyList()
        val q = query.trim()
        return d.cards.filter { c ->
            filters.matches(c) && (q.isEmpty() || c.name.contains(q, ignoreCase = true))
        }
    }

    fun switchLang(target: Lang) {
        if (target == lang) return
        lang = target
        prefs.edit().putString("lang", target.code).apply()
        load(target)
    }

    fun refresh() = load(lang)

    private fun load(target: Lang) {
        job?.cancel()
        job = viewModelScope.launch {
            val s = Strings(target)
            if (data?.lang != target) {
                loading = true
                data = repo.loadLocal(target)
                loading = data == null
            }
            val result = repo.refresh(target)
            if (target != lang) return@launch
            if (result != null) {
                data = result.data
                message = s.updatedFrom(result.source)
            } else if (data != null) {
                message = s.offlineCache
            }
            loading = false
        }
    }

    fun openDetail(id: Long) {
        if (data?.byId?.containsKey(id) == true) detailStack = detailStack + id
    }

    fun closeDetail() {
        if (detailStack.isNotEmpty()) detailStack = detailStack.dropLast(1)
    }
}
