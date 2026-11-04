package br.com.empresa.suporte.repository;

import br.com.empresa.suporte.model.Categoria;

import org.springframework.data.jpa.repository.*;

public interface CategoriaRepository
        extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {}
