package com.codeflex.bilheteria.models;

import jakarta.persistence.*;

@Entity
public class Sala {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer id;

  @Column(unique = true, nullable = false)
  public String nome;

  public int fileiras;
  public int colunas;
  public String formato;
  public boolean ativa = true;
}
