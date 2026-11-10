package com.example.codeplaxpagamentosapi.exeptions;

import org.springframework.http.HttpStatus;

public class RegraNegocioException extends ErroAplicacao {
  public RegraNegocioException(String mensagem) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, "regra_negocio", mensagem);
  }
}
