package com.svwb.cardlist.data

import org.json.JSONObject

enum class Lang(val code: String, val label: String) {
    CHS("chs", "简"),
    CHT("cht", "繁");

    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code } ?: CHS
    }
}

data class Evo(
    val skillText: String,
    val flavourText: String,
    val imageHash: String,
)

data class SpecificEffect(
    val id: Long,
    val cost: Int,
    val skillText: String,
)

data class Card(
    val id: Long,
    val name: String,
    val cost: Int,
    val atk: Int,
    val life: Int,
    val clazz: Int,
    val type: Int,
    val rarity: Int,
    val setId: Int,
    val tribes: List<Int>,
    val skillText: String,
    val flavourText: String,
    val cv: String,
    val illustrator: String,
    val imageHash: String,
    val isToken: Boolean,
    val inRotation: Boolean,
    val evo: Evo?,
    val related: List<Long>,
    val effects: List<SpecificEffect>,
) {
    /** 1 = 随从, 2 = 护符（含 type 3）, 4 = 法术 */
    val typeGroup: Int get() = if (type == 3) 2 else type
}

class CardData(
    val lang: Lang,
    val byId: Map<Long, Card>,
    /** 官方顺序，已剔除 token */
    val cards: List<Card>,
    val setNames: Map<Int, String>,
    val tribeNames: Map<Int, String>,
) {
    /** 卡包按新到旧排列，仅包含最外层出现过的卡包 */
    val setOrder: List<Int> = cards.map { it.setId }.distinct().sortedDescending()

    companion object {
        fun parse(lang: Lang, json: String): CardData {
            val root = JSONObject(json)
            val details = root.getJSONObject("card_details")
            val relatedObj = root.optJSONObject("cards")
            val effectObj = root.optJSONObject("specific_effect_card_info")

            val byId = HashMap<Long, Card>(details.length() * 2)
            val keys = details.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = details.getJSONObject(key)
                val c = entry.getJSONObject("common")
                val id = c.getLong("card_id")

                val evoObj = entry.optJSONObject("evo")
                val evo = evoObj?.let {
                    Evo(
                        skillText = it.optString("skill_text"),
                        flavourText = it.optString("flavour_text"),
                        imageHash = it.optString("card_image_hash"),
                    )
                }

                val rel = relatedObj?.optJSONObject(key)
                val related = rel?.optJSONArray("related_card_ids").toLongList().filter { it != id }
                val effects = rel?.optJSONArray("specific_effect_card_ids").toLongList().mapNotNull { eid ->
                    effectObj?.optJSONObject(eid.toString())?.let {
                        SpecificEffect(eid, it.optInt("cost"), it.optString("skill_text"))
                    }
                }

                val tribesArr = c.optJSONArray("tribes")
                val tribes = buildList {
                    if (tribesArr != null) for (i in 0 until tribesArr.length()) add(tribesArr.getInt(i))
                }.filter { it != 0 }

                byId[id] = Card(
                    id = id,
                    name = c.optString("name"),
                    cost = c.optInt("cost"),
                    atk = c.optInt("atk"),
                    life = c.optInt("life"),
                    clazz = c.optInt("class"),
                    type = c.optInt("type"),
                    rarity = c.optInt("rarity"),
                    setId = c.optInt("card_set_id"),
                    tribes = tribes,
                    skillText = c.optString("skill_text"),
                    flavourText = c.optString("flavour_text"),
                    cv = c.optString("cv"),
                    illustrator = c.optString("illustrator"),
                    imageHash = c.optString("card_image_hash"),
                    isToken = c.optBoolean("is_token"),
                    inRotation = c.optBoolean("is_include_rotation"),
                    evo = evo,
                    related = related,
                    effects = effects,
                )
            }

            val order = root.getJSONArray("sort_card_id_list").toLongList()
            val cards = order.mapNotNull { byId[it] }.filter { !it.isToken }

            return CardData(
                lang = lang,
                byId = byId,
                cards = cards,
                setNames = root.optJSONObject("card_set_names").toIntStringMap(),
                tribeNames = root.optJSONObject("tribe_names").toIntStringMap(),
            )
        }

        private fun org.json.JSONArray?.toLongList(): List<Long> {
            if (this == null) return emptyList()
            return List(length()) { getLong(it) }
        }

        private fun JSONObject?.toIntStringMap(): Map<Int, String> {
            if (this == null) return emptyMap()
            val map = LinkedHashMap<Int, String>()
            val it = keys()
            while (it.hasNext()) {
                val k = it.next()
                k.toIntOrNull()?.let { id -> map[id] = getString(k) }
            }
            return map
        }
    }
}
