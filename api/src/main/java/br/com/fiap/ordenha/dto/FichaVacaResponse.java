package br.com.fiap.ordenha.dto;

import java.util.List;

/**
 * Ficha da vaca para o app: dados, últimas ordenhas, última condutividade contra a média,
 * alertas abertos e tratamentos.
 */
public record FichaVacaResponse(VacaResponse vaca,
                                Double ultimaCondutividade,
                                Double mediaCondutividade,
                                Double mediaLitros,
                                List<OrdenhaResponse> ultimasOrdenhas,
                                List<AlertaResponse> alertasAbertos,
                                List<TratamentoResponse> tratamentos) {
}
