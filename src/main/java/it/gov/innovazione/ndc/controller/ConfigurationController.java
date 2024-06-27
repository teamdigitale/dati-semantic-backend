package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.AlertableEvent;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/config/{repoId}")
@Slf4j
public class ConfigurationController {

    private final ActualConfigService configService;
    private final NdcEventPublisher eventPublisher;

    @GetMapping
    public Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry> getConfig(
            @PathVariable String repoId) {
        if (repoId.equals("ndc")) {
            return configService.getNdcConfiguration().getValue();
        }
        return configService.getRepoConfiguration(repoId).getValue();
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public void setConfig(
            @PathVariable String repoId,
            @RequestBody Map<ActualConfigService.ConfigKey, Object> config,
            Principal principal) {
        if (repoId.equals("ndc")) {
            configService.setConfig(config, principal.getName());
            return;
        }
        configService.setConfig(config, principal.getName(), repoId);
        eventPublisher.publishAlertableEvent("Configuration", WebConfigAlertableEvent.builder()
                .repoId(repoId)
                .config(config)
                .build());
    }

    @PutMapping("/{configKey}")
    @ResponseStatus(ACCEPTED)
    public void updateRepository(
            @PathVariable String repoId,
            @PathVariable ActualConfigService.ConfigKey configKey,
            @RequestParam String value,
            Principal principal) {
        if (repoId.equals("ndc")) {
            configService.writeConfigKey(configKey, principal.getName(), value);
            return;
        }
        configService.writeConfigKey(configKey, principal.getName(), value, repoId);
        eventPublisher.publishAlertableEvent("Configuration",
                WebConfigAlertableEvent.builder()
                        .repoId(repoId)
                        .config(Map.of(configKey, value))
                        .build());
    }

    @DeleteMapping("/{configKey}")
    @ResponseStatus(ACCEPTED)
    public void deleteRepository(
            @PathVariable String repoId,
            @PathVariable ActualConfigService.ConfigKey configKey,
            Principal principal) {
        if (repoId.equals("ndc")) {
            configService.removeConfigKey(configKey, principal.getName());
            return;
        }
        configService.removeConfigKey(configKey, principal.getName(), repoId);
        eventPublisher.publishAlertableEvent("Configuration",
                WebConfigAlertableEvent.builder()
                        .repoId(repoId)
                        .config(Map.of(configKey, "removed"))
                        .build());
    }

    @Builder
    @RequiredArgsConstructor
    private static class WebConfigAlertableEvent implements AlertableEvent {
        private final String repoId;
        private final Map<ActualConfigService.ConfigKey, Object> config;

        @Override
        public String getName() {
            return "Configuration updated";
        }

        @Override
        public String getDescription() {
            return "Configuration updated for repository " + repoId;
        }

        @Override
        public EventCategory getCategory() {
            return EventCategory.APPLICATION;
        }

        @Override
        public Severity getSeverity() {
            return Severity.INFO;
        }

        @Override
        public Map<String, Object> getContext() {
            return config.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        }
    }
}
