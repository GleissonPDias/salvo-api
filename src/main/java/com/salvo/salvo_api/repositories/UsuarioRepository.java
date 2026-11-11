package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.domain.usuario.PerfilUsuario;
import com.salvo.salvo_api.entities.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"oficina"})
    Optional<Usuario> findByAuthUserId(UUID authUserId);

    @EntityGraph(attributePaths = {"oficina"})
    List<Usuario> findByOficinaIdAndAtivoTrue(Long oficinaId);

    List<Usuario> findByOficinaIdAndAtivoTrueAndExecutaOsTrueOrderByLiderDescNomeAsc(Long oficinaId);

    Optional<Usuario> findByIdAndOficinaId(Long id, Long oficinaId);

    boolean existsByEmailIgnoreCase(String email);

    long countByOficinaIdAndPerfilAndAtivoTrue(Long oficinaId, PerfilUsuario perfil);

    @Query("""
        select u
          from Usuario u
         where u.oficina.id = :oficinaId
           and (:ativo is null or u.ativo = :ativo)
           and (:executaOs is null or u.executaOs = :executaOs)
           and (
                :busca is null
                or lower(u.nome) like lower(concat('%', :busca, '%'))
                or lower(u.email) like lower(concat('%', :busca, '%'))
           )
         order by u.ativo desc, u.nome asc
        """)
    List<Usuario> buscarEquipe(
        @Param("oficinaId") Long oficinaId,
        @Param("busca") String busca,
        @Param("ativo") Boolean ativo,
        @Param("executaOs") Boolean executaOs
    );
}
