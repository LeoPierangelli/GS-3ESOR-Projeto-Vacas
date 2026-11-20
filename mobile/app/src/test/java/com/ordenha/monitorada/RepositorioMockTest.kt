package com.ordenha.monitorada

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Confere que o mock do app é a mesma carga do banco (banco/02_carga_inicial.sql) e que os
 * alertas abertos batem com as regras da API (RegrasAlertaService): média das 7 ordenhas anteriores.
 */
class RepositorioMockTest {

    @Before
    fun loginJoao() {
        assertTrue(RepositorioMock.entrar("joao@exemplo.com", "senha123"))
    }

    /** Histórico da vaca ANTES da ordenha informada, da mais recente para a mais antiga. */
    private fun anteriores(o: Ordenha) =
        RepositorioMock.ordenhasDaVaca(o.vacaId).filter { it.dataHora < o.dataHora }

    private fun ordenhaDoAlerta(a: Alerta) = RepositorioMock.ordenhas.first { it.id == a.ordenhaId }

    @Test
    fun cargaIgualAoBanco() {
        assertEquals(2, RepositorioMock.propriedades.size)
        assertEquals(8, RepositorioMock.vacas.size)
        assertEquals(140, RepositorioMock.ordenhas.size)      // 7 vacas em lactação x 10 dias x 2 turnos
        assertEquals(21, RepositorioMock.alertas.size)
        assertEquals(7, RepositorioMock.alertas.count { !it.resolvido })
        assertEquals("BR-003", RepositorioMock.vacas.first { it.id == 3 }.brinco)
        assertEquals("Mimosa", RepositorioMock.vacas.first { it.id == 3 }.nome)
    }

    @Test
    fun cadaVacaEmLactacaoTemDezDiasEDoisTurnos() {
        RepositorioMock.vacas.forEach { vaca ->
            val leituras = RepositorioMock.ordenhasDaVaca(vaca.id)
            if (vaca.status == StatusVaca.SECA) {
                assertTrue("Vaca seca ${vaca.id} não é ordenhada", leituras.isEmpty())
            } else {
                assertEquals("Vaca ${vaca.id}", 20, leituras.size)
                assertEquals(10, leituras.map { it.dataHora.substring(0, 10) }.distinct().size)
                assertTrue(leituras.all { it.litros > 0 && it.condutividade > 0 })
            }
        }
    }

    @Test
    fun loginMostraSoAsVacasDoProdutor() {
        assertEquals(listOf(1, 2, 3, 4, 5), RepositorioMock.vacasDoUsuario().map { it.id })
        assertNull("Vaca 6 é da Maria", RepositorioMock.vacaPorId(6))

        assertTrue(RepositorioMock.entrar("maria@exemplo.com", "senha123"))
        assertEquals(listOf(6, 7, 8), RepositorioMock.vacasDoUsuario().map { it.id })

        assertFalse(RepositorioMock.entrar("joao@exemplo.com", "errada"))
        assertFalse(RepositorioMock.entrar("ninguem@exemplo.com", "senha123"))
    }

    @Test
    fun suspeitaDeMastiteDaMimosaSegueARegraDaApi() {
        val abertos = RepositorioMock.alertasAbertosDaVaca(3).filter { it.tipo == TipoAlerta.SUSPEITA_MASTITE }
        assertTrue(abertos.isNotEmpty())
        abertos.forEach { a ->
            val o = ordenhaDoAlerta(a)
            val media = anteriores(o).take(Regras.JANELA_MEDIA_ORDENHAS).map { it.condutividade }.average()
            assertTrue("ordenha ${o.id}", o.condutividade > media * (1 + Regras.LIMITE_CONDUTIVIDADE_PCT / 100))
        }
    }

    @Test
    fun quedaDeProducaoDaEstrelaSegueARegraDaApi() {
        val a = RepositorioMock.alertasAbertosDaVaca(4).single { it.tipo == TipoAlerta.QUEDA_PRODUCAO }
        val o = ordenhaDoAlerta(a)
        val hist = anteriores(o)
        val recentes = listOf(o.litros) + hist.take(2).map { it.litros }   // a atual e as 2 anteriores
        val media = hist.drop(2).take(Regras.JANELA_MEDIA_ORDENHAS).map { it.litros }.average()
        assertTrue(recentes.all { it < media * 0.8 })
    }

    @Test
    fun pintadaEmTratamentoTemLeiteEmCarencia() {
        assertTrue(RepositorioMock.entrar("maria@exemplo.com", "senha123"))
        assertEquals(StatusVaca.EM_TRATAMENTO, RepositorioMock.vacaPorId(6)!!.status)
        assertEquals(2, RepositorioMock.tratamentosDaVaca(6).size)
        assertTrue(RepositorioMock.alertasAbertosDaVaca(6).any { it.tipo == TipoAlerta.LEITE_EM_CARENCIA })
    }

    @Test
    fun todosOsAlertasReferenciamVacasEOrdenhasExistentes() {
        RepositorioMock.alertas.forEach { alerta ->
            assertNotNull(RepositorioMock.vacas.firstOrNull { it.id == alerta.vacaId })
            assertTrue(RepositorioMock.ordenhas.any { it.id == alerta.ordenhaId && it.vacaId == alerta.vacaId })
        }
    }

    @Test
    fun cadastroRecusaBrincoRepetidoNaPropriedade() {
        assertNotNull(RepositorioMock.adicionarVaca("BR-001", "Repetida", "2023-01-01"))
    }
}
