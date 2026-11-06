package com.svwb.cardlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.svwb.cardlist.ui.CardListScreen
import com.svwb.cardlist.ui.CardViewModel
import com.svwb.cardlist.ui.DetailScreen

class MainActivity : ComponentActivity() {

    private val vm: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6B46C1))) {
                Surface(Modifier.fillMaxSize()) {
                    val top = vm.detailStack.lastOrNull()
                    val data = vm.data
                    BackHandler(enabled = top != null) { vm.closeDetail() }

                    Box(Modifier.fillMaxSize()) {
                        CardListScreen(vm)
                        if (top != null && data != null) {
                            Surface(Modifier.fillMaxSize()) {
                                DetailScreen(
                                    cardId = top,
                                    data = data,
                                    strings = vm.strings,
                                    onOpen = vm::openDetail,
                                    onBack = vm::closeDetail,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
