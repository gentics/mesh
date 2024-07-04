package com.gentics.mesh.plugin.export.handler;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.plugin.export.exception.ValidationException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.hibernate.validator.HibernateValidator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Handler which decodes entities form the request body into the model classes and validates them.
 */
public class DecodeValidateHandler<T> implements Handler<RoutingContext> {

    public static final String CONTEXT_PROPERTY_NAME = "export-plugin-decoded-validated";

    private static Logger log = LoggerFactory.getLogger(DecodeValidateHandler.class);

    private ValidatorFactory factory;
    private Class<T> decodeType;

    public DecodeValidateHandler(Class<T> decodeType) {
        factory = Validation
                .byProvider(HibernateValidator.class)
                .configure()
                .ignoreXmlConfiguration()
                .buildValidatorFactory();
        this.decodeType = decodeType;
    }

    @Override
    public void handle(RoutingContext rc) {
        try {
            T obj = Json.decodeValue(rc.getBodyAsString(), decodeType);
            Set<ConstraintViolation<Object>> violations = factory.getValidator().validate(obj);
            if (!violations.isEmpty()) {
                violations.forEach(violation -> log.error(violation.getMessage()));

                rc.fail(new ValidationException(violations));
                return;
            }

            rc.put(CONTEXT_PROPERTY_NAME, obj);
            rc.next();
        } catch (DecodeException e) {
            rc.fail(new GenericRestException(HttpResponseStatus.BAD_REQUEST, e.getMessage()));
        }
    }
}
