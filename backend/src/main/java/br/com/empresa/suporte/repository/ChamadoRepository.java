package br.com.empresa.suporte.repository;
import br.com.empresa.suporte.model.Chamado;
import org.springframework.data.jpa.repository.*;
public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {
    boolean existsByResponsavelIdAndStatusIn(Long id, java.util.Collection<br.com.empresa.suporte.model.Enums.Status> estados);
}
