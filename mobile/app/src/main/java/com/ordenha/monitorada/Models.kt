package com.ordenha.monitorada

// Nomes iguais aos do banco/API (combinado do grupo): usuario, propriedade, vaca, ordenha, alerta, tratamento

data class Propriedade(
    val id: Int,
    val usuarioId: Int,
    val nome: String,
    val municipio: String,
    val uf: String
)

enum class StatusVaca { ATIVA, EM_TRATAMENTO, SECA }
enum class TipoAlerta { SUSPEITA_MASTITE, QUEDA_PRODUCAO, LEITE_EM_CARENCIA }
enum class Turno { MANHA, TARDE }

data class Vaca(
    val id: Int,
    val propriedadeId: Int,
    val brinco: String,
    val nome: String,
    val dataNascimento: String, // ISO: 2021-03-10
    val status: StatusVaca
)

data class Ordenha(
    val id: Int,
    val vacaId: Int,
    val dataHora: String, // ISO: 2026-11-18T06:10:00
    val turno: Turno,
    val litros: Double,
    val condutividade: Double
)

data class Alerta(
    val id: Int,
    val vacaId: Int,
    val ordenhaId: Int?,
    val tipo: TipoAlerta,
    val mensagem: String,
    val dataHora: String,
    val resolvido: Boolean
)

data class Tratamento(
    val id: Int,
    val vacaId: Int,
    val medicamento: String,
    val dataInicio: String,
    val dataFim: String,
    val diasCarencia: Int,
    val observacao: String
)
