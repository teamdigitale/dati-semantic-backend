package it.gov.innovazione.ndc.config;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Value("#{'${harvester.auth.user:admin}'}")
    private final String user;

    @Value("#{'${harvester.auth.password:password}'}")
    private final String password;

    @Bean
    @SneakyThrows
    SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(req ->
                        req.requestMatchers("/jobs/**", "/config/**").hasRole("HARVESTER")
                                .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username(user)
                        .password("{noop}" + password)
                        .roles("HARVESTER")
                        .build());
    }
}
