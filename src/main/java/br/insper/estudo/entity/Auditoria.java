package br.insper.estudo.entity;

import br.insper.estudo.observer.Operacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabela de auditoria: cada CREATE/DELETE vira uma linha com data/hora e tipo da operação. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long avaliacaoId;

    @Enumerated(EnumType.STRING)
    private Operacao operacao;

    private LocalDateTime dataHora;

    public Auditoria(Long avaliacaoId, Operacao operacao, LocalDateTime dataHora) {
        this.avaliacaoId = avaliacaoId;
        this.operacao = operacao;
        this.dataHora = dataHora;
    }
}
