package com.example.codeplaxpagamentosapi.exeptions;

import org.springframework.http.HttpStatus;

public class BilheteriaIndisponivelException extends ErroAplicacao {
  public BilheteriaIndisponivelException() {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        "bilheteria_indisponivel",
        "Não foi possível consultar o preço na Bilheteria");
  }
}
