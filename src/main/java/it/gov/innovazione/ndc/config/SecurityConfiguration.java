package it.gov.innovazione.ndc.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Configurazione security: basic auth e/o OAuth2 Resource Server (Keycloak).
 *
 * <p>I due meccanismi sono governati da flag indipendenti:
 * <ul>
 *   <li>{@code harvester.security.basic.enabled} — default {@code true} (back-compat)</li>
 *   <li>{@code harvester.security.oauth2.enabled} — default {@code false} (opt-in)</li>
 * </ul>
 *
 * <p>Possono essere attivi contemporaneamente: Spring registra sia
 * {@code BasicAuthenticationFilter} (header {@code Authorization: Basic ...}) sia
 * {@code BearerTokenAuthenticationFilter} (header {@code Authorization: Bearer ...}).
 * Le due forme di credential viaggiano in pacchetti diversi e non si calpestano.
 *
 * <p>Per back-compat la legacy property {@code harvester.security.mode} (valori
 * {@code basic|oauth2}) viene letta come fallback per derivare i default dei due
 * flag. Deprecata: pianificata la rimozione dopo qualche release.
 *
 * <h2>Authorize chain</h2>
 * Quando solo {@code basic} e' attivo si usano i ruoli legacy ({@code HARVESTER}
 * gate-all). Quando {@code oauth2} e' attivo si usa la matrice fine-grained
 * ({@code NDC_ADMIN}, {@code NDC_VIEWER}, {@code NDC_SERVICE}). Se entrambi sono
 * attivi, {@code HARVESTER} e' accettato ovunque sia accettato {@code NDC_ADMIN}
 * — cosi' tool/script esistenti che usano basic restano funzionanti durante la
 * migrazione.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    @Value("${harvester.auth.user:admin}")
    private String basicUser;

    @Value("${harvester.auth.password:password}")
    private String basicPassword;

    /**
     * Default-derivato da {@code mode}: basic e' attivo se mode != oauth2 (cioe' basic o assente).
     * Override esplicito con {@code harvester.security.basic.enabled}.
     */
    @Value("${harvester.security.basic.enabled:#{'${harvester.security.mode:basic}' != 'oauth2'}}")
    private boolean basicEnabled;

    /**
     * Default-derivato da {@code mode}: oauth2 e' attivo se mode == oauth2.
     * Override esplicito con {@code harvester.security.oauth2.enabled}.
     */
    @Value("${harvester.security.oauth2.enabled:#{'${harvester.security.mode:basic}' == 'oauth2'}}")
    private boolean oauth2Enabled;

    @Value("${harvester.security.oauth2.issuer-uri:}")
    private String issuerUri;

    @Value("${harvester.security.oauth2.accepted-audiences:ndc-backend}")
    private List<String> acceptedAudiences;

    @Value("${harvester.security.oauth2.accepted-authorized-parties:ndc-admin-bff-client,ndc-cron-service-client}")
    private List<String> acceptedAuthorizedParties;

    @Bean
    @SneakyThrows
    SecurityFilterChain filterChain(HttpSecurity http) {
        if (!basicEnabled && !oauth2Enabled) {
            log.warn("Security: both basic and oauth2 disabled — all protected endpoints will reject (401).");
        }
        log.info("Security configured: basic={} oauth2={}", basicEnabled, oauth2Enabled);

        http.csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(this::configureAuthorize);

        if (basicEnabled) {
            http.httpBasic(Customizer.withDefaults());
        }
        if (oauth2Enabled) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }
        return http.build();
    }

    /**
     * Authorize rules. Matrice fine-grained quando oauth2 e' attivo; quando lo
     * e' anche basic, HARVESTER viene "innestato" tra i ruoli admin (back-compat).
     * In modalita' basic-only si torna a HARVESTER gate-all su {@code /jobs/**} e {@code /config/**}.
     */
    private void configureAuthorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        if (!oauth2Enabled) {
            req.requestMatchers("/jobs/**", "/config/**").hasRole("HARVESTER")
                    .anyRequest().permitAll();
            return;
        }
        String[] adminRoles = basicEnabled
                ? new String[]{"NDC_ADMIN", "HARVESTER"}
                : new String[]{"NDC_ADMIN"};
        String[] adminOrService = basicEnabled
                ? new String[]{"NDC_ADMIN", "NDC_SERVICE", "HARVESTER"}
                : new String[]{"NDC_ADMIN", "NDC_SERVICE"};
        String[] adminOrViewer = basicEnabled
                ? new String[]{"NDC_ADMIN", "NDC_VIEWER", "HARVESTER"}
                : new String[]{"NDC_ADMIN", "NDC_VIEWER"};

        req.requestMatchers(HttpMethod.POST, "/jobs/clear").hasAnyRole(adminRoles)
                .requestMatchers(HttpMethod.POST, "/jobs/harvest").hasAnyRole(adminOrService)
                .requestMatchers(HttpMethod.DELETE, "/jobs/**").hasAnyRole(adminRoles)
                .requestMatchers(HttpMethod.GET, "/jobs/**").hasAnyRole(adminOrViewer)
                .requestMatchers(HttpMethod.POST, "/config/**").hasAnyRole(adminRoles)
                .requestMatchers(HttpMethod.PUT, "/config/**").hasAnyRole(adminRoles)
                .requestMatchers(HttpMethod.PATCH, "/config/**").hasAnyRole(adminRoles)
                .requestMatchers(HttpMethod.DELETE, "/config/**").hasAnyRole(adminRoles)
                .requestMatchers(HttpMethod.GET, "/config/**").hasAnyRole(adminOrViewer)
                .anyRequest().permitAll();
    }

    /** User in-memory per basic auth. Esportato solo se basic e' attivo. */
    @Bean
    @ConditionalOnProperty(prefix = "harvester.security.basic", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username(basicUser)
                        .password("{noop}" + basicPassword)
                        .roles("HARVESTER")
                        .build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "harvester.security.oauth2", name = "enabled", havingValue = "true")
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                audienceValidator(acceptedAudiences),
                authorizedPartyValidator(acceptedAuthorizedParties)));
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
     * Mappa {@code realm_access.roles} -> {@code ROLE_NDC_<UPPER>} (es. "ndc-admin" -> "ROLE_NDC_ADMIN").
     *
     * <p>Principal name: cascata {@code preferred_username} -> {@code azp} -> {@code sub}. Senza
     * fallback, {@code Principal.getName()} potrebbe essere null per service account, e gli INSERT
     * su colonne audit NOT NULL ({@code HARVESTER_RUN.STARTED_BY}) falliscono.
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
