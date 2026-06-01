package it.gov.innovazione.ndc.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server (Keycloak).
 *
 * <p>Attiva quando {@code harvester.security.mode=oauth2}. La modalita' default
 * resta {@code basic} (vedi {@link SecurityConfiguration}).
 *
 * <p>Verifica offline ad ogni request:
 *   - firma JWT (JWKS recuperato da issuer-uri)
 *   - iss = issuer Keycloak configurato
 *   - aud contiene almeno uno degli audience accettati
 *   - azp ∈ lista client OAuth conosciuti (security best practice)
 *   - exp non scaduto
 *
 * <p>Mappa il claim {@code realm_access.roles} di Keycloak (es. "ndc-admin") in
 * authorities Spring nella forma {@code ROLE_NDC_ADMIN}, usabili via
 * {@code hasRole("NDC_ADMIN")} / {@code @PreAuthorize}.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "harvester.security", name = "mode", havingValue = "oauth2")
public class OAuth2ResourceServerSecurityConfiguration {

    @Value("${harvester.security.oauth2.issuer-uri}")
    private String issuerUri;

    @Value("${harvester.security.oauth2.accepted-audiences:ndc-backend}")
    private List<String> acceptedAudiences;

    @Value("${harvester.security.oauth2.accepted-authorized-parties:ndc-admin-bff-client,ndc-cron-service-client}")
    private List<String> acceptedAuthorizedParties;

    @Bean
    @SneakyThrows
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) {
        return http.csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // /jobs/clear: solo admin (clear-cache distruttivo).
                        .requestMatchers(HttpMethod.POST, "/jobs/clear")
                                .hasRole("NDC_ADMIN")
                        // /jobs/harvest e DELETE /jobs/harvest/run: admin o service account.
                        .requestMatchers(HttpMethod.POST, "/jobs/harvest")
                                .hasAnyRole("NDC_ADMIN", "NDC_SERVICE")
                        .requestMatchers(HttpMethod.DELETE, "/jobs/**")
                                .hasRole("NDC_ADMIN")
                        // Lettura /jobs/**: admin o viewer.
                        .requestMatchers(HttpMethod.GET, "/jobs/**")
                                .hasAnyRole("NDC_ADMIN", "NDC_VIEWER")
                        // Scrittura /config/**: solo admin.
                        .requestMatchers(HttpMethod.POST, "/config/**")
                                .hasRole("NDC_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/config/**")
                                .hasRole("NDC_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/config/**")
                                .hasRole("NDC_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/config/**")
                                .hasRole("NDC_ADMIN")
                        // Lettura /config/**: admin o viewer.
                        .requestMatchers(HttpMethod.GET, "/config/**")
                                .hasAnyRole("NDC_ADMIN", "NDC_VIEWER")
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /**
     * JwtDecoder con JWKS auto-fetch dall'issuer + validatori custom.
     *
     * <p>Default su iss/exp + audience e azp (custom).
     */
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        OAuth2TokenValidator<Jwt> withDefaults = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = audienceValidator(acceptedAudiences);
        OAuth2TokenValidator<Jwt> withAzp = authorizedPartyValidator(acceptedAuthorizedParties);
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                withDefaults, withAudience, withAzp));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(List<String> accepted) {
        return jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.stream().anyMatch(accepted::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_audience",
                    "Token audience " + aud + " not in accepted " + accepted,
                    null));
        };
    }

    private OAuth2TokenValidator<Jwt> authorizedPartyValidator(List<String> accepted) {
        return jwt -> {
            String azp = jwt.getClaimAsString("azp");
            if (azp == null) {
                // azp e' opzionale per RFC, ma noi lo richiediamo per sicurezza.
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_azp", "Token missing required azp claim", null));
            }
            if (!accepted.contains(azp)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_azp",
                        "Token azp '" + azp + "' not in accepted " + accepted,
                        null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    /**
     * Estrae i ruoli Keycloak da {@code realm_access.roles} e li converte in
     * authorities Spring {@code ROLE_NDC_<UPPER>}. Es. "ndc-admin" -> "ROLE_NDC_ADMIN".
     *
     * <p>Principal name: fallback in cascata {@code preferred_username} -> {@code azp}
     * -> {@code sub}. Gli utenti umani hanno {@code preferred_username}; i service
     * account possono averlo nullo (depend dal realm mapper) e in quel caso usiamo
     * il client id ({@code azp}). Last resort: il subject. Senza questo fallback,
     * {@code Principal.getName()} potrebbe essere null e gli INSERT su colonne
     * audit NOT NULL (es. {@code HARVESTER_RUN.STARTED_BY}) falliscono in silenzio.
     */
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : extractRealmRoles(jwt)) {
                authorities.add(new SimpleGrantedAuthority(
                        "ROLE_" + role.toUpperCase().replace('-', '_')));
            }
            return authorities;
        });
        delegate.setPrincipalClaimName("preferred_username");
        return jwt -> {
            AbstractAuthenticationToken token = delegate.convert(jwt);
            String name = jwt.getClaimAsString("preferred_username");
            if (name == null || name.isBlank()) {
                name = jwt.getClaimAsString("azp");
            }
            if (name == null || name.isBlank()) {
                name = jwt.getSubject();
            }
            return new JwtAuthenticationToken(jwt, token.getAuthorities(), name);
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map) {
            Object roles = ((Map<String, Object>) map).get("roles");
            if (roles instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
        }
        return List.of();
    }
}
