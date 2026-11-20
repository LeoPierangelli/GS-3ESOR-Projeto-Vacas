package com.ordenha.monitorada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Valida o calendário real (inclui anos bissextos) e impede datas futuras.
private fun dataValida(texto: String): Boolean {
    val partes = texto.split("/")
    if (partes.size != 3 || partes[0].length != 2 || partes[1].length != 2 || partes[2].length != 4) return false
    val dia = partes[0].toIntOrNull() ?: return false
    val mes = partes[1].toIntOrNull() ?: return false
    val ano = partes[2].toIntOrNull() ?: return false
    return try {
        val data = java.time.LocalDate.of(ano, mes, dia)
        !data.isAfter(java.time.LocalDate.now())
    } catch (_: java.time.DateTimeException) {
        false
    }
}

// Insere as barras automaticamente para permitir digitação apenas numérica.
private fun formatarEntradaData(valor: String): String {
    val digitos = valor.filter(Char::isDigit).take(8)
    return buildString {
        digitos.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append('/')
            append(char)
        }
    }
}

// "10/03/2021" -> "2021-03-10" (mesmo formato do banco)
private fun paraIso(texto: String): String {
    val (d, m, a) = texto.split("/")
    return "$a-$m-$d"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroVacaScreen(onVoltar: () -> Unit, onSalvo: () -> Unit) {
    var brinco by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var nascimento by remember { mutableStateOf("") }

    var erroBrinco by remember { mutableStateOf<String?>(null) }
    var erroNome by remember { mutableStateOf<String?>(null) }
    var erroNascimento by remember { mutableStateOf<String?>(null) }
    var erroGeral by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun salvar() {
        erroGeral = null
        erroBrinco = if (brinco.isBlank()) "Informe o brinco" else null
        erroNome = if (nome.isBlank()) "Informe o nome" else null
        erroNascimento = if (!dataValida(nascimento.trim())) "Informe uma data válida, no formato dd/mm/aaaa, que não seja futura" else null
        if (erroBrinco != null || erroNome != null || erroNascimento != null) return

        scope.launch {
            carregando = true
            delay(800) // simula o POST /vacas
            val erro = RepositorioMock.adicionarVaca(brinco.trim(), nome.trim(), paraIso(nascimento.trim()))
            carregando = false
            if (erro == null) {
                RepositorioMock.mensagem = "Vaca ${nome.trim()} cadastrada com sucesso"
                onSalvo()
            } else {
                erroGeral = erro // equivale ao 409 da API
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastrar vaca") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao Rebanho")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = brinco,
                onValueChange = { brinco = it },
                label = { Text("Brinco") },
                singleLine = true,
                isError = erroBrinco != null,
                supportingText = { erroBrinco?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                isError = erroNome != null,
                supportingText = { erroNome?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nascimento,
                onValueChange = { nascimento = formatarEntradaData(it) },
                label = { Text("Data de nascimento (dd/mm/aaaa)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = erroNascimento != null,
                supportingText = { erroNascimento?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            erroGeral?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = { salvar() },
                enabled = !carregando,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (carregando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
