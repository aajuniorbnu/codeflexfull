package com.example.codeplaxpagamentosapi;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.codeplaxpagamentosapi.config.ClienteHttpConfig;
import com.example.codeplaxpagamentosapi.controllers.CobrancaController;
import com.example.codeplaxpagamentosapi.exeptions.TratadorDeErros;
import com.example.codeplaxpagamentosapi.models.Cobranca;
import com.example.codeplaxpagamentosapi.repositories.CobrancasRepository;
import com.example.codeplaxpagamentosapi.service.CobrancaService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CobrancaController.class)
@Import({CobrancaService.class, ClienteHttpConfig.class, TratadorDeErros.class})
class PagamentosParte2Tests {
  static HttpServer servidor;
  static volatile int respostaStatus = 200;
  static volatile String resposta = "{\"id\":1,\"preco\":150.00,\"nome\":\"Show\"}";
  static volatile long atraso;
  static volatile String caminho;

  @Autowired MockMvc mvc;
  @MockitoBean CobrancasRepository repository;

  @DynamicPropertySource
  static void configurarBilheteria(DynamicPropertyRegistry registry) throws Exception {
    servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    servidor.createContext(
        "/eventos/",
        exchange -> {
          caminho = exchange.getRequestURI().getPath();
          try {
            if (atraso > 0) Thread.sleep(atraso);
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(respostaStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    servidor.start();
    registry.add("bilheteria.url", () -> "http://localhost:" + servidor.getAddress().getPort());
  }

  @AfterAll
  static void encerrar() {
    servidor.stop(0);
  }

  @BeforeEach
  void preparar() {
    respostaStatus = 200;
    resposta = "{\"id\":1,\"preco\":150.00,\"nome\":\"Show\"}";
    atraso = 0;
    caminho = null;
    when(repository.save(any()))
        .thenAnswer(
            call -> {
              Cobranca cobranca = call.getArgument(0);
              cobranca.setId(1);
              cobranca.aoCriar();
              return cobranca;
            });
  }

  String corpo(String cartao, String valor) {
    return "{\"valor\":"
        + valor
        + ",\"cartao_final\":\""
        + cartao
        + "\",\"evento_id\":1,\"quantidade\":2}";
  }

  @Test
  void aprovarComPrecoCorretoMesmoComEscalasDiferentes() throws Exception {
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("4242", "300.0")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADA"))
        .andExpect(jsonPath("$.evento_id").value(1))
        .andExpect(jsonPath("$.quantidade").value(2))
        .andExpect(jsonPath("$.cartao_final").value("4242"))
        .andExpect(jsonPath("$.criado_em").exists())
        .andExpect(jsonPath("$.webhook_entregue").value(false));
    Assertions.assertEquals("/eventos/1", caminho);
  }

  @Test
  void recusarCartao0000() throws Exception {
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("0000", "300")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECUSADA"));
  }

  @Test
  void naoSalvarValorDivergente() throws Exception {
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("4242", "299.99")))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.codigo").value("valor_divergente"))
        .andExpect(jsonPath("$.detalhes").isEmpty());
    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"valor\":300,\"cartao_final\":\"4242\",\"quantidade\":2}",
        "{\"valor\":300,\"cartao_final\":\"4242\",\"evento_id\":1}",
        "{\"valor\":300,\"cartao_final\":\"4242\",\"evento_id\":1,\"quantidade\":0}",
        "{\"valor\":300,\"cartao_final\":\"4242\",\"evento_id\":1,\"quantidade\":-1}"
      })
  void validarAntesDeConsultarBilheteria(String json) throws Exception {
    mvc.perform(post("/cobrancas").contentType("application/json").content(json))
        .andExpect(status().isBadRequest());
    Assertions.assertNull(caminho);
    verify(repository, never()).save(any());
  }

  @Test
  void eventoInexistente() throws Exception {
    respostaStatus = 404;
    resposta = "{}";
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("4242", "300")))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.codigo").value("regra_negocio"));
    verify(repository, never()).save(any());
  }

  @Test
  void falhaNaBilheteria() throws Exception {
    respostaStatus = 500;
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("4242", "300")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.codigo").value("bilheteria_indisponivel"));
    verify(repository, never()).save(any());
  }

  @Test
  void respostaSemPreco() throws Exception {
    resposta = "{\"id\":1}";
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("4242", "300")))
        .andExpect(status().isServiceUnavailable());
    verify(repository, never()).save(any());
  }

  @Test
  void timeoutNaBilheteria() throws Exception {
    atraso = 3500;
    mvc.perform(post("/cobrancas").contentType("application/json").content(corpo("4242", "300")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.codigo").value("bilheteria_indisponivel"));
    verify(repository, never()).save(any());
    // Aguarda a requisição simulada terminar antes do próximo teste.
    Thread.sleep(600);
  }

  @Test
  void consultarCobrancaAusente() throws Exception {
    when(repository.findById(99)).thenReturn(Optional.empty());
    mvc.perform(get("/cobrancas/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensagem").value("Cobrança não encontrada"));
  }
}
