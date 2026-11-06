package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.plugin.export.validaton.CsvMappingConsistency;

import javax.validation.Valid;
import java.util.List;

@CsvMappingConsistency
public class CsvMappingDefinition extends AbstractCsvMappingDefinition {

    @JsonProperty
    @JsonPropertyDescription("The GraphQL query to use for loading references")
    private String referenceGraphQuery;

    @JsonProperty
    @JsonPropertyDescription("The Search query to use for filtering references")
    private String referenceSearchQuery;

    @JsonProperty
    @JsonPropertyDescription("The maximal count of elements that should be output")
    private Integer referenceMaximalNodes;

    @JsonProperty
    @JsonPropertyDescription("The path to the graph-query elements to be inserted")
    private String referenceElementsPath = "nodes.elements";

    @Valid
    @JsonProperty
    @JsonPropertyDescription("A sub mapping which is needed when the mapping type is `SUB_NODE` or `FOREIGN_REFERENCE`.")
    private List<CsvSubMappingDefinition> subMapping;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("A Filter that filters each sub-element")
    private Filter subFilter;

    public List<CsvSubMappingDefinition> getSubMapping() {
        return subMapping;
    }

    public CsvMappingDefinition setSubMapping(List<CsvSubMappingDefinition> subMapping) {
        this.subMapping = subMapping;
        return this;
    }

    public String getReferenceGraphQuery() {
        return referenceGraphQuery;
    }

    public CsvMappingDefinition setReferenceGraphQuery(String referenceGraphQuery) {
        this.referenceGraphQuery = referenceGraphQuery;
        return this;
    }

    public String getReferenceSearchQuery() {
        return referenceSearchQuery;
    }

    public CsvMappingDefinition setReferenceSearchQuery(String referenceSearchQuery) {
        this.referenceSearchQuery = referenceSearchQuery;
        return this;
    }

    public Integer getReferenceMaximalNodes() {
        if (this.referenceMaximalNodes == null) {
            return 9999;
        }

        return referenceMaximalNodes;
    }

    public CsvMappingDefinition setReferenceMaximalNodes(Integer referenceMaximalNodes) {
        this.referenceMaximalNodes = referenceMaximalNodes;
        return this;
    }

    public String getReferenceElementsPath() {
        return referenceElementsPath;
    }

    public CsvMappingDefinition setReferenceElementsPath(String referenceElementsPath) {
        this.referenceElementsPath = referenceElementsPath;
        return this;
    }

    public Filter getSubFilter() {
        return subFilter;
    }

    public void setSubFilter(Filter subFilter) {
        this.subFilter = subFilter;
    }
}
