package com.ordenha.monitorada

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Arquivo único de mock. Quando a API estiver pronta, só este arquivo muda:
 * as telas continuam chamando as mesmas funções.
 */
object RepositorioMock {
    const val EMAIL_DEMO = "produtor@teste.com"
    const val SENHA_DEMO = "123456"

    // Mensagem de sucesso que a tela Rebanho mostra depois de um cadastro
    var mensagem by mutableStateOf<String?>(null)

    private var proximoId = 9

    // Duas propriedades de demonstração, conforme a carga inicial planejada.
    val propriedades = listOf(
        Propriedade(1, 1, "Sítio Boa Vista", "São Carlos", "SP"),
        Propriedade(2, 2, "Fazenda Esperança", "Araraquara", "SP")
    )

    val vacas = mutableStateListOf(
        Vaca(1, 1, "101", "Mimosa", "2021-03-10", StatusVaca.ATIVA),
        Vaca(2, 1, "102", "Estrela", "2020-08-22", StatusVaca.ATIVA),
        Vaca(3, 1, "103", "Bela", "2019-05-14", StatusVaca.ATIVA), // suspeita de mastite
        Vaca(4, 1, "104", "Pintada", "2021-11-02", StatusVaca.ATIVA), // queda de produção
        Vaca(5, 2, "201", "Fumaça", "2020-01-30", StatusVaca.EM_TRATAMENTO),
        Vaca(6, 2, "202", "Dorinha", "2018-09-09", StatusVaca.SECA),
        Vaca(7, 2, "203", "Canela", "2022-04-17", StatusVaca.ATIVA),
        Vaca(8, 2, "204", "Jade", "2021-07-25", StatusVaca.ATIVA)
    )

    val ordenhas: List<Ordenha> = gerarOrdenhas()

    val alertas: List<Alerta> = listOf(
        Alerta(1, 3, 60, TipoAlerta.SUSPEITA_MASTITE,
            "Suspeita de mastite: faça o teste da caneca ou o CMT", "2026-11-18T16:10:00", false),
        Alerta(2, 4, 80, TipoAlerta.QUEDA_PRODUCAO,
            "Produção em queda", "2026-11-18T16:10:00", false),
        Alerta(3, 5, 100, TipoAlerta.LEITE_EM_CARENCIA,
            "Não enviar este leite ao tanque", "2026-11-18T06:10:00", false)
    )

    val tratamentos: List<Tratamento> = listOf(
        Tratamento(1, 5, "Antibiótico (exemplo)", "2026-11-16", "2026-11-19", 4,
            "Leite em carência até 23/11")
    )

    // ---------- consultas usadas pelas telas ----------

    fun vacaPorId(id: Int): Vaca? = vacas.firstOrNull { it.id == id }

    fun ordenhasDaVaca(vacaId: Int): List<Ordenha> =
        ordenhas.filter { it.vacaId == vacaId }.sortedByDescending { it.dataHora }

    fun alertasAbertos(): List<Alerta> = alertas.filter { !it.resolvido }

    fun alertasAbertosDaVaca(vacaId: Int): List<Alerta> =
        alertasAbertos().filter { it.vacaId == vacaId }

    fun tratamentosDaVaca(vacaId: Int): List<Tratamento> =
        tratamentos.filter { it.vacaId == vacaId }

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
        val propriedadeDestino = 1
        if (vacas.any {
                it.propriedadeId == propriedadeDestino && it.brinco.equals(brinco, ignoreCase = true)
            }
        ) {
            return "Já existe uma vaca com o brinco $brinco nesta propriedade"
        }
        vacas.add(Vaca(proximoId++, propriedadeDestino, brinco, nome, dataNascimentoIso, StatusVaca.ATIVA))
        return null
    }

    // ---------- geração das ordenhas (10 dias, 2 por dia) ----------

    private fun arred(x: Double): Double = Math.round(x * 10) / 10.0

    private fun gerarOrdenhas(): List<Ordenha> {
        // vacaId, litros base, condutividade base (faixa normal 4 a 6 mS/cm)
        val perfis = listOf(
            Triple(1, 12.0, 5.0), Triple(2, 10.5, 4.8),
            Triple(3, 11.4, 5.0), Triple(4, 13.0, 5.2),
            Triple(5, 9.0, 5.1), Triple(6, 8.5, 4.9),
            Triple(7, 11.0, 5.3), Triple(8, 10.0, 4.7)
        )
        val lista = mutableListOf<Ordenha>()
        var id = 1
        for ((vacaId, litrosBase, condBase) in perfis) {
            for (dia in 9..18) {
                for (turno in Turno.values()) {
                    val t = if (turno == Turno.MANHA) 0 else 1
                    val variacao = ((dia + t + vacaId) % 3 - 1) * 0.2
                    var litros = litrosBase + (if (t == 0) 0.4 else -0.4) + variacao
                    var cond = condBase + variacao / 4

                    // Bela: condutividade sobe nos últimos 2 dias
                    if (vacaId == 3 && dia == 17) cond = 6.0
                    if (vacaId == 3 && dia == 18) cond = 6.8
                    // Pintada: litros caem nas últimas 3 ordenhas consecutivas.
                    if (vacaId == 4 && (dia == 18 || (dia == 17 && t == 1))) litros = litrosBase * 0.7

                    val hora = if (t == 0) "06:10:00" else "16:10:00"
                    lista.add(
                        Ordenha(
                            id++, vacaId,
                            String.format("2026-11-%02dT%s", dia, hora),
                            turno, arred(litros), arred(cond)
                        )
                    )
                }
            }
        }
        return lista
    }
}
