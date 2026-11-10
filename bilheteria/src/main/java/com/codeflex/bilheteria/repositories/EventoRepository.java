package com.codeflex.bilheteria.repositories;

import com.codeflex.bilheteria.models.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select e from Evento e where e.id=:id")
  java.util.Optional<Evento> bloquear(
      @org.springframework.data.repository.query.Param("id") Integer id);

  java.util.List<Evento> findByFilmeIdOrderByInicioAsc(Integer filmeId);

  java.util.List<Evento> findBySalaId(Integer salaId);
}
