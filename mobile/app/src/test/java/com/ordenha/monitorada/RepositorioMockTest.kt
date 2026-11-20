package com.ordenha.monitorada

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioMockTest {
    @Test
    fun cargaInicialTemDuasPropriedadesEOitoVacas() {
        assertEquals(2, RepositorioMock.propriedades.size)
        assertEquals(8, RepositorioMock.vacas.size)
    }

    @Test
    fun cadaVacaTemDezDiasEDoisTurnosDeOrdenha() {
        RepositorioMock.vacas.forEach { vaca ->
            val leituras = RepositorioMock.ordenhasDaVaca(vaca.id)
            assertEquals("Vaca ${vaca.id}", 20, leituras.size)
            assertEquals(10, leituras.map { it.dataHora.substring(0, 10) }.distinct().size)
            assertTrue(leituras.all { it.litros > 0 && it.condutividade > 0 })
        }
        assertEquals(160, RepositorioMock.ordenhas.size)
    }

    @Test
    fun alertaDeMastiteCorrespondeACondutividadeAcimaDeQuinzePorCento() {
        val comparacao = RepositorioMock.condutividadeVsMedia(3)
        assertNotNull(comparacao)
        val (ultima, media) = comparacao!!
        assertTrue((ultima / media - 1.0) * 100 > Regras.LIMITE_CONDUTIVIDADE_PCT)
        assertTrue(RepositorioMock.alertasAbertosDaVaca(3).any { it.tipo == TipoAlerta.SUSPEITA_MASTITE })
    }

    @Test
    fun alertaDeQuedaTemTresOrdenhasConsecutivasVintePorCentoAbaixoDaMedia() {
        val leituras = RepositorioMock.ordenhasDaVaca(4)
        val ultimasTres = leituras.take(3).map { it.litros }
        val mediaAnterior = leituras.drop(3).take(7).map { it.litros }.average()
        assertEquals(3, ultimasTres.size)
        assertTrue(ultimasTres.all { it <= mediaAnterior * 0.8 })
        assertTrue(RepositorioMock.alertasAbertosDaVaca(4).any { it.tipo == TipoAlerta.QUEDA_PRODUCAO })
    }

    @Test
    fun tratamentoTemAlertaDeCarenciaAssociado() {
        assertTrue(RepositorioMock.tratamentosDaVaca(5).isNotEmpty())
        assertTrue(RepositorioMock.alertasAbertosDaVaca(5).any { it.tipo == TipoAlerta.LEITE_EM_CARENCIA })
    }

    @Test
    fun todosOsAlertasReferenciamVacasEOrdenhasExistentes() {
        RepositorioMock.alertas.forEach { alerta ->
            assertNotNull(RepositorioMock.vacaPorId(alerta.vacaId))
            alerta.ordenhaId?.let { id ->
                assertTrue(RepositorioMock.ordenhas.any { it.id == id && it.vacaId == alerta.vacaId })
            }
        }
    }
}
