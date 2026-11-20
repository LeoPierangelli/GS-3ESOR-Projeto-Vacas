package com.ordenha.monitorada

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * Arquivo único de acesso aos dados. Os dados vêm de DadosCarga.kt, que é a MESMA carga inicial
 * do banco (gerada por banco/gerar_dados.py): mesmos usuários, vacas, ordenhas e alertas da API.
 * Quando o app for consumir a API, só este arquivo muda: as telas chamam as mesmas funções.
 */
object RepositorioMock {

    // Mensagem de sucesso que a tela Rebanho mostra depois de um cadastro
    var mensagem by mutableStateOf<String?>(null)

    /** Usuário logado. Como na API, cada produtor só enxerga as vacas da própria propriedade. */
    var usuarioId by mutableStateOf<Int?>(null)
        private set

    internal val usuarios: List<UsuarioCarga> = DadosCarga.usuarios
    val propriedades: List<Propriedade> = DadosCarga.propriedades
    val vacas = mutableStateListOf<Vaca>().apply { addAll(DadosCarga.vacas) }

    private var proximoId = DadosCarga.vacas.maxOf { it.id } + 1

    // Datas relativas ao dia de hoje, como na carga do banco (CURDATE() - n dias).
    private fun dia(relativo: Int): String = LocalDate.now().plusDays(relativo.toLong()).toString()

    val ordenhas: List<Ordenha> = DadosCarga.ordenhas.map {
        Ordenha(it.id, it.vacaId, "${dia(it.dia)}T${it.hora}:00", it.turno, it.litros, it.condutividade)
    }

    val alertas: List<Alerta> = DadosCarga.alertas.map { a ->
        val ordenha = ordenhas.first { it.id == a.ordenhaId }
        Alerta(a.id, a.vacaId, a.ordenhaId, a.tipo, a.mensagem, ordenha.dataHora, a.resolvido)
    }

    val tratamentos: List<Tratamento> = DadosCarga.tratamentos.map { t ->
        Tratamento(t.id, t.vacaId, t.medicamento, dia(t.inicio), t.fim?.let { dia(it) }, t.diasCarencia, t.observacao)
    }

    // ---------- login (simula POST /auth/login) ----------

    /** Confere email e senha. Retorna true e guarda o usuário logado se estiverem certos. */
    fun entrar(email: String, senha: String): Boolean {
        val usuario = usuarios.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        if (usuario == null || senha != DadosCarga.SENHA) return false
        usuarioId = usuario.id
        return true
    }

    fun sair() {
        usuarioId = null
    }

    fun nomeUsuario(): String? = usuarios.firstOrNull { it.id == usuarioId }?.nome

    private fun propriedadesDoUsuario(): List<Propriedade> = propriedades.filter { it.usuarioId == usuarioId }

    // ---------- consultas usadas pelas telas ----------

    /** Simula GET /vacas: só as vacas das propriedades do usuário logado. */
    fun vacasDoUsuario(): List<Vaca> {
        val ids = propriedadesDoUsuario().map { it.id }
        return vacas.filter { it.propriedadeId in ids }
    }

    fun vacaPorId(id: Int): Vaca? = vacasDoUsuario().firstOrNull { it.id == id }

    fun ordenhasDaVaca(vacaId: Int): List<Ordenha> =
        ordenhas.filter { it.vacaId == vacaId }.sortedByDescending { it.dataHora }

    /** Alertas abertos das vacas do usuário logado. */
    fun alertasAbertos(): List<Alerta> {
        val ids = vacasDoUsuario().map { it.id }.toSet()
        return alertas.filter { !it.resolvido && it.vacaId in ids }
    }

    fun alertasAbertosDaVaca(vacaId: Int): List<Alerta> =
        alertas.filter { !it.resolvido && it.vacaId == vacaId }.sortedByDescending { it.dataHora }

    fun tratamentosDaVaca(vacaId: Int): List<Tratamento> =
        tratamentos.filter { it.vacaId == vacaId }.sortedByDescending { it.dataInicio }

    /** Retorna (última leitura, média das 7 ordenhas anteriores) ou null se faltar histórico. */
    fun condutividadeVsMedia(vacaId: Int): Pair<Double, Double>? {
        val lista = ordenhasDaVaca(vacaId)
        if (lista.size < 2) return null
        val ultima = lista.first().condutividade
        val media = lista.drop(1).take(Regras.JANELA_MEDIA_ORDENHAS).map { it.condutividade }.average()
        return ultima to media
    }

    /** Simula o POST /vacas. Retorna mensagem de erro (como o 409 da API) ou null se deu certo. */
    fun adicionarVaca(brinco: String, nome: String, dataNascimentoIso: String): String? {
        val propriedadeDestino = propriedadesDoUsuario().firstOrNull()?.id
            ?: return "Faça login para cadastrar uma vaca"
        if (vacas.any { it.propriedadeId == propriedadeDestino && it.brinco.equals(brinco, ignoreCase = true) }) {
            return "Já existe uma vaca com o brinco $brinco nesta propriedade"
        }
        vacas.add(Vaca(proximoId++, propriedadeDestino, brinco, nome, dataNascimentoIso, StatusVaca.ATIVA))
        return null
    }
}
