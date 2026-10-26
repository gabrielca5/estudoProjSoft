package br.insper.estudo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * CAMADA ENTITY: representa uma TABELA do banco. Cada atributo vira uma coluna.
 * Não é devolvida direto pela API: quem sai/entra pela API são os DTOs (pacote dto).
 *
 * "deletado" implementa deleção LÓGICA (pedida no Exercício 13/09): a linha continua no banco.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String autor;

    private String conteudo;

    private Integer nota;

    private LocalDateTime dataAvaliacao;

    private boolean deletado = false;
}
