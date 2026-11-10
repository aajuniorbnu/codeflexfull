package com.codeflex.bilheteria;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.codeflex.bilheteria.exception.ErroAplicacao;
import com.codeflex.bilheteria.models.Filme;
import com.codeflex.bilheteria.repositories.FilmeRepository;
import com.codeflex.bilheteria.service.TmdbService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.*;

class TmdbServiceTests {
  HttpServer server;
  TmdbService service;
  FilmeRepository repo;
  String auth, path;
  int status;
  String response;

  @BeforeEach
  void setup() throws Exception {
    repo = mock(FilmeRepository.class);
    when(repo.findByTmdbId(anyInt())).thenReturn(Optional.empty());
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    status = 200;
    response = "{}";
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/",
        e -> {
          auth = e.getRequestHeaders().getFirst("Authorization");
          path = e.getRequestURI().toString();
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          e.getResponseHeaders().set("Content-Type", "application/json");
          e.sendResponseHeaders(status, bytes.length);
          e.getResponseBody().write(bytes);
          e.close();
        });
    server.start();
    service =
        new TmdbService(
            repo, "http://localhost:" + server.getAddress().getPort(), "token-de-teste", "");
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void importaDetalhesECertificacaoBrasileira() {
    response =
        """
{"id":1,"title":"Filme","overview":"Sinopse","runtime":110,"poster_path":"/poster.jpg","backdrop_path":"/back.jpg","vote_average":8.1,"release_date":"2026-11-01","genres":[{"name":"Aventura"}],"credits":{"cast":[{"name":"Ator"}]},"videos":{"results":[{"site":"YouTube","type":"Trailer","key":"abcdefghijk"}]},"release_dates":{"results":[{"iso_3166_1":"BR","release_dates":[{"certification":"12"}]}]}}
""";
    Filme f = service.importar(1, "EM_CARTAZ");
    assertEquals("12", f.classificacao);
    assertEquals("Ator", f.elenco);
    assertEquals("abcdefghijk", f.trailer);
    assertFalse(f.demonstracao);
    assertEquals("Bearer token-de-teste", auth);
    assertFalse(path.contains("token-de-teste"));
    verify(repo).save(f);
  }

  @Test
  void limite429MantemCache() {
    status = 429;
    var error = assertThrows(ErroAplicacao.class, () -> service.importar(1, "EM_CARTAZ"));
    assertEquals(503, error.status);
    verify(repo, never()).save(any());
  }

  @Test
  void pesquisaEncodaTitulo() {
    response = "{\"results\":[]}";
    service.buscar("Duna & cinema");
    assertTrue(path.contains("query="));
    assertTrue(path.contains("%26"));
  }
}
