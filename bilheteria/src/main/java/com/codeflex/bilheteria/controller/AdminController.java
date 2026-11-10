package com.codeflex.bilheteria.controller;

import com.codeflex.bilheteria.dto.Dados.*;
import com.codeflex.bilheteria.exception.ErroAplicacao;
import com.codeflex.bilheteria.models.*;
import com.codeflex.bilheteria.repositories.*;
import com.codeflex.bilheteria.service.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
  private final SalaRepository salas;
  private final EventoRepository eventos;
  private final IngressoRepository ingressos;
  private final FilmeRepository filmes;
  private final EventoService service;
  private final TmdbService tmdb;

  public AdminController(
      SalaRepository s,
      EventoRepository e,
      IngressoRepository i,
      FilmeRepository f,
      EventoService service,
      TmdbService tmdb) {
    salas = s;
    eventos = e;
    ingressos = i;
    filmes = f;
    this.service = service;
    this.tmdb = tmdb;
  }

  @GetMapping("/salas")
  public List<Sala> salas() {
    return salas.findAll();
  }

  @PostMapping("/salas")
  public Sala criar(@RequestBody @Valid SalaCriar d) {
    var s = new Sala();
    s.nome = d.nome();
    s.fileiras = d.fileiras();
    s.colunas = d.colunas();
    s.formato = d.formato();
    return salas.save(s);
  }

  @PutMapping("/salas/{id}")
  @Transactional
  public Sala atualizar(@PathVariable Integer id, @RequestBody @Valid SalaCriar d) {
    var s = salas.bloquear(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Sala"));
    if (!eventos.findBySalaId(id).isEmpty())
      throw ErroAplicacao.regra("Sala com sessões não pode ter sua planta alterada");
    s.nome = d.nome();
    s.fileiras = d.fileiras();
    s.colunas = d.colunas();
    s.formato = d.formato();
    return salas.save(s);
  }

  @DeleteMapping("/salas/{id}")
  @Transactional
  public void remover(@PathVariable Integer id) {
    var s = salas.bloquear(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Sala"));
    if (eventos.findBySalaId(id).stream().anyMatch(e -> e.ativa))
      throw ErroAplicacao.regra("Sala com sessões ativas não pode ser removida");
    s.ativa = false;
    salas.save(s);
  }

  @GetMapping("/sessoes")
  public List<Evento> sessoes() {
    return eventos.findAll();
  }

  @PostMapping("/sessoes")
  public Evento sessao(@RequestBody @Valid SessaoCriar d) {
    return service.sessao(d, null);
  }

  @PutMapping("/sessoes/{id}")
  public Evento atualizar(@PathVariable Integer id, @RequestBody @Valid SessaoCriar d) {
    return service.sessao(d, id);
  }

  @DeleteMapping("/sessoes/{id}")
  public void excluir(@PathVariable Integer id) {
    service.excluir(id);
  }

  @GetMapping("/tmdb/status")
  public Map<String, Object> tmdbStatus() {
    return tmdb.status();
  }

  @PostMapping("/tmdb/sincronizar")
  public Map<String, Object> sincronizar() {
    return tmdb.sincronizar();
  }

  @GetMapping("/tmdb/buscar")
  public Object buscar(@RequestParam String titulo) {
    return tmdb.buscar(titulo);
  }

  @PostMapping("/tmdb/importar/{id}")
  public Filme importar(@PathVariable Integer id) {
    return tmdb.importar(id, "EM_CARTAZ");
  }

  @GetMapping("/relatorios")
  public Map<String, Object> relatorio() {
    var lista = ingressos.findAll();
    var total = lista.stream().map(i -> i.valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    var quantidade = lista.stream().mapToInt(i -> i.quantidade).sum();
    var porFilme = new LinkedHashMap<String, Integer>();
    var porDia = new TreeMap<String, BigDecimal>();
    for (var i : lista) {
      porFilme.merge(i.evento.nome, i.quantidade, Integer::sum);
      porDia.merge(i.criadoEm.toLocalDate().toString(), i.valor, BigDecimal::add);
    }
    return Map.of(
        "faturamento",
        total,
        "ingressos",
        quantidade,
        "pedidos",
        lista.size(),
        "por_filme",
        porFilme,
        "por_dia",
        porDia);
  }
}
