package com.example.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Controller
public class HomeController {

    private final JsonMapper jsonMapper;

    public HomeController(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("user", user);
        return "index";
    }

    /**
     * ログインで得たトークンの中身を表示する学習用ページ。
     * 実アプリではトークンを画面に出さないこと。
     */
    @GetMapping("/tokens")
    public String tokens(@AuthenticationPrincipal OidcUser user,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            Model model) {
        String accessToken = client.getAccessToken().getTokenValue();
        model.addAttribute("user", user);
        model.addAttribute("idTokenClaims", pretty(user.getIdToken().getClaims()));
        model.addAttribute("userInfoClaims", user.getUserInfo() == null ? "(UserInfo 未取得)" : pretty(user.getUserInfo().getClaims()));
        model.addAttribute("accessToken", accessToken);
        model.addAttribute("accessTokenHeader", decodeJwtPart(accessToken, 0));
        model.addAttribute("accessTokenPayload", decodeJwtPart(accessToken, 1));
        model.addAttribute("scopes", client.getAccessToken().getScopes());
        model.addAttribute("expiresAt", client.getAccessToken().getExpiresAt());
        model.addAttribute("hasRefreshToken", client.getRefreshToken() != null);
        return "tokens";
    }

    /** JWT は "ヘッダー.ペイロード.署名" を Base64URL エンコードしたもの。署名検証はしていない（表示専用）。 */
    private String decodeJwtPart(String jwt, int index) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            return "(JWT 形式ではありません)";
        }
        String json = new String(Base64.getUrlDecoder().decode(parts[index]), StandardCharsets.UTF_8);
        return pretty(jsonMapper.readTree(json));
    }

    private String pretty(Object value) {
        Object tree = value instanceof JsonNode ? value : jsonMapper.valueToTree(value);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
    }
}
