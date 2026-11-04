package br.com.empresa.suporte.repository;

import br.com.empresa.suporte.model.Comentario;

import org.springframework.data.jpa.repository.*;

public interface ComentarioRepository
        extends JpaRepository<Comentario, Long>, JpaSpecificationExecutor<Comentario> {
    org.springframework.data.domain.Page<Comentario> findByChamadoId(
            Long id, org.springframework.data.domain.Pageable page);

    org.springframework.data.domain.Page<Comentario> findByChamadoIdAndVisibilidade(
            Long id,
            br.com.empresa.suporte.model.Enums.Visibilidade visibilidade,
            org.springframework.data.domain.Pageable page);
}
