package br.com.fiap.ordenha.dto;

/** Regras de validação reaproveitadas pelos DTOs de entrada. */
public final class Validacao {

    /** Bloqueia os caracteres usados para injetar HTML/script (proteção contra XSS armazenado). */
    public static final String SEM_HTML = "^[^<>]*$";
    public static final String SEM_HTML_MSG = "não pode conter os caracteres < ou >";

    private Validacao() {
    }
}
