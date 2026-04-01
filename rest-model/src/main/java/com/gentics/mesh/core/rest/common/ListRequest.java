package com.gentics.mesh.core.rest.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Rest model POJO for list requests.
 * 
 * @param <T>
 */
public class ListRequest<T> implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Array which contains the elements.")
	private List<T> data = new ArrayList<>();

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
	 * @return 
	 */
	public ListRequest<T> add(T e) {
		data.add(e);
		return this;
	}
}
