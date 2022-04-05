package com.gentics.mesh.plugin.export.exception;

import com.gentics.mesh.core.rest.error.AbstractRestException;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 * This exception will be thrown when a validation exception is encountered.
 */
public class ValidationException extends AbstractRestException {
    public ValidationException(Set<ConstraintViolation<Object>> violations) {
        super(HttpResponseStatus.BAD_REQUEST, "error_json_parse");
        this.violations = violations;
    }

    private Set<ConstraintViolation<Object>> violations;

    public Set<ConstraintViolation<Object>> getViolations() {
        return violations;
    }

    public void setViolations(Set<ConstraintViolation<Object>> violations) {
        this.violations = violations;
    }

    @Override
    public String getType() {
        return ValidationException.class.getSimpleName();
    }
}
