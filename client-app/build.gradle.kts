description = "OAuth2 Client / OIDC Login (Authorization Code + PKCE)"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    // Spring Boot 4 では RestClient.Builder の自動設定はこのスターターに分離されている
    implementation("org.springframework.boot:spring-boot-starter-restclient")
}
