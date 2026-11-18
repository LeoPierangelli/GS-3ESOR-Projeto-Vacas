package br.com.fiap.ordenha.dto;

import java.util.List;

/** Resposta do POST /ordenhas: a leitura gravada e os alertas que ela disparou (lista vazia se nenhum). */
public record OrdenhaRegistradaResponse(OrdenhaResponse ordenha, List<AlertaResponse> alertasGerados) {
}
