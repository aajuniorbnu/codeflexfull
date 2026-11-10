package com.codeflex.bilheteria;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.codeflex.bilheteria.dto.Dados.ReservaCriar;
import com.codeflex.bilheteria.models.*;
import com.codeflex.bilheteria.repositories.*;
import com.codeflex.bilheteria.service.CompraService;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoCompraTests {
  static HttpServer server;
  static AtomicInteger calls = new AtomicInteger();
  static volatile String lastBody;

  @DynamicPropertySource
  static void remote(DynamicPropertyRegistry r) throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/cobrancas",
        e -> {
          calls.incrementAndGet();
          lastBody = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          String status = lastBody.contains("0000") ? "RECUSADA" : "APROVADA";
          byte[] body =
              ("{\"id\":7,\"status\":\"" + status + "\"}").getBytes(StandardCharsets.UTF_8);
          e.getResponseHeaders().set("Content-Type", "application/json");
          e.sendResponseHeaders(200, body.length);
          e.getResponseBody().write(body);
          e.close();
        });
    server.start();
    r.add("pagamentos.url", () -> "http://localhost:" + server.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    server.stop(0);
  }

  @Autowired MockMvc mvc;
  @Autowired EventoRepository eventos;
  @Autowired SalaRepository salas;
  @Autowired FilmeRepository filmes;
  @Autowired IngressoRepository ingressos;
  @Autowired ReservaRepository reservas;
  @Autowired AssentoVendidoRepository vendidos;
  @Autowired CompraService compras;
  Evento evento;
  Filme filme;
  Sala sala;

  @BeforeEach
  void setup() {
    vendidos.deleteAll();
    ingressos.deleteAll();
    reservas.deleteAll();
    eventos.deleteAll();
    salas.deleteAll();
    filmes.deleteAll();
    calls.set(0);
    filme = new Filme();
    filme.titulo = "Filme de teste";
    filme.duracao = 100;
    filme = filmes.save(filme);
    sala = new Sala();
    sala.nome = "Sala teste";
    sala.fileiras = 2;
    sala.colunas = 3;
    sala.formato = "2D";
    sala = salas.save(sala);
    evento = new Evento();
    evento.nome = filme.titulo;
    evento.filme = filme;
    evento.sala = sala;
    evento.data = LocalDate.now().plusDays(1);
    evento.inicio = evento.data.atTime(18, 0);
    evento.fim = evento.inicio.plusMinutes(120);
    evento.preco = new BigDecimal("30.00");
    evento.vagas = 6;
    evento = eventos.save(evento);
  }

  JsonNode json(String path, String body, int status) throws Exception {
    return new JsonMapper()
        .readTree(
            mvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().is(status))
                .andReturn()
                .getResponse()
                .getContentAsString());
  }

  String reserva(String assento) throws Exception {
    return json(
            "/reservas",
            "{\"evento_id\":" + evento.id + ",\"assentos\":[\"" + assento + "\"]}",
            200)
        .path("token")
        .asText();
  }

  String checkout(String token, String cartao) {
    return "{\"reserva_token\":\""
        + token
        + "\",\"cpf\":\"12345678901\",\"cartao_final\":\""
        + cartao
        + "\"}";
  }

  @Test
  void compraAprovadaBaixaVagasEConsultaPreco() throws Exception {
    var body =
        json(
            "/eventos/" + evento.id + "/comprar",
            "{\"cpf\":\"12345678901\",\"quantidade\":2,\"cartao_final\":\"4242\"}",
            200);
    assertEquals("PAGO", body.path("status").asText());
    assertEquals("12345678901", body.path("cpf").asText());
    assertEquals(7, body.path("cobranca_id").asInt());
    assertEquals(4, eventos.findById(evento.id).orElseThrow().vagas);
    assertEquals(2, vendidos.count());
    var sent = new JsonMapper().readTree(lastBody);
    assertEquals(evento.id, sent.path("evento_id").asInt());
    assertEquals(0, new BigDecimal("60").compareTo(sent.path("valor").decimalValue()));
  }

  @Test
  void recusaNaoCriaIngressoNemBaixaVagas() throws Exception {
    assertEquals(
        "regra_negocio",
        json(
                "/eventos/" + evento.id + "/comprar",
                "{\"cpf\":\"12345678901\",\"quantidade\":2,\"cartao_final\":\"0000\"}",
                422)
            .path("codigo")
            .asText());
    assertEquals(6, eventos.findById(evento.id).orElseThrow().vagas);
    assertEquals(0, ingressos.count());
  }

  @Test
  void semVagasNaoChamaPagamento() throws Exception {
    json(
        "/eventos/" + evento.id + "/comprar",
        "{\"cpf\":\"12345678901\",\"quantidade\":7,\"cartao_final\":\"4242\"}",
        422);
    assertEquals(0, calls.get());
  }

  @Test
  void cpfInvalidoRetorna400() throws Exception {
    json(
        "/eventos/" + evento.id + "/comprar",
        "{\"cpf\":\"abc\",\"quantidade\":1,\"cartao_final\":\"4242\"}",
        400);
    assertEquals(0, calls.get());
  }

  @Test
  void eventoAusente404() throws Exception {
    mvc.perform(get("/eventos/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("nao_encontrado"));
  }

  @Test
  void reservaBloqueiaAssentoELiberacaoPermiteNovaReserva() throws Exception {
    String token = reserva("A1");
    json("/reservas", "{\"evento_id\":" + evento.id + ",\"assentos\":[\"A1\"]}", 409);
    mvc.perform(delete("/reservas/" + token)).andExpect(status().isOk());
    assertNotEquals(token, reserva("A1"));
  }

  @Test
  void reservaExpiradaNaoCobra() throws Exception {
    String token = reserva("A1");
    var r = reservas.findByToken(token).orElseThrow();
    r.expiraEm = LocalDateTime.now().minusSeconds(1);
    reservas.save(r);
    json("/checkout", checkout(token, "4242"), 422);
    assertEquals(0, calls.get());
    assertNotEquals(token, reserva("A1"));
  }

  @Test
  void checkoutIdempotente() throws Exception {
    String token = reserva("A1");
    var first = json("/checkout", checkout(token, "4242"), 200);
    var second = json("/checkout", checkout(token, "4242"), 200);
    assertEquals(first.path("id"), second.path("id"));
    assertEquals(1, calls.get());
    assertEquals(1, ingressos.count());
    assertEquals(5, eventos.findById(evento.id).orElseThrow().vagas);
    mvc.perform(get("/ingressos/" + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cpf").doesNotExist());
  }

  @Test
  void concorrenciaSoReservaUmaVez() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    try {
      var start = new CountDownLatch(1);
      Callable<Boolean> task =
          () -> {
            start.await();
            try {
              compras.reservar(new ReservaCriar(evento.id, List.of("B2")));
              return true;
            } catch (RuntimeException e) {
              return false;
            }
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      start.countDown();
      assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void adminExigeChave() throws Exception {
    mvc.perform(get("/admin/relatorios")).andExpect(status().isUnauthorized());
    mvc.perform(get("/admin/relatorios").header("X-Admin-Token", "chave-de-teste"))
        .andExpect(status().isOk());
  }

  @Test
  void impedeSobreposicaoDeSessao() throws Exception {
    String body =
        "{\"filme_id\":"
            + filme.id
            + ",\"sala_id\":"
            + sala.id
            + ",\"inicio\":\""
            + evento.inicio.plusMinutes(20)
            + "\",\"preco\":30,\"idioma\":\"Dublado\"}";
    mvc.perform(
            post("/admin/sessoes")
                .header("X-Admin-Token", "chave-de-teste")
                .contentType("application/json")
                .content(body))
        .andExpect(status().is(422));
  }

  @Test
  void tmdbSemCredencial503() throws Exception {
    mvc.perform(post("/admin/tmdb/sincronizar").header("X-Admin-Token", "chave-de-teste"))
        .andExpect(status().isServiceUnavailable());
  }
}
