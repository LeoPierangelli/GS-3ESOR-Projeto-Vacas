package com.ordenha.monitorada

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta do protótipo no Figma ("Cores Ordenha"). Os selos de status
// (CorNormal, CorSuspeita, CorTratamento, CorSeca) ficam em Componentes.kt.
private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0A3D12),
    secondary = Color(0xFF558B2F),
    background = Color(0xFFF6F9F2),
    surface = Color(0xFFF6F9F2),
    surfaceVariant = Color(0xFFE4EDDF),
    surfaceContainerHighest = Color(0xFFE4EDDF), // cor padrão dos Cards
    onBackground = Color(0xFF1B1F1A),
    onSurface = Color(0xFF1B1F1A),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF74796F),
    error = Color(0xFFC62828)
)

@Composable
fun OrdenhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaClaro, content = content)
}
