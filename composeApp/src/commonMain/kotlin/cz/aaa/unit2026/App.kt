package cz.aaa.unit2026

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cz.aaa.unit2026.ui.screens.HomeScreen
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

@Composable
@Preview
fun App() {
    OpenJetTracksTheme {
        Surface(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            HomeScreen()
        }
    }
}
