package com.codeflex.bilheteria.service;

import com.codeflex.bilheteria.dto.Dados.*;
import com.codeflex.bilheteria.exception.ErroAplicacao;
import com.codeflex.bilheteria.models.*;
import com.codeflex.bilheteria.repositories.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;

@Service
public class CompraService {
  private final EventoRepository eventos;
  private final ReservaRepository reservas;
  private final IngressoRepository ingressos;
  private final AssentoVendidoRepository vendidos;
  private final RestClient pagamentos;

  public CompraService(
      EventoRepository e,
      ReservaRepository r,
      IngressoRepository i,
      AssentoVendidoRepository v,
      @Qualifier("pagamentosClient") RestClient p) {
    eventos = e;
    reservas = r;
    ingressos = i;
    vendidos = v;
    pagamentos = p;
  }

  private Evento bloquear(Integer id) {
    var e = eventos.bloquear(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Evento"));
    if (!e.ativa
        || (e.inicio != null && !e.inicio.isAfter(LocalDateTime.now()))
        || e.data.isBefore(LocalDate.now()))
      throw ErroAplicacao.regra("Sessão encerrada ou indisponível");
    return e;
  }

  private Set<String> ocupados(Integer id) {
    var set = new HashSet<String>();
    for (var a : vendidos.findByEventoId(id)) set.add(a.assento);
    for (var r : reservas.findByEventoId(id))
      if ("ATIVA".equals(r.status) && r.expiraEm.isAfter(LocalDateTime.now()))
        set.addAll(List.of(r.assentos.split(",")));
    return set;
  }

  private List<String> planta(Evento e) {
    if (e.sala == null) return List.of();
    var result = new ArrayList<String>();
    for (int f = 0; f < e.sala.fileiras; f++)
      for (int c = 1; c <= e.sala.colunas; c++) result.add("" + (char) ('A' + f) + c);
    return result;
  }

  public Map<String, Object> mapa(Integer id) {
    var e = eventos.findById(id).orElseThrow(() -> ErroAplicacao.naoEncontrado("Evento"));
    return Map.of("evento", e, "assentos", planta(e), "ocupados", ocupados(id));
  }

  @Transactional
  public Reserva reservar(ReservaCriar dto) {
    var e = bloquear(dto.eventoId());
    var solicitados = new LinkedHashSet<>(dto.assentos());
    if (solicitados.size() != dto.assentos().size())
      throw ErroAplicacao.regra("Assentos repetidos");
    if (e.sala == null || !planta(e).containsAll(solicitados))
      throw ErroAplicacao.regra("Assento inválido para esta sala");
    if (solicitados.size() > e.vagas || solicitados.stream().anyMatch(ocupados(e.id)::contains))
      throw new ErroAplicacao(
          409, "assento_indisponivel", "Um dos assentos já foi reservado. Escolha outro lugar.");
    var r = new Reserva();
    r.token = UUID.randomUUID().toString();
    r.evento = e;
    r.assentos = String.join(",", solicitados);
    r.status = "ATIVA";
    r.expiraEm = LocalDateTime.now().plusMinutes(5);
    return reservas.save(r);
  }

  @Transactional
  public void liberar(String token) {
    var r = reservas.findByToken(token).orElseThrow(() -> ErroAplicacao.naoEncontrado("Reserva"));
    bloquear(r.evento.id);
    if ("ATIVA".equals(r.status)) {
      r.status = "CANCELADA";
      reservas.save(r);
    }
  }

  @Transactional
  public Ingresso checkout(Checkout dto) {
    var r =
        reservas
            .findByToken(dto.reservaToken())
            .orElseThrow(() -> ErroAplicacao.naoEncontrado("Reserva"));
    var e = bloquear(r.evento.id);
    var existente = ingressos.findByToken(r.token);
    if (existente.isPresent()) return existente.get();
    if (!"ATIVA".equals(r.status) || !r.expiraEm.isAfter(LocalDateTime.now()))
      throw ErroAplicacao.regra("Reserva expirada. Selecione os assentos novamente.");
    var seats = List.of(r.assentos.split(","));
    var ingresso = pagar(e, dto.cpf(), dto.cartaoFinal(), seats.size(), seats, r.token);
    r.status = "CONCLUIDA";
    reservas.save(r);
    return ingresso;
  }

  @Transactional
  public Ingresso comprar(Integer id, Compra dto) {
    var e = bloquear(id);
    List<String> seats = List.of();
    if (e.sala != null) {
      var ocupados = ocupados(id);
      seats =
          planta(e).stream().filter(s -> !ocupados.contains(s)).limit(dto.quantidade()).toList();
      if (seats.size() < dto.quantidade())
        throw ErroAplicacao.regra("Vagas insuficientes para o evento");
    }
    return pagar(
        e, dto.cpf(), dto.cartaoFinal(), dto.quantidade(), seats, UUID.randomUUID().toString());
  }

  private Ingresso pagar(
      Evento e, String cpf, String finalCartao, int quantidade, List<String> seats, String token) {
    if (e.vagas < quantidade) throw ErroAplicacao.regra("Vagas insuficientes para o evento");
    var valor = e.preco.multiply(BigDecimal.valueOf(quantidade));
    CobrancaResposta resposta;
    try {
      resposta =
          pagamentos
              .post()
              .uri("/cobrancas")
              .header("Idempotency-Key", token)
              .body(new CobrancaCriar(valor, finalCartao, e.id, quantidade))
              .retrieve()
              .body(CobrancaResposta.class);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().is4xxClientError())
        throw ErroAplicacao.regra("Pagamento não autorizado pelo serviço de Pagamentos");
      throw new ErroAplicacao(
          503, "pagamentos_indisponivel", "Pagamentos está indisponível. Tente novamente.");
    } catch (RestClientException ex) {
      throw new ErroAplicacao(
          503, "pagamentos_indisponivel", "Pagamentos não respondeu. Tente novamente.");
    }
    if (resposta == null || resposta.id() == null)
      throw new ErroAplicacao(503, "pagamentos_indisponivel", "Resposta de Pagamentos inválida");
    if ("RECUSADA".equals(resposta.status()))
      throw ErroAplicacao.regra("Pagamento recusado. Confira os quatro últimos dígitos do cartão.");
    if (!"APROVADA".equals(resposta.status()))
      throw new ErroAplicacao(503, "pagamentos_indisponivel", "Pagamento ainda não confirmado");
    var i = new Ingresso();
    i.token = token;
    i.evento = e;
    i.cpf = cpf;
    i.quantidade = quantidade;
    i.cobrancaId = resposta.id();
    i.status = "PAGO";
    i.assentos = String.join(",", seats);
    i.valor = valor;
    i = ingressos.save(i);
    for (String seat : seats) {
      var a = new AssentoVendido();
      a.evento = e;
      a.assento = seat;
      a.ingresso = i;
      vendidos.save(a);
    }
    e.vagas -= quantidade;
    eventos.save(e);
    return i;
  }

  public Ingresso ingresso(String token) {
    return ingressos.findByToken(token).orElseThrow(() -> ErroAplicacao.naoEncontrado("Ingresso"));
  }
}
