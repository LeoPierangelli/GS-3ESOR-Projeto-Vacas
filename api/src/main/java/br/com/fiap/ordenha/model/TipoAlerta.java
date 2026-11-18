package br.com.fiap.ordenha.model;

public enum TipoAlerta {
    SUSPEITA_MASTITE("Suspeita de mastite: faça o teste da caneca ou o CMT"),
    QUEDA_PRODUCAO("Produção em queda"),
    LEITE_EM_CARENCIA("Não enviar este leite ao tanque");

    private final String mensagem;

    TipoAlerta(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
