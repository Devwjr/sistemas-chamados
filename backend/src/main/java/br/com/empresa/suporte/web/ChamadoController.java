package br.com.empresa.suporte.web;

import br.com.empresa.suporte.model.Enums.*;
import br.com.empresa.suporte.service.ChamadoService;
import br.com.empresa.suporte.web.Dtos.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ChamadoController {
    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping("/chamados")
    public Pagina<ChamadoView> list(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) LocalDate de,
            @RequestParam(required = false) LocalDate ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "criadoEm,desc") String sort) {
        return service.list(
                busca,
                status,
                prioridade,
                categoriaId,
                responsavelId,
                setorId,
                de,
                ate,
                Paging.page(page, size, sort, Set.of("criadoEm", "atualizadoEm", "id", "titulo")));
    }

    @PostMapping("/chamados")
    public ResponseEntity<ChamadoView> create(@Valid @RequestBody CriarChamadoRequest body) {
        var c = service.create(body);
        return ResponseEntity.created(URI.create("/api/chamados/" + c.id())).body(c);
    }

    @GetMapping("/chamados/{id}")
    public ChamadoView detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/chamados/{id}/assumir")
    public ChamadoView assume(@PathVariable Long id, @Valid @RequestBody VersaoRequest body) {
        return service.assume(id, body);
    }

    @PatchMapping("/chamados/{id}/responsavel")
    public ChamadoView assign(@PathVariable Long id, @Valid @RequestBody ResponsavelRequest body) {
        return service.assign(id, body);
    }

    @PatchMapping("/chamados/{id}/classificacao")
    public ChamadoView classify(
            @PathVariable Long id, @Valid @RequestBody ClassificacaoRequest body) {
        return service.classify(id, body);
    }

    @PostMapping("/chamados/{id}/transicoes")
    public ChamadoView transition(
            @PathVariable Long id, @Valid @RequestBody TransicaoRequest body) {
        return service.transition(id, body);
    }

    @GetMapping("/chamados/{id}/comentarios")
    public Pagina<ComentarioView> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.comments(id, Paging.timeline(page, size));
    }

    @PostMapping("/chamados/{id}/comentarios")
    public ChamadoView comment(@PathVariable Long id, @Valid @RequestBody ComentarioRequest body) {
        return service.addComment(id, body);
    }

    @GetMapping("/chamados/{id}/historico")
    public Pagina<HistoricoView> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.history(id, Paging.timeline(page, size));
    }

    @GetMapping("/painel")
    public Map<String, Object> dashboard() {
        return service.dashboard();
    }
}
