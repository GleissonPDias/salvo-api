package com.salvo.salvo_api.repositories;

import com.salvo.salvo_api.entities.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriaRepository extends
    JpaRepository<Auditoria, Long>,
    JpaSpecificationExecutor<Auditoria> {
}
