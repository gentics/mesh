package com.gentics.mesh.plugin.export.exception;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;

public class ElasticSearchException extends AbstractRestException {
    private JsonObject errorDetails;

    public ElasticSearchException(HttpErrorException original) {
        super(HttpResponseStatus.valueOf(original.getStatusCode()), "search_error_query", original);
        this.errorDetails = new JsonObject(original.getBody());
    }

    public JsonObject getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(JsonObject errorDetails) {
        this.errorDetails = errorDetails;
    }

    @Override
    public String getType() {
        return ElasticSearchException.class.getSimpleName();
    }
}
