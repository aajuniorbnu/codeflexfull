package com.codeflex.bilheteria.models;

import jakarta.persistence.*;

@Entity
public class Reserva {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer id;

  @Column(unique = true, nullable = false)
  public String token;

  @ManyToOne(optional = false)
  public Evento evento;

  public String assentos;
  public String status;
  public java.time.LocalDateTime expiraEm;
}
