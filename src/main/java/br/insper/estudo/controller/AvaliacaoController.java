package br.insper.estudo.controller;

import br.insper.estudo.dto.AvaliacaoRequest;
import br.insper.estudo.dto.AvaliacaoResponse;
import br.insper.estudo.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CAMADA CONTROLLER: porta de entrada HTTP. Deve ser "magro":
 *   1. recebe a requisição (rota, path variable, query param, body)
 *   2. chama o service
 *   3. devolve a resposta com o STATUS certo
 * Nenhuma regra de negócio aqui.
 */
@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public AvaliacaoResponse criar(@RequestBody @Valid AvaliacaoRequest request) {
        return service.criar(request);
    }

    @GetMapping // GET /avaliacoes?autor=gab
    public List<AvaliacaoResponse> listar(@RequestParam(required = false) String autor) {
        return service.listar(autor);
    }

    @GetMapping("/{id}")
    public AvaliacaoResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
