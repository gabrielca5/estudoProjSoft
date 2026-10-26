package br.insper.estudo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Aula 08: Segurança.
 *
 * Profile padrão  -> tudo liberado (para estudar as outras aulas sem precisar de token).
 * Profile "auth"  -> a API vira um RESOURCE SERVER OAuth2: só aceita requisições com JWT válido do Auth0.
 *                    Rode com: SPRING_PROFILES_ACTIVE=auth
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Profile("!auth")
    public SecurityFilterChain semAutenticacao(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Profile("auth")
    public SecurityFilterChain comOAuth2(HttpSecurity http,
                                         JwtAuthenticationConverter conversor) throws Exception {
        http.authorizeHttpRequests(a -> a
                    // AUTORIZAÇÃO (RBAC): regras por rota, da mais específica para a mais geral
                    .requestMatchers(HttpMethod.GET, "/avaliacoes/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/avaliacoes/**").hasRole("ADMIN")
                    // AUTENTICAÇÃO: qualquer outra rota exige token válido
                    .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable) // API stateless com token: sem cookie de sessão, sem CSRF
            .cors(Customizer.withDefaults())
            .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)));
        return http.build();
    }

    /**
     * O Auth0 coloca as roles numa claim customizada do JWT (ex.: "https://social-insper.com/roles": ["ADMIN"]).
     * Este conversor transforma cada role em uma authority "ROLE_ADMIN", que é o que hasRole("ADMIN") verifica.
     */
    @Bean
    @Profile("auth")
    public JwtAuthenticationConverter jwtAuthenticationConverter(@Value("${auth.roles-claim}") String claim) {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(claim);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(roles);
        return conversor;
    }
}
