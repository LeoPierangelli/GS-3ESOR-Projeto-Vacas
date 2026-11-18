package br.com.fiap.ordenha.exception;

/** Vira 409 (email ou brinco repetido). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
