package com.example.resource;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    /** 認証不要 */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of("message", "誰でもアクセスできる公開 API です", "time", Instant.now().toString());
    }

    /** ROLE_user が必要 */
    @GetMapping("/user")
    public Map<String, Object> userEndpoint(JwtAuthenticationToken auth) {
        return describe("user 権限で呼べる API です", auth);
    }

    /** ROLE_admin が必要 */
    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint(JwtAuthenticationToken auth) {
        return describe("admin 権限で呼べる API です", auth);
    }

    private Map<String, Object> describe(String message, JwtAuthenticationToken auth) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("principal", auth.getName());
        body.put("authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        body.put("clientId", auth.getToken().getClaimAsString("azp"));
        body.put("audience", auth.getToken().getAudience());
        body.put("expiresAt", String.valueOf(auth.getToken().getExpiresAt()));
        return body;
    }
}
