package com.gentics.mesh.core.rest.graphql;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class GraphQLError implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The error message.")
	private String message;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Type of the error.")
	private String type;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Mesh element id which is related to the error.")
	private String elementId;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Mesh element type which is related to the error.")
	private String elementType;

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of locations which are related to the error.")
	private List<ErrorLocation> locations;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path of the error.")
	private String path;

	/**
	 * Return the error message.
	 * 
	 * @return
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the error message.
	 * 
	 * @param message
	 * @return Fluent API
	 */
	public GraphQLError setMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Return the type of the error.
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of the error.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	public GraphQLError setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Return the id of the element which is related to the error.
	 * 
	 * @return
	 */
	public String getElementId() {
		return elementId;
	}

	/**
	 * Set the id of the element which is related to the error.
	 * 
	 * @param elementId
	 * @return Fluent API
	 */
	public GraphQLError setElementId(String elementId) {
		this.elementId = elementId;
		return this;
	}

	/**
	 * Return the type of the element which is related to the error.
	 * 
	 * @return
	 */
	public String getElementType() {
		return elementType;
	}

	/**
	 * Set the type of the element which is related to the error.
	 * 
	 * @param elementType
	 * @return Fluent API
	 */
	public GraphQLError setElementType(String elementType) {
		this.elementType = elementType;
		return this;
	}

	/**
	 * Return the list of error locations.
	 * 
	 * @return
	 */
	public List<ErrorLocation> getLocations() {
		return locations;
	}

	/**
	 * Set the list of error locations.
	 * 
	 * @param locations
	 * @return
	 */
	public GraphQLError setLocations(List<ErrorLocation> locations) {
		this.locations = locations;
		return this;
	}

	/**
	 * Return the path of the error.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path of the error.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public GraphQLError setPath(String path) {
		this.path = path;
		return this;
	}


}
