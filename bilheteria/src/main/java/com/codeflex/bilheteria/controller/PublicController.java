package com.codeflex.bilheteria.controller;

import com.codeflex.bilheteria.dto.Dados.*;
import com.codeflex.bilheteria.exception.ErroAplicacao;
import com.codeflex.bilheteria.models.*;
import com.codeflex.bilheteria.repositories.*;
import com.codeflex.bilheteria.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Bilheteria", description = "Catálogo, sessões, reservas e ingressos")
public class PublicController {
  private final EventoService eventos;
  private final CompraService compras;
  private final FilmeRepository filmes;

  public PublicController(EventoService e, CompraService c, FilmeRepository f) {
    eventos = e;
    compras = c;
    filmes = f;
  }

  @GetMapping("/filmes")
  @Operation(summary = "Consultar catálogo armazenado localmente")
  public List<Filme> filmes() {
    return filmes.findAll(org.springframework.data.domain.Sort.by("id"));
  }

  @GetMapping("/filmes/{id}")
  public Filme filme(@PathVariable Integer id) {
    return filmes.findById(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Filme"));
  }

  @GetMapping("/filmes/{id}/sessoes")
  public List<Evento> sessoes(@PathVariable Integer id) {
    return eventos.sessoes(id);
  }

  @GetMapping("/eventos")
  public List<Evento> eventos() {
    return eventos.listar();
  }

  @PostMapping("/eventos")
  @Operation(summary = "Criar evento (contrato acadêmico v1)")
  public Evento criar(@RequestBody @Valid EventoCriar d) {
    return eventos.criar(d);
  }

  @GetMapping("/eventos/{id}")
  @Operation(summary = "Consultar evento e preço para Pagamentos")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Evento encontrado"),
    @ApiResponse(responseCode = "404", description = "nao_encontrado")
  })
  public Evento evento(@PathVariable Integer id) {
    return eventos.obter(id);
  }

  @PostMapping("/eventos/{id}/comprar")
  @Operation(summary = "Comprar ingresso (contrato v1)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ingresso PAGO"),
    @ApiResponse(responseCode = "400", description = "Entrada inválida"),
    @ApiResponse(responseCode = "404", description = "Evento inexistente"),
    @ApiResponse(
        responseCode = "422",
        description = "regra_negocio: sem vagas ou pagamento recusado"),
    @ApiResponse(responseCode = "503", description = "pagamentos_indisponivel")
  })
  public IngressoResposta comprar(@PathVariable Integer id, @RequestBody @Valid Compra d) {
    return IngressoResposta.de(compras.comprar(id, d));
  }

  @GetMapping("/eventos/{id}/assentos")
  public Map<String, Object> assentos(@PathVariable Integer id) {
    return compras.mapa(id);
  }

  @PostMapping("/reservas")
  @Operation(summary = "Reservar assentos por cinco minutos")
  public Reserva reservar(@RequestBody @Valid ReservaCriar d) {
    return compras.reservar(d);
  }

  @DeleteMapping("/reservas/{token}")
  public void liberar(@PathVariable String token) {
    compras.liberar(token);
  }

  @PostMapping("/checkout")
  @Operation(summary = "Confirmar reserva com pagamento simulado")
  public Ingresso checkout(@RequestBody @Valid Checkout d) {
    return compras.checkout(d);
  }

  @GetMapping("/ingressos/{token}")
  @Operation(summary = "Consultar ingresso pelo código privado de confirmação")
  public Ingresso ingresso(@PathVariable String token) {
    return compras.ingresso(token);
  }
}
