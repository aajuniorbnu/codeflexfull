package com.example.codeplaxpagamentosapi.controllers;

import com.example.codeplaxpagamentosapi.dto.cobrancas.CobrancaCriarDto;
import com.example.codeplaxpagamentosapi.dto.cobrancas.ErroResposta;
import com.example.codeplaxpagamentosapi.models.Cobranca;
import com.example.codeplaxpagamentosapi.service.CobrancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cobrancas")
@Tag(name = "cobrancas", description = "Pagamento")
public class CobrancaController {
  private final CobrancaService service;

  public CobrancaController(CobrancaService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(
      summary = "Criar cobrança",
      description = "Confere preço × quantidade na Bilheteria. Final 0000 é recusado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cobrança criada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
    @ApiResponse(
        responseCode = "422",
        description = "valor_divergente: valor incorreto; regra_negocio: evento inexistente",
        content = @Content(schema = @Schema(implementation = ErroResposta.class))),
    @ApiResponse(
        responseCode = "503",
        description = "bilheteria_indisponivel",
        content = @Content(schema = @Schema(implementation = ErroResposta.class)))
  })
  public Cobranca criar(
      @RequestBody @Valid CobrancaCriarDto dados,
      @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
    return service.criar(dados, chave);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar cobrança")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cobrança encontrada"),
    @ApiResponse(
        responseCode = "404",
        description = "Cobrança não encontrada",
        content = @Content(schema = @Schema(implementation = ErroResposta.class)))
  })
  public Cobranca obterPorID(@PathVariable Integer id) {
    return service.obterPorId(id);
  }
}
