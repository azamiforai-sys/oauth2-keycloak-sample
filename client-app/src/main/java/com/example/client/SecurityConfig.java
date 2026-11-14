package com.example.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Client（OIDC ログイン）の設定。
 *
 * <p>流れ:
 * <ol>
 *   <li>未ログインで保護ページへアクセス → /oauth2/authorization/keycloak へリダイレクト</li>
 *   <li>Keycloak の認可エンドポイントへリダイレクト（response_type=code, state, nonce, PKCE）</li>
 *   <li>ログイン後、Keycloak が /login/oauth2/code/keycloak?code=... へリダイレクト</li>
 *   <li>Spring Security がトークンエンドポイントで code をトークン（ID/アクセス/リフレッシュ）に交換</li>
 *   <li>ID トークンを検証して OidcUser を作り、セッションに保存</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/error", "/css/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(pkceResolver(clientRegistrationRepository))))
            // OAuth2AuthorizedClient（アクセストークン等）の保存・利用を有効化
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)));
        return http.build();
    }

    /**
     * 機密クライアント（client_secret あり）でも PKCE（code_challenge / code_verifier）を付与する。
     * 認可コード横取り攻撃への対策で、OAuth 2.1 / Security BCP では全クライアントに推奨されている。
     */
    private DefaultOAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
            repo, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /**
     * ログアウト時に Keycloak のセッションも終了させる（OIDC RP-Initiated Logout）。
     * これが無いと、アプリだけログアウトしても再ログイン時にパスワード入力なしで入れてしまう。
     */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repo) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }
}
