package com.gentics.mesh.plugin.search.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.plugin.search.HandlerType;

import java.util.Objects;

public class CustomQueryOptions {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The name of this custom query. Used as filename for the query, and route for the handler")
	private String name;

	@JsonProperty(defaultValue = "SEARCH")
	@JsonPropertyDescription("The type of the query (either SEARCH or AUTOCOMPLETE)")
	private HandlerType type;

	@JsonProperty(defaultValue = "null")
	@JsonPropertyDescription("The GraphQL query to use to load additional data about hits")
	private String resultQuery;

	@JsonProperty(defaultValue = "0")
	@JsonPropertyDescription("How many search hits should be returned per page. (0 means the general pageSize configuration should be used")
	private int pageSize;

	@JsonCreator
	public CustomQueryOptions(String name) {
		this(name, HandlerType.SEARCH, null, 0);
	}

	@JsonCreator
	public CustomQueryOptions(
			@JsonProperty("name") String name,
			@JsonProperty("type") HandlerType type,
			@JsonProperty("resultQuery") String resultQuery,
			@JsonProperty("pageSize") int pageSize) {
		this.name = name;
		this.type = type;
		this.resultQuery = resultQuery;
		this.pageSize = pageSize;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HandlerType getType() {
		return type;
	}

	public void setType(HandlerType type) {
		this.type = type;
	}

	public String getResultQuery() {
		return resultQuery;
	}

	public void setResultQuery(String resultQuery) {
		this.resultQuery = resultQuery;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other == null || getClass() != other.getClass()) {
			return false;
		}

		CustomQueryOptions otherOptions = (CustomQueryOptions) other;

		return name.equals(otherOptions.name) &&
			type == otherOptions.type &&
			Objects.equals(resultQuery, otherOptions.resultQuery);
	}
}
