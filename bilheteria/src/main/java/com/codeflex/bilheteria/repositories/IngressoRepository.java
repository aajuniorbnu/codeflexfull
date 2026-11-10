package com.codeflex.bilheteria.repositories;

import com.codeflex.bilheteria.models.Ingresso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngressoRepository extends JpaRepository<Ingresso, Integer> {
  java.util.Optional<Ingresso> findByToken(String token);

  java.util.List<Ingresso> findByEventoId(Integer eventoId);
}
