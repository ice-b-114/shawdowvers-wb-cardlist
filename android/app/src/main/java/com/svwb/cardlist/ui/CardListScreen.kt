@file:OptIn(ExperimentalMaterial3Api::class)

package com.svwb.cardlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.svwb.cardlist.data.Card
import com.svwb.cardlist.data.Lang

@Composable
fun CardListScreen(vm: CardViewModel) {
    val strings = vm.strings
    val data = vm.data
    val snackbar = remember { SnackbarHostState() }
    var showFilter by remember { mutableStateOf(false) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val cards = if (data != null) vm.visibleCards() else emptyList()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(shadowElevation = 3.dp) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(strings.appName, style = MaterialTheme.typography.titleLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Lang.entries.forEach { l ->
                                TextButton(onClick = { vm.switchLang(l) }) {
                                    Text(
                                        l.label,
                                        style = if (l == vm.lang) MaterialTheme.typography.titleMedium
                                        else MaterialTheme.typography.bodyMedium,
                                        color = if (l == vm.lang) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { vm.refresh() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = strings.refresh)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = vm.query,
                        onValueChange = { vm.query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(strings.searchHint) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (vm.query.isNotEmpty()) {
                                IconButton(onClick = { vm.query = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = strings.clear)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(24.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (data != null) strings.countText(cards.size) else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val active = vm.filters.activeCount
                        FilledTonalButton(onClick = { showFilter = true }, enabled = data != null) {
                            if (active > 0) {
                                BadgedBox(badge = { Badge { Text(active.toString()) } }) {
                                    Text(strings.filter)
                                }
                            } else {
                                Text(strings.filter)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                data == null && vm.loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(strings.loading, modifier = Modifier.padding(top = 12.dp))
                }
                data == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.loadFailed, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    FilledTonalButton(onClick = { vm.refresh() }) { Text(strings.refresh) }
                }
                cards.isEmpty() -> Text(strings.noResult, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardThumb(card, vm.lang) { vm.openDetail(card.id) }
                    }
                }
            }
        }
    }

    if (showFilter && data != null) {
        FilterSheet(
            data = data,
            strings = strings,
            filters = vm.filters,
            resultCount = cards.size,
            onChange = { vm.filters = it },
            onDismiss = { showFilter = false },
        )
    }
}

@Composable
fun CardThumb(card: Card, lang: Lang, onClick: () -> Unit) {
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
}

@Composable
fun CardImage(card: Card, hash: String, lang: Lang, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(hash, lang) {
        ImageRequest.Builder(context)
            .data(cardImageUrl(lang, hash))
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = card.name,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp),
                )
            }
        },
    )
}
