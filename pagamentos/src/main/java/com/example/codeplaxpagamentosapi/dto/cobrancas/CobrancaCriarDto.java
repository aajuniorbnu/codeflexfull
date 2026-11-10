package com.example.codeplaxpagamentosapi.dto.cobrancas;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CobrancaCriarDto(
    @NotNull @Positive @Schema(example = "300.00", description = "Valor maior que zero")
        BigDecimal valor,
    @NotBlank
        @Pattern(regexp = "[0-9]{4}")
        @JsonProperty("cartao_final")
        @Schema(example = "4242", description = "Somente os 4 últimos dígitos do cartão")
        String cartaoFinal,
    @NotNull
        @JsonProperty("evento_id")
        @Schema(example = "1", description = "ID do evento na Bilheteria")
        Integer eventoId,
    @NotNull @Positive @Schema(example = "2", description = "Quantidade de ingressos, no mínimo 1")
        Integer quantidade,
    @JsonProperty("callback_url") @Schema(description = "Opcional na parte 2; não envia webhook")
        String callbackUrl) {}
