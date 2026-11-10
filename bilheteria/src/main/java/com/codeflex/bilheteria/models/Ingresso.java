package com.codeflex.bilheteria.models;

import jakarta.persistence.*;

@Entity
public class Ingresso {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer id;

  @Column(unique = true, nullable = false)
  public String token;

  @ManyToOne(optional = false)
  public Evento evento;

  @com.fasterxml.jackson.annotation.JsonIgnore public String cpf;
  public Integer quantidade;
  public Integer cobrancaId;
  public String status;
  public String assentos;
  public java.math.BigDecimal valor;
  public java.time.LocalDateTime criadoEm;

  @PrePersist
  public void criar() {
    if (criadoEm == null) criadoEm = java.time.LocalDateTime.now();
  }
}
