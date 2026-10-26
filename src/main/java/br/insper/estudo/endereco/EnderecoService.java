package br.insper.estudo.endereco;

import org.springframework.stereotype.Service;

/**
 * Padrão "cache-aside":
 *   1. procura no cache  -> HIT: devolve na hora
 *   2. MISS: chama o serviço externo
 *   3. guarda no cache para a próxima vez
 */
@Service
public class EnderecoService {

    private final EnderecoCache cache;
    private final ViaCepClient client;

    public EnderecoService(EnderecoCache cache, ViaCepClient client) {
        this.cache = cache;
        this.client = client;
    }

    public Endereco buscar(String cep) {
        String limpo = cep.replaceAll("\\D", "");
        if (limpo.length() != 8) {
            throw new IllegalArgumentException("CEP deve ter 8 dígitos");
        }
        return cache.obter(limpo).orElseGet(() -> {
            Endereco e = client.buscar(limpo);
            cache.guardar(limpo, e);
            return e;
        });
    }
}
