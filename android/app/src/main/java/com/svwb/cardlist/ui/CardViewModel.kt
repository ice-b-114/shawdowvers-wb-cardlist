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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        if (costs.isNotEmpty() && costs.none { if (it >= MAX_COST_CHIP) c.cost >= MAX_COST_CHIP else c.cost == it }) return false
        if (types.isNotEmpty() && c.typeGroup !in types) return false
        if (rarities.isNotEmpty() && c.rarity !in rarities) return false
        return when (format) {
            FormatFilter.ALL -> true
            FormatFilter.ROTATION -> c.inRotation
            FormatFilter.UNLIMITED_ONLY -> !c.inRotation
        }
    }

    companion object {
        /** 费用筛选的最大档位，7 表示 7 及以上 */
        const val MAX_COST_CHIP = 7
    }
}

class CardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CardRepository(app)
    private val prefs = app.getSharedPreferences("settings", 0)

    /** 两种语言的卡表同时驻留内存，切换语言只切换引用，不联网 */
    private var store by mutableStateOf<Map<Lang, CardData>>(emptyMap())

    var lang by mutableStateOf(Lang.from(prefs.getString("lang", null)))
        private set
    var loading by mutableStateOf(true)
        private set
    var message by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var filters by mutableStateOf(Filters())
    var detailStack by mutableStateOf<List<Long>>(emptyList())
        private set

    val data: CardData? get() = store[lang]
    val strings: Strings get() = Strings(lang)

    private var job: Job? = null

    init {
        loadAll()
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
    }

    /** 仅在应用启动和点击刷新按钮时调用 */
    fun refresh() = loadAll()

    private fun loadAll() {
        job?.cancel()
        job = viewModelScope.launch {
            if (data == null) loading = true
            for (l in Lang.entries) {
                if (store[l] == null) {
                    repo.loadLocal(l)?.let { store = store + (l to it) }
                }
            }
            if (data != null) loading = false

            val results = Lang.entries.map { l -> async { l to repo.refresh(l) } }.awaitAll()
            var source: String? = null
            for ((l, r) in results) {
                if (r != null) {
                    store = store + (l to r.data)
                    source = r.source
                }
            }
            val s = strings
            message = when {
                source != null -> s.updatedFrom(source)
                data != null -> s.offlineCache
                else -> null
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
