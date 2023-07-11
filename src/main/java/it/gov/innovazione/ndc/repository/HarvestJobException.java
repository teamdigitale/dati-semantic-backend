package it.gov.innovazione.ndc.repository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class HarvestJobException extends RuntimeException {

    public HarvestJobException(String message) {
        super(message);
    }

    public HarvestJobException(Exception e) {
        super(e);
    }
}
