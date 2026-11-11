package br.com.empresa.suporte;

import static org.junit.jupiter.api.Assertions.*;

import br.com.empresa.suporte.model.Chamado;
import br.com.empresa.suporte.model.Categoria;
import br.com.empresa.suporte.model.Usuario;
import br.com.empresa.suporte.repository.ChamadoRepository;
import br.com.empresa.suporte.repository.CategoriaRepository;
import br.com.empresa.suporte.repository.UsuarioRepository;

import org.hibernate.Hibernate;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_INTEGRATION", matches = "true")
class PostgresIntegrationTest {
    @Autowired UsuarioRepository users;
    @Autowired CategoriaRepository categories;
    @Autowired ChamadoRepository tickets;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void migrationsBootstrapAndTicketQueriesLoadReferences() {
        Usuario admin = users.findByEmail("admin@empresa.local").orElseThrow();
        Categoria category = categories.findAll().getFirst();
        Chamado ticket = newTicket(admin, category);
        tickets.saveAndFlush(ticket);
        Long id = ticket.id;
        entityManager.clear();

        Chamado detail = tickets.findDetalheById(id).orElseThrow();
        assertTrue(Hibernate.isInitialized(detail.categoria));
        assertTrue(Hibernate.isInitialized(detail.solicitante));
        assertTrue(Hibernate.isInitialized(detail.setorOrigem));
        assertEquals(category.nome, detail.categoria.nome);

        entityManager.clear();
        Chamado listed = tickets.findAll((root, query, builder) -> builder.equal(root.get("id"), id), PageRequest.of(0, 10))
                .getContent().getFirst();
        assertTrue(Hibernate.isInitialized(listed.categoria));
        assertTrue(Hibernate.isInitialized(listed.solicitante));
        assertEquals(admin.nome, listed.solicitante.nome);
    }

    @Test
    void optimisticVersionRejectsStaleConcurrentUpdate() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> {
            Usuario admin = users.findByEmail("admin@empresa.local").orElseThrow();
            Chamado ticket = newTicket(admin, categories.findAll().getFirst());
            return tickets.saveAndFlush(ticket).id;
        });
        assertNotNull(id);

        var firstContext = entityManagerFactory.createEntityManager();
        var staleContext = entityManagerFactory.createEntityManager();
        try {
            firstContext.getTransaction().begin();
            staleContext.getTransaction().begin();
            Chamado first = firstContext.find(Chamado.class, id);
            Chamado stale = staleContext.find(Chamado.class, id);

            first.titulo = "Atualização concorrente A";
            firstContext.flush();
            firstContext.getTransaction().commit();

            stale.titulo = "Atualização concorrente B";
            assertThrows(OptimisticLockException.class, staleContext::flush);
        } finally {
            if (staleContext.getTransaction().isActive()) staleContext.getTransaction().rollback();
            if (firstContext.getTransaction().isActive()) firstContext.getTransaction().rollback();
            staleContext.close();
            firstContext.close();
            transactions.executeWithoutResult(status -> tickets.deleteById(id));
        }
    }

    private Chamado newTicket(Usuario requester, Categoria category) {
        Chamado ticket = new Chamado();
        ticket.titulo = "Chamado para teste PostgreSQL";
        ticket.descricao = "Descrição de teste com o tamanho mínimo necessário.";
        ticket.impacto = br.com.empresa.suporte.model.Enums.Impacto.INDIVIDUAL;
        ticket.solicitante = requester;
        ticket.setorOrigem = requester.setor;
        ticket.categoria = category;
        return ticket;
    }
}
