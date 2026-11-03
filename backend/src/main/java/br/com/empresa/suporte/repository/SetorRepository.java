package br.com.empresa.suporte.repository;
import br.com.empresa.suporte.model.Setor;
import org.springframework.data.jpa.repository.*;
public interface SetorRepository extends JpaRepository<Setor, Long>, JpaSpecificationExecutor<Setor> {

}
