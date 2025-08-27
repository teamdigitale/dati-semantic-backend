package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.AlerterService;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class MapUtils {
    public static String defaultIfNull(String createdBy) {
        if (StringUtils.isEmpty(createdBy)) {
            return AlerterService.getUser();
        }
        return createdBy;
    }
}
