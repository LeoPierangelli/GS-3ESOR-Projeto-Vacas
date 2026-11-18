package br.com.fiap.ordenha.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordenha")
public class Ordenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaca_id", nullable = false)
    private Vaca vaca;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Turno turno;

    @Column(nullable = false)
    private Double litros;

    @Column(nullable = false)
    private Double condutividade;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vaca getVaca() { return vaca; }
    public void setVaca(Vaca vaca) { this.vaca = vaca; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }
    public Double getLitros() { return litros; }
    public void setLitros(Double litros) { this.litros = litros; }
    public Double getCondutividade() { return condutividade; }
    public void setCondutividade(Double condutividade) { this.condutividade = condutividade; }
}
