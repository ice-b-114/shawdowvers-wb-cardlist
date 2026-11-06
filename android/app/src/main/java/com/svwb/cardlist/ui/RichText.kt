package com.svwb.cardlist.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.svwb.cardlist.data.Lang

fun cardImageUrl(lang: Lang, hash: String) =
    "https://shadowverse-wb.com/uploads/card_image/${lang.code}/card/$hash.png"

private val KEYWORD_COLOR = Color(0xFF9A5A00)
private val TAG_REGEX = Regex("<color=Keyword>(.*?)</color>|<[^>]+>", RegexOption.DOT_MATCHES_ALL)

/** 把官方文本里的 <color=Keyword> 标签转换为高亮粗体，其余标签直接去掉 */
fun parseSkillText(raw: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in TAG_REGEX.findAll(raw)) {
        append(raw.substring(last, m.range.first))
        m.groups[1]?.value?.let {
            withStyle(SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold)) { append(it) }
        }
        last = m.range.last + 1
    }
    append(raw.substring(last))
}

@Composable
fun SkillText(raw: String, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp) {
    val text = remember(raw) { parseSkillText(raw) }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize, lineHeight = fontSize * 1.5f),
    )
}
