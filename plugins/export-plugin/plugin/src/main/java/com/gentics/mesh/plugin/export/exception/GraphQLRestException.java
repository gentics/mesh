package com.gentics.mesh.plugin.export.exception;

import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLError;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Set;

public class GraphQLRestException extends AbstractRestException {
    private Set<GraphQLError> errors;

    public GraphQLRestException(Set<GraphQLError> errors) {
        super(HttpResponseStatus.BAD_REQUEST, "error_json_parse");
        this.errors = errors;
    }

    public Set<GraphQLError> getErrors() {
        return errors;
    }

    public void setErrors(Set<GraphQLError> errors) {
        this.errors = errors;
    }

    @Override
    public String getType() {
        return GraphQLRestException.class.getSimpleName();
    }
}
