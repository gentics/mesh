package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import io.vertx.core.json.JsonObject;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The model for the csv request download
 */
public class CsvRequest implements RestModel {

    @NotNull
    @Valid
    @JsonProperty(required = true)
    @JsonPropertyDescription("Variables which are forwarded to the GraphQL query")
    private CsvRequestGraphQLVariables variables;

    @NotEmpty
    @JsonProperty(required = true)
    @JsonPropertyDescription("A GraphQL query.")
    private String query;

    @NotEmpty
    @Valid
    @JsonProperty(required = true)
    @JsonPropertyDescription("The mapping for the csv export.")
    private List<CsvMappingDefinition> mapping;

    public CsvRequest() {
    }

    public List<CsvMappingDefinition> getMapping() {
        return mapping;
    }

    public void setMapping(List<CsvMappingDefinition> mapping) {
        this.mapping = mapping;
    }

    public CsvRequestGraphQLVariables getVariables() {
        return variables;
    }

    public void setVariables(CsvRequestGraphQLVariables variables) {
        this.variables = variables;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public GraphQLRequest buildGraphQLRequest() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(this.getQuery());
        request.setVariables(JsonObject.mapFrom(this.getVariables()));
        return request;
    }
}

