@file:OptIn(ExperimentalMaterial3Api::class)

package com.svwb.cardlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.svwb.cardlist.data.Card
import com.svwb.cardlist.data.CardData
import com.svwb.cardlist.data.Lang

@Composable
fun DetailScreen(
    cardId: Long,
    data: CardData,
    strings: Strings,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val card = data.byId[cardId] ?: return
    var showEvo by remember(cardId) { mutableStateOf(false) }
    val evo = card.evo?.takeIf { it.imageHash.isNotEmpty() }
    val related = card.related.mapNotNull { data.byId[it] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CardImage(
                    card = card,
                    hash = if (showEvo && evo != null) evo.imageHash else card.imageHash,
                    lang = data.lang,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(530f / 687f),
                )
            }
            if (evo != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = { showEvo = !showEvo }) {
                        Text(if (showEvo) strings.showBase else strings.showEvo)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (card.isToken) {
                    Text(
                        strings.tokenTag,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                InfoRow(strings.detailCost, card.cost.toString())
                if (card.typeGroup == 1) {
                    InfoRow(strings.detailAtk, card.atk.toString())
                    InfoRow(strings.detailLife, card.life.toString())
                }
                InfoRow(
                    strings.secClass,
                    strings.className(card.clazz) + " / " + strings.typeName(card.typeGroup) +
                        " / " + strings.rarityName(card.rarity),
                )
                InfoRow(strings.detailSet, data.setNames[card.setId] ?: card.setId.toString())
                if (card.tribes.isNotEmpty()) {
                    InfoRow(strings.detailTribe, card.tribes.joinToString("、") { data.tribeNames[it] ?: "?" })
                }
                if (!card.isToken) {
                    InfoRow(
                        strings.detailFormat,
                        if (card.inRotation) strings.formatBoth else strings.formatUnlimited,
                    )
                }
            }

            if (card.skillText.isNotBlank()) {
                Block(strings.detailSkill) { SkillText(card.skillText) }
            }
            if (evo != null && evo.skillText.isNotBlank() && evo.skillText != card.skillText) {
                Block(strings.detailEvo) { SkillText(evo.skillText) }
            }
            if (card.effects.isNotEmpty()) {
                Block(strings.detailSpecific) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        card.effects.forEach { e ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("[${e.cost}]", style = MaterialTheme.typography.labelLarge)
                                SkillText(e.skillText)
                            }
                        }
                    }
                }
            }
            if (card.flavourText.isNotBlank()) {
                Block(strings.detailFlavour) {
                    Text(
                        card.flavourText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (card.cv.isNotBlank() || card.illustrator.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (card.cv.isNotBlank()) InfoRow(strings.detailCv, card.cv)
                    if (card.illustrator.isNotBlank()) InfoRow(strings.detailIllustrator, card.illustrator)
                }
            }

            if (related.isNotEmpty()) {
                Block(strings.detailRelated) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(related, key = { it.id }) { r ->
                            RelatedThumb(r, data.lang, strings) { onOpen(r.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedThumb(card: Card, lang: Lang, strings: Strings, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        CardImage(
            card = card,
            hash = card.imageHash,
            lang = lang,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(530f / 687f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
        )
        Text(
            card.name + if (card.isToken) "\n(${strings.tokenTag})" else "",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Block(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}
