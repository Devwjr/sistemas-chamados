package br.com.empresa.suporte.repository;
import br.com.empresa.suporte.model.Usuario;
import org.springframework.data.jpa.repository.*;
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    java.util.Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
