package it.gov.innovazione.ndc.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import it.gov.innovazione.ndc.harvester.service.ActualConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/config")
@Slf4j
public class ConfigurationController {

    private final ActualConfigService configService;

    @GetMapping("/metadata")
    @Operation(
            operationId = "getConfigMetadata",
            description = "Catalog of all configuration keys with type, scope, read-only flag and allowed values",
            summary = "Get configuration keys metadata")
    public List<ConfigKeyMetadata> getMetadata() {
        return Arrays.stream(ActualConfigService.ConfigKey.values())
                .map(ConfigKeyMetadata::of)
                .toList();
    }

    @GetMapping("/{repoId}")
    @Operation(
            operationId = "getConfig",
            description = "Get the configuration for a repository",
            summary = "Get the configuration for a repository")
    public Map<ActualConfigService.ConfigKey, ConfigService.ConfigEntry> getConfig(
            @PathVariable String repoId) {
        if (repoId.equals("ndc")) {
            return configService.getNdcConfiguration().getValue();
        }
        return configService.getRepoConfiguration(repoId).getValue();
    }

    @PostMapping("/{repoId}")
    @ResponseStatus(CREATED)
    @Operation(
            operationId = "setConfig",
            description = "Set the configuration for a repository",
            summary = "Set the configuration for a repository")
    public void setConfig(
            @PathVariable String repoId,
            @RequestBody Map<ActualConfigService.ConfigKey, Object> config,
            Principal principal) {
        config.keySet().forEach(ConfigurationController::assertWritable);
        if (repoId.equals("ndc")) {
            configService.setConfig(config, principal.getName());
            return;
        }
        configService.setConfig(config, principal.getName(), repoId);
    }

    @PutMapping("/{repoId}/{configKey}")
    @ResponseStatus(ACCEPTED)
    @Operation(
            operationId = "updateRepository",
            description = "Update a configuration key for a repository",
            summary = "Update a configuration key for a repository")
    public void updateRepository(
            @PathVariable String repoId,
            @PathVariable ActualConfigService.ConfigKey configKey,
            @RequestParam String value,
            Principal principal) {
        assertWritable(configKey);
        if (repoId.equals("ndc")) {
            configService.writeConfigKey(configKey, principal.getName(), value);
            return;
        }
        configService.writeConfigKey(configKey, principal.getName(), value, repoId);
    }

    @DeleteMapping("/{repoId}/{configKey}")
    @ResponseStatus(ACCEPTED)
    @Operation(
            operationId = "deleteRepository",
            description = "Delete a configuration key for a repository",
            summary = "Delete a configuration key for a repository")
    public void deleteRepository(
            @PathVariable String repoId,
            @PathVariable ActualConfigService.ConfigKey configKey,
            Principal principal) {
        assertWritable(configKey);
        if (repoId.equals("ndc")) {
            configService.removeConfigKey(configKey, principal.getName());
            return;
        }
        configService.removeConfigKey(configKey, principal.getName(), repoId);
    }

    private static void assertWritable(ActualConfigService.ConfigKey key) {
        if (key.isReadOnly()) {
            // 409 Conflict: la chiave esiste ma non e' scrivibile per natura.
            // Non e' un problema di permessi (sarebbe 403): la chiave e'
            // gestita programmaticamente dall'harvester (es. ACTIVE_INSTANCE
            // via DefaultInstanceManager.switchInstances) ed e' read-only per
            // tutti i chiamanti.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configuration key " + key + " is read-only");
        }
    }

    /**
     * Metadata di una ConfigKey esposto al FE: una unica source-of-truth per
     * tipo, scope di applicabilita', flag read-only ed eventuali allowedValues.
     */
    public record ConfigKeyMetadata(
            String name,
            String description,
            ActualConfigService.Type type,
            boolean readOnly,
            List<ActualConfigService.Scope> scopes,
            List<String> allowedValues) {

        static ConfigKeyMetadata of(ActualConfigService.ConfigKey key) {
            return new ConfigKeyMetadata(
                    key.name(),
                    key.getDescription(),
                    key.getType(),
                    key.isReadOnly(),
                    key.getScopes().stream().toList(),
                    key.getAllowedValues());
        }
    }
}
