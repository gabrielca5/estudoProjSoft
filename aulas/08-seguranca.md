# Aula 08: Segurança (e OAuth2)

> Código desta aula: `config/SecurityConfig.java`, `src/main/resources/application-auth.properties`, teste `config/SegurancaTests.java`.
> Repositórios do professor: `partida` (`WebSecurityConfig`) e `front` (React com Auth0).

## Panorama

**Todo software acessível na internet está sendo atacado.** As ameaças conhecidas precisam ser prevenidas em todo o ciclo de desenvolvimento. A referência é o **OWASP Top 10** (e agora o OWASP Top 10 for agentic AI).

Requisitos de segurança:
- controle de acesso (autenticação e autorização)
- criptografia
- gerenciamento de segredos
- atualização de dependências
- prevenção de ataques
- auditoria

## Controle de acesso

Dois objetivos diferentes, que você precisa saber separar:

| | Pergunta | Falhou → status |
|---|---|---|
| **Autenticação** | **quem** é você? | 401 Unauthorized |
| **Autorização** | você **pode** acessar este recurso? | 403 Forbidden |

Autenticação normalmente é login e senha, mas existem certificados digitais e chaves.

**Princípio do menor privilégio**: cada usuário/serviço tem só as permissões de que precisa.

Duas formas de implementar autorização:
- **RBAC** (Role-Based Access Control): por **papel**. "Só ADMIN pode excluir."
- **ABAC** (Attribute-Based Access Control): por **atributos** do usuário, do recurso ou do contexto. "Só o autor da avaliação pode editá-la", "só em horário comercial".

A autorização deve existir no **front** (esconder o botão) **e no back** (barrar de verdade). Só no front não é segurança: qualquer um chama a API pelo Postman.

## Criptografia

Quase toda aplicação trata dados sensíveis. Texto puro trafegando ou armazenado = exposto a vazamento.

| Tipo | Como funciona | Uso típico |
|---|---|---|
| **Simétrica** | uma chave cifra e decifra | cifrar dados em disco (AES) |
| **Assimétrica** | par de chaves: pública cifra/verifica, privada decifra/assina | HTTPS, **assinatura do JWT** |
| **Hash** | mão única, não tem como voltar | guardar senha (bcrypt) |

## Gerenciamento de segredos

Senhas e chaves **nunca** no código nem em arquivo aberto, e compartilhadas com o mínimo de pessoas. Ferramentas: **GitHub Secrets** (usado no `deploy.yml`), HashiCorp Vault. No projeto: `${DB_PASSWORD}` no `application.properties` e `${{ secrets.X }}` nos workflows.

## Atualização de dependências

Bibliotecas têm vulnerabilidades. A base **CVE** lista as falhas conhecidas. Casos famosos: **Log4Shell** (CVE-2021-44228, no Log4j) e **Spring4Shell** (CVE-2022-22965). Manter dependências atualizadas e verificar CVEs (Dependabot, por exemplo) faz parte do processo.

## Ataques comuns

| Ataque | Ideia | Defesa básica |
|---|---|---|
| **SQL Injection** | entrada do usuário vira parte do SQL | queries parametrizadas (o JPA já faz) |
| **XSS** (Script Injection) | injetar JavaScript que roda no navegador de outro usuário | escapar saída, validar entrada |
| **CORS** mal configurado | qualquer site chama sua API com as credenciais do usuário | liberar só as origens necessárias |
| **DDoS** | derrubar o serviço com volume | rate limit, CDN, escala |
| **Man-in-the-Middle** | interceptar o tráfego | HTTPS |

Nenhuma aplicação está 100% à prova de ataques, mas boas práticas evitam a maior parte.

---

## OAuth2

Implementar login "simples" é fácil. Os detalhes difíceis são: **armazenar** tokens, **expiração**, **revogação** e **integração com outras aplicações**. Para isso existem protocolos. O principal hoje é o **OAuth2**.

OAuth2 é um **protocolo**: define regras e fluxos de comunicação. Permite que um aplicativo obtenha **acesso limitado** a recursos protegidos **em nome de um usuário, sem conhecer a senha dele**. Não impõe implementação. Implementações: **Auth0** (usado na disciplina), Keycloak, AWS Cognito, Spring Security.

### Os 4 papéis

| Papel | Quem é | Na disciplina |
|---|---|---|
| **Resource Owner** | o usuário, dono dos dados | você |
| **Client** | o app que quer acessar | front em React |
| **Authorization Server** | autentica e emite tokens | Auth0 |
| **Resource Server** | a API que protege os dados | sua API Spring |

### O fluxo

```
1. Usuário clica "Login" no front (Client)
2. Front redireciona para a página de login do Auth0 (Authorization Server)
3. Usuário digita a senha NO AUTH0 (o front nunca vê a senha)
4. Auth0 redireciona de volta para o front com um JWT
5. Front chama a API com o header:  Authorization: Bearer <jwt>
6. API (Resource Server) valida a ASSINATURA do JWT com a chave pública do Auth0
   (busca a chave uma vez em https://SEU-TENANT.auth0.com/.well-known/jwks.json)
7. Assinatura válida + não expirado + audience certa → atende. Senão → 401.
```

O **JWT** (JSON Web Token) é um JSON assinado com três partes `header.payload.assinatura`. O payload tem quem é o usuário (`sub`), validade (`exp`), para qual API é (`aud`), quem emitiu (`iss`) e claims customizadas, como as **roles**. A API confia porque a assinatura só pode ter sido feita pela chave privada do Auth0.

### Autorização com roles

O Auth0 permite atribuir roles aos usuários e colocar esse dado no token, numa claim customizada. No front do professor:

```js
const roles = payload["https://social-insper.com/roles"] || [];
setIsAdmin(roles.includes("ADMIN"));
```

O mesmo dado é usado no back para RBAC.

## Como está no projeto

Dependência: `spring-boot-starter-oauth2-resource-server`.

Para estudar as outras aulas sem token, a segurança só liga com o **profile `auth`** (`SPRING_PROFILES_ACTIVE=auth`). O `application-auth.properties` aponta para o Auth0:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://SEU-TENANT.us.auth0.com/
spring.security.oauth2.resourceserver.jwt.audiences=...
auth.roles-claim=https://social-insper.com/roles
```

`SecurityConfig` com profile `auth`:

```java
http.authorizeHttpRequests(a -> a
        .requestMatchers(HttpMethod.GET, "/avaliacoes/**").permitAll()      // público
        .requestMatchers(HttpMethod.DELETE, "/avaliacoes/**").hasRole("ADMIN") // RBAC
        .anyRequest().authenticated())                                      // resto: precisa de token
    .csrf(AbstractHttpConfigurer::disable)
    .cors(Customizer.withDefaults())
    .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)));
```

- As regras são avaliadas **na ordem**: da mais específica para a mais geral.
- `hasRole("ADMIN")` procura a authority `ROLE_ADMIN`. O `JwtAuthenticationConverter` lê a claim de roles do token e adiciona o prefixo `ROLE_`.
- CSRF desligado porque a API é stateless com token no header (CSRF explora cookies de sessão).

O `SegurancaTests` prova o comportamento sem precisar do Auth0 de verdade (o `jwt()` do spring-security-test cria um token já validado):

| Cenário | Esperado |
|---|---|
| GET sem token | 200 (público) |
| POST sem token | 401 |
| DELETE com token ROLE_USER | 403 |
| DELETE com token ROLE_ADMIN | passa da segurança (404 porque o id não existe) |

Para ligar de verdade: siga o "Guia Configuração: Auth0, Front-End e Back-End" do Blackboard e troque as URLs no `application-auth.properties` (ou pelas variáveis `AUTH0_ISSUER`, `AUTH0_AUDIENCE`).

## Pratique

1. Explique a diferença entre 401 e 403 com um exemplo.
2. Por que o front nunca recebe a senha do usuário no OAuth2?
3. Como a API sabe que o JWT não foi forjado?
4. Adicione uma regra: só ADMIN pode `POST /pagamentos`. Escreva o teste do 403.
