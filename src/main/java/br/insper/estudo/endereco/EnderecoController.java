package br.insper.estudo.endereco;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/enderecos")
public class EnderecoController {

    private final EnderecoService service;

    public EnderecoController(EnderecoService service) {
        this.service = service;
    }

    // GET /enderecos/01310100  -> 1ª vez vai ao ViaCEP, 2ª vez vem do Redis
    @GetMapping("/{cep}")
    public Endereco buscar(@PathVariable String cep) {
        return service.buscar(cep);
    }
}
