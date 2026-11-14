package com.example.client;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

/**
 * Resource Server の API を呼び出して、結果（ステータスとボディ）を表示する。
 */
@Controller
public class ApiCallController {

    private final RestClient resourceServerClient;

    public ApiCallController(RestClient resourceServerClient) {
        this.resourceServerClient = resourceServerClient;
    }

    @GetMapping("/api-call")
    public String callApi(@RequestParam(defaultValue = "user") String path,
            @RequestParam(defaultValue = "keycloak") String registration,
            Model model) {
        if (!path.matches("public|user|admin") || !registration.matches("keycloak|batch")) {
            throw new IllegalArgumentException("unsupported parameter");
        }
        model.addAttribute("path", "/api/" + path);
        model.addAttribute("registration", registration);
        try {
            String body = resourceServerClient.get()
                .uri("/api/{path}", path)
                // どのクライアント登録のアクセストークンを付けるか
                //   keycloak: ログインユーザーのトークン（Authorization Code）
                //   batch   : クライアント自身のトークン（Client Credentials）
                .attributes(clientRegistrationId(registration))
                .retrieve()
                .body(String.class);
            model.addAttribute("status", 200);
            model.addAttribute("body", body);
        } catch (RestClientResponseException e) {
            // 401: トークンなし/不正、403: トークンは有効だが権限不足
            model.addAttribute("status", e.getStatusCode().value());
            model.addAttribute("body", e.getResponseHeaders() != null
                && e.getResponseHeaders().getFirst("WWW-Authenticate") != null
                    ? "WWW-Authenticate: " + e.getResponseHeaders().getFirst("WWW-Authenticate")
                    : e.getResponseBodyAsString());
        }
        return "api-call";
    }
}
