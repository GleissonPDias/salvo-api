package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.domain.usuario.ModuloPermissao;
import com.salvo.salvo_api.entities.UsuarioPermissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UsuarioPermissaoRepository extends JpaRepository<UsuarioPermissao, Long> {

    @Query("select p.modulo from UsuarioPermissao p where p.usuarioId = :usuarioId")
    List<ModuloPermissao> buscarModulos(@Param("usuarioId") Long usuarioId);

    @Query("select p from UsuarioPermissao p where p.usuarioId in :usuarioIds")
    List<UsuarioPermissao> buscarPorUsuarios(@Param("usuarioIds") Collection<Long> usuarioIds);

    void deleteByUsuarioId(Long usuarioId);
}
