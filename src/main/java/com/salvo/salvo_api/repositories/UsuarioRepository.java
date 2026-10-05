package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"oficina"})
    Optional<Usuario> findByAuthUserId(UUID authUserId);

    @EntityGraph(attributePaths = {"oficina"})
    List<Usuario> findByOficinaIdAndAtivoTrue(Long oficinaId);
}
