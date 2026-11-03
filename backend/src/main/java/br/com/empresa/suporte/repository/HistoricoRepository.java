package br.com.empresa.suporte.repository;
import br.com.empresa.suporte.model.Historico;
import org.springframework.data.jpa.repository.*;
public interface HistoricoRepository extends JpaRepository<Historico, Long>, JpaSpecificationExecutor<Historico> {
    org.springframework.data.domain.Page<Historico> findByChamadoId(Long id, org.springframework.data.domain.Pageable page);
}
