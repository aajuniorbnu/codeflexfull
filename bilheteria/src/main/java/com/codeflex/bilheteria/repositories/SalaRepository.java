package com.codeflex.bilheteria.repositories;

import com.codeflex.bilheteria.models.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, Integer> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select s from Sala s where s.id=:id")
  java.util.Optional<Sala> bloquear(
      @org.springframework.data.repository.query.Param("id") Integer id);
}
