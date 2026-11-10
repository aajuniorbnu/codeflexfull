package com.example.codeplaxpagamentosapi.dto.cobrancas;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ErroResposta(
    @Schema(example = "nao_encontrado") String codigo,
    @Schema(example = "cobranca não encontrada") String mensagem,
    List<?> detalhes) {}
