package com.example.resource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Keycloak を起動せずに、認可ルールを検証するテスト。
 * 任意のクレームを持つ Jwt を組み立て、本番と同じ JwtAuthenticationConverter で認証情報に変換する。
 */
@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
class ApiControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtAuthenticationConverter converter;

    // issuer-uri へ接続しないようにモック化
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void publicApiNeedsNoToken() throws Exception {
        mvc.perform(get("/api/public")).andExpect(status().isOk());
    }

    @Test
    void userApiWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/user")).andExpect(status().isUnauthorized());
    }

    @Test
    void userApiWithUserRoleIs200() throws Exception {
        mvc.perform(get("/api/user").with(keycloakJwt("alice", "user")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.principal").value("alice"));
    }

    @Test
    void adminApiWithUserRoleIs403() throws Exception {
        mvc.perform(get("/api/admin").with(keycloakJwt("alice", "user")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminApiWithAdminRoleIs200() throws Exception {
        mvc.perform(get("/api/admin").with(keycloakJwt("bob", "user", "admin")))
            .andExpect(status().isOk());
    }

    /** Keycloak 形式のクレーム（realm_access.roles）を持つ JWT を作り、本番と同じ変換器で Authentication にする */
    private RequestPostProcessor keycloakJwt(String user, String... roles) {
        Jwt token = Jwt.withTokenValue("dummy")
            .header("alg", "RS256")
            .subject("00000000-0000-0000-0000-000000000000")
            .claim("preferred_username", user)
            .claim("azp", "web-client")
            .audience(List.of("sample-api"))
            .claim("scope", "openid profile email")
            .claim("realm_access", Map.of("roles", List.of(roles)))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
        return authentication(converter.convert(token));
    }
}
