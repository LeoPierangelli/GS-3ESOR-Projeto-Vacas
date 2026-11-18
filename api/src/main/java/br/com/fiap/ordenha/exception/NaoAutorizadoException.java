package br.com.fiap.ordenha.exception;

/** Vira 401 (login errado). */
public class NaoAutorizadoException extends RuntimeException {
    public NaoAutorizadoException(String mensagem) {
        super(mensagem);
    }
}
