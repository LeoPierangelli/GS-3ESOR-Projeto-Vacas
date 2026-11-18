package br.com.fiap.ordenha.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tratamento")
public class Tratamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaca_id", nullable = false)
    private Vaca vaca;

    @Column(nullable = false, length = 100)
    private String medicamento;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    /** Nulo enquanto o tratamento não terminou. */
    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "dias_carencia", nullable = false)
    private Integer diasCarencia;

    @Column(length = 255)
    private String observacao;

    /** Dia a partir do qual o leite pode voltar ao tanque; nulo se o tratamento ainda não terminou. */
    public LocalDate getDataLiberacao() {
        return dataFim == null ? null : dataFim.plusDays(diasCarencia);
    }

    /** O leite está em carência do início do tratamento até a data de liberação (inclusive). */
    public boolean emCarencia(LocalDate dia) {
        if (dia.isBefore(dataInicio)) {
            return false;
        }
        LocalDate liberacao = getDataLiberacao();
        return liberacao == null || !dia.isAfter(liberacao);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vaca getVaca() { return vaca; }
    public void setVaca(Vaca vaca) { this.vaca = vaca; }
    public String getMedicamento() { return medicamento; }
    public void setMedicamento(String medicamento) { this.medicamento = medicamento; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public Integer getDiasCarencia() { return diasCarencia; }
    public void setDiasCarencia(Integer diasCarencia) { this.diasCarencia = diasCarencia; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
