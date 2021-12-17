package it.teamdigitale.ndc.dto;

import it.teamdigitale.ndc.gen.dto.Theme;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ThemeConverter implements Converter<String, Theme> {
    @Override
    public Theme convert(String source) {
        return Theme.fromValue(source);
    }
}