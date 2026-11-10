package com.example.codeplaxpagamentosapi.repositories;

import com.example.codeplaxpagamentosapi.models.Cobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CobrancasRepository extends JpaRepository<Cobranca, Integer> {
  java.util.Optional<com.example.codeplaxpagamentosapi.models.Cobranca> findByChaveIdempotencia(
      String chave);
}
