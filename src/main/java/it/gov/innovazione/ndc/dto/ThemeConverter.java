package it.gov.innovazione.ndc.dto;

import it.gov.innovazione.ndc.gen.dto.Theme;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ThemeConverter implements Converter<String, Theme> {
    @Override
    public Theme convert(String source) {
        return Theme.fromValue(source);
    }
}