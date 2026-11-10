package com.codeflex.bilheteria.service;

import com.codeflex.bilheteria.exception.ErroAplicacao;
import com.codeflex.bilheteria.models.Filme;
import com.codeflex.bilheteria.repositories.FilmeRepository;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import tools.jackson.databind.JsonNode;

@Service
public class TmdbService {
  private final FilmeRepository filmes;
  private final RestClient client;
  private final String token, key;
  private final AtomicBoolean executando = new AtomicBoolean();
  private volatile LocalDateTime ultimaSincronizacao;
  private volatile String situacao = "Ainda não sincronizado";

  public TmdbService(
      FilmeRepository filmes,
      @Value("${tmdb.url}") String url,
      @Value("${tmdb.token}") String token,
      @Value("${tmdb.key}") String key) {
    this.filmes = filmes;
    this.token = token;
    this.key = key;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  public boolean configurado() {
    return !token.isBlank() || !key.isBlank();
  }

  private JsonNode get(String path, Map<String, String> params) {
    if (!configurado())
      throw new ErroAplicacao(
          503, "catalogo_nao_configurado", "Configure TMDB_API_TOKEN ou TMDB_API_KEY no servidor");
    try {
      return client
          .get()
          .uri(
              builder -> {
                builder.path(path).queryParam("language", "pt-BR");
                params.forEach(builder::queryParam);
                if (token.isBlank()) builder.queryParam("api_key", key);
                return builder.build();
              })
          .headers(
              h -> {
                if (!token.isBlank()) h.setBearerAuth(token);
              })
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException e) {
      throw new ErroAplicacao(
          503,
          "catalogo_indisponivel",
          e.getStatusCode().value() == 429
              ? "Limite do TMDB atingido. A atualização será tentada no próximo ciclo."
              : "Não foi possível consultar o catálogo TMDB");
    } catch (RestClientException e) {
      throw new ErroAplicacao(
          503,
          "catalogo_indisponivel",
          "O catálogo TMDB não respondeu; os filmes já importados continuam disponíveis");
    }
  }

  public JsonNode buscar(String titulo) {
    return get("/search/movie", Map.of("query", titulo, "include_adult", "false")).path("results");
  }

  public Filme importar(Integer id, String categoria) {
    JsonNode d = get("/movie/" + id, Map.of("append_to_response", "videos,credits,release_dates"));
    var f = filmes.findByTmdbId(id).orElseGet(Filme::new);
    f.tmdbId = id;
    f.titulo = d.path("title").asText();
    f.sinopse = d.path("overview").asText();
    f.duracao = d.path("runtime").asInt(120);
    if (f.duracao <= 0) f.duracao = 120;
    f.posterPath = d.path("poster_path").asText("");
    f.backdropPath = d.path("backdrop_path").asText("");
    f.nota = d.path("vote_average").asDouble(0);
    f.categoria = categoria;
    f.demonstracao = false;
    f.atualizadoEm = LocalDateTime.now();
    String data = d.path("release_date").asText("");
    if (!data.isBlank()) f.lancamento = LocalDate.parse(data);
    var generos = new ArrayList<String>();
    for (var g : d.path("genres")) generos.add(g.path("name").asText());
    f.generos = String.join(", ", generos);
    var elenco = new ArrayList<String>();
    for (var g : d.path("credits").path("cast")) {
      if (elenco.size() == 5) break;
      elenco.add(g.path("name").asText());
    }
    f.elenco = String.join(", ", elenco);
    f.classificacao = "Não informada";
    for (var country : d.path("release_dates").path("results"))
      if ("BR".equals(country.path("iso_3166_1").asText()))
        for (var release : country.path("release_dates")) {
          String c = release.path("certification").asText();
          if (!c.isBlank()) {
            f.classificacao = c;
            break;
          }
        }
    f.trailer = null;
    for (var v : d.path("videos").path("results"))
      if ("YouTube".equals(v.path("site").asText())
          && "Trailer".equals(v.path("type").asText())
          && v.path("key").asText().matches("[A-Za-z0-9_-]{11}")) {
        f.trailer = v.path("key").asText();
        break;
      }
    return filmes.save(f);
  }

  public Map<String, Object> status() {
    return Map.of(
        "configurado",
        configurado(),
        "situacao",
        situacao,
        "ultima_sincronizacao",
        ultimaSincronizacao == null ? "" : ultimaSincronizacao.toString(),
        "executando",
        executando.get());
  }

  public Map<String, Object> sincronizar() {
    if (!executando.compareAndSet(false, true))
      throw new ErroAplicacao(
          409, "sincronizacao_em_andamento", "Já existe uma sincronização em andamento");
    try {
      var ids = new HashSet<Integer>();
      int count = 0;
      for (String tipo : List.of("now_playing", "upcoming")) {
        var lista = get("/movie/" + tipo, Map.of("region", "BR", "page", "1"));
        for (var item : lista.path("results")) {
          int id = item.path("id").asInt();
          if (!ids.add(id)) continue;
          importar(id, tipo.equals("now_playing") ? "EM_CARTAZ" : "EM_BREVE");
          count++;
          try {
            Thread.sleep(150);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErroAplicacao(
                503, "sincronizacao_interrompida", "Sincronização interrompida");
          }
        }
      }
      ultimaSincronizacao = LocalDateTime.now();
      situacao = "Atualizado: " + count + " filmes";
      return status();
    } catch (RuntimeException e) {
      situacao = "Falha na atualização; catálogo anterior preservado";
      throw e;
    } finally {
      executando.set(false);
    }
  }

  @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
  public void agendado() {
    if (configurado())
      try {
        sincronizar();
      } catch (RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Falha na sincronização TMDB; próxima tentativa no ciclo seguinte");
      }
  }
}
