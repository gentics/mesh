package com.gentics.mesh.core.rest.graphql;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST model for location information (within the JSON) of a graphql error.
 */
public class ErrorLocation implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Error line number.")
	private int line;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Error column number.")
	private int column;

	/**
	 * Return the line number on which the error occurred.
	 * 
	 * @return
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Set the line number on which the error occurred.
	 * 
	 * @param line
	 * @return Fluent API
	 */
	public ErrorLocation setLine(int line) {
		this.line = line;
		return this;
	}

	/**
	 * Return the column number on which the error occurred.
	 * 
	 * @return
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Set the column number on which the error occurred.
	 * 
	 * @param column
	 * @return Fluent API
	 */
	public ErrorLocation setColumn(int column) {
		this.column = column;
		return this;
	}
}
