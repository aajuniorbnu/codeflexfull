package com.codeflex.bilheteria.exception;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class TratadorDeErros {
  public record ErroResposta(String codigo, String mensagem, List<?> detalhes) {}

  @ExceptionHandler(ErroAplicacao.class)
  public ResponseEntity<ErroResposta> tratar(ErroAplicacao e) {
    return ResponseEntity.status(e.status)
        .body(new ErroResposta(e.codigo, e.getMessage(), List.of()));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ErroResposta> conflito() {
    return ResponseEntity.status(409)
        .body(
            new ErroResposta(
                "conflito", "O registro já existe ou o assento não está disponível", List.of()));
  }
}
