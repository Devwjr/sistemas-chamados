package br.com.empresa.suporte.repository;

import br.com.empresa.suporte.model.Chamado;
import br.com.empresa.suporte.model.Enums.Status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ChamadoRepository
        extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

    boolean existsByResponsavelIdAndStatusIn(Long id, Collection<Status> estados);

    @Query("""
            select c from Chamado c
            join fetch c.categoria
            join fetch c.solicitante
            join fetch c.setorOrigem
            left join fetch c.responsavel
            where c.id = :id
            """)
    Optional<Chamado> findDetalheById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"categoria", "solicitante", "setorOrigem", "responsavel"})
    Page<Chamado> findAll(Specification<Chamado> spec, Pageable pageable);
}
