package com.example.codeplaxpagamentosapi.service;

import com.example.codeplaxpagamentosapi.dto.bilheteria.EventoRespostaDto;
import com.example.codeplaxpagamentosapi.dto.cobrancas.CobrancaCriarDto;
import com.example.codeplaxpagamentosapi.enums.StatusCobranca;
import com.example.codeplaxpagamentosapi.exeptions.BilheteriaIndisponivelException;
import com.example.codeplaxpagamentosapi.exeptions.NaoEncontradoExeptions;
import com.example.codeplaxpagamentosapi.exeptions.RegraNegocioException;
import com.example.codeplaxpagamentosapi.exeptions.ValorDivergenteException;
import com.example.codeplaxpagamentosapi.models.Cobranca;
import com.example.codeplaxpagamentosapi.repositories.CobrancasRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class CobrancaService {
  private final CobrancasRepository repository;

  private final RestClient bilheteriaClient;

  public CobrancaService(
      CobrancasRepository repository, @Qualifier("bilheteriaClient") RestClient bilheteriaClient) {
    this.repository = repository;
    this.bilheteriaClient = bilheteriaClient;
  }

  @Transactional
  public Cobranca criar(CobrancaCriarDto dado) {
    return criar(dado, null);
  }

  @Transactional
  public Cobranca criar(CobrancaCriarDto dado, String chave) {
    if (chave != null) {
      var anterior = repository.findByChaveIdempotencia(chave);
      if (anterior.isPresent()) {
        var c = anterior.get();
        if (c.getValor().compareTo(dado.valor()) != 0
            || !java.util.Objects.equals(c.getEventoId(), dado.eventoId())
            || !java.util.Objects.equals(c.getQuantidade(), dado.quantidade())
            || !c.getCartaoFinal().equals(dado.cartaoFinal()))
          throw new com.example.codeplaxpagamentosapi.exeptions.RegraNegocioException(
              "Chave de pagamento já utilizada com outros dados");
        return c;
      }
    }

    var evento = obterEvento(dado.eventoId());
    var valorEsperado = evento.preco().multiply(BigDecimal.valueOf(dado.quantidade()));
    if (dado.valor().compareTo(valorEsperado) != 0) {
      throw new ValorDivergenteException();
    }

    var cobranca =
        Cobranca.builder()
            .valor(dado.valor())
            .cartaoFinal(dado.cartaoFinal())
            .eventoId(dado.eventoId())
            .quantidade(dado.quantidade())
            .callbackUrl(dado.callbackUrl())
            .status(
                "0000".equals(dado.cartaoFinal())
                    ? StatusCobranca.RECUSADA
                    : StatusCobranca.APROVADA)
            .tentativasWebhook(0)
            .webhookEntregue(false)
            .build();

    if (cobranca.getStatus() == StatusCobranca.APROVADA) cobranca.setChaveIdempotencia(chave);
    return this.repository.save(cobranca);
  }

  @Transactional
  public Cobranca obterPorId(int id) {
    var cobranca =
        repository
            .findById(id)
            .orElseThrow(() -> new NaoEncontradoExeptions("Cobrança não encontrada"));
    return cobranca;
  }

  private EventoRespostaDto obterEvento(Integer id) {
    try {
      var evento =
          bilheteriaClient.get().uri("/eventos/{id}", id).retrieve().body(EventoRespostaDto.class);
      if (evento == null || evento.preco() == null || evento.preco().signum() < 0) {
        throw new BilheteriaIndisponivelException();
      }
      return evento;
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        throw new RegraNegocioException("Evento não encontrado na Bilheteria");
      }
      throw new BilheteriaIndisponivelException();
    } catch (RestClientException exception) {
      throw new BilheteriaIndisponivelException();
    }
  }
}
