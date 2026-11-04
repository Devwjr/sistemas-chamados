package br.com.empresa.suporte.web;

import br.com.empresa.suporte.service.AdminService;
import br.com.empresa.suporte.web.Dtos.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/categorias")
    public Pagina<CadastroView> categories(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.categories(ativo, Paging.named(page, size));
    }

    @GetMapping("/setores")
    @PreAuthorize("hasRole('ADMIN')")
    public Pagina<CadastroView> sectors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.sectors(Paging.named(page, size));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public Pagina<UsuarioView> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.users(Paging.named(page, size));
    }

    @GetMapping("/usuarios/elegiveis-atendimento")
    @PreAuthorize("hasAnyRole('ADMIN','TECNICO')")
    public Pagina<Ref> eligible(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.eligible(Paging.named(page, size));
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CadastroView> category(@Valid @RequestBody CadastroRequest body) {
        var c = service.category(null, body);
        return ResponseEntity.created(URI.create("/api/categorias/" + c.id())).body(c);
    }

    @PatchMapping("/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CadastroView category(@PathVariable Long id, @Valid @RequestBody CadastroRequest body) {
        return service.category(id, body);
    }

    @PostMapping("/setores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CadastroView> sector(@Valid @RequestBody CadastroRequest body) {
        var c = service.sector(null, body);
        return ResponseEntity.created(URI.create("/api/setores/" + c.id())).body(c);
    }

    @PatchMapping("/setores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CadastroView sector(@PathVariable Long id, @Valid @RequestBody CadastroRequest body) {
        return service.sector(id, body);
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioView> user(@Valid @RequestBody UsuarioRequest body) {
        var c = service.user(null, body);
        return ResponseEntity.created(URI.create("/api/usuarios/" + c.id())).body(c);
    }

    @PatchMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioView user(@PathVariable Long id, @Valid @RequestBody UsuarioRequest body) {
        return service.user(id, body);
    }

    @PostMapping("/usuarios/{id}/redefinir-senha")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reset(
            @PathVariable Long id, @Valid @RequestBody ResetSenhaRequest body) {
        service.reset(id, body);
        return ResponseEntity.noContent().build();
    }
}
