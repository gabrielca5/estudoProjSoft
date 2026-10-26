package br.insper.estudo.endereco;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

/**
 * Testa o padrão cache-aside (Aula 09) sem Redis e sem ViaCEP: os dois são mocks.
 * @InjectMocks cria o EnderecoService e injeta os @Mock automaticamente (estilo do professor).
 */
@ExtendWith(MockitoExtension.class)
class EnderecoServiceTests {

    @Mock
    private EnderecoCache cache;

    @Mock
    private ViaCepClient client;

    @InjectMocks
    private EnderecoService service;

    private final Endereco paulista = new Endereco("01310-100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");

    @Test
    void cacheHit_naoChamaServicoExterno() {
        Mockito.when(cache.obter("01310100")).thenReturn(Optional.of(paulista));

        Endereco e = service.buscar("01310-100");

        Assertions.assertEquals("SP", e.uf());
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void cacheMiss_chamaServicoEGuardaNoCache() {
        Mockito.when(cache.obter("01310100")).thenReturn(Optional.empty());
        Mockito.when(client.buscar("01310100")).thenReturn(paulista);

        service.buscar("01310100");

        Mockito.verify(client).buscar("01310100");
        Mockito.verify(cache).guardar("01310100", paulista);
    }

    @Test
    void cepInvalido_deveLancarExcecao() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.buscar("123"));
        Mockito.verifyNoInteractions(cache, client);
    }
}
