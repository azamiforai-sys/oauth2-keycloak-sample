package com.example.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * アクセストークンを取得・更新する中心的なコンポーネント。
     * <ul>
     *   <li>authorizationCode: ログイン済みユーザーのトークンを使う</li>
     *   <li>refreshToken: 期限切れならリフレッシュトークンで自動更新</li>
     *   <li>clientCredentials: ユーザーなしでクライアント自身としてトークンを取得</li>
     * </ul>
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
            .authorizationCode()
            .refreshToken()
            .clientCredentials()
            .build();
        DefaultOAuth2AuthorizedClientManager manager =
            new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    /**
     * Resource Server 呼び出し用の RestClient。
     * インターセプターがリクエストごとに "Authorization: Bearer {access_token}" を付与する。
     * どのクライアント登録のトークンを使うかは、リクエスト属性で指定する（ApiCallController 参照）。
     */
    @Bean
    RestClient resourceServerClient(RestClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${app.resource-server.base-url}") String baseUrl) {
        return builder
            .baseUrl(baseUrl)
            .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(authorizedClientManager))
            .build();
    }
}
