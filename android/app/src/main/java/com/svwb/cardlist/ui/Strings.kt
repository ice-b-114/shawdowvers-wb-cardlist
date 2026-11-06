package com.svwb.cardlist.ui

import com.svwb.cardlist.data.Lang

/** 界面文案与游戏术语，随简/繁切换 */
class Strings(val lang: Lang) {
    private fun t(chs: String, cht: String) = if (lang == Lang.CHS) chs else cht

    val appName = t("影之诗查卡器", "闇影詩章查卡器")
    val searchHint = t("搜索卡名", "搜尋卡名")
    val filter = t("筛选", "篩選")
    val reset = t("重置", "重置")
    val done = t("完成", "完成")
    val clear = t("清除", "清除")
    val refresh = t("刷新", "重新整理")
    val back = t("返回", "返回")
    val noResult = t("没有符合条件的卡牌", "沒有符合條件的卡片")
    val loading = t("正在加载卡表…", "正在載入卡表…")
    val loadFailed = t("卡表加载失败，请检查网络后刷新", "卡表載入失敗，請檢查網路後重新整理")
    fun countText(n: Int) = t("共 $n 张", "共 $n 張")
    fun updatedFrom(source: String) = t("卡表已更新（来源：$source）", "卡表已更新（來源：$source）")
    val offlineCache = t("无法连接服务器，使用本地卡表", "無法連線伺服器，使用本機卡表")

    val secSet = t("卡包", "卡包")
    val secClass = t("职业", "職業")
    val secCost = t("费用", "費用")
    val secType = t("类型", "類型")
    val secRarity = t("稀有度", "稀有度")
    val secFormat = t("使用范围", "使用範圍")

    val formatAll = t("不限", "不限")
    val formatRotation = t("指定系列", "指定系列")
    val formatUnlimited = t("仅无限制", "僅無限制")
    val formatBoth = t("指定系列、无限制", "指定系列、無限制")

    fun className(c: Int) = if (lang == Lang.CHS) {
        listOf("中立", "精灵", "皇家护卫", "巫师", "龙族", "梦魇", "主教", "复仇者")
    } else {
        listOf("中立", "精靈", "皇家護衛", "巫師", "龍族", "夢魘", "主教", "復仇者")
    }.getOrElse(c) { "?" }

    fun typeName(group: Int) = when (group) {
        1 -> t("随从", "從者")
        2 -> t("护符", "護符")
        4 -> t("法术", "法術")
        else -> "?"
    }

    fun rarityName(r: Int) = when (r) {
        1 -> t("铜卡", "青銅卡")
        2 -> t("银卡", "白銀卡")
        3 -> t("金卡", "黃金卡")
        4 -> t("虹卡", "傳說卡")
        else -> "?"
    }

    fun costName(c: Int) = if (c >= 10) "10+" else c.toString()

    val detailCost = t("费用", "費用")
    val detailAtk = t("攻击力", "攻擊力")
    val detailLife = t("生命值", "生命值")
    val detailSet = t("卡包", "卡包")
    val detailTribe = t("种族", "種族")
    val detailSkill = t("卡牌效果", "卡片效果")
    val detailEvo = t("进化后", "進化後")
    val detailFlavour = t("背景故事", "背景故事")
    val detailRelated = t("相关卡牌（含衍生卡）", "相關卡片（含衍生卡）")
    val detailSpecific = t("特殊效果", "特殊效果")
    val detailFormat = t("使用范围", "使用範圍")
    val detailCv = "CV"
    val detailIllustrator = t("插画", "插畫")
    val tokenTag = t("衍生卡", "衍生卡")
    val showEvo = t("查看进化图", "查看進化圖")
    val showBase = t("查看原图", "查看原圖")
}
