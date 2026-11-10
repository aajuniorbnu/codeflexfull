package com.codeflex.bilheteria.repositories;

import com.codeflex.bilheteria.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
  java.util.Optional<Reserva> findByToken(String token);

  java.util.List<Reserva> findByEventoId(Integer eventoId);
}
