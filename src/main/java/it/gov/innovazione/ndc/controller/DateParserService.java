package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.controller.date.DateParameter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DateParserService {

    public DateParameter parseDateParams(String date, String startDate, String endDate, DateParameter.Granularity granularity) {
        validateDateParams(date, startDate, endDate, granularity);
        return DateParameter.of(date, startDate, endDate, granularity);
    }

    private void validateDateParams(String date, String startDate, String endDate, DateParameter.Granularity granularity) {
        if (date != null && (startDate != null || endDate != null)) {
            validateDate(date, "date", granularity);
            throw new IllegalArgumentException("Only one of date or startDate/endDate must be provided");
        }
        if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
            DateParameter.DateFormat startDateFormat = validateDate(startDate, "startDate", granularity);
            DateParameter.DateFormat endDateFormat = validateDate(endDate, "endDate", granularity);
            if (startDateFormat != endDateFormat) {
                throw new IllegalArgumentException("startDate and endDate must have the same format");
            }
            return;
        }
        if (Objects.nonNull(startDate)) {
            validateDate(startDate, "startDate", granularity);
        }
        if (Objects.nonNull(endDate)) {
            validateDate(endDate, "endDate", granularity);
        }
    }

    private DateParameter.DateFormat validateDate(String date, String fieldName, DateParameter.Granularity granularity) {
        DateParameter.DateFormat detectedFormat = DateParameter.DateFormat.from(date)
                .orElseThrow(() -> new IllegalArgumentException("Invalid format for '" + fieldName + "' parameter, please use one of the following: " + DateParameter.DateFormat.getSupportedFormats()));

        if (detectedFormat.getUnsupportedGranularities().contains(granularity)) {
            throw new IllegalArgumentException("Invalid granularity for format " + detectedFormat.getFormat() + " for '" + fieldName + "' parameter");
        }

        return detectedFormat;
    }
}
