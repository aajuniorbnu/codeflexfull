package com.example.codeplaxpagamentosapi.models;

import com.example.codeplaxpagamentosapi.enums.StatusCobranca;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "cobrancas")
public class Cobranca {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Column(unique = true)
  private String chaveIdempotencia;

  @Column(nullable = false)
  private BigDecimal valor;

  @Column(length = 4, nullable = false)
  private String cartaoFinal;

  private Integer eventoId;
  private Integer quantidade;

  @Column(columnDefinition = "text")
  private String callbackUrl;

  @Column(nullable = false, updatable = false)
  private LocalDateTime criadoEm;

  @PrePersist
  public void aoCriar() {
    if (criadoEm == null) criadoEm = LocalDateTime.now();
  }

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private StatusCobranca status = StatusCobranca.PENDENTE;

  @Column() @Builder.Default private Integer tentativasWebhook = 0;

  @Column() @Builder.Default private Boolean webhookEntregue = false;
}
