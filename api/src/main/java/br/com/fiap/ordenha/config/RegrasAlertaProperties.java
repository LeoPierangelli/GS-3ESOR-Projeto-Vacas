package br.com.fiap.ordenha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Constantes configuráveis das regras de alerta (application.properties, prefixo ordenha.regras).
 * São valores iniciais do MVP, não parâmetros veterinários validados.
 */
@ConfigurationProperties(prefix = "ordenha.regras")
public record RegrasAlertaProperties(double limiteMastite, double limiteQueda, int janela, int ordenhasSeguidas,
                                     int historicoMinimo) {
}
