package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.ServicoPecaSugerida;
import com.salvo.salvo_api.entities.ServicoPecaSugeridaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServicoPecaSugeridaRepository
    extends JpaRepository<ServicoPecaSugerida, ServicoPecaSugeridaId> {

    List<ServicoPecaSugerida> findByServicoId(Long servicoId);

    boolean existsByServicoIdAndPecaId(Long servicoId, Long pecaId);

    @Modifying
    @Query("DELETE FROM ServicoPecaSugerida s WHERE s.servicoId = :servicoId")
    void deleteByServicoId(@Param("servicoId") Long servicoId);
}
