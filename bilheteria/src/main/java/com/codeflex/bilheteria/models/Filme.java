package com.codeflex.bilheteria.models;

import jakarta.persistence.*;

@Entity
public class Filme {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer id;

  @Column(unique = true)
  public Integer tmdbId;

  public String titulo;

  @Column(length = 6000)
  public String sinopse;

  public Integer duracao;
  public String classificacao;
  public String generos;
  public String posterPath;
  public String backdropPath;
  public String trailer;

  @Column(length = 1200)
  public String elenco;

  public Double nota;
  public String categoria;
  public boolean demonstracao;
  public java.time.LocalDate lancamento;
  public java.time.LocalDateTime atualizadoEm;
}
