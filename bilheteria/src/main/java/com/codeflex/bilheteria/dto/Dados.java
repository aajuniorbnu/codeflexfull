package com.codeflex.bilheteria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class Dados {
  private Dados() {}

  public record EventoCriar(
      @NotBlank String nome,
      @NotNull @FutureOrPresent LocalDate data,
      @NotNull @Positive BigDecimal preco,
      @NotNull @PositiveOrZero Integer vagas) {}

  public record Compra(
      @NotBlank @Pattern(regexp = "[0-9]{11}") String cpf,
      @NotNull @Min(1) Integer quantidade,
      @NotBlank @Pattern(regexp = "[0-9]{4}") String cartaoFinal) {}

  public record ReservaCriar(
      @NotNull Integer eventoId,
      @NotEmpty @Size(max = 8) List<@Pattern(regexp = "[A-Z][0-9]{1,2}") String> assentos) {}

  public record Checkout(
      @NotBlank String reservaToken,
      @NotBlank @Pattern(regexp = "[0-9]{11}") String cpf,
      @NotBlank
          @Pattern(regexp = "[0-9]{4}")
          @Schema(example = "4242", description = "Pagamento simulado: 0000 recusa")
          String cartaoFinal) {}

  public record SalaCriar(
      @NotBlank @Size(max = 80) String nome,
      @Min(1) @Max(20) int fileiras,
      @Min(1) @Max(20) int colunas,
      @NotBlank String formato) {}

  public record SessaoCriar(
      @NotNull Integer filmeId,
      @NotNull Integer salaId,
      @NotNull @Future LocalDateTime inicio,
      @NotNull @Positive BigDecimal preco,
      @NotBlank String idioma) {}

  public record CobrancaCriar(
      BigDecimal valor,
      @com.fasterxml.jackson.annotation.JsonProperty("cartao_final") String cartaoFinal,
      @com.fasterxml.jackson.annotation.JsonProperty("evento_id") Integer eventoId,
      Integer quantidade) {}

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record CobrancaResposta(Integer id, String status) {}

  public record IngressoResposta(
      Integer id,
      com.codeflex.bilheteria.models.Evento evento,
      String cpf,
      Integer quantidade,
      Integer cobrancaId,
      String status,
      LocalDateTime criadoEm,
      String token,
      String assentos,
      BigDecimal valor) {
    public static IngressoResposta de(com.codeflex.bilheteria.models.Ingresso i) {
      return new IngressoResposta(
          i.id,
          i.evento,
          i.cpf,
          i.quantidade,
          i.cobrancaId,
          i.status,
          i.criadoEm,
          i.token,
          i.assentos,
          i.valor);
    }
  }
}
