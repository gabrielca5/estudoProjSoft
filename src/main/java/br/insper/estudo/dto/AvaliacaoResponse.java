package br.insper.estudo.dto;

import br.insper.estudo.entity.Avaliacao;

import java.time.LocalDateTime;

/**
 * DTO de SAÍDA: o formato do JSON que a API devolve.
 * Repare que "deletado" não aparece: é detalhe interno, o cliente não precisa saber.
 */
public record AvaliacaoResponse(
        Long id,
        String autor,
        String conteudo,
        Integer nota,
        LocalDateTime dataAvaliacao
) {
    /** Converte Entity -> DTO. */
    public static AvaliacaoResponse from(Avaliacao a) {
        return new AvaliacaoResponse(a.getId(), a.getAutor(), a.getConteudo(), a.getNota(), a.getDataAvaliacao());
    }
}
