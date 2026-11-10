package com.codeflex.bilheteria.models;

import jakarta.persistence.*;

@Entity
public class Evento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer id;

  public String nome;
  public java.time.LocalDate data;

  @Column(nullable = false)
  public java.math.BigDecimal preco;

  public Integer vagas;
  @ManyToOne public Filme filme;
  @ManyToOne public Sala sala;
  public java.time.LocalDateTime inicio;
  public java.time.LocalDateTime fim;
  public String idioma;
  public boolean ativa = true;
  public java.time.LocalDateTime criadoEm;

  @PrePersist
  public void criar() {
    if (criadoEm == null) criadoEm = java.time.LocalDateTime.now();
  }
}
