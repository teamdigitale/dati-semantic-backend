package it.teamdigitale.ndc.harvester.csv;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FirstColumnExtractor implements HeadersToIdNameExtractor {
    @Override
    public String extract(List<String> headerNames) {
        return headerNames.get(0);
    }
}
