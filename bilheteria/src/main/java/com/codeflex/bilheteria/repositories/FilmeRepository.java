package com.codeflex.bilheteria.repositories;

import com.codeflex.bilheteria.models.Filme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmeRepository extends JpaRepository<Filme, Integer> {
  java.util.Optional<Filme> findByTmdbId(Integer id);
}
