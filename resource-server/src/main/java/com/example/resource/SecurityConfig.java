package com.example.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource Server の設定。
 *
 * <ul>
 *   <li>Authorization ヘッダーの Bearer トークン（JWT）を検証する</li>
 *   <li>署名検証用の公開鍵は issuer の JWKS エンドポイントから自動取得される</li>
 *   <li>iss / exp / aud は application.yml の設定に従って検証される</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public").permitAll()
                // Keycloak の realm ロール "user" が必要（ROLE_user 権限）
                .requestMatchers("/api/user").hasRole("user")
                // realm ロール "admin" が必要
                .requestMatchers("/api/admin").hasRole("admin")
                .anyRequest().authenticated())
            // API はステートレス（セッションを作らない）
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * JWT から Spring Security の Authentication を作る変換器。
     *
     * <p>デフォルトでは scope クレームだけが "SCOPE_xxx" 権限になる。
     * Keycloak は realm ロールを "realm_access.roles" クレームに入れるので、
     * それを "ROLE_xxx" 権限に変換して追加する。
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        Converter<Jwt, Collection<GrantedAuthority>> authorities = jwt -> {
            List<GrantedAuthority> result = new ArrayList<>(scopes.convert(jwt));
            result.addAll(KeycloakRoles.fromRealmAccess(jwt.getClaimAsMap("realm_access")));
            return result;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        // principal 名を sub（UUID）ではなくユーザー名にする
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    static final class KeycloakRoles {

        private KeycloakRoles() {
        }

        static List<GrantedAuthority> fromRealmAccess(Map<String, Object> realmAccess) {
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        }
    }
}
