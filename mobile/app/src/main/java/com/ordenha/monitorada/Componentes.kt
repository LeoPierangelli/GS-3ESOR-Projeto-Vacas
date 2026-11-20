package com.ordenha.monitorada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

// Status nunca só por cor: sempre cor + ícone + texto
data class Selo(val texto: String, val icone: ImageVector, val cor: Color)

val CorNormal = Color(0xFF2E7D32)
val CorSuspeita = Color(0xFFC62828)
val CorTratamento = Color(0xFF1565C0)
val CorSeca = Color(0xFF616161)

fun seloDaVaca(v: Vaca): Selo = when {
    v.status == StatusVaca.EM_TRATAMENTO -> Selo("Em tratamento", Icons.Default.Info, CorTratamento)
    v.status == StatusVaca.SECA -> Selo("Seca", Icons.Default.Info, CorSeca)
    RepositorioMock.alertasAbertosDaVaca(v.id).isNotEmpty() -> Selo("Suspeita", Icons.Default.Warning, CorSuspeita)
    else -> Selo("Normal", Icons.Default.CheckCircle, CorNormal)
}

@Composable
fun SeloStatus(selo: Selo) {
    Surface(shape = RoundedCornerShape(50), color = selo.cor.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(selo.icone, contentDescription = null, tint = selo.cor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(selo.texto, color = selo.cor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SecaoCard(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            conteudo()
        }
    }
}

// "2026-11-18T06:10:00" -> "18/11 06:10"
fun formatarDataHora(iso: String): String =
    "${iso.substring(8, 10)}/${iso.substring(5, 7)} ${iso.substring(11, 16)}"

// "2021-03-10" -> "10/03/2021"
fun formatarData(iso: String): String =
    "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}"

// 6.8 -> "6,8" (sempre no formato brasileiro, independente do idioma do aparelho)
fun formatarNumero(valor: Double): String = String.format(Locale("pt", "BR"), "%.1f", valor)
