package com.codeflex.bilheteria.models;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "assento"}))
public class AssentoVendido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer id;

  @ManyToOne(optional = false)
  public Evento evento;

  @Column(nullable = false)
  public String assento;

  @ManyToOne(optional = false)
  public Ingresso ingresso;
}
