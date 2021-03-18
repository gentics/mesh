package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import io.vertx.core.json.JsonObject;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

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

    @JsonProperty(required = true)
    @JsonPropertyDescription("The path in the request that leads to the elements which should be handled.")
    private String elementsPath = "nodes.elements";

    @JsonProperty(required = true)
    @JsonPropertyDescription("The path in the request which determines if there's more pages.")
    private String hasMorePath = "nodes.hasNextPage";

    @JsonProperty
    @JsonPropertyDescription("The maximal amount of Nodes (Not rows!) the export should contain")
    private Integer maximalNodes = 0;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Filter to use for each element")
    public Filter elementFilter;

    @JsonIgnore
    private String projectName;

    @JsonIgnore
    private Map<String, Map<String, String>> configValues;

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

    public String getElementsPath() {
        return elementsPath;
    }

    public CsvRequest setElementsPath(String elementsPath) {
        this.elementsPath = elementsPath;
        return this;
    }

    public String getHasMorePath() {
        return hasMorePath;
    }

    public CsvRequest setHasMorePath(String hasMorePath) {
        this.hasMorePath = hasMorePath;
        return this;
    }

    public Integer getMaximalNodes() {
        return maximalNodes;
    }

    public CsvRequest setMaximalNodes(Integer maximalNodes) {
        this.maximalNodes = maximalNodes;
        return this;
    }

    public String getProjectName() {
        return projectName;
    }

    public CsvRequest setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public Map<String, Map<String, String>> getConfigValues() {
        return configValues;
    }

    public CsvRequest setConfigValues(Map<String, Map<String, String>> configValues) {
        this.configValues = configValues;
        return this;
    }

    public Filter getElementFilter() {
        return elementFilter;
    }

    public CsvRequest setElementFilter(Filter elementFilter) {
        this.elementFilter = elementFilter;
        return this;
    }

    public GraphQLRequest buildGraphQLRequest() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(this.getQuery());
        request.setVariables(JsonObject.mapFrom(this.getVariables()));
        return request;
    }
}

