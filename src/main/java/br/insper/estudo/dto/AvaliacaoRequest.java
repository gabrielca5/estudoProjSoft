package br.insper.estudo.dto;

import br.insper.estudo.entity.Avaliacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CAMADA DTO (Data Transfer Object): o formato do JSON que ENTRA na API.
 *
 * Por que não receber a Entity direto?
 *  - o cliente não pode mandar campos que não deveria (id, deletado, dataAvaliacao)
 *  - a validação de entrada fica aqui (@NotBlank, @Min...) e o Spring devolve 400 sozinho com @Valid
 *  - dá para mudar a tabela sem quebrar o contrato da API
 *
 * "record" = classe imutável com construtor, getters (autor(), nota()...), equals e toString prontos.
 */
public record AvaliacaoRequest(
        @NotBlank String autor,
        @NotBlank String conteudo,
        @NotNull @Min(1) @Max(5) Integer nota
) {
    /** Converte DTO -> Entity. Só copia o que o cliente pode definir. */
    public Avaliacao toEntity() {
        Avaliacao a = new Avaliacao();
        a.setAutor(autor);
        a.setConteudo(conteudo);
        a.setNota(nota);
        return a;
    }
}
