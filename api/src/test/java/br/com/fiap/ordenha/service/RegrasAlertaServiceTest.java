package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.config.RegrasAlertaProperties;
import br.com.fiap.ordenha.model.Tratamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de unidade das três regras de alerta (sem banco, sem subir a API). */
class RegrasAlertaServiceTest {

    // Mesmos valores do application.properties: 15%, 20%, janela 7, 3 seguidas, mínimo 3.
    private final RegrasAlertaService regras =
            new RegrasAlertaService(new RegrasAlertaProperties(0.15, 0.20, 7, 3, 3));

    private static List<Double> repetir(double valor, int vezes) {
        return Collections.nCopies(vezes, valor);
    }

    @Test
    @DisplayName("Mastite: condutividade 20% acima da média dispara alerta")
    void mastiteDisparaAcimaDoLimite() {
        assertTrue(regras.suspeitaMastite(6.0, repetir(5.0, 7)));
    }

    @Test
    @DisplayName("Mastite: condutividade 10% acima da média não dispara")
    void mastiteNaoDisparaDentroDoLimite() {
        assertFalse(regras.suspeitaMastite(5.5, repetir(5.0, 7)));
    }

    @Test
    @DisplayName("Mastite: sem histórico mínimo não dá para comparar, então não dispara")
    void mastiteSemHistorico() {
        assertFalse(regras.suspeitaMastite(9.0, repetir(5.0, 2)));
    }

    @Test
    @DisplayName("Queda: 3 ordenhas seguidas 25% abaixo da média disparam alerta")
    void quedaDisparaComTresSeguidas() {
        // 2 anteriores baixas (7.5) + 7 normais (10.0); a atual também é baixa
        List<Double> historico = new java.util.ArrayList<>(repetir(7.5, 2));
        historico.addAll(repetir(10.0, 7));
        assertTrue(regras.quedaProducao(7.5, historico));
    }

    @Test
    @DisplayName("Queda: uma ordenha baixa isolada não dispara")
    void quedaNaoDisparaComUmaSo() {
        assertFalse(regras.quedaProducao(7.5, repetir(10.0, 9)));
    }

    @Test
    @DisplayName("Carência: ordenha dentro do período de carência dispara; depois da liberação não")
    void carencia() {
        Tratamento t = new Tratamento();
        t.setDataInicio(LocalDate.of(2026, 11, 10));
        t.setDataFim(LocalDate.of(2026, 11, 12));
        t.setDiasCarencia(4); // liberação em 16/11

        assertTrue(regras.leiteEmCarencia(LocalDate.of(2026, 11, 16), List.of(t)));
        assertFalse(regras.leiteEmCarencia(LocalDate.of(2026, 11, 17), List.of(t)));
        assertFalse(regras.leiteEmCarencia(LocalDate.of(2026, 11, 9), List.of(t)));
    }

    @Test
    @DisplayName("Carência: tratamento sem data de fim mantém o leite em carência")
    void carenciaTratamentoEmAndamento() {
        Tratamento t = new Tratamento();
        t.setDataInicio(LocalDate.of(2026, 11, 10));
        t.setDiasCarencia(4);
        assertTrue(regras.leiteEmCarencia(LocalDate.of(2026, 12, 1), List.of(t)));
    }
}
