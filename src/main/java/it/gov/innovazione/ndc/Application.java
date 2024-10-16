package it.gov.innovazione.ndc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Schema - Semantic Backend",
                version = "0.0.1",
                description = """
                        This API exposes information from Schema, the National Data Catalog for Semantic Interoperability including the REST API
                        for accessing controlled vocabularies.
                        
                        
                        It is used as a backend service for the [schema.gov.it](https://schema.gov.it) website and to expose functionalities in 
                        an interoperable way.
                        
                        
                        Provided information can be used to discover ontologies, controlled vocabularies and schemas indexed by Schema, and to 
                        ease the creation of semantically interoperable digital services such as web forms and APIs.
                        
                        
                        **This API is a beta release, and it can change in the future during the consolidation phase of Schema.**""",
                contact = @Contact(
                        name = "Dipartimento per la Trasformazione digitale",
                        email = "info@teamdigitale.governo.it"),
                license = @License(
                        name = " BSD-3-Clause",
                        url = "https://opensource.org/licenses/BSD-3-Clause"),
                summary = "Expose ontology, controlled vocabularies and schemas information from Schema."


        ))
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

}
