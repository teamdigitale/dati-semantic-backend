package it.gov.innovazione.ndc.alerter.data;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@ConditionalOnMissingBean(DataInitializer.class)
public class DefaultInitializer implements DataInitializer {

    private final Collection<Initializer> initializers;

    @Override
    @EventListener(ApplicationStartedEvent.class)
    public void initData() {
        initializers.forEach(Initializer::init);
    }
}
