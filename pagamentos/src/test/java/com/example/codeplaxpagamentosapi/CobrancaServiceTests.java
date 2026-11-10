package com.example.codeplaxpagamentosapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.codeplaxpagamentosapi.dto.cobrancas.CobrancaCriarDto;
import com.example.codeplaxpagamentosapi.enums.StatusCobranca;
import com.example.codeplaxpagamentosapi.exeptions.ValorDivergenteException;
import com.example.codeplaxpagamentosapi.repositories.CobrancasRepository;
import com.example.codeplaxpagamentosapi.service.CobrancaService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CobrancaServiceTests {
  private final CobrancasRepository repository = mock(CobrancasRepository.class);
  private final RestClient.Builder builder = RestClient.builder().baseUrl("http://bilheteria");
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  private final RestClient bilheteria = builder.build();
  private final CobrancaService service = new CobrancaService(repository, bilheteria);

  @ParameterizedTest
  @CsvSource({"4242,APROVADA", "0000,RECUSADA"})
  void conferirPrecoAntesDeSalvar(String cartao, StatusCobranca status) {
    servidor
        .expect(requestTo("http://bilheteria/eventos/1"))
        .andRespond(withSuccess("{\"id\":1,\"preco\":150.00}", MediaType.APPLICATION_JSON));
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var resultado = service.criar(new CobrancaCriarDto(new BigDecimal("300"), cartao, 1, 2, null));
    assertEquals(status, resultado.getStatus());
    assertEquals(1, resultado.getEventoId());
    assertEquals(2, resultado.getQuantidade());
    servidor.verify();
    verify(repository).save(resultado);
  }

  @Test
  void impedirPersistenciaDeValorDivergente() {
    servidor
        .expect(requestTo("http://bilheteria/eventos/1"))
        .andRespond(withSuccess("{\"id\":1,\"preco\":150.00}", MediaType.APPLICATION_JSON));
    var erro =
        assertThrows(
            ValorDivergenteException.class,
            () ->
                service.criar(new CobrancaCriarDto(new BigDecimal("299.99"), "4242", 1, 2, null)));
    assertEquals("valor_divergente", erro.getCodigo());
    verifyNoInteractions(repository);
  }

  @Test
  void reutilizarPagamentoAprovadoPelaChave() {
    var anterior = new com.example.codeplaxpagamentosapi.models.Cobranca();
    anterior.setId(7);
    anterior.setValor(new BigDecimal("300.00"));
    anterior.setCartaoFinal("4242");
    anterior.setEventoId(1);
    anterior.setQuantidade(2);
    anterior.setStatus(StatusCobranca.APROVADA);
    when(repository.findByChaveIdempotencia("reserva-teste"))
        .thenReturn(java.util.Optional.of(anterior));
    var resultado =
        service.criar(
            new CobrancaCriarDto(new BigDecimal("300"), "4242", 1, 2, null), "reserva-teste");
    assertEquals(7, resultado.getId());
    verify(repository, never()).save(any());
  }
}
