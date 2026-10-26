# Aula 02: Manutenibilidade (SOLID e padrões de projeto)

> Código desta aula: `observer/` (Observer), `pagamento/` (Strategy + Factory), `repository/` (Repository), injeção de dependência em todo service.

## O que é manutenibilidade

A pergunta do slide: *"quanto custa fazer uma mudança pequena em um sistema grande?"*

Manutenibilidade é o esforço para quatro coisas:

| | Significa |
|---|---|
| **Entender** | achar rápido onde uma regra está |
| **Modificar** | mudar sem espalhar alteração pelo sistema |
| **Testar** | validar a mudança com pouco esforço |
| **Corrigir** | resolver bug sem efeito cascata |

Objetivo: **mudanças futuras previsíveis, seguras e baratas.**

## Por que sistemas ficam difíceis de manter

- **Acoplamento**: mudar A obriga a mudar B, C e D.
- **Complexidade**: caminhos demais para entender e testar.
- **Duplicação**: a mesma regra copiada em vários lugares (você corrige em um e esquece os outros).
- **Dependências frágeis**: para alterar algo você precisa conhecer detalhes internos de outra parte.

Consequência econômica em cadeia: mais tempo para entender → mais risco para alterar → mais bugs e retrabalho → time mais lento. **O custo de cada mudança aumenta.**

## O que queremos no design

- **Baixo acoplamento**: poucas dependências entre módulos.
- **Alta coesão**: coisas relacionadas ficam juntas.
- **Responsabilidades claras**: cada parte tem um propósito.
- **Testabilidade**: dá para testar cada parte isolada.
- **Mudanças localizadas**: alterar uma regra mexe em poucos arquivos.
- **Dependências explícitas**: dá para ver de onde vem cada colaboração (injeção por construtor faz isso).

A arquitetura em camadas do projeto (ver `guias/01-arquitetura-em-camadas.md`) é aplicação direta disso.

## Como medir

| Categoria | Métricas |
|---|---|
| Complexidade | complexidade ciclomática (número de caminhos), complexidade cognitiva (quão difícil de ler) |
| Estrutura | acoplamento, coesão, ciclos de dependência |
| Código | duplicação, code smells, tamanho |
| Mudança | lead time (tempo da ideia ao deploy), change failure rate (% de deploys que quebram), defeitos |

**Nenhuma métrica sozinha responde "este sistema é manutenível?"**. Ferramentas que ajudam: clean code, SOLID, padrões, testes (unidade, integração, E2E), análise estática (bad smells, falhas de segurança), observabilidade.

---

## SOLID

Não é o objetivo, é uma **ferramenta** para pensar em mudança, responsabilidades e dependências.

### S: Single Responsibility (responsabilidade única)

Uma classe deve ter **um conjunto coerente de motivos para mudar**.

Antes: `OrderService` calcula preço, salva no banco, envia e-mail e gera PDF. Quatro motivos para mudar (regra de preço, banco, provedor de e-mail, layout do PDF).

Depois: `PricingService`, `OrderRepository`, `NotificationService`, `PdfGenerator`.

Pergunta-teste: *quantas razões diferentes podem fazer esta classe mudar?*

No projeto: o `AvaliacaoService` não grava auditoria nem faz moderação. Isso foi para os observers.

### O: Open/Closed (aberto para extensão, fechado para modificação)

Adicionar comportamento novo sem sair alterando o que já funciona.

```java
// ruim: cada forma nova de pagamento = mais um if aqui
if (method == PIX)    ...
if (method == CARD)   ...
if (method == BOLETO) ...
```

Com Strategy, ApplePay vira **uma classe nova**; o resto não muda. O ponto não é "nunca alterar código", é **reduzir o raio de impacto** de uma regra nova.

### L: Liskov Substitution

Qualquer implementação deve poder substituir a outra sem surpresas: respeitar o contrato. Sinal de alerta: subclasse que exige "tratamento especial" (`if (p instanceof ProcessadorBoleto) ...`).

### I: Interface Segregation

Evite interfaces "Deus" com 20 métodos. Quem depende de uma interface deve depender só do que usa.

### D: Dependency Inversion

Código de alto nível depende de **abstrações**, não de detalhes. O `AvaliacaoService` depende da interface `AvaliacaoObserver`, não de `AuditoriaObserver`. Isso isola banco, APIs externas, filas e frameworks e é o que permite **mockar** nos testes.

---

## Padrões de projeto

Soluções recorrentes para problemas recorrentes. **Não são regras universais.**

### Strategy

**Problema**: lógica condicional que cresce a cada comportamento novo.
**Solução**: uma interface, uma implementação por comportamento.

Código: `pagamento/ProcessadorPagamento.java` (interface) e `ProcessadorPix`, `ProcessadorCartao`, `ProcessadorBoleto`.

```java
public interface ProcessadorPagamento {
    TipoPagamento tipo();
    String processar(BigDecimal valor);
}
```

Liga com: Open/Closed e testabilidade (cada estratégia testada sozinha).

### Factory

**Problema**: `new Pix()`, `new Card()` espalhados em vários lugares.
**Solução**: um lugar que decide qual objeto usar.

Código: `pagamento/PagamentoFactory.java`. O truque de Spring: ela recebe `List<ProcessadorPagamento>` (o Spring injeta todas as implementações) e monta um mapa tipo → processador.

```java
public PagamentoFactory(List<ProcessadorPagamento> lista) {
    lista.forEach(p -> processadores.put(p.tipo(), p));
}
```

O professor mostra as duas versões no repositório `pagamento`: a factory "na mão" com if/else e o `Map<String, Processador>` injetado pelo Spring (nome do bean → implementação).

Teste: `curl -X POST localhost:8080/pagamentos -H "Content-Type: application/json" -d '{"tipo":"PIX","valor":100}'`

### Observer (o que caiu na prova)

**Problema**: quando algo acontece (criou avaliação), várias outras coisas precisam reagir (auditar, moderar, mandar e-mail). Colocar tudo no service viola Single Responsibility.

**Solução**: o "sujeito" mantém uma lista de "observadores" e avisa todos quando um evento acontece. Ele não sabe o que cada um faz.

```
          AvaliacaoService (sujeito)
                │ notificar(avaliacao, CREATE)
      ┌─────────┴──────────┐
      ▼                    ▼
AuditoriaObserver    ModeracaoObserver
(grava no banco)     (nota <= 2 → log de alerta)
```

Código: pacote `observer/`.

1. **Interface** (`AvaliacaoObserver`): `void atualizar(Avaliacao a, Operacao op)`.
2. **Observers concretos** com `@Component` (`AuditoriaObserver`, `ModeracaoObserver`).
3. **Sujeito** (`AvaliacaoService`): recebe `List<AvaliacaoObserver>` (o Spring injeta todos os `@Component` que implementam a interface) e chama cada um em `notificar(...)`.

Para adicionar um terceiro comportamento: crie outro `@Component implements AvaliacaoObserver`. **O service não muda.** Isso é Open/Closed na prática.

Na intermediária o professor escreveu "não encontrei o observable". Lição: deixe óbvio (pacote `observer`, nomes `XxxObserver`, comentário dizendo onde o padrão foi aplicado).

### Outros padrões

- Já usados sem perceber: **Repository** (`AvaliacaoRepository`), **Injeção de Dependência** (construtores dos services), **Decorator**.
- Recomendados para estudar: **Builder**, **Observable/Listener**, **Composite**, **Adapter**.

## Conclusão do slide

1. Manutenibilidade é sobre **custo e risco de mudança**.
2. Acoplamento, coesão, complexidade e testabilidade são as propriedades-chave.
3. SOLID ajuda a raciocinar sobre responsabilidades e dependências.
4. Padrões são soluções recorrentes, não regras universais.
5. Sem métricas é impossível saber se a manutenibilidade é boa.

## Pratique

1. Crie `ProcessadorApplePay` e confira que nada mais precisou mudar.
2. Crie um `EmailObserver` que imprime "e-mail enviado" quando uma avaliação é excluída.
3. Explique qual princípio SOLID cada um dos exercícios acima demonstra.
