package com.codeflex.bilheteria.repositories;

import com.codeflex.bilheteria.models.AssentoVendido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssentoVendidoRepository extends JpaRepository<AssentoVendido, Integer> {
  java.util.List<AssentoVendido> findByEventoId(Integer eventoId);
}
