package it.gov.innovazione.ndc.alerter.data.initializer;

import it.gov.innovazione.ndc.alerter.data.Initializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
public class InitializerConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataInitializer.class)
    public DataInitializer defaultInitializer(Collection<Initializer> initializers) {
        return new DefaultInitializer(initializers);
    }
}
