package br.com.fiap.ordenha.service;

import br.com.fiap.ordenha.config.RegrasAlertaProperties;
import br.com.fiap.ordenha.model.Tratamento;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * As três regras de alerta, sem acesso a banco (fácil de testar). Os históricos chegam ordenados da
 * ordenha mais recente para a mais antiga e NÃO incluem a leitura que está sendo avaliada.
 * O alerta é sempre uma suspeita, nunca um diagnóstico: por isso a comparação é com a média da própria vaca.
 */
@Service
public class RegrasAlertaService {

    private final RegrasAlertaProperties regras;

    public RegrasAlertaService(RegrasAlertaProperties regras) {
        this.regras = regras;
    }

    /** Quantas ordenhas anteriores o OrdenhaService precisa buscar para avaliar todas as regras. */
    public int tamanhoHistoricoNecessario() {
        return regras.janela() + regras.ordenhasSeguidas() - 1;
    }

    /** Suspeita de mastite: condutividade X% acima da média das últimas N ordenhas da vaca. */
    public boolean suspeitaMastite(double condutividadeAtual, List<Double> condutividadesAnteriores) {
        if (condutividadesAnteriores.size() < regras.historicoMinimo()) {
            return false;
        }
        double media = media(condutividadesAnteriores.subList(0, Math.min(regras.janela(), condutividadesAnteriores.size())));
        return condutividadeAtual > media * (1 + regras.limiteMastite());
    }

    /**
     * Queda de produção: a leitura atual e as (ordenhasSeguidas - 1) anteriores ficaram todas X% abaixo da
     * média das N ordenhas que vieram antes delas.
     */
    public boolean quedaProducao(double litrosAtual, List<Double> litrosAnteriores) {
        int recentes = regras.ordenhasSeguidas() - 1;
        if (litrosAnteriores.size() < recentes + regras.historicoMinimo()) {
            return false;
        }
        List<Double> base = litrosAnteriores.subList(recentes, Math.min(recentes + regras.janela(), litrosAnteriores.size()));
        double limite = media(base) * (1 - regras.limiteQueda());
        if (litrosAtual >= limite) {
            return false;
        }
        return litrosAnteriores.subList(0, recentes).stream().allMatch(l -> l < limite);
    }

    /** Leite em carência: existe tratamento cuja carência ainda não terminou no dia da ordenha. */
    public boolean leiteEmCarencia(LocalDate diaOrdenha, List<Tratamento> tratamentos) {
        return tratamentos.stream().anyMatch(t -> t.emCarencia(diaOrdenha));
    }

    private static double media(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
