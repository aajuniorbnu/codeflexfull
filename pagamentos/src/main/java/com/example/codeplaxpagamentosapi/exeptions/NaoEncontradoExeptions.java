package com.example.codeplaxpagamentosapi.exeptions;

import org.springframework.http.HttpStatus;

public class NaoEncontradoExeptions extends ErroAplicacao {
  public NaoEncontradoExeptions(String mensagem) {
    super(HttpStatus.NOT_FOUND, "nao_encontrado", mensagem);
  }
}
