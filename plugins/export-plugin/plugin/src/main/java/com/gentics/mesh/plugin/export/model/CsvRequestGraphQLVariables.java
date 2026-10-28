package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

public class CsvRequestGraphQLVariables {

    @JsonPropertyDescription("The page at which to start the query, defaults to `1`")
    private Long page = 1L;

    @JsonPropertyDescription("The number of nodes to fetch per request (internally). Defaults to `25`")
    private Integer perPage = 25;

    @NotEmpty
    @JsonProperty(required = true)
    @JsonPropertyDescription("The elastic search query which is used to fetch the objects.")
    private String query;

    @JsonPropertyDescription("The language to use when fetching the nodes")
    private List<String> lang;

    public CsvRequestGraphQLVariables() {
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getLang() {
        return lang;
    }

    public void setLang(List<String> lang) {
        this.lang = lang;
    }
}
