package com.example.codeplaxpagamentosapi.exeptions;

import org.springframework.http.HttpStatus;

public class ValorDivergenteException extends ErroAplicacao {
  public ValorDivergenteException() {
    super(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "valor_divergente",
        "Valor diferente do preço do evento multiplicado pela quantidade");
  }
}
