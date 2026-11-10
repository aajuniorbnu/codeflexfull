package com.codeflex.bilheteria.service;

import com.codeflex.bilheteria.dto.Dados.*;
import com.codeflex.bilheteria.exception.ErroAplicacao;
import com.codeflex.bilheteria.models.*;
import com.codeflex.bilheteria.repositories.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {
  private final EventoRepository eventos;
  private final FilmeRepository filmes;
  private final SalaRepository salas;
  private final IngressoRepository ingressos;
  private final ReservaRepository reservas;

  public EventoService(
      EventoRepository e,
      FilmeRepository f,
      SalaRepository s,
      IngressoRepository i,
      ReservaRepository r) {
    eventos = e;
    filmes = f;
    salas = s;
    ingressos = i;
    reservas = r;
  }

  public Evento obter(Integer id) {
    return eventos.findById(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Evento"));
  }

  public List<Evento> listar() {
    return eventos.findAll().stream().filter(e -> e.ativa).toList();
  }

  public List<Evento> sessoes(Integer filmeId) {
    return eventos.findByFilmeIdOrderByInicioAsc(filmeId).stream()
        .filter(e -> e.ativa && e.inicio != null && e.inicio.isAfter(LocalDateTime.now()))
        .toList();
  }

  @Transactional
  public Evento criar(EventoCriar dto) {
    var e = new Evento();
    e.nome = dto.nome();
    e.data = dto.data();
    e.preco = dto.preco();
    e.vagas = dto.vagas();
    return eventos.save(e);
  }

  @Transactional
  public Evento sessao(SessaoCriar dto, Integer id) {
    var sala = salas.bloquear(dto.salaId()).orElseThrow(() -> ErroAplicacao.naoEncontrado("Sala"));
    if (!sala.ativa) throw ErroAplicacao.regra("Sala desativada");
    var filme =
        filmes.findById(dto.filmeId()).orElseThrow(() -> ErroAplicacao.naoEncontrado("Filme"));
    var fim = dto.inicio().plusMinutes((filme.duracao == null ? 120 : filme.duracao) + 20);
    for (var outra : eventos.findBySalaId(sala.id))
      if (!Objects.equals(outra.id, id)
          && outra.ativa
          && outra.inicio != null
          && outra.inicio.isBefore(fim)
          && outra.fim.isAfter(dto.inicio()))
        throw ErroAplicacao.regra(
            "A sala já tem uma sessão nesse horário (inclui 20 minutos de intervalo)");
    var e =
        id == null
            ? new Evento()
            : eventos.bloquear(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Evento"));
    if (id != null) verificarReservas(id);
    if (id != null && !ingressos.findByEventoId(id).isEmpty())
      throw ErroAplicacao.regra("Sessão com ingressos vendidos não pode ser alterada");
    e.nome = filme.titulo;
    e.filme = filme;
    e.sala = sala;
    e.inicio = dto.inicio();
    e.fim = fim;
    e.data = dto.inicio().toLocalDate();
    e.preco = dto.preco();
    e.idioma = dto.idioma();
    e.vagas = sala.fileiras * sala.colunas;
    e.ativa = true;
    return eventos.save(e);
  }

  @Transactional
  public void excluir(Integer id) {
    var e = eventos.bloquear(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Evento"));
    verificarReservas(id);
    if (!ingressos.findByEventoId(id).isEmpty())
      throw ErroAplicacao.regra("Sessão com ingressos vendidos não pode ser excluída");
    e.ativa = false;
    eventos.save(e);
  }

  private void verificarReservas(Integer id) {
    if (reservas.findByEventoId(id).stream()
        .anyMatch(r -> "ATIVA".equals(r.status) && r.expiraEm.isAfter(LocalDateTime.now())))
      throw ErroAplicacao.regra("Sessão com reservas ativas não pode ser alterada");
  }
}
