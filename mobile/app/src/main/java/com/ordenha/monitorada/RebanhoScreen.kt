package com.ordenha.monitorada

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RebanhoScreen(
    onFicha: (Int) -> Unit,
    onCadastro: () -> Unit,
    onSair: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val mensagem = RepositorioMock.mensagem

    // Retorno de sucesso vindo do Cadastro
    LaunchedEffect(mensagem) {
        if (mensagem != null) {
            snackbar.showSnackbar(mensagem)
            RepositorioMock.mensagem = null
        }
    }

    val abertos = RepositorioMock.alertasAbertos().size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(RepositorioMock.nomeUsuario()?.let { "Rebanho · $it" } ?: "Rebanho") },
                actions = {
                    TextButton(onClick = onSair, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Sair", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCadastro,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Cadastrar vaca", style = MaterialTheme.typography.titleMedium) }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val cor = if (abertos > 0) CorSuspeita else CorNormal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (abertos > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null, tint = cor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (abertos > 0) "$abertos alertas abertos" else "Nenhum alerta aberto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cor
                        )
                    }
                }
            }
            items(RepositorioMock.vacasDoUsuario(), key = { it.id }) { vaca ->
                VacaCard(vaca, onClick = { onFicha(vaca.id) })
            }
        }
    }
}

@Composable
private fun VacaCard(vaca: Vaca, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(vaca.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Brinco ${vaca.brinco}", style = MaterialTheme.typography.bodyLarge)
            }
            SeloStatus(seloDaVaca(vaca))
        }
    }
}
