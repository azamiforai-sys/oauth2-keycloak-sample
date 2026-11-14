plugins {
    // build.gradle.kts の toolchain（Java 21）に合う JDK が無ければ自動ダウンロードする
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "oauth2-keycloak-sample"

include("client-app", "resource-server")
