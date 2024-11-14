package com.gentics.mesh.core.rest.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Batch request
 * 
 * @param <T>
 */
public class ListRequest<T> implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Array of the requests.")
	private List<T> data = new ArrayList<>();

	/**
	 * Empty ctor.
	 */
	public ListRequest() {		
	}

	/**
	 * Return the list data.
	 * 
	 * @return List data
	 */
	public List<T> getData() {
		return data;
	}

	/**
	 * Add the given element to the list.
	 * 
	 * @param e
	 */
	public void add(T e) {
		data.add(e);
	}
}
