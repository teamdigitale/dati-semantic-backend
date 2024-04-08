package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import it.gov.innovazione.ndc.harvester.service.ConfigService;
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

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/config/ndc")
@Slf4j
public class ConfigurationController {

    private final ActualConfigService configService;

    @GetMapping
    public Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry> getConfig() {
        return configService.getNdcConfiguration().getValue();
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public void setConfig(
            @RequestBody Map<ActualConfigService.ConfigKey, Object> config,
            Principal principal) {
        configService.setNdConfig(config, principal.getName());
    }

    @PutMapping("/{configKey}")
    @ResponseStatus(ACCEPTED)
    public void updateRepository(
            @PathVariable ActualConfigService.ConfigKey configKey,
            @RequestParam String value,
            Principal principal) {
        configService.writeConfigKey(configKey, principal.getName(), value);
    }

    @DeleteMapping("/{configKey}")
    @ResponseStatus(ACCEPTED)
    public void deleteRepository(
            @PathVariable ActualConfigService.ConfigKey configKey,
            Principal principal) {
        configService.removeConfigKey(configKey, principal.getName());
    }
}
