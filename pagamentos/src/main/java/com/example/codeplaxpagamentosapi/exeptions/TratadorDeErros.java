package com.example.codeplaxpagamentosapi.exeptions;

import com.example.codeplaxpagamentosapi.dto.cobrancas.ErroResposta;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

  @ExceptionHandler(ErroAplicacao.class)
  public ResponseEntity<ErroResposta> tratarErroAplicacap(ErroAplicacao exeption) {
    return ResponseEntity.status(exeption.getStatus())
        .body(new ErroResposta(exeption.getCodigo(), exeption.getMessage(), List.of()));
  }
}
