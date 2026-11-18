package br.com.fiap.ordenha.exception;

/** Vira 404. Também é usada quando o recurso existe mas é de outro usuário, para não revelar que existe. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
