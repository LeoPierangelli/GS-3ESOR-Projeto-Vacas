package com.ordenha.monitorada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaVacaScreen(vacaId: Int, onVoltar: () -> Unit) {
    val vaca = RepositorioMock.vacaPorId(vacaId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vaca?.nome ?: "Vaca") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao Rebanho")
                    }
                }
            )
        }
    ) { padding ->
        if (vaca == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vaca não encontrada")
            }
            return@Scaffold
        }

        val ordenhas = RepositorioMock.ordenhasDaVaca(vaca.id)
        val cond = RepositorioMock.condutividadeVsMedia(vaca.id)
        val alertas = RepositorioMock.alertasAbertosDaVaca(vaca.id)
        val tratamentos = RepositorioMock.tratamentosDaVaca(vaca.id)

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Brinco ${vaca.brinco} · nasc. ${formatarData(vaca.dataNascimento)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    SeloStatus(seloDaVaca(vaca))
                }
            }

            // Condutividade contra a média
            item {
                SecaoCard("Condutividade") {
                    if (cond == null) {
                        Text("Ainda sem leituras suficientes")
                    } else {
                        val (ultima, media) = cond
                        val diff = (ultima / media - 1) * 100
                        val acima = diff > Regras.LIMITE_CONDUTIVIDADE_PCT
                        val cor = if (acima) CorSuspeita else CorNormal
                        Text("Última leitura: ${formatarCondutividade(ultima)} mS", style = MaterialTheme.typography.titleLarge)
                        Text("Média das ${Regras.JANELA_MEDIA_ORDENHAS} ordenhas anteriores: ${formatarCondutividade(media)} mS")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (acima) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null, tint = cor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (acima) "${diff.roundToInt()}% acima da média" else "Dentro do esperado",
                                color = cor, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Alertas abertos
            item {
                SecaoCard("Alertas") {
                    if (alertas.isEmpty()) {
                        Text("Nenhum alerta aberto")
                    } else {
                        alertas.forEach { a ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CorSuspeita)
                                Spacer(Modifier.width(8.dp))
                                Text("${a.mensagem}\n${formatarDataHora(a.dataHora)}")
                            }
                        }
                    }
                }
            }

            // Tratamentos
            item {
                SecaoCard("Tratamentos") {
                    if (tratamentos.isEmpty()) {
                        Text("Nenhum tratamento registrado")
                    } else {
                        tratamentos.forEach { t ->
                            Text(t.medicamento, fontWeight = FontWeight.SemiBold)
                            Text("${formatarData(t.dataInicio)} a ${t.dataFim?.let { formatarData(it) } ?: "em andamento"} · carência de ${t.diasCarencia} dias")
                            if (t.observacao.isNotBlank()) Text(t.observacao)
                        }
                    }
                }
            }

            // Últimas ordenhas
            item {
                SecaoCard("Últimas ordenhas") {
                    ordenhas.take(8).forEachIndexed { i, o ->
                        if (i > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatarDataHora(o.dataHora))
                            Text("${formatarNumero(o.litros)} L", fontWeight = FontWeight.SemiBold)
                            Text("${formatarCondutividade(o.condutividade)} mS")
                        }
                    }
                }
            }
        }
    }
}
