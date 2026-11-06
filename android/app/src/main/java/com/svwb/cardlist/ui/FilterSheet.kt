@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.svwb.cardlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.svwb.cardlist.data.CardData

@Composable
fun FilterSheet(
    data: CardData,
    strings: Strings,
    filters: Filters,
    resultCount: Int,
    onChange: (Filters) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { onChange(Filters()) }) { Text(strings.reset) }
                Text(strings.countText(resultCount), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss) { Text(strings.done) }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Section(strings.secSet) {
                    data.setOrder.forEach { id ->
                        Chip(data.setNames[id] ?: id.toString(), id in filters.sets) {
                            onChange(filters.copy(sets = filters.sets.toggle(id)))
                        }
                    }
                }
                Section(strings.secClass) {
                    (0..7).forEach { c ->
                        Chip(strings.className(c), c in filters.classes) {
                            onChange(filters.copy(classes = filters.classes.toggle(c)))
                        }
                    }
                }
                Section(strings.secCost) {
                    (0..10).forEach { c ->
                        Chip(strings.costName(c), c in filters.costs) {
                            onChange(filters.copy(costs = filters.costs.toggle(c)))
                        }
                    }
                }
                Section(strings.secType) {
                    listOf(1, 4, 2).forEach { t ->
                        Chip(strings.typeName(t), t in filters.types) {
                            onChange(filters.copy(types = filters.types.toggle(t)))
                        }
                    }
                }
                Section(strings.secRarity) {
                    (1..4).forEach { r ->
                        Chip(strings.rarityName(r), r in filters.rarities) {
                            onChange(filters.copy(rarities = filters.rarities.toggle(r)))
                        }
                    }
                }
                Section(strings.secFormat) {
                    listOf(
                        FormatFilter.ALL to strings.formatAll,
                        FormatFilter.ROTATION to strings.formatRotation,
                        FormatFilter.UNLIMITED_ONLY to strings.formatUnlimited,
                    ).forEach { (f, label) ->
                        Chip(label, filters.format == f) { onChange(filters.copy(format = f)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) { content() }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private fun <T> Set<T>.toggle(v: T): Set<T> = if (v in this) this - v else this + v
