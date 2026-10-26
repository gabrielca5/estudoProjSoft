package br.insper.estudo.endereco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Só os campos que interessam da resposta do ViaCEP. Os outros campos do JSON são ignorados. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Endereco(String cep, String logradouro, String bairro, String localidade, String uf) {
}
