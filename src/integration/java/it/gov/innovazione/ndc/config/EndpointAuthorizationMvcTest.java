package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.controller.DashboardController;
import it.gov.innovazione.ndc.controller.DateParserService;
import it.gov.innovazione.ndc.service.DashboardService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la classificazione pubblico/protetto di tutti gli endpoint applicativi.
 *
 * <p>I mapping vengono letti con {@link RequestMappingHandlerMapping} da tutti i
 * {@code @RestController} del package (incluse le interfacce generate da OpenAPI).
 * Un endpoint nuovo o rimosso che non compare in {@link #EXPECTED} fa fallire il test:
 * va classificato esplicitamente qui e, se pubblico, in {@link SecurityConfiguration}.
 */
@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = {
    "harvester.auth.user=test-user",
    "harvester.auth.password=test-password"
})
class EndpointAuthorizationMvcTest {

    private static final String BASE_PACKAGE = "it.gov.innovazione.ndc";
    private static final boolean PUBLIC = true;
    private static final boolean PROTECTED = false;

    private static final Map<String, Boolean> EXPECTED = new TreeMap<>(Map.ofEntries(
            // consultazione pubblica
            Map.entry("GET /semantic-assets", PUBLIC),
            Map.entry("GET /semantic-assets/by-iri", PUBLIC),
            Map.entry("GET /semantic-assets/changelog", PUBLIC),
            Map.entry("GET /semantic-assets/mlt", PUBLIC),
            Map.entry("GET /semantic-assets/rights-holders", PUBLIC),
            Map.entry("GET /semantic-assets/stats", PUBLIC),
            Map.entry("GET /vocabularies", PUBLIC),
            Map.entry("GET /vocabularies/{agency_id}/{key_concept}", PUBLIC),
            Map.entry("GET /vocabularies/{agency_id}/{key_concept}/{id}", PUBLIC),
            Map.entry("GET /status", PUBLIC),
            Map.entry("GET /check-url", PUBLIC),
            Map.entry("GET /harvest/vocabularies.db", PUBLIC),
            // validazione on-demand
            Map.entry("POST /validate", PUBLIC),
            Map.entry("POST /validate/syntax", PUBLIC),
            Map.entry("POST /validate/repo/{owner}/{repo}", PUBLIC),
            Map.entry("POST /validate/repo/{owner}/{repo}/{revision:.+}", PUBLIC),
            Map.entry("GET /validate/repo/{validationId}", PUBLIC),
            // dashboard
            Map.entry("GET /dashboard/aggregated-count-data", PROTECTED),
            Map.entry("GET /dashboard/aggregated-time-data", PROTECTED),
            Map.entry("GET /dashboard/raw-data", PROTECTED),
            // job di harvesting
            Map.entry("POST /jobs/harvest", PROTECTED),
            Map.entry("GET /jobs/harvest/run", PROTECTED),
            Map.entry("GET /jobs/harvest/run/by-batch", PROTECTED),
            Map.entry("GET /jobs/harvest/running", PROTECTED),
            Map.entry("DELETE /jobs/harvest/run", PROTECTED),
            Map.entry("POST /jobs/clear", PROTECTED),
            // configurazione e repository
            Map.entry("GET /config/metadata", PROTECTED),
            Map.entry("GET /config/{repoId}", PROTECTED),
            Map.entry("POST /config/{repoId}", PROTECTED),
            Map.entry("PUT /config/{repoId}/{configKey}", PROTECTED),
            Map.entry("DELETE /config/{repoId}/{configKey}", PROTECTED),
            Map.entry("GET /config/repository", PROTECTED),
            Map.entry("GET /config/repository/inspect", PROTECTED),
            Map.entry("POST /config/repository", PROTECTED),
            Map.entry("PATCH /config/repository/{id}", PROTECTED),
            Map.entry("DELETE /config/repository/{id}", PROTECTED),
            Map.entry("GET /config/repository/{id}/validation-report", PROTECTED),
            Map.entry("GET /config/repository/{id}/conformance", PROTECTED),
            Map.entry("GET /config/repository/{id}/runs/{runId}/delta", PROTECTED),
            Map.entry("GET /config/repository/{id}/runs/latest/delta", PROTECTED),
            Map.entry("GET /config/repository/{id}/runs/{runId}/delta/summary", PROTECTED),
            Map.entry("GET /config/repository/{id}/runs/latest/delta/summary", PROTECTED),
            // alerter
            Map.entry("GET /user", PROTECTED),
            Map.entry("GET /user/{id}", PROTECTED),
            Map.entry("POST /user", PROTECTED),
            Map.entry("PATCH /user", PROTECTED),
            Map.entry("DELETE /user/{id}", PROTECTED),
            Map.entry("GET /profile", PROTECTED),
            Map.entry("GET /profile/{id}", PROTECTED),
            Map.entry("POST /profile", PROTECTED),
            Map.entry("PATCH /profile", PROTECTED),
            Map.entry("DELETE /profile/{id}", PROTECTED),
            Map.entry("GET /event", PROTECTED),
            Map.entry("GET /event/{id}", PROTECTED),
            Map.entry("POST /event", PROTECTED),
            Map.entry("PATCH /event", PROTECTED),
            Map.entry("DELETE /event/{id}", PROTECTED)));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private DateParserService dateParserService;

    /** Tutti gli endpoint applicativi, come "METODO /pattern". */
    static Set<String> discoverEndpoints() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
                // lazy: servono solo i tipi dei controller, non le istanze
                ((AbstractBeanDefinition) definition).setLazyInit(true);
                context.registerBeanDefinition(definition.getBeanClassName(), definition);
            }
            context.refresh();
            RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
            mapping.setApplicationContext(context);
            mapping.afterPropertiesSet();

            Set<String> endpoints = new TreeSet<>();
            mapping.getHandlerMethods().keySet().forEach(info -> {
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                assertThat(methods).as("mapping senza metodo HTTP esplicito: %s", info).isNotEmpty();
                for (String pattern : info.getPatternValues()) {
                    methods.forEach(method -> endpoints.add(method.name() + " " + pattern));
                }
            });
            return endpoints;
        }
    }

    private static String concretePath(String pattern) {
        return pattern.replaceAll("\\{[^/]+}", "x");
    }

    @Test
    void everyEndpointIsClassified() {
        Set<String> discovered = discoverEndpoints();
        assertThat(discovered)
                .as("endpoint non classificati (aggiungerli a EXPECTED e, se pubblici, a SecurityConfiguration)")
                .isSubsetOf(EXPECTED.keySet());
        assertThat(EXPECTED.keySet())
                .as("endpoint classificati ma non piu' esistenti")
                .isSubsetOf(discovered);
    }

    void assertAnonymousAccessMatchesClassification() throws Exception {
        for (Map.Entry<String, Boolean> entry : EXPECTED.entrySet()) {
            String[] parts = entry.getKey().split(" ", 2);
            int status = mockMvc.perform(request(HttpMethod.valueOf(parts[0]), concretePath(parts[1])))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            if (entry.getValue()) {
                assertThat(status).as("anonimo su endpoint pubblico %s", entry.getKey()).isNotEqualTo(401);
            } else {
                assertThat(status).as("anonimo su endpoint protetto %s", entry.getKey()).isEqualTo(401);
            }
        }
    }

    void assertAnonymousIsRejected(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Nested
    @TestPropertySource(properties = {
        "harvester.security.basic.enabled=true",
        "harvester.security.oauth2.enabled=false"
    })
    class BasicOnly {

        @Test
        void anonymousAccessMatchesClassification() throws Exception {
            assertAnonymousAccessMatchesClassification();
        }

        @Test
        void harvesterCanReadDashboard() throws Exception {
            mockMvc.perform(get("/dashboard/aggregated-count-data").with(httpBasic("test-user", "test-password")))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/dashboard/unknown", "/jobs/unknown", "/unknown"})
        void anonymousIsRejectedOnUnmappedPaths(String path) throws Exception {
            assertAnonymousIsRejected(path);
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "harvester.security.basic.enabled=true",
        "harvester.security.oauth2.enabled=true",
        "harvester.security.oauth2.issuer-uri=http://localhost:0/realms/test"
    })
    class BasicAndOauth2 {

        @Test
        void anonymousAccessMatchesClassification() throws Exception {
            assertAnonymousAccessMatchesClassification();
        }

        @ParameterizedTest
        @ValueSource(strings = {"/dashboard/unknown", "/jobs/unknown", "/config/unknown", "/actuator/env", "/unknown"})
        void anonymousIsRejectedOnUnmappedPaths(String path) throws Exception {
            assertAnonymousIsRejected(path);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/error", "/v3/api-docs", "/swagger-ui/index.html", "/actuator/health/readiness"})
        void infrastructurePathsArePublic(String path) throws Exception {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as("anonimo su %s", path).isNotEqualTo(401);
        }

        @Test
        void anonymousCannotPatchJobs() throws Exception {
            mockMvc.perform(patch("/jobs/harvest")).andExpect(status().isUnauthorized());
        }

        @Test
        void viewerCannotPatchJobs() throws Exception {
            mockMvc.perform(patch("/jobs/harvest")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_NDC_VIEWER"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void viewerCanReadDashboard() throws Exception {
            mockMvc.perform(get("/dashboard/aggregated-count-data")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_NDC_VIEWER"))))
                    .andExpect(status().isOk());
        }

        @Test
        void serviceCannotReadDashboard() throws Exception {
            mockMvc.perform(get("/dashboard/aggregated-count-data")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_NDC_SERVICE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void harvesterCanReadDashboard() throws Exception {
            mockMvc.perform(get("/dashboard/aggregated-count-data").with(httpBasic("test-user", "test-password")))
                    .andExpect(status().isOk());
        }
    }
}
