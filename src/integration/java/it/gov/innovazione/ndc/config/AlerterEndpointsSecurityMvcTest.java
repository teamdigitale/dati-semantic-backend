package it.gov.innovazione.ndc.config;

import it.gov.innovazione.ndc.alerter.controller.EventController;
import it.gov.innovazione.ndc.alerter.controller.ProfileController;
import it.gov.innovazione.ndc.alerter.controller.UserController;
import it.gov.innovazione.ndc.alerter.data.EventMapper;
import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.data.ProfileMapper;
import it.gov.innovazione.ndc.alerter.data.ProfileService;
import it.gov.innovazione.ndc.alerter.data.UserMapper;
import it.gov.innovazione.ndc.alerter.data.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, ProfileController.class, EventController.class})
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = {
    "harvester.auth.user=test-user",
    "harvester.auth.password=test-password"
})
class AlerterEndpointsSecurityMvcTest {

    private static final String BODY = "{\"name\":\"x\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private ProfileService profileService;
    @MockitoBean
    private ProfileMapper profileMapper;
    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private EventMapper eventMapper;

    @BeforeEach
    void setUp() {
        when(userService.getPaginated(any())).thenReturn(emptyPage());
        when(profileService.getPaginated(any())).thenReturn(emptyPage());
        when(eventService.getPaginated(any())).thenReturn(emptyPage());
    }

    private static <T> Page<T> emptyPage() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    }

    void assertAnonymousIsRejected(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(path + "/1")).andExpect(status().isUnauthorized());
    }

    @Nested
    @TestPropertySource(properties = {
        "harvester.security.basic.enabled=true",
        "harvester.security.oauth2.enabled=false"
    })
    class BasicOnly {

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void anonymousIsRejected(String path) throws Exception {
            assertAnonymousIsRejected(path);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void wrongCredentialsAreRejected(String path) throws Exception {
            mockMvc.perform(get(path).with(httpBasic("test-user", "wrong")))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void harvesterCanRead(String path) throws Exception {
            mockMvc.perform(get(path).with(httpBasic("test-user", "test-password")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "harvester.security.basic.enabled=true",
        "harvester.security.oauth2.enabled=true",
        "harvester.security.oauth2.issuer-uri=http://localhost:0/realms/test"
    })
    class BasicAndOauth2 {

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void anonymousIsRejected(String path) throws Exception {
            assertAnonymousIsRejected(path);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void viewerIsForbidden(String path) throws Exception {
            mockMvc.perform(get(path).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_NDC_VIEWER"))))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void serviceIsForbidden(String path) throws Exception {
            mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_NDC_SERVICE"))))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void adminCanRead(String path) throws Exception {
            mockMvc.perform(get(path).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_NDC_ADMIN"))))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/user", "/profile", "/event"})
        void harvesterCanRead(String path) throws Exception {
            mockMvc.perform(get(path).with(httpBasic("test-user", "test-password")))
                    .andExpect(status().isOk());
        }
    }
}
