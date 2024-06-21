package it.gov.innovazione.ndc.alerter.data.initializer;

import it.gov.innovazione.ndc.alerter.data.Initializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;

import java.util.Collection;

@RequiredArgsConstructor
public class DefaultInitializer implements DataInitializer {

    private final Collection<Initializer> initializers;

    @Override
    @EventListener(ApplicationStartedEvent.class)
    public void initData() {
        initializers.forEach(Initializer::init);
    }
}
