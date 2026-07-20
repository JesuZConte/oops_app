package com.zconte.oopsapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zconte.oopsapp.BuildConfig
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.ui.components.ThemedCard
import com.zconte.oopsapp.ui.theme.OopsTheme
import com.zconte.oopsapp.ui.theme.PaperAccentAmber

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        ThemedCard(accentColor = MaterialTheme.colorScheme.secondary) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TEMA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                ThemeOptionRow("Sistema", ThemeMode.SYSTEM, themeMode, viewModel::setThemeMode)
                ThemeOptionRow("Claro", ThemeMode.LIGHT, themeMode, viewModel::setThemeMode)
                ThemeOptionRow("Oscuro", ThemeMode.DARK, themeMode, viewModel::setThemeMode)
            }
        }

        ThemedCard(
            accentColor = if (OopsTheme.extendedColors.isDark) MaterialTheme.colorScheme.tertiary else PaperAccentAmber
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "VERSIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val selected = selectedMode == mode
    val extended = OopsTheme.extendedColors
    val accent = MaterialTheme.colorScheme.secondary
    val ringColor = if (selected) accent else extended.hardShadowColor.takeIf { !extended.isDark } ?: MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .then(
                    if (selected && extended.isDark) {
                        Modifier.shadow(elevation = 6.dp, shape = CircleShape, ambientColor = accent, spotColor = accent)
                    } else {
                        Modifier
                    }
                )
                .border(width = 2.dp, color = ringColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(accent, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}