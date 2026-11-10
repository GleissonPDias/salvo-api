package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Box;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoxRepository extends JpaRepository<Box, Long> {
    Optional<Box> findByIdAndOficinaId(Long id, Long oficinaId);
}
