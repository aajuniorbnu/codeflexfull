package com.codeflex.bilheteria.exception;

public class ErroAplicacao extends RuntimeException {
  public final int status;
  public final String codigo;

  public ErroAplicacao(int status, String codigo, String mensagem) {
    super(mensagem);
    this.status = status;
    this.codigo = codigo;
  }

  public static ErroAplicacao naoEncontrado(String nome) {
    return new ErroAplicacao(404, "nao_encontrado", nome + " não encontrado");
  }

  public static ErroAplicacao regra(String mensagem) {
    return new ErroAplicacao(422, "regra_negocio", mensagem);
  }
}
