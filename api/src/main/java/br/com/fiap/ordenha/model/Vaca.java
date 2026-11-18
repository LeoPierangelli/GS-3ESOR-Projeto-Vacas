package br.com.fiap.ordenha.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "vaca")
public class Vaca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propriedade_id", nullable = false)
    private Propriedade propriedade;

    @Column(nullable = false, length = 20)
    private String brinco;

    @Column(length = 60)
    private String nome;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVaca status = StatusVaca.ATIVA;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Propriedade getPropriedade() { return propriedade; }
    public void setPropriedade(Propriedade propriedade) { this.propriedade = propriedade; }
    public String getBrinco() { return brinco; }
    public void setBrinco(String brinco) { this.brinco = brinco; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public StatusVaca getStatus() { return status; }
    public void setStatus(StatusVaca status) { this.status = status; }
}
