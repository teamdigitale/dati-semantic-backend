package it.gov.innovazione.ndc.harvester.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class OnceLogger {

    private final Set<String> categories = new HashSet<>();

    public void log(String category, Runnable logAction) {
        if (categories.add(category)) {
            logAction.run();
        }
    }

}
